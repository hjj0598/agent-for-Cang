package com.example.demo.service;

import com.example.demo.dto.CangjieMemoryGuardResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
public class CangjieMemoryGuardService {

    private static final Logger log = LoggerFactory.getLogger(CangjieMemoryGuardService.class);
    private static final int DEFAULT_TIMEOUT_MS = 800;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${app.cangjie.memory-guard.enabled:false}")
    private boolean enabled;

    @Value("${app.cangjie.memory-guard.url:http://localhost:9001/analyze-memory}")
    private String url;

    @Value("${app.cangjie.memory-guard.timeout-ms:800}")
    private Integer timeoutMs;

    public CangjieMemoryGuardResult analyze(String content) {
        if (!enabled || !hasText(url) || !hasText(content)) {
            return null;
        }

        try {
            String body = objectMapper.writeValueAsString(Map.of("content", content.trim()));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url.trim()))
                    .timeout(Duration.ofMillis(safeTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(safeTimeoutMs()))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("仓颉候选记忆规则检测返回非成功状态: {}", response.statusCode());
                return null;
            }

            return parseResult(response.body());
        } catch (Exception e) {
            log.warn("仓颉候选记忆规则检测不可用，已降级为原有 Java 逻辑: {}", e.getMessage());
            return null;
        }
    }

    private CangjieMemoryGuardResult parseResult(String responseBody) throws Exception {
        if (!hasText(responseBody)) {
            return null;
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode payload = root.has("data") && !root.get("data").isNull()
                ? root.get("data")
                : root;

        CangjieMemoryGuardResult result = objectMapper.treeToValue(payload, CangjieMemoryGuardResult.class);
        if (result == null || (!hasText(result.getMemoryType()) && !hasText(result.getTrustLevel())
                && !hasText(result.getFactType()) && !hasText(result.getReason()))) {
            return null;
        }
        return result;
    }

    private int safeTimeoutMs() {
        if (timeoutMs == null || timeoutMs < 100) {
            return DEFAULT_TIMEOUT_MS;
        }
        return Math.min(timeoutMs, 5000);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
