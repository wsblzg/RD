package com.example.service;

import com.example.dto.YcCaptchaVerifyDTO;
import com.example.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginCaptchaServiceTest {

    @Test
    void captchaTokenIsBoundToIpAndCanOnlyBeUsedOnce() {
        LoginCaptchaService service = new LoginCaptchaService();
        Map<String, Object> challenge = service.createChallenge("127.0.0.1");

        YcCaptchaVerifyDTO dto = new YcCaptchaVerifyDTO();
        dto.setChallengeId((String) challenge.get("challengeId"));
        dto.setTarget((String) challenge.get("target"));
        dto.setElapsedMs(1500L);

        String token = (String) service.verify(dto, "127.0.0.1").get("captchaToken");

        assertThrows(CustomException.class, () -> service.consume(token, "10.0.0.2"));
        assertTrue(service.consume(token, "127.0.0.1"));
        assertThrows(CustomException.class, () -> service.consume(token, "127.0.0.1"));
    }
}
