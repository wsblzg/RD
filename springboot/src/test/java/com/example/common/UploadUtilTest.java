package com.example.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadUtilTest {

    @Test
    void validateAi3dModelSizeRejectsOversizedFile() {
        assertDoesNotThrow(() -> UploadUtil.validateAi3dModelSize(100L * 1024 * 1024));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UploadUtil.validateAi3dModelSize(100L * 1024 * 1024 + 1)
        );

        assertTrue(exception.getMessage().contains("100MB"));
    }

    @Test
    void ai3dObjectNameIsStableAndSafe() {
        assertEquals(
                "project-media/models/ai3d/7/job-20260716/model.glb",
                UploadUtil.buildAi3dObjectName(7L, "job/20260716")
        );
    }

    @Test
    void ai3dCoverObjectNameIsStableAndSafe() {
        assertEquals(
                "project-media/models/ai3d/7/job-20260716/cover.webp",
                UploadUtil.buildAi3dCoverObjectName(7L, "job/20260716")
        );
    }

    @Test
    void ai3dCoverMustBeWebp() {
        byte[] webp = new byte[] {
                'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'
        };
        UploadUtil.validateAi3dCoverBytes(webp);
        assertThrows(
                IllegalArgumentException.class,
                () -> UploadUtil.validateAi3dCoverBytes(new byte[] { 'P', 'N', 'G' })
        );
    }
}
