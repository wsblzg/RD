package com.example.service.impl;

import com.example.service.RagClient;
import com.example.service.RagClient.RagResponse;
import com.example.service.RagClient.RagSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class RagClientImpl implements RagClient {

    private static final Logger log = LoggerFactory.getLogger(RagClientImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String queryPath;

    @Autowired
    public RagClientImpl(RestTemplateBuilder builder,
                         @Value("${ai.rag.base-url:http://127.0.0.1:17690}") String baseUrl,
                         @Value("${ai.rag.query-path:/query}") String queryPath,
                         @Value("${ai.rag.timeout-ms:25000}") int timeoutMs) {
        this(
                builder
                        .setConnectTimeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                        .setReadTimeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                        .build(),
                baseUrl,
                queryPath
        );
    }

    RagClientImpl(RestTemplate restTemplate, String baseUrl, String queryPath) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.baseUrl = trimRightSlash(baseUrl);
        this.queryPath = normalizePath(queryPath);
    }

    @Override
    public RagResponse query(String question, String example) {
        if (question == null || question.trim().isEmpty()) {
            return RagResponse.unavailable("RAG服务暂时不可用：问题不能为空");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("query", question.trim());
            if (example != null && !example.trim().isEmpty()) {
                body.add("example", example.trim());
            }

            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + queryPath).build(true).toUri();
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("RAG service returned non-2xx status: {}", response.getStatusCodeValue());
                return RagResponse.unavailable("RAG服务暂时不可用：HTTP " + response.getStatusCodeValue());
            }

            return parseResponse(response.getBody());
        } catch (RestClientException e) {
            log.warn("RAG service request failed: {}", e.getMessage());
            return RagResponse.unavailable("RAG服务暂时不可用：" + e.getMessage());
        } catch (Exception e) {
            log.warn("RAG service response parse failed: {}", e.getMessage());
            return RagResponse.unavailable("RAG服务暂时不可用：" + e.getMessage());
        }
    }

    private RagResponse parseResponse(String body) throws Exception {
        if (body == null || body.trim().isEmpty()) {
            return RagResponse.unavailable("RAG服务暂时不可用：响应为空");
        }

        JsonNode root = objectMapper.readTree(body);
        if (root.hasNonNull("error")) {
            return RagResponse.unavailable("RAG服务暂时不可用：" + root.path("error").asText());
        }

        String answer = root.path("answer").asText("");
        if (answer.trim().isEmpty()) {
            return RagResponse.unavailable("RAG服务暂时不可用：回答为空");
        }

        List<RagSource> sources = new ArrayList<>();
        JsonNode sourcesNode = root.path("sources");
        if (sourcesNode.isArray()) {
            for (JsonNode sourceNode : sourcesNode) {
                sources.add(new RagSource(
                        sourceNode.path("path").asText(""),
                        sourceNode.path("content").asText("")
                ));
            }
        }

        return RagResponse.available(answer, sources);
    }

    private static String trimRightSlash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "http://127.0.0.1:17690";
        }
        return value.trim().replaceAll("/+$", "");
    }

    private static String normalizePath(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "/query";
        }
        String path = value.trim();
        return path.startsWith("/") ? path : "/" + path;
    }
}
