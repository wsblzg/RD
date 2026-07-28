package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
public class XilingTokenService {

    private static final int MAX_EXPIRE_HOURS = 168;

    private final String appId;
    private final String appKey;
    private final Clock clock;

    @Autowired
    public XilingTokenService(
            @Value("${xiling.app-id:${XILING_APP_ID:}}") String appId,
            @Value("${xiling.app-key:${XILING_APP_KEY:}}") String appKey
    ) {
        this(appId, appKey, Clock.systemUTC());
    }

    XilingTokenService(String appId, String appKey, Clock clock) {
        this.appId = appId == null ? "" : appId.trim();
        this.appKey = appKey == null ? "" : appKey.trim();
        this.clock = clock;
    }

    public String generateToken(int expireHours) {
        if (appId.isEmpty() || appKey.isEmpty()) {
            throw new IllegalStateException(
                    "曦灵动态令牌未配置，请设置 XILING_APP_ID 和 XILING_APP_KEY"
            );
        }
        if (expireHours < 1 || expireHours > MAX_EXPIRE_HOURS) {
            throw new IllegalArgumentException("expireHours 必须在 1 到 168 之间");
        }

        Instant expiresAt = clock.instant().plus(expireHours, ChronoUnit.HOURS);
        String expireTime = DateTimeFormatter.ISO_INSTANT.format(expiresAt);
        String signature = hmacSha256Hex(appKey, appId + expireTime);
        return appId + "/" + signature + "/" + expireTime;
    }

    private String hmacSha256Hex(String key, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception error) {
            throw new IllegalStateException("曦灵动态令牌签名失败", error);
        }
    }
}
