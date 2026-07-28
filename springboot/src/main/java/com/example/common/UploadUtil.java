package com.example.common;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.UUID;

@Component
public class UploadUtil {

    private static final Logger log = LoggerFactory.getLogger(UploadUtil.class);
    private static final long AI3D_MODEL_MAX_SIZE = 100L * 1024 * 1024;
    private static final long AI3D_COVER_MAX_SIZE = 10L * 1024 * 1024;

    private static String endpoint;
    private static String accessKeyId;
    private static String accessKeySecret;
    private static String bucketName;
    private static String urlPrefix;

    @Value("${aliyun.oss.endpoint}")
    public void setEndpoint(String endpoint) {
        UploadUtil.endpoint = endpoint;
    }

    @Value("${aliyun.oss.access-key-id}")
    public void setAccessKeyId(String accessKeyId) {
        UploadUtil.accessKeyId = accessKeyId;
    }

    @Value("${aliyun.oss.access-key-secret}")
    public void setAccessKeySecret(String accessKeySecret) {
        UploadUtil.accessKeySecret = accessKeySecret;
    }

    @Value("${aliyun.oss.bucket}")
    public void setBucketName(String bucketName) {
        UploadUtil.bucketName = bucketName;
    }

    @Value("${aliyun.oss.url-prefix}")
    public void setUrlPrefix(String urlPrefix) {
        UploadUtil.urlPrefix = urlPrefix;
    }

