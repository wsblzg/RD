package com.example.service;

import java.time.Duration;
import java.time.LocalDateTime;

public final class AiModelWorkPolicy {
    public static final Duration FIRST_QUERY_DELAY = Duration.ofMinutes(5);
    public static final Duration TEMPORARY_LIFETIME = Duration.ofHours(24);
    public static final Duration PERSIST_SAFETY_WINDOW = Duration.ofMinutes(10);

    private AiModelWorkPolicy() {
    }

    public static boolean canQuery(LocalDateTime submittedAt, LocalDateTime now) {
        return submittedAt != null && now != null && !now.isBefore(submittedAt.plus(FIRST_QUERY_DELAY));
    }

    public static boolean isTemporaryUsable(String storageStatus, LocalDateTime expiresAt, LocalDateTime now) {
        return "TEMPORARY".equals(storageStatus)
                && expiresAt != null
                && now != null
                && now.isBefore(expiresAt);
    }

    public static boolean canStartPersist(String storageStatus, String chargeStatus) {
        if ("TEMPORARY".equals(storageStatus)) {
            return "NONE".equals(chargeStatus) || "REFUNDED".equals(chargeStatus);
        }
        return "PERSIST_FAILED".equals(storageStatus)
                && ("REFUNDED".equals(chargeStatus) || "FREE".equals(chargeStatus));
    }

    public static boolean canPersistUntil(LocalDateTime expiresAt, LocalDateTime now) {
        return expiresAt != null
                && now != null
                && now.isBefore(expiresAt.minus(PERSIST_SAFETY_WINDOW));
    }

    public static boolean canPublish(String storageStatus) {
        return "PERMANENT".equals(storageStatus);
    }

    public static boolean shouldDeleteExpiredTemporary(String generationStatus,
                                                       String storageStatus,
                                                       LocalDateTime expiresAt,
                                                       LocalDateTime now) {
        boolean unsaved = "TEMPORARY".equals(storageStatus) || "PERSIST_FAILED".equals(storageStatus);
        return "READY".equals(generationStatus)
                && unsaved
                && expiresAt != null
                && now != null
                && !expiresAt.isAfter(now);
    }

    public static boolean shouldRefundGeneration(boolean taskRecorded) {
        return !taskRecorded;
    }

    public static boolean shouldRefundPersistence(boolean persistStarted, boolean persistCompleted) {
        return persistStarted && !persistCompleted;
    }
}
