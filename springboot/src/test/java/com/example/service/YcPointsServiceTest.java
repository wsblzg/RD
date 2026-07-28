package com.example.service;

import com.example.entity.YcAiModelWork;
import com.example.entity.YcUserAccount;
import com.example.mapper.YcCollectibleMapper;
import com.example.mapper.YcPointsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YcPointsServiceTest {

    private final YcCollectibleMapper collectibleMapper = mock(YcCollectibleMapper.class);
    private final YcPointsMapper pointsMapper = mock(YcPointsMapper.class);
    private final YcPointsService service = new YcPointsService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ycCollectibleMapper", collectibleMapper);
        ReflectionTestUtils.setField(service, "ycPointsMapper", pointsMapper);
    }

    @Test
    void generationRefundHappensOnlyAfterStateClaimSucceeds() {
        YcUserAccount user = user(7L);
        YcAiModelWork work = work(9L, 7L);
        work.setGenerationChargeStatus("CHARGED");
        work.setGenerationPointsCost(10);
        when(collectibleMapper.failAiModelGeneration(9L, 7L, "生成失败", "REFUNDED")).thenReturn(0);

        assertFalse(service.refundFailedAi3dGeneration(user, work, "生成失败"));

        verify(pointsMapper, never()).refundUserPoints(7L, 10);
    }

    @Test
    void stalePersistenceRefundHappensOnlyAfterStateClaimSucceeds() {
        YcAiModelWork work = work(9L, 7L);
        work.setPersistChargeStatus("CHARGED");
        LocalDateTime cutoff = LocalDateTime.of(2026, 7, 16, 10, 0);
        when(collectibleMapper.failStaleAiModelPersist(
                9L, 7L, cutoff, "REFUNDED", "永久保存超时，已自动恢复"
        )).thenReturn(1);

        assertTrue(service.recoverStaleAi3dPersist(work, cutoff));

        verify(pointsMapper).refundUserPoints(7L, 10);
    }

    private static YcUserAccount user(Long id) {
        YcUserAccount user = new YcUserAccount();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setPointsIsUnlimited(0);
        return user;
    }

    private static YcAiModelWork work(Long id, Long userId) {
        YcAiModelWork work = new YcAiModelWork();
        work.setId(id);
        work.setUserId(userId);
        return work;
    }
}
