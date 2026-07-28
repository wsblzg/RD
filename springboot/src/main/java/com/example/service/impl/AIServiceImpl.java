package com.example.service.impl;

import com.example.service.AIService;
import com.example.service.RagClient;
import com.example.service.RagClient.RagResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * AI服务实现类
 * 支持讯飞星火和阿里云通义千问
 */
@Service
public class AIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    @Value("${ai.provider:xunfei}")
    private String provider;

    // 讯飞星火配置
    @Value("${ai.xunfei.app-id:}")
    private String xunfeiAppId;

    @Value("${ai.xunfei.api-key:}")
    private String xunfeiApiKey;

    @Value("${ai.xunfei.api-secret:}")
    private String xunfeiApiSecret;

    @Value("${ai.xunfei.api-url:https://spark-api.xf-yun.com/v3.5/chat}")
    private String xunfeiApiUrl;

    @Value("${ai.xunfei.domain:generalv3.5}")
    private String xunfeiDomain;

    @Value("${ai.timeout:30000}")
    private int timeout;

    @Value("${ai.rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${ai.rag.example:请基于检索到的柴烧非遗资料回答。回答要通俗、准确，不能替代专家鉴定；如资料不足，请说明目前知识库中没有足够依据。}")
    private String ragExample;

    @Value("${ai.nvidia.api-url:https://integrate.api.nvidia.com/v1}")
    private String nvidiaApiUrl;

    @Value("${ai.nvidia.api-key:}")
    private String nvidiaApiKey;

    @Value("${ai.nvidia.vision-model:nvidia/nemotron-nano-12b-v2-vl}")
    private String nvidiaVisionModel;

    @Value("${ai.nvidia.vision-timeout-ms:60000}")
    private int nvidiaVisionTimeoutMs;

    @Value("${ai.deepseek.api-url:https://api.deepseek.com}")
    private String deepseekApiUrl;

    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.model:deepseek-v4-flash}")
    private String deepseekModel;

    @Value("${ai.deepseek.timeout-ms:60000}")
    private int deepseekTimeoutMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RagClient ragClient;
    private Integer appliedVisionTimeoutMs;

    @Autowired
    public AIServiceImpl(RagClient ragClient) {
        this(ragClient, new RestTemplate());
    }

    AIServiceImpl(RagClient ragClient, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.ragClient = ragClient;
    }

    @Override
    public String generateImageDescription(MultipartFile imageFile) {
        try {
            byte[] imageBytes = imageFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String contentType = imageFile.getContentType();
            return generateImageDescriptionFromBase64(base64Image, contentType);
        } catch (Exception e) {
            log.error("生成图片描述失败: {}", e.getMessage(), e);
            return generateFallbackImageDescription();
        }
    }

    @Override
    public String generateImageDescriptionFromBase64(String imageBase64) {
        return generateImageDescriptionFromBase64(imageBase64, "image/jpeg");
    }

    private String generateImageDescriptionFromBase64(String imageBase64, String contentType) {
        if (!isNvidiaVisionConfigured()) {
            log.warn("NVIDIA vision API key is not configured, using low-confidence fallback");
            return generateFallbackImageDescription();
        }
        try {
            String cleanBase64 = normalizeBase64Image(imageBase64);
            String mimeType = normalizeImageMimeType(contentType);
            String dataUrl = "data:" + mimeType + ";base64," + cleanBase64;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(nvidiaApiKey.trim());

            Map<String, Object> imageUrl = new HashMap<>();
            imageUrl.put("url", dataUrl);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", buildVisionPrompt());

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrl);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", Arrays.asList(textPart, imagePart));

            Map<String, Object> request = new HashMap<>();
            request.put("model", nvidiaVisionModel);
            request.put("messages", Collections.singletonList(message));
            request.put("temperature", 0);
            request.put("max_tokens", 600);

            applyVisionTimeoutIfNeeded();
            ResponseEntity<String> response = restTemplate.exchange(
                nvidiaApiUrl.replaceAll("/+$", "") + "/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
            );
            return parseNvidiaVisionResponse(response.getBody());
        } catch (Exception e) {
            log.error("NVIDIA视觉鉴赏失败: {}", e.getMessage(), e);
            return generateFallbackImageDescription();
        }
    }

    private void applyVisionTimeoutIfNeeded() {
        if (appliedVisionTimeoutMs != null && appliedVisionTimeoutMs == nvidiaVisionTimeoutMs) {
            return;
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(nvidiaVisionTimeoutMs);
        requestFactory.setReadTimeout(nvidiaVisionTimeoutMs);
        restTemplate.setRequestFactory(requestFactory);
        appliedVisionTimeoutMs = nvidiaVisionTimeoutMs;
    }

    private boolean isNvidiaVisionConfigured() {
        return nvidiaApiKey != null && !nvidiaApiKey.trim().isEmpty();
    }

    private String normalizeBase64Image(String imageBase64) {
        if (imageBase64 == null) {
            return "";
        }
        String value = imageBase64.trim();
        int commaIndex = value.indexOf(',');
        if (value.startsWith("data:") && commaIndex >= 0) {
            value = value.substring(commaIndex + 1);
        }
        return value.replaceAll("\\s+", "");
    }

    private String normalizeImageMimeType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return "image/jpeg";
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).trim();
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        if (!normalized.startsWith("image/")) {
            return "image/jpeg";
        }
        return normalized;
    }

    private String buildVisionPrompt() {
        return String.join("\n",
            "你是柴烧陶瓷图片辅助鉴赏模型。请先判断图片是否为陶瓷/柴烧陶瓷相关图片，再决定置信度。",
            "必须只返回合法JSON，不要Markdown，不要解释JSON之外的文字。",
            "JSON结构固定为：",
            "{\"summary\":\"...\",\"confidence\":0.0,\"isCeramic\":false,\"isWoodFiredCeramic\":false,\"features\":[{\"label\":\"...\",\"value\":\"...\",\"evidence\":\"...\"}]}",
            "规则：",
            "1. 如果图片明显不是陶瓷、器物、窑炉、陶艺制作或柴烧相关场景，confidence必须小于0.35，summary说明无法作为柴烧作品鉴赏。",
            "2. 如果只是普通陶瓷但不能确认柴烧，confidence控制在0.35到0.65之间，并说明只能做有限观察。",
            "3. 只有图片确实呈现柴烧作品常见特征，如火痕、落灰、自然灰釉、釉色流动、窑变肌理，confidence才可以高于0.7。",
            "4. features最多3项，必须基于图片可见内容，不要编造看不见的窑位、烧制温度、传承人或价格。",
            "5. 结论只能用于学习参考，不能替代专家鉴定。"
        );
    }

    String parseNvidiaVisionResponse(String responseBody) throws Exception {
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("NVIDIA视觉模型返回为空");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = message == null ? "" : String.valueOf(message.get("content"));
        String jsonText = extractJsonObject(content);
        Map<String, Object> parsed = objectMapper.readValue(jsonText, Map.class);
        return objectMapper.writeValueAsString(normalizeVisionReport(parsed));
    }

    private String extractJsonObject(String text) {
        if (text == null) {
            throw new IllegalArgumentException("视觉模型返回内容为空");
        }
        String value = text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("视觉模型未返回JSON: " + text);
        }
        return value.substring(start, end + 1);
    }

    private Map<String, Object> normalizeVisionReport(Map<String, Object> parsed) {
        Map<String, Object> report = new LinkedHashMap<>();
        boolean isCeramic = asBoolean(parsed.get("isCeramic"), asBoolean(parsed.get("is_ceramic"), false));
        boolean isWoodFired = asBoolean(parsed.get("isWoodFiredCeramic"), asBoolean(parsed.get("is_wood_fired_ceramic"), false));
        double confidence = clamp(asDouble(parsed.get("confidence"), 0.2), 0, 1);
        if (!isCeramic) {
            confidence = Math.min(confidence, 0.34);
        } else if (!isWoodFired) {
            confidence = Math.min(confidence, 0.65);
        }

        report.put("source", "api");
        report.put("provider", "NVIDIA " + nvidiaVisionModel);
        report.put("summary", asString(parsed.get("summary"), isCeramic ? "图片可作为陶瓷作品观察参考。" : "图片未呈现明确的柴烧陶瓷作品特征，无法进行柴烧鉴赏。"));
        report.put("confidence", confidence);
        report.put("isCeramic", isCeramic);
        report.put("isWoodFiredCeramic", isWoodFired);
        report.put("features", normalizeFeatures(parsed.get("features"), isCeramic));
        report.put("generatedAt", new Date().toInstant().toString());
        return report;
    }

    private List<Map<String, String>> normalizeFeatures(Object rawFeatures, boolean isCeramic) {
        List<Map<String, String>> features = new ArrayList<>();
        if (rawFeatures instanceof List) {
            for (Object item : (List<?>) rawFeatures) {
                if (item instanceof Map) {
                    Map<?, ?> raw = (Map<?, ?>) item;
                    Map<String, String> feature = new LinkedHashMap<>();
                    feature.put("label", asString(raw.get("label"), "可见特征"));
                    feature.put("value", asString(raw.get("value"), "待判断"));
                    feature.put("evidence", asString(raw.get("evidence"), "基于图片可见内容。"));
                    features.add(feature);
                    if (features.size() >= 3) {
                        break;
                    }
                }
            }
        }
        if (features.isEmpty()) {
            Map<String, String> feature = new LinkedHashMap<>();
            feature.put("label", isCeramic ? "图像可见性" : "非柴烧图像");
            feature.put("value", isCeramic ? "可做有限观察" : "未识别到柴烧陶瓷主体");
            feature.put("evidence", isCeramic ? "模型仅基于上传图片进行辅助说明。" : "图片缺少陶瓷器型、火痕、落灰、灰釉等可见特征。");
            features.add(feature);
        }
        return features;
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return fallback;
    }

    private double asDouble(Object value, double fallback) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    @Override
    public String generateArticleSummary(String content, int maxLength) {
        if ("xunfei".equals(provider) && isXunfeiConfigured()) {
            return generateSummaryWithXunfei(content, maxLength);
        }
        return generateFallbackSummary(content, maxLength);
    }

    @Override
    public String recommendArticles(Long userId, int limit) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("articleIds", new ArrayList<>());
            result.put("message", "基于用户行为的智能推荐");
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("AI推荐失败: {}", e.getMessage(), e);
            return "{\"success\": false, \"articleIds\": []}";
        }
    }

    @Override
    public String chatbotAnswer(String question, String context) {
        if (ragEnabled) {
            RagResponse ragResponse = ragClient.query(question, ragExample);
            if (ragResponse.isAvailable()) {
                return cleanChatbotAnswer(ragResponse.getAnswer());
            }
            log.warn("RAG chatbot fallback triggered: {}", ragResponse.getAnswer());
        }
        if ("xunfei".equals(provider) && isXunfeiConfigured()) {
            return chatWithXunfei(question, context);
        }
        return generateFallbackChatbotAnswer(question);
    }

    private String cleanChatbotAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().replaceFirst(
                "^(?:(?:根据|基于)(?:以上|上述)?(?:参考文档|参考资料|检索资料|检索到的资料)(?:内容)?[，,:：]?\\s*)+",
                ""
        ).trim();
    }

    @Override
    public String moderateContent(String content) {
        try {
            if (content == null || content.isEmpty()) {
                return "{\"isPass\": true, \"reason\": \"\"}";
            }
            List<String> sensitiveWords = Arrays.asList("违禁词1", "违禁词2", "广告", "诈骗");

            for (String word : sensitiveWords) {
                if (content.contains(word)) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("isPass", false);
                    result.put("reason", "包含敏感词: " + word);
                    return objectMapper.writeValueAsString(result);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("isPass", true);
            result.put("reason", "");
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"isPass\": true, \"reason\": \"\"}";
        }
    }

    @Override
    public String optimizeCeramicPrompt(String message, String style, String vessel) {
        String idea = unwrapCeramicPrompt(message);
        String fallback = buildCeramicPrompt(idea, style, vessel);
        if (deepseekApiKey == null || deepseekApiKey.trim().isEmpty()) {
            return fallback;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepseekApiKey.trim());

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", deepseekModel);
            request.put("temperature", 0.5);
            request.put("max_tokens", 500);
            request.put("messages", Arrays.asList(
                    Map.of(
                            "role", "system",
                            "content", "你是陶瓷3D创作提示词编辑。只输出一段可直接用于3D生成的中文提示词，不要标题、序号、Markdown或解释。"
                    ),
                    Map.of(
                            "role", "user",
                            "content", "创作想法：" + idea + "\n风格：" + safeText(style, "柴烧") + "\n器型：" + safeText(vessel, "陶瓷作品")
                    )
            ));

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(deepseekTimeoutMs);
            requestFactory.setReadTimeout(deepseekTimeoutMs);
            restTemplate.setRequestFactory(requestFactory);

            ResponseEntity<String> response = restTemplate.exchange(
                    deepseekApiUrl.replaceAll("/+$", "") + "/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );
            String optimized = parseChatContent(response.getBody());
            return optimized.isEmpty() ? fallback : optimized;
        } catch (Exception e) {
            log.warn("陶瓷提示词优化失败，已使用本地模板: {}", e.getMessage());
            return fallback;
        }
    }

    private String buildCeramicPrompt(String message, String style, String vessel) {
        return "请生成一件" + safeText(style, "柴烧") + safeText(vessel, "陶瓷作品")
                + "，主题为“" + safeText(message, "具有东方审美的陶瓷作品")
                + "”。要求器型比例协调，表面保留手作痕迹，火痕、落灰与釉色变化清晰，适合在线 3D 展示。";
    }

    private String unwrapCeramicPrompt(String message) {
        String value = safeText(message, "具有东方审美的陶瓷作品");
        String marker = "，主题为“";
        String suffix = "”。要求器型比例协调，表面保留手作痕迹，火痕、落灰与釉色变化清晰，适合在线 3D 展示。";
        while (value.startsWith("请生成一件") && value.endsWith(suffix)) {
            int start = value.indexOf(marker);
            if (start < 0) {
                break;
            }
            value = value.substring(start + marker.length(), value.length() - suffix.length()).trim();
        }
        return value;
    }

    private String parseChatContent(String responseBody) throws Exception {
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message == null ? "" : safeText(message.get("content"), "");
    }

    private String safeText(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    // ========== 讯飞星火 AI 实现 ==========

    private boolean isXunfeiConfigured() {
        return xunfeiAppId != null && !xunfeiAppId.isEmpty()
            && xunfeiApiKey != null && !xunfeiApiKey.isEmpty()
            && xunfeiApiSecret != null && !xunfeiApiSecret.isEmpty();
    }

    private String generateSummaryWithXunfei(String content, int maxLength) {
        // 讯飞星火使用WebSocket连接,HTTP REST调用较复杂
        log.debug("Xunfei AI: using local summary fallback (HTTP REST not implemented)");
        return generateFallbackSummary(content, maxLength);
    }

    private String chatWithXunfei(String question, String context) {
        // 讯飞星火使用WebSocket连接,HTTP REST调用较复杂
        log.debug("Xunfei AI: using local chatbot fallback (HTTP REST not implemented)");
        return generateFallbackChatbotAnswer(question);
    }

    private String buildRagExample(String context) {
        if (context == null || context.trim().isEmpty()) {
            return ragExample;
        }
        return ragExample + "\n\n用户当前页面或补充上下文：" + context.trim();
    }

    private Map<String, Object> buildXunfeiRequest(String content) {
        Map<String, Object> request = new HashMap<>();

        // header
        Map<String, Object> header = new HashMap<>();
        header.put("app_id", xunfeiAppId);
        header.put("uid", "user_" + System.currentTimeMillis());
        request.put("header", header);

        // parameter
        Map<String, Object> parameter = new HashMap<>();
        Map<String, Object> chat = new HashMap<>();
        chat.put("domain", xunfeiDomain);
        chat.put("temperature", 0.7);
        chat.put("max_tokens", 2048);
        parameter.put("chat", chat);
        request.put("parameter", parameter);

        // payload
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        List<Map<String, String>> text = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", content);
        text.add(userMessage);
        message.put("text", text);
        payload.put("message", message);
        request.put("payload", payload);

        return request;
    }

    private String generateXunfeiAuth() {
        try {
            URL url = new URL(xunfeiApiUrl);
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());

            String preStr = "host: " + url.getHost() + "\n" +
                          "date: " + date + "\n" +
                          "GET " + url.getPath() + " HTTP/1.1";

            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(xunfeiApiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
            String sha = Base64.getEncoder().encodeToString(hexDigits);

            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    xunfeiApiKey, "hmac-sha256", "host date request-line", sha);

            return Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("生成讯飞Auth失败: {}", e.getMessage(), e);
            return "";
        }
    }

    private String parseXunfeiResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> payload = (Map<String, Object>) response.get("payload");
            Map<String, Object> choices = (Map<String, Object>) payload.get("choices");
            List<Map<String, Object>> textList = (List<Map<String, Object>>) choices.get("text");

            if (textList != null && !textList.isEmpty()) {
                return (String) textList.get(0).get("content");
            }
        } catch (Exception e) {
            log.error("解析讯飞响应失败: {}", e.getMessage(), e);
        }
        return "AI服务暂时不可用";
    }

    // ========== 本地备用方案 ==========

    private String generateFallbackImageDescription() {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("source", "fallback");
            result.put("provider", "本地低置信度回退");
            result.put("summary", "在线视觉模型暂时不可用，无法基于图片像素完成可靠柴烧鉴赏。请稍后重试，或改用柴烧知识问答描述作品特征。");
            result.put("confidence", 0.12);
            result.put("isCeramic", false);
            result.put("isWoodFiredCeramic", false);
            Map<String, String> feature = new LinkedHashMap<>();
            feature.put("label", "鉴赏状态");
            feature.put("value", "未完成可靠识别");
            feature.put("evidence", "当前结果来自本地回退，不代表图片内容判断。");
            result.put("features", Collections.singletonList(feature));
            result.put("generatedAt", new Date().toInstant().toString());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"source\":\"fallback\",\"provider\":\"本地低置信度回退\",\"summary\":\"在线视觉模型暂时不可用，无法完成可靠柴烧鉴赏。\",\"confidence\":0.12,\"isCeramic\":false,\"isWoodFiredCeramic\":false,\"features\":[{\"label\":\"鉴赏状态\",\"value\":\"未完成可靠识别\",\"evidence\":\"当前结果来自本地回退。\"}]}";
        }
    }

    private String generateFallbackSummary(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        if (maxLength <= 0) {
            return "";
        }

        // 移除HTML标签
        String text = stripHtml(content);

        // 如果文本长度小于最大长度,直接返回
        if (text.length() <= maxLength) {
            return text;
        }

        // 截取并在句号处截断
        String summary = text.substring(0, maxLength);
        int lastPeriod = summary.lastIndexOf('。');
        if (lastPeriod > maxLength / 2) {
            summary = summary.substring(0, lastPeriod + 1);
        } else {
            summary += "...";
        }

        return summary;
    }

    private String generateFallbackChatbotAnswer(String question) {
        if (question == null || question.isEmpty()) {
            return "感谢提问。你可以继续描述具体器型、烧制工艺或页面功能需求，我会给出更有针对性的解答。";
        }
        Map<String, String> qaMap = new HashMap<>();
        qaMap.put("如何注册", "点击页面右上角“注册”，填写用户名、邮箱与密码后即可完成注册。");
        qaMap.put("忘记密码", "请在登录页点击“忘记密码”，通过邮箱验证码完成重置。");
        qaMap.put("如何预约", "可在“到访与参与”页面提交预约信息，工作人员会在24小时内确认。");
        qaMap.put("数字藏品", "数字藏品模块用于展示与收藏陶瓷器物，可在“藏品总览”查看在架内容。");
        qaMap.put("如何联系", "可通过页面底部联系方式提交咨询，也可以直接在问答窗口继续提问。");

        // 简单的关键词匹配
        for (Map.Entry<String, String> entry : qaMap.entrySet()) {
            if (question.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "当前问题我已记录。建议补充关键词（如窑变、拉坯、导览、藏品、预约），我可以给出更精准的回答。";
    }

    // ========== 工具方法 ==========

    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]*>", "").trim();
    }
}