    public static String upload(MultipartFile file) throws IOException {
        ensureOssConfigured();
        String originalFilename = file.getOriginalFilename();
        String ext = getExtensionWithDot(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + ext;

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, fileName, file.getInputStream());
            return urlPrefix + fileName;
        } catch (Exception e) {
            log.error("OSS上传失败: {}", e.getMessage(), e);
            throw new IOException("文件上传失败: " + e.getMessage(), e);
        } finally {
            shutdownOssClient(ossClient);
        }
    }

    public static String uploadWithUsername(MultipartFile file, String username) throws IOException {
        ensureOssConfigured();
        log.debug("uploadWithUsername 开始, 用户名: {}", username);
        String originalFilename = file.getOriginalFilename();
        String ext = getExtensionWithDot(originalFilename);
        // 使用用户名作为文件名前缀，这样同一用户上传的头像会覆盖之前的
        String fileName = "avatar/" + username + "_avatar" + ext;
        log.debug("OSS文件名: {}, 文件大小: {} bytes", fileName, file.getSize());

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, fileName, file.getInputStream());
            String resultUrl = urlPrefix + fileName;
            log.debug("uploadWithUsername 完成, URL: {}", resultUrl);
            return resultUrl;
        } catch (Exception e) {
            log.error("OSS上传失败: {}", e.getMessage(), e);
            throw new IOException("文件上传失败: " + e.getMessage(), e);
        } finally {
            shutdownOssClient(ossClient);
        }
    }

    public static String uploadPhotoWall(MultipartFile file) throws IOException {
        ensureOssConfigured();
        String originalFilename = file.getOriginalFilename();
        String ext = getExtensionWithDot(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = "photo-wall/" + uuid + ext;

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, fileName, file.getInputStream());
            return urlPrefix + fileName;
        } catch (Exception e) {
            log.error("OSS上传照片墙失败: {}", e.getMessage(), e);
            throw new IOException("文件上传失败: " + e.getMessage(), e);
        } finally {
            shutdownOssClient(ossClient);
        }
    }

    public static String uploadCollectibleModel(MultipartFile file) throws IOException {
        ensureOssConfigured();
        validateAi3dModelSize(file == null ? 0 : file.getSize());
        String originalFilename = file.getOriginalFilename();
        String ext = getExtensionWithDot(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = "project-media/models/" + uuid + ext.toLowerCase();

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, fileName, file.getInputStream());
            return urlPrefix + fileName;
        } catch (Exception e) {
            log.error("OSS上传模型失败: {}", e.getMessage(), e);
            throw new IOException("模型上传失败: " + e.getMessage(), e);
        } finally {
            shutdownOssClient(ossClient);
        }
    }

    public static String uploadCollectibleModelBytes(byte[] data, String fileName) throws IOException {
        ensureOssConfigured();
        validateAi3dModelSize(data == null ? 0 : data.length);
        String ext = getExtensionWithDot(fileName).toLowerCase();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectName = "project-media/models/" + uuid + ext;

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(data));
            return urlPrefix + objectName;
        } catch (Exception e) {
            log.error("OSS上传模型失败: {}", e.getMessage(), e);
            throw new IOException("模型上传失败: " + e.getMessage(), e);
        } finally {
            shutdownOssClient(ossClient);
        }
    }

    public static String uploadAi3dModelBytes(byte[] data, Long userId, String taskId) throws IOException {
        ensureOssConfigured();
        validateAi3dModelSize(data == null ? 0 : data.length);
        String objectName = buildAi3dObjectName(userId, taskId);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("model/gltf-binary");
        metadata.setContentLength(data.length);

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(data), metadata);
            return urlPrefix + objectName;
        } catch (Exception e) {
            log.error("AI 3D 模型上传失败: {}", e.getMessage(), e);
            throw new IOException("作品保存失败，请稍后重试", e);
        } finally {
            shutdownOssClient(ossClient);
        }
    }

    static String buildAi3dObjectName(Long userId, String taskId) {
        String safeTaskId = String.valueOf(taskId == null ? "unknown" : taskId)
                .replaceAll("[^A-Za-z0-9_-]", "-");
        return "project-media/models/ai3d/" + userId + "/" + safeTaskId + "/model.glb";
    }

    public static String uploadAi3dCoverBytes(byte[] data, Long userId, String taskId) throws IOException {
        ensureOssConfigured();
        validateAi3dCoverBytes(data);
        String objectName = buildAi3dCoverObjectName(userId, taskId);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/webp");
        metadata.setContentLength(data.length);

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(data), metadata);
            return urlPrefix + objectName;
        } catch (Exception e) {
            log.error("AI 3D 作品封面上传失败: {}", e.getMessage(), e);
            throw new IOException("作品封面保存失败", e);
        } finally {
            shutdownOssClient(ossClient);
        }
    }

    static String buildAi3dCoverObjectName(Long userId, String taskId) {
        String modelObjectName = buildAi3dObjectName(userId, taskId);
        return modelObjectName.substring(0, modelObjectName.lastIndexOf('/') + 1) + "cover.webp";
    }

    static void validateAi3dCoverBytes(byte[] data) {
        if (data == null || data.length < 12 || data.length > AI3D_COVER_MAX_SIZE
                || data[0] != 'R' || data[1] != 'I' || data[2] != 'F' || data[3] != 'F'
                || data[8] != 'W' || data[9] != 'E' || data[10] != 'B' || data[11] != 'P') {
            throw new IllegalArgumentException("作品封面必须是 10MB 以内的 WebP 图片");
        }
    }

    public static String uploadCollectibleCover(MultipartFile file) throws IOException {
        ensureOssConfigured();
        String originalFilename = file.getOriginalFilename();
        String ext = getExtensionWithDot(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = "project-media/covers/" + uuid + ext.toLowerCase();

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, fileName, file.getInputStream());
            return urlPrefix + fileName;
        } catch (Exception e) {
            log.error("OSS上传封面失败: {}", e.getMessage(), e);
            throw new IOException("封面上传失败: " + e.getMessage(), e);
        } finally {
            shutdownOssClient(ossClient);
        }
    }

    public static String uploadShopProductCover(MultipartFile file) throws IOException {
        ensureOssConfigured();
        String originalFilename = file.getOriginalFilename();
        String ext = getExtensionWithDot(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = "project-media/shop-products/" + uuid + ext.toLowerCase();

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, fileName, file.getInputStream());
            return urlPrefix + fileName;
        } catch (Exception e) {
            log.error("OSS上传商城商品封面失败: {}", e.getMessage(), e);
            throw new IOException("商城商品封面上传失败: " + e.getMessage(), e);
        } finally {
            shutdownOssClient(ossClient);
        }
    }

    private static void shutdownOssClient(OSS ossClient) {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
            } catch (Exception e) {
                log.warn("关闭OSS客户端失败: {}", e.getMessage());
            }
        }
    }

    private static String getExtensionWithDot(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex);
    }

    private static void ensureOssConfigured() throws IOException {
        if (isBlank(endpoint) || isBlank(accessKeyId) || isBlank(accessKeySecret) || isBlank(bucketName) || isBlank(urlPrefix)) {
            throw new IOException("OSS未配置，请设置 OSS_ENDPOINT、OSS_ACCESS_KEY_ID、OSS_ACCESS_KEY_SECRET、OSS_BUCKET、OSS_URL_PREFIX");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static void validateAi3dModelSize(long size) {
        if (size > AI3D_MODEL_MAX_SIZE) {
            throw new IllegalArgumentException("glb 文件不能超过 100MB");
        }
    }
}
