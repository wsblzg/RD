package com.example.service.impl;

import com.example.service.RagClient.RagResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagClientImplTest {

    @Test
    void queryReturnsAnswerAndSourcesWhenRagServiceSucceeds() {
        FakeRestTemplate restTemplate = new FakeRestTemplate(
                200,
                "{\"answer\":\"柴烧落灰会形成自然灰釉层。\",\"sources\":[{\"path\":\"docs/火痕落灰.txt\",\"content\":\"落灰说明\"}]}"
        );
        RagClientImpl client = new RagClientImpl(restTemplate, "http://127.0.0.1:17690", "/query");

        RagResponse response = client.query("什么是柴烧落灰", "按结论和说明回答");

        assertTrue(response.isAvailable());
        assertEquals("柴烧落灰会形成自然灰釉层。", response.getAnswer());
        assertEquals(1, response.getSources().size());
        assertEquals("docs/火痕落灰.txt", response.getSources().get(0).getPath());
        assertEquals(
                "query=%E4%BB%80%E4%B9%88%E6%98%AF%E6%9F%B4%E7%83%A7%E8%90%BD%E7%81%B0"
                        + "&example=%E6%8C%89%E7%BB%93%E8%AE%BA%E5%92%8C%E8%AF%B4%E6%98%8E%E5%9B%9E%E7%AD%94",
                restTemplate.lastBody
        );
    }

    @Test
    void queryReturnsUnavailableResponseWhenRagServiceFails() {
        FakeRestTemplate restTemplate = new FakeRestTemplate(500, "{\"error\":\"boom\"}");
        RagClientImpl client = new RagClientImpl(restTemplate, "http://127.0.0.1:17690", "/query");

        RagResponse response = client.query("什么是柴烧落灰", null);

        assertFalse(response.isAvailable());
        assertTrue(response.getAnswer().contains("RAG服务暂时不可用"));
    }

    private static final class FakeRestTemplate extends RestTemplate {
        private final int status;
        private final String responseBody;
        private String lastBody;

        private FakeRestTemplate(int status, String responseBody) {
            this.status = status;
            this.responseBody = responseBody;
        }

        @Override
        public <T> T execute(URI url, HttpMethod method, RequestCallback requestCallback,
                             ResponseExtractor<T> responseExtractor) {
            try {
                MockClientHttpRequest request = new MockClientHttpRequest(method, url);
                request.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                if (requestCallback != null) {
                    requestCallback.doWithRequest(request);
                }
                this.lastBody = new String(request.getBodyAsBytes(), StandardCharsets.UTF_8);
                MockClientHttpResponse response = new MockClientHttpResponse(
                        responseBody.getBytes(StandardCharsets.UTF_8),
                        HttpStatus.valueOf(status)
                );
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return responseExtractor.extractData(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
