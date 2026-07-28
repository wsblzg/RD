package com.example.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelWorkPolicyTest {

    @Test
    void waitsFiveMinutesBeforeFirstStatusCheck() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 7, 16, 10, 0);

        assertFalse(AiModelWorkPolicy.canQuery(submittedAt, LocalDateTime.of(2026, 7, 16, 10, 4, 59)));
        assertTrue(AiModelWorkPolicy.canQuery(submittedAt, LocalDateTime.of(2026, 7, 16, 10, 5)));
    }

    @Test
    void temporaryModelIsUsableOnlyBeforeExpiry() {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 17, 10, 0);

        assertTrue(AiModelWorkPolicy.isTemporaryUsable("TEMPORARY", expiresAt, LocalDateTime.of(2026, 7, 17, 9, 59)));
        assertFalse(AiModelWorkPolicy.isTemporaryUsable("TEMPORARY", expiresAt, expiresAt));
        assertFalse(AiModelWorkPolicy.isTemporaryUsable("PERMANENT", expiresAt, LocalDateTime.of(2026, 7, 17, 9, 59)));
    }

    @Test
    void temporaryWorkExpiresAfterFullTwentyFourHours() {
        assertEquals(java.time.Duration.ofHours(24), AiModelWorkPolicy.TEMPORARY_LIFETIME);
    }

    @Test
    void deletesOnlyExpiredReadyUnsavedWork() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 17, 10, 0);
        LocalDateTime expired = now.minusSeconds(1);
        LocalDateTime future = now.plusSeconds(1);

        assertTrue(AiModelWorkPolicy.shouldDeleteExpiredTemporary("READY", "TEMPORARY", expired, now));
        assertTrue(AiModelWorkPolicy.shouldDeleteExpiredTemporary("READY", "PERSIST_FAILED", expired, now));
        assertFalse(AiModelWorkPolicy.shouldDeleteExpiredTemporary("READY", "PERMANENT", expired, now));
        assertFalse(AiModelWorkPolicy.shouldDeleteExpiredTemporary("RUNNING", "TEMPORARY", expired, now));
        assertFalse(AiModelWorkPolicy.shouldDeleteExpiredTemporary("READY", "TEMPORARY", future, now));
    }

    @Test
    void permanentWorkCannotBeChargedAgain() {
        assertTrue(AiModelWorkPolicy.canStartPersist("TEMPORARY", "NONE"));
        assertTrue(AiModelWorkPolicy.canStartPersist("PERSIST_FAILED", "REFUNDED"));
        assertFalse(AiModelWorkPolicy.canStartPersist("PERSISTING", "CHARGED"));
        assertFalse(AiModelWorkPolicy.canStartPersist("PERMANENT", "CHARGED"));
    }

    @Test
    void permanentSaveKeepsTenMinuteSafetyWindow() {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 17, 10, 0);

        assertTrue(AiModelWorkPolicy.canPersistUntil(expiresAt, LocalDateTime.of(2026, 7, 17, 9, 49, 59)));
        assertFalse(AiModelWorkPolicy.canPersistUntil(expiresAt, LocalDateTime.of(2026, 7, 17, 9, 50)));
    }

    @Test
    void onlyPermanentWorkCanBePublished() {
        assertTrue(AiModelWorkPolicy.canPublish("PERMANENT"));
        assertFalse(AiModelWorkPolicy.canPublish("TEMPORARY"));
        assertFalse(AiModelWorkPolicy.canPublish("PERSISTING"));
    }

    @Test
    void selectsGlbResultInsteadOfFirstRemoteFile() {
        List<Map<String, Object>> files = List.of(
                Map.of("Type", "OBJ", "Url", "https://example.com/work.obj"),
                Map.of("Type", "GLB", "Url", "https://example.com/work.glb")
        );

        Map<String, Object> selected = TencentAi3dService.selectGlbFile(files);

        assertEquals("https://example.com/work.glb", selected.get("Url"));
        assertNull(TencentAi3dService.selectGlbFile(List.of(Map.of("Type", "OBJ", "Url", "https://example.com/work.obj"))));
    }

    @Test
    void stateTransitionMustUpdateOneRow() {
        assertDoesNotThrow(() -> TencentAi3dService.requireUpdated(1));
        assertThrows(IllegalStateException.class, () -> TencentAi3dService.requireUpdated(0));
    }

    @Test
    void refundsOnlyBeforeDurableStateIsReached() {
        assertTrue(AiModelWorkPolicy.shouldRefundGeneration(false));
        assertFalse(AiModelWorkPolicy.shouldRefundGeneration(true));
        assertTrue(AiModelWorkPolicy.shouldRefundPersistence(true, false));
        assertFalse(AiModelWorkPolicy.shouldRefundPersistence(true, true));
        assertFalse(AiModelWorkPolicy.shouldRefundPersistence(false, false));
    }

    @Test
    void resourceShortageMessageDoesNotExposeProviderDiagnostics() {
        assertEquals(
                "当前生成服务较为繁忙，请稍后再试。",
                TencentAi3dService.toPublicServiceMessage("ResourceInsufficient", "资源不足")
        );
        assertEquals(
                "当前生成服务暂不可用，请稍后再试。",
                TencentAi3dService.toPublicServiceMessage("AuthFailure.SignatureFailure", "签名错误")
        );
    }

    @Test
    void convertsTencentPreviewPngOrJpegToWebpAtDeliveryTime() {
        assertEquals(
                "https://demo.cos.ap-guangzhou.tencentcos.cn/model.png?imageMogr2/format/webp/ignore-error/1",
                TencentAi3dService.toWebpPreviewUrl("https://demo.cos.ap-guangzhou.tencentcos.cn/model.png")
        );
        assertEquals(
                "https://demo.cos.ap-guangzhou.tencentcos.cn/model.jpeg?token=abc&imageMogr2/format/webp/ignore-error/1",
                TencentAi3dService.toWebpPreviewUrl("https://demo.cos.ap-guangzhou.tencentcos.cn/model.jpeg?token=abc")
        );
        assertEquals(
                "https://example.com/model.png",
                TencentAi3dService.toWebpPreviewUrl("https://example.com/model.png")
        );
        assertEquals(
                "https://demo.cos.ap-guangzhou.tencentcos.cn/model.glb",
                TencentAi3dService.toWebpPreviewUrl("https://demo.cos.ap-guangzhou.tencentcos.cn/model.glb")
        );
    }

    @Test
    void permanentWorkNeverReturnsExpiredTemporaryCover() {
        assertEquals(
                "/青花梅瓶.webp",
                TencentAi3dService.resolveWorkCoverUrl(
                        "PERMANENT",
                        "https://demo.cos.ap-guangzhou.tencentcos.cn/preview.png?expired=1"
                )
        );
        assertEquals(
                "https://demo.oss-cn-shenzhen.aliyuncs.com/project-media/models/ai3d/7/job/cover.webp",
                TencentAi3dService.resolveWorkCoverUrl(
                        "PERMANENT",
                        "https://demo.oss-cn-shenzhen.aliyuncs.com/project-media/models/ai3d/7/job/cover.webp"
                )
        );
    }

    @Test
    void remoteAssetUrlRequiresHttpsAndExactTencentDomainBoundary() {
        assertTrue(TencentAi3dService.isTrustedTencentAssetUrl(
                "https://demo.cos.ap-guangzhou.myqcloud.com/model.glb"
        ));
        assertTrue(TencentAi3dService.isTrustedTencentAssetUrl(
                "https://demo.tencentcos.cn/model.glb"
        ));
        assertFalse(TencentAi3dService.isTrustedTencentAssetUrl(
                "http://demo.tencentcos.cn/model.glb"
        ));
        assertFalse(TencentAi3dService.isTrustedTencentAssetUrl(
                "https://eviltencentcos.cn/model.glb"
        ));
        assertFalse(TencentAi3dService.isTrustedTencentAssetUrl(
                "https://example.com/model.glb"
        ));
    }

    @Test
    void remoteAssetReaderRejectsContentBeyondLimit() {
        assertThrows(
                IOException.class,
                () -> TencentAi3dService.readAtMost(new ByteArrayInputStream(new byte[4]), 3)
        );
    }
}
