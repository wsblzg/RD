package com.example.service.impl;

import com.example.service.RagClient;
import com.example.service.RagClient.RagResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void imageDescriptionCapsConfidenceWhenNvidiaSaysImageIsNotCeramic() throws Exception {
        AIServiceImpl service = newService();

        String result = service.parseNvidiaVisionResponse(
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"summary\\\":\\\"普通风景照\\\",\\\"confidence\\\":0.95,\\\"isCeramic\\\":false,\\\"isWoodFiredCeramic\\\":false,\\\"features\\\":[{\\\"label\\\":\\\"主体\\\",\\\"value\\\":\\\"非陶瓷\\\",\\\"evidence\\\":\\\"没有器型\\\"}]}\"}}]}"
        );

        JsonNode json = objectMapper.readTree(result);
        assertEquals("api", json.get("source").asText());
        assertFalse(json.get("isCeramic").asBoolean());
        assertFalse(json.get("isWoodFiredCeramic").asBoolean());
        assertTrue(json.get("confidence").asDouble() <= 0.34);
    }

    @Test
    void imageDescriptionCapsConfidenceWhenNvidiaCannotConfirmWoodFiredCeramic() throws Exception {
        AIServiceImpl service = newService();

        String result = service.parseNvidiaVisionResponse(
                "{\"choices\":[{\"message\":{\"content\":\"```json\\n{\\\"summary\\\":\\\"普通陶瓷器物\\\",\\\"confidence\\\":0.92,\\\"is_ceramic\\\":true,\\\"is_wood_fired_ceramic\\\":false,\\\"features\\\":[]}\\n```\"}}]}"
        );

        JsonNode json = objectMapper.readTree(result);
        assertTrue(json.get("isCeramic").asBoolean());
        assertFalse(json.get("isWoodFiredCeramic").asBoolean());
        assertTrue(json.get("confidence").asDouble() <= 0.65);
    }

    @Test
    void imageDescriptionFallsBackToLowConfidenceWhenNvidiaKeyIsMissing() throws Exception {
        AIServiceImpl service = newService();
        ReflectionTestUtils.setField(service, "nvidiaApiKey", "");

        String result = service.generateImageDescriptionFromBase64("ZmFrZQ==");

        JsonNode json = objectMapper.readTree(result);
        assertEquals("fallback", json.get("source").asText());
        assertEquals(0.12, json.get("confidence").asDouble());
    }

    @Test
    void ceramicPromptOptimizationDoesNotWrapAnOptimizedPromptAgain() {
        AIServiceImpl service = newService();
        ReflectionTestUtils.setField(service, "deepseekApiKey", "");

        String first = service.optimizeCeramicPrompt(
                "适合送礼的青花梅瓶，瓶身有山水纹样，整体端庄但不厚重",
                "青花瓷",
                "花瓶"
        );
        String second = service.optimizeCeramicPrompt(first, "青花瓷", "花瓶");
        String repeated = "请生成一件青花瓷花瓶，主题为“" + first
                + "”。要求器型比例协调，表面保留手作痕迹，火痕、落灰与釉色变化清晰，适合在线 3D 展示。";
        String cleaned = service.optimizeCeramicPrompt(repeated, "青花瓷", "花瓶");

        assertEquals(first, second);
        assertEquals(first, cleaned);
        assertEquals(first.indexOf("请生成一件"), first.lastIndexOf("请生成一件"));
    }

    private AIServiceImpl newService() {
        AIServiceImpl service = new AIServiceImpl(
                (question, example) -> RagResponse.unavailable("not used"),
                new RestTemplate()
        );
        ReflectionTestUtils.setField(service, "nvidiaApiUrl", "https://integrate.api.nvidia.com/v1");
        ReflectionTestUtils.setField(service, "nvidiaApiKey", "nvapi-test");
        ReflectionTestUtils.setField(service, "nvidiaVisionModel", "nvidia/nemotron-nano-12b-v2-vl");
        ReflectionTestUtils.setField(service, "nvidiaVisionTimeoutMs", 60000);
        return service;
    }
}
