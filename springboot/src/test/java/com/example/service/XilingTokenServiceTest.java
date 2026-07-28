package com.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XilingTokenServiceTest {

    @Test
    void springSelectsTheConfiguredProductionConstructor() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    context,
                    "xiling.app-id=context-app-id",
                    "xiling.app-key=context-app-key"
            );
            context.register(XilingTokenService.class);
            context.refresh();

            assertNotNull(context.getBean(XilingTokenService.class));
        }
    }

    @Test
    void generatesDeterministicHmacTokenFromConfiguredCredentials() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC);
        XilingTokenService service = new XilingTokenService("app-id", "secret", clock);

        String token = service.generateToken(24);

        assertEquals(
                "app-id/8bbfe094fde120f839457666d33c2066817cc2fcf1a48c4964c2a0b6ea97444a/2026-07-25T00:00:00Z",
                token
        );
    }

    @Test
    void rejectsMissingCredentialsAndInvalidExpiry() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC);

        assertThrows(
                IllegalStateException.class,
                () -> new XilingTokenService("", "", clock).generateToken(24)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new XilingTokenService("app-id", "secret", clock).generateToken(0)
        );
    }
}
