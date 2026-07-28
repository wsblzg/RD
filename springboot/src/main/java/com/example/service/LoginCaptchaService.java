package com.example.service;

import com.example.dto.YcCaptchaVerifyDTO;
import com.example.exception.CustomException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginCaptchaService {

    private static final long CHALLENGE_TTL_MS = 3 * 60 * 1000L;
    private static final long TOKEN_TTL_MS = 2 * 60 * 1000L;
    private static final long MIN_PLAY_MS = 1000L;
    private static final long FAILURE_WINDOW_MS = 10 * 60 * 1000L;
    private static final long BLOCK_MS = 15 * 60 * 1000L;
    private static final int MAX_FAILURES = 10;
    private static final String[] TARGETS = {
            "duck", "bear", "panda", "bunny", "dino", "penguin",
            "fox", "frog", "whale", "cat", "puppy", "unicorn"
    };
    private static final Set<String> TARGET_SET = Set.of(TARGETS);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final Map<String, CaptchaToken> tokens = new ConcurrentHashMap<>();
    private final Map<String, FailureBucket> failures = new ConcurrentHashMap<>();

    public Map<String, Object> createChallenge(String clientIp) {
        cleanup();
        ensureNotBlocked(clientIp);
        String target = TARGETS[random.nextInt(TARGETS.length)];
        String challengeId = UUID.randomUUID().toString();
        challenges.put(challengeId, new Challenge(target, clientIp, expiresAt(CHALLENGE_TTL_MS)));
        return Map.of(
                "challengeId", challengeId,
                "target", target,
                "expiresInSeconds", CHALLENGE_TTL_MS / 1000
        );
    }

    public Map<String, Object> verify(YcCaptchaVerifyDTO dto, String clientIp) {
        cleanup();
        ensureNotBlocked(clientIp);
        if (dto == null || isBlank(dto.getChallengeId()) || isBlank(dto.getTarget())) {
            recordFailure(clientIp);
            throw new CustomException("400", "验证码参数不能为空");
        }
        if (!TARGET_SET.contains(dto.getTarget())) {
            recordFailure(clientIp);
            throw new CustomException("400", "验证码目标无效");
        }

        Challenge challenge = challenges.remove(dto.getChallengeId());
        long now = now();
        if (challenge == null || challenge.expiresAt < now) {
            recordFailure(clientIp);
            throw new CustomException("401", "验证码已过期，请重试");
        }
        if (!challenge.clientIp.equals(clientIp) || !challenge.target.equals(dto.getTarget())) {
            recordFailure(clientIp);
            throw new CustomException("401", "验证码校验失败");
        }
        if (dto.getElapsedMs() == null || dto.getElapsedMs() < MIN_PLAY_MS) {
            recordFailure(clientIp);
            throw new CustomException("429", "操作过快，请重试");
        }

        String token = UUID.randomUUID().toString();
        tokens.put(token, new CaptchaToken(clientIp, expiresAt(TOKEN_TTL_MS)));
        return Map.of(
                "captchaToken", token,
                "expiresInSeconds", TOKEN_TTL_MS / 1000
        );
    }

    public boolean consume(String token, String clientIp) {
        cleanup();
        ensureNotBlocked(clientIp);
        if (isBlank(token)) {
            recordFailure(clientIp);
            throw new CustomException("401", "请先完成人机验证");
        }
        CaptchaToken captchaToken = tokens.get(token);
        if (captchaToken == null || captchaToken.expiresAt < now()) {
            tokens.remove(token);
            recordFailure(clientIp);
            throw new CustomException("401", "人机验证已过期，请重试");
        }
        if (!captchaToken.clientIp.equals(clientIp)) {
            recordFailure(clientIp);
            throw new CustomException("401", "人机验证来源不一致，请重试");
        }
        tokens.remove(token);
        failures.remove(clientIp);
        return true;
    }

    private void ensureNotBlocked(String clientIp) {
        FailureBucket bucket = failures.get(clientIp);
        if (bucket != null && bucket.blockedUntil > now()) {
            throw new CustomException("429", "验证失败次数过多，请稍后再试");
        }
    }

    private void recordFailure(String clientIp) {
        long now = now();
        failures.compute(clientIp, (key, bucket) -> {
            if (bucket == null || bucket.windowStart + FAILURE_WINDOW_MS < now) {
                return new FailureBucket(now, 1, 0);
            }
            int count = bucket.count + 1;
            long blockedUntil = count >= MAX_FAILURES ? now + BLOCK_MS : bucket.blockedUntil;
            return new FailureBucket(bucket.windowStart, count, blockedUntil);
        });
    }

    private void cleanup() {
        long now = now();
        challenges.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        tokens.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        failures.entrySet().removeIf(entry -> entry.getValue().blockedUntil < now
                && entry.getValue().windowStart + FAILURE_WINDOW_MS < now);
    }

    private long expiresAt(long ttlMs) {
        return now() + ttlMs;
    }

    private long now() {
        return Instant.now().toEpochMilli();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class Challenge {
        private final String target;
        private final String clientIp;
        private final long expiresAt;

        private Challenge(String target, String clientIp, long expiresAt) {
            this.target = target;
            this.clientIp = clientIp;
            this.expiresAt = expiresAt;
        }
    }

    private static class CaptchaToken {
        private final String clientIp;
        private final long expiresAt;

        private CaptchaToken(String clientIp, long expiresAt) {
            this.clientIp = clientIp;
            this.expiresAt = expiresAt;
        }
    }

    private static class FailureBucket {
        private final long windowStart;
        private final int count;
        private final long blockedUntil;

        private FailureBucket(long windowStart, int count, long blockedUntil) {
            this.windowStart = windowStart;
            this.count = count;
            this.blockedUntil = blockedUntil;
        }
    }
}
