package com.example.service;

import com.example.common.UploadUtil;
import com.example.entity.YcAiModelWork;
import com.example.entity.YcUserAccount;
import com.example.exception.CustomException;
import com.example.mapper.YcCollectibleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class TencentAi3dService {
    private static final Logger log = LoggerFactory.getLogger(TencentAi3dService.class);
    private static final String DEFAULT_COVER_URL = "/青花梅瓶.webp";
    private static final long REMOTE_MODEL_MAX_SIZE = 100L * 1024 * 1024;
    private static final long REMOTE_COVER_MAX_SIZE = 10L * 1024 * 1024;
    private static final int STALE_PERSIST_LIMIT = 100;
    private static final String HOST = "ai3d.tencentcloudapi.com";
    private static final String ENDPOINT = "https://" + HOST;
    private static final String SERVICE = "ai3d";
    private static final String VERSION = "2025-05-13";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpClient remoteFileClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private YcCollectibleMapper ycCollectibleMapper;

    @Autowired
    private YcPointsService ycPointsService;

    @Value("${ai.tencent.ai3d.secret-id:}")
    private String secretId;

    @Value("${ai.tencent.ai3d.secret-key:}")
    private String secretKey;

    @Value("${ai.tencent.ai3d.region:ap-guangzhou}")
    private String region;

    public Map<String, Object> submitRapidJob(String prompt, String style, String vessel, MultipartFile image) throws Exception {
        ensureConfigured();

        Map<String, Object> request = new LinkedHashMap<>();
        if (image != null && !image.isEmpty()) {
            request.put("ImageBase64", Base64.getEncoder().encodeToString(image.getBytes()));
        } else if (StringUtils.hasText(prompt)) {
            request.put("Prompt", clipPrompt(prompt, style, vessel));
        } else {
            throw new CustomException("400", "请先填写创作描述或上传参考图片");
        }
        request.put("ResultFormat", "GLB");
        request.put("EnablePBR", true);

        Map<String, Object> response = callTencent("SubmitHunyuanTo3DRapidJob", request);
        String jobId = String.valueOf(response.get("JobId"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", jobId);
        data.put("status", "pending");
        data.put("message", "任务已提交，正在生成 3D 模型");
        return data;
    }

    public Map<String, Object> createModelTask(YcUserAccount user,
                                                String prompt,
                                                String style,
                                                String vessel,
                                                MultipartFile image) throws Exception {
        if (user == null || user.getId() == null) {
            throw new CustomException("401", "请先登录后再生成作品");
        }
        if (ycCollectibleMapper.selectActiveAiModelTask(user.getId()) != null) {
            throw new CustomException("409", "已有作品正在生成，请等待当前任务完成");
        }

        boolean charged = ycPointsService.spendForAi3d(user);
        boolean taskRecorded = false;
        try {
            Map<String, Object> submitted = submitRapidJob(prompt, style, vessel, image);
            String taskId = String.valueOf(submitted.get("taskId"));
            if (!StringUtils.hasText(taskId) || "null".equalsIgnoreCase(taskId)) {
                throw new IllegalStateException("生成任务创建失败");
            }

            YcAiModelWork work = new YcAiModelWork();
            work.setUserId(user.getId());
            work.setTaskId(taskId);
            work.setWorkCode(createWorkCode());
            work.setTitle(buildWorkTitle(style, vessel));
            work.setPrompt(prompt);
            work.setStyle(style);
            work.setVessel(vessel);
            work.setModelFormat("glb");
            work.setGenerationStatus("SUBMITTED");
            work.setStorageStatus("NONE");
            work.setGenerationPointsCost(ycPointsService.getAi3dCostPoints());
            work.setPersistPointsCost(ycPointsService.getAi3dPersistCostPoints());
            work.setGenerationChargeStatus(charged ? "CHARGED" : "FREE");
            work.setPersistChargeStatus("NONE");
            work.setStatus(1);
            ycCollectibleMapper.insertAiModelWork(work);
            taskRecorded = true;

            YcAiModelWork saved = ycCollectibleMapper.selectAiModelWorkByTaskId(taskId, user.getId());
            Map<String, Object> result = toWorkView(saved == null ? work : saved);
            result.put("points", ycPointsService.getSummary(user));
            result.put("message", "作品已进入生成队列，预计 5 分钟后可查看结果");
            return result;
        } catch (Exception e) {
            if (AiModelWorkPolicy.shouldRefundGeneration(taskRecorded)) {
                ycPointsService.refundAi3d(user, charged);
            }
            throw e;
        }
    }

    public Map<String, Object> queryRapidJob(String jobId) throws Exception {
        ensureConfigured();
        if (!StringUtils.hasText(jobId)) {
            throw new IllegalArgumentException("缺少任务 ID");
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("JobId", jobId);
        Map<String, Object> response = callTencent("QueryHunyuanTo3DRapidJob", request);

        String remoteStatus = String.valueOf(response.getOrDefault("Status", ""));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", jobId);
        data.put("remoteStatus", remoteStatus);
        data.put("status", toLocalStatus(remoteStatus));

        if (StringUtils.hasText((String) response.get("ErrorMessage"))) {
            data.put("message", response.get("ErrorMessage"));
        }

        Object files = response.get("ResultFile3Ds");
        if (files instanceof List) {
            Map<String, Object> file = selectGlbFile((List<?>) files);
            if (file != null) {
                String modelUrl = String.valueOf(file.get("Url"));
                String coverUrl = trimToNull(String.valueOf(file.get("PreviewImageUrl")));
                data.put("modelUrl", modelUrl);
                data.put("downloadUrl", modelUrl);
                data.put("modelFormat", "glb");
                data.put("coverUrl", coverUrl);
                data.put("coverWebpUrl", toWebpPreviewUrl(coverUrl));
                data.put("title", "AI 陶瓷 3D 作品");
            }
        }
        return data;
    }

    static Map<String, Object> selectGlbFile(List<?> files) {
        if (files == null) {
            return null;
        }
        for (Object item : files) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> file = (Map<?, ?>) item;
            Object typeValue = file.get("Type");
            Object urlValue = file.get("Url");
            String type = typeValue == null ? "" : String.valueOf(typeValue);
            String url = urlValue == null ? "" : String.valueOf(urlValue);
            if ("GLB".equalsIgnoreCase(type) || url.toLowerCase(Locale.ROOT).contains(".glb")) {
                Map<String, Object> result = new LinkedHashMap<>();
                file.forEach((key, value) -> result.put(String.valueOf(key), value));
                return result;
            }
        }
        return null;
    }

    static void requireUpdated(int affectedRows) {
        if (affectedRows <= 0) {
            throw new IllegalStateException("作品状态更新失败");
        }
    }

    public Map<String, Object> queryModelTask(Long userId, String taskId) throws Exception {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        YcAiModelWork work = ycCollectibleMapper.selectAiModelWorkByTaskId(taskId, userId);
        if (work == null) {
            throw new CustomException("404", "生成任务不存在");
        }
        if ("READY".equals(work.getGenerationStatus()) || "FAILED".equals(work.getGenerationStatus())) {
            return toWorkView(work);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!AiModelWorkPolicy.canQuery(work.getCreatedAt(), now)) {
            Map<String, Object> waiting = toWorkView(work);
            waiting.put("message", "作品正在生成，预计 5 分钟后查看结果");
            return waiting;
        }

        Map<String, Object> remote = queryRapidJob(taskId);
        String status = String.valueOf(remote.getOrDefault("status", "pending"));
        if ("ready".equals(status)) {
            String modelUrl = trimToNull(String.valueOf(remote.get("modelUrl")));
            if (modelUrl == null || "null".equalsIgnoreCase(modelUrl)) {
                ycPointsService.refundFailedAi3dGeneration(userById(userId), work, "生成结果缺少 GLB 文件");
            } else {
                LocalDateTime generatedAt = LocalDateTime.now();
                requireUpdated(ycCollectibleMapper.completeAiModelGeneration(
                        work.getId(),
                        userId,
                        modelUrl,
                        trimToNull(String.valueOf(remote.get("coverUrl"))),
                        "glb",
                        generatedAt,
                        generatedAt.plus(AiModelWorkPolicy.TEMPORARY_LIFETIME)
                ));
            }
        } else if ("failed".equals(status)) {
            ycPointsService.refundFailedAi3dGeneration(userById(userId), work, "作品生成失败");
        } else {
            ycCollectibleMapper.updateAiModelGenerationRunning(work.getId(), userId);
        }
        YcAiModelWork refreshed = ycCollectibleMapper.selectAiModelWorkByTaskId(taskId, userId);
        return toWorkView(refreshed == null ? work : refreshed);
    }

    public List<Map<String, Object>> listUserModelWorks(Long userId, String scope, Integer limit) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        String normalizedScope = "permanent".equalsIgnoreCase(scope)
                ? "permanent"
                : ("temporary".equalsIgnoreCase(scope) ? "temporary" : "all");
        int size = limit == null ? 20 : Math.max(1, Math.min(limit, 50));
        List<YcAiModelWork> works = ycCollectibleMapper.selectUserAiModelWorks(userId, normalizedScope, size);
        return works == null ? List.of() : works.stream().map(this::toWorkView).toList();
    }

    public Map<String, Object> getModelWorkById(Long id, Long requesterId) {
        if (id == null) {
            throw new IllegalArgumentException("作品ID不能为空");
        }
        YcAiModelWork work = requesterId == null ? null : ycCollectibleMapper.selectAiModelWorkById(id, requesterId);
        if (work == null) {
            work = ycCollectibleMapper.selectPermanentAiModelWorkById(id);
        }
        if (work == null) {
            throw new CustomException("404", "作品不存在或暂不可查看");
        }
        return toWorkView(work);
    }

    public Map<String, Object> getModelSession(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        YcAiModelWork activeTask = ycCollectibleMapper.selectActiveAiModelTask(userId);
        YcAiModelWork latestPreview = ycCollectibleMapper.selectLatestAiModelPreview(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeTask", activeTask == null ? null : toWorkView(activeTask));
        result.put("latestPreview", latestPreview == null ? null : toWorkView(latestPreview));
        return result;
    }

    public Map<String, Object> persistModelWork(YcUserAccount user, String taskId) throws Exception {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (!StringUtils.hasText(taskId)) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        YcAiModelWork work = ycCollectibleMapper.selectAiModelWorkByTaskId(taskId, user.getId());
        if (work == null) {
            throw new CustomException("404", "作品不存在");
        }
        if ("PERMANENT".equals(work.getStorageStatus())) {
            return toWorkView(work);
        }
        if (!"READY".equals(work.getGenerationStatus())
                || !AiModelWorkPolicy.canStartPersist(work.getStorageStatus(), work.getPersistChargeStatus())
                || !AiModelWorkPolicy.canPersistUntil(work.getExpiresAt(), LocalDateTime.now())) {
            throw new CustomException("409", "当前作品暂不可永久保存，请刷新后重试");
        }

        boolean started = false;
        boolean charged = false;
        boolean completed = false;
        try {
            charged = ycPointsService.startAi3dPersist(user, work);
            started = true;
            byte[] bytes = downloadTrustedTencentAsset(work.getModelUrl(), REMOTE_MODEL_MAX_SIZE);
            if (bytes == null || bytes.length == 0) {
                throw new IOException("模型文件为空");
            }
            String savedUrl = UploadUtil.uploadAi3dModelBytes(bytes, user.getId(), taskId);
            String savedCoverUrl = persistCover(work, user.getId(), taskId);
            requireUpdated(ycCollectibleMapper.completeAiModelPersist(
                    work.getId(),
                    user.getId(),
                    savedUrl,
                    savedCoverUrl,
                    (long) bytes.length
            ));
            completed = true;
            YcAiModelWork refreshed = ycCollectibleMapper.selectAiModelWorkByTaskId(taskId, user.getId());
            Map<String, Object> result = toWorkView(refreshed == null ? work : refreshed);
            result.put("points", ycPointsService.getSummary(user));
            result.put("message", "作品已永久保存");
            return result;
        } catch (Exception e) {
            if (AiModelWorkPolicy.shouldRefundPersistence(started, completed)) {
                ycPointsService.refundAi3dPersist(user, work, charged, "作品保存失败");
            }
            throw e;
        }
    }

    private String persistCover(YcAiModelWork work, Long userId, String taskId) {
        String sourceUrl = toWebpPreviewUrl(work.getCoverUrl());
        if (!StringUtils.hasText(sourceUrl)) {
            return DEFAULT_COVER_URL;
        }
        try {
            byte[] coverBytes = downloadTrustedTencentAsset(sourceUrl, REMOTE_COVER_MAX_SIZE);
            return UploadUtil.uploadAi3dCoverBytes(coverBytes, userId, taskId);
        } catch (Exception e) {
            log.warn("AI 3D 作品封面保存失败，使用默认封面，任务ID={}", taskId);
            return DEFAULT_COVER_URL;
        }
    }

    @Scheduled(
            fixedDelayString = "${ai.tencent.ai3d.cleanup-interval-ms:300000}",
            initialDelayString = "${ai.tencent.ai3d.cleanup-initial-delay-ms:60000}"
    )
    public void deleteExpiredTemporaryWorks() {
        LocalDateTime staleCutoff = LocalDateTime.now().minusMinutes(15);
        List<YcAiModelWork> stalePersists = ycCollectibleMapper.selectStaleAiModelPersists(
                staleCutoff,
                STALE_PERSIST_LIMIT
        );
        if (stalePersists != null) {
            for (YcAiModelWork work : stalePersists) {
                try {
                    ycPointsService.recoverStaleAi3dPersist(work, staleCutoff);
                } catch (Exception error) {
                    log.warn("AI 3D 作品永久保存超时恢复失败，作品ID={}", work.getId());
                }
            }
        }
        int deleted = ycCollectibleMapper.deleteExpiredTemporaryAiModelWorks();
        if (deleted > 0) {
            log.info("已清理 {} 条超过 24 小时且未保存的 AI 作品", deleted);
        }
    }

    public ResponseEntity<InputStreamResource> fetchModelFile(String url, String range) throws Exception {
        if (!isTrustedTencentAssetUrl(url)) {
            throw new IllegalArgumentException("模型地址不在允许的资源域名内");
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .GET();
        if (StringUtils.hasText(range)) {
            requestBuilder.header(HttpHeaders.RANGE, range.trim());
        }

        HttpResponse<InputStream> response = remoteFileClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );
        if (response.statusCode() != 200 && response.statusCode() != 206) {
            response.body().close();
            throw new IOException("远端作品文件读取失败");
        }

        long totalSize = remoteTotalSize(response);
        if (totalSize > REMOTE_MODEL_MAX_SIZE) {
            response.body().close();
            throw new IOException("远端作品文件超过大小限制");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("model/gltf-binary"));
        headers.setCacheControl("private, max-age=300");
        headers.set("X-Accel-Buffering", "no");
        copyRemoteHeader(response, headers, HttpHeaders.ACCEPT_RANGES);
        copyRemoteHeader(response, headers, HttpHeaders.CONTENT_RANGE);
        copyRemoteHeader(response, headers, HttpHeaders.ETAG);
        copyRemoteHeader(response, headers, HttpHeaders.LAST_MODIFIED);
        long contentLength = response.headers().firstValueAsLong(HttpHeaders.CONTENT_LENGTH).orElse(-1);
        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }

        InputStreamResource resource = new InputStreamResource(
                new LimitedInputStream(response.body(), REMOTE_MODEL_MAX_SIZE)
        );
        return new ResponseEntity<>(resource, headers, response.statusCode());
    }

    private long remoteTotalSize(HttpResponse<?> response) {
        String contentRange = response.headers().firstValue(HttpHeaders.CONTENT_RANGE).orElse("");
        int slashIndex = contentRange.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex + 1 < contentRange.length()) {
            try {
                return Long.parseLong(contentRange.substring(slashIndex + 1));
            } catch (NumberFormatException ignored) {
                // 无法取得总大小时，继续使用当前响应长度做安全限制。
            }
        }
        return response.headers().firstValueAsLong(HttpHeaders.CONTENT_LENGTH).orElse(-1);
    }

    private void copyRemoteHeader(HttpResponse<?> response, HttpHeaders target, String headerName) {
        response.headers().firstValue(headerName).ifPresent(value -> target.set(headerName, value));
    }

    private byte[] downloadTrustedTencentAsset(String url, long maxSize) throws Exception {
        if (!isTrustedTencentAssetUrl(url)) {
            throw new IllegalArgumentException("模型地址不在允许的资源域名内");
        }
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<InputStream> response = remoteFileClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IOException("远端作品文件读取失败");
        }
        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        if (contentLength > maxSize) {
            response.body().close();
            throw new IOException("远端作品文件超过大小限制");
        }
        try (InputStream input = response.body()) {
            return readAtMost(input, maxSize);
        }
    }

    static boolean isTrustedTencentAssetUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return false;
            }
            String host = uri.getHost();
            return host != null && (
                    matchesDomain(host, "tencentcos.cn")
                            || matchesDomain(host, "tencentcos.com")
                            || matchesDomain(host, "myqcloud.com")
            );
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    static String proxyModelUrl(String url) {
        if (!isTrustedTencentAssetUrl(url)) {
            return url;
        }
        return "/api/ceramic-creation/model-file?url="
                + URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    private static boolean matchesDomain(String host, String domain) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain);
    }

    static byte[] readAtMost(InputStream input, long maxSize) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxSize) {
                throw new IOException("远端作品文件超过大小限制");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static final class LimitedInputStream extends FilterInputStream {
        private final long maxSize;
        private long count;

        private LimitedInputStream(InputStream input, long maxSize) {
            super(input);
            this.maxSize = maxSize;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                count++;
                ensureWithinLimit();
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                count += read;
                ensureWithinLimit();
            }
            return read;
        }

        private void ensureWithinLimit() throws IOException {
            if (count > maxSize) {
                throw new IOException("远端作品文件超过大小限制");
            }
        }
    }

    private Map<String, Object> callTencent(String action, Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        HttpHeaders headers = signedHeaders(action, body);
        ResponseEntity<String> entity = restTemplate.postForEntity(ENDPOINT, new HttpEntity<>(body.getBytes(StandardCharsets.UTF_8), headers), String.class);
        Map<String, Object> root = objectMapper.readValue(entity.getBody(), Map.class);
        Map<String, Object> response = (Map<String, Object>) root.get("Response");
        if (response == null) {
            throw new IllegalStateException("腾讯云返回为空");
        }
        Object error = response.get("Error");
        if (error instanceof Map) {
            Map<?, ?> e = (Map<?, ?>) error;
            String code = String.valueOf(e.get("Code"));
            String message = String.valueOf(e.get("Message"));
            log.warn("AI 3D 服务调用失败，code={}, message={}", code, message);
            throw new IllegalStateException(toPublicServiceMessage(code, message));
        }
        return response;
    }

    static String toPublicServiceMessage(String code, String message) {
        if ("ResourceInsufficient".equals(code)) {
            return "当前生成服务较为繁忙，请稍后再试。";
        }
        return "当前生成服务暂不可用，请稍后再试。";
    }

    static String toWebpPreviewUrl(String sourceUrl) {
        String url = StringUtils.hasText(sourceUrl) ? sourceUrl.trim() : null;
        if (url == null || "null".equalsIgnoreCase(url) || url.contains("imageMogr2/format/webp")) {
            return url;
        }
        try {
            URI uri = URI.create(url);
            String path = String.valueOf(uri.getPath()).toLowerCase(Locale.ROOT);
            boolean convertibleImage = path.endsWith(".png")
                    || path.endsWith(".jpg")
                    || path.endsWith(".jpeg");
            if (!isTrustedTencentAssetUrl(url) || !convertibleImage) {
                return url;
            }
            return url + (url.contains("?") ? "&" : "?") + "imageMogr2/format/webp/ignore-error/1";
        } catch (IllegalArgumentException error) {
            return url;
        }
    }

    static String resolveWorkCoverUrl(String storageStatus, String coverUrl) {
        String url = StringUtils.hasText(coverUrl) ? coverUrl.trim() : null;
        if (url == null || "null".equalsIgnoreCase(url)) {
            return DEFAULT_COVER_URL;
        }
        if ("PERMANENT".equals(storageStatus)) {
            if (isTrustedTencentAssetUrl(url)) {
                return DEFAULT_COVER_URL;
            }
        }
        return url;
    }

    private HttpHeaders signedHeaders(String action, String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String date = DATE_FORMAT.format(Instant.ofEpochSecond(timestamp));
        String contentType = "application/json; charset=utf-8";
        String canonicalHeaders = "content-type:" + contentType + "\n" + "host:" + HOST + "\n";
        String signedHeaders = "content-type;host";
        String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + sha256Hex(payload);
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, SERVICE);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = bytesToHex(hmac256(secretSigning, stringToSign));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Host", HOST);
        headers.set("X-TC-Action", action);
        headers.set("X-TC-Version", VERSION);
        headers.set("X-TC-Timestamp", String.valueOf(timestamp));
        headers.set("X-TC-Region", region);
        headers.set("Authorization", "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature);
        return headers;
    }

    private String clipPrompt(String prompt, String style, String vessel) {
        String text = (StringUtils.hasText(style) || StringUtils.hasText(vessel))
                ? String.format("%s，风格：%s，器型：%s", prompt, style, vessel)
                : prompt;
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    private String toLocalStatus(String status) {
        if ("DONE".equals(status)) return "ready";
        if ("FAIL".equals(status)) return "failed";
        if ("RUN".equals(status)) return "running";
        return "pending";
    }

    private Map<String, Object> toWorkView(YcAiModelWork work) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (work == null) {
            return result;
        }
        result.put("id", work.getId());
        result.put("userId", work.getUserId());
        result.put("taskId", work.getTaskId());
        result.put("authorName", work.getAuthorName());
        result.put("workCode", work.getWorkCode());
        result.put("title", work.getTitle());
        result.put("prompt", work.getPrompt());
        result.put("style", work.getStyle());
        result.put("vessel", work.getVessel());
        String coverUrl = resolveWorkCoverUrl(work.getStorageStatus(), work.getCoverUrl());
        result.put("coverUrl", coverUrl);
        result.put("coverWebpUrl", toWebpPreviewUrl(coverUrl));
        String modelUrl = "PERMANENT".equals(work.getStorageStatus()) && StringUtils.hasText(work.getOssUrl())
                ? work.getOssUrl()
                : work.getModelUrl();
        result.put("modelUrl", proxyModelUrl(modelUrl));
        result.put("modelFormat", work.getModelFormat());
        result.put("generationStatus", work.getGenerationStatus());
        result.put("storageStatus", work.getStorageStatus());
        result.put("status", toPublicStatus(work.getGenerationStatus()));
        result.put("temporary", "TEMPORARY".equals(work.getStorageStatus()) || "PERSIST_FAILED".equals(work.getStorageStatus()));
        result.put("permanent", "PERMANENT".equals(work.getStorageStatus()));
        result.put("canPersist", AiModelWorkPolicy.canStartPersist(work.getStorageStatus(), work.getPersistChargeStatus())
                && AiModelWorkPolicy.canPersistUntil(work.getExpiresAt(), LocalDateTime.now()));
        result.put("generatedAt", work.getGeneratedAt());
        result.put("expiresAt", work.getExpiresAt());
        result.put("persistedAt", work.getPersistedAt());
        result.put("firstQueryAt", work.getCreatedAt() == null
                ? null
                : work.getCreatedAt().plus(AiModelWorkPolicy.FIRST_QUERY_DELAY));
        result.put("persistPointsCost", work.getPersistPointsCost() == null
                ? ycPointsService.getAi3dPersistCostPoints()
                : work.getPersistPointsCost());
        result.put("createdAt", work.getCreatedAt());
        result.put("updatedAt", work.getUpdatedAt());
        return result;
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(secretId) || !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("作品生成服务暂不可用，请联系管理员");
        }
    }

    private YcUserAccount userById(Long userId) {
        YcUserAccount user = ycCollectibleMapper.selectUserById(userId);
        if (user == null) {
            throw new CustomException("404", "用户不存在");
        }
        return user;
    }

    private String createWorkCode() {
        return "AI3D-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    }

    private String buildWorkTitle(String style, String vessel) {
        String title = StringUtils.hasText(style) || StringUtils.hasText(vessel)
                ? String.valueOf(style == null ? "" : style) + String.valueOf(vessel == null ? "" : vessel)
                : "AI 陶瓷 3D 作品";
        return StringUtils.hasText(title) ? title : "AI 陶瓷 3D 作品";
    }

    private String toPublicStatus(String generationStatus) {
        if ("READY".equals(generationStatus)) return "ready";
        if ("FAILED".equals(generationStatus)) return "failed";
        if ("RUNNING".equals(generationStatus)) return "running";
        return "pending";
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] hmac256(byte[] key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b & 0xff));
        }
        return builder.toString();
    }
}
