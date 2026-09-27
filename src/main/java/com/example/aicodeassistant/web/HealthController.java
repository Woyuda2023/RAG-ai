package com.example.aicodeassistant.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查：云端大模型 API（OpenAI 兼容）与 MySQL 连接状态。
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${llm.base-url:https://api.deepseek.com}")
    private String llmBaseUrl;

    @Value("${llm.api-key:}")
    private String llmApiKey;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("llm", llmStatus());
        body.put("mysql", mysqlStatus());
        return body;
    }

    /**
     * 检查云端 API 连通性：请求 {base-url}/models。
     * 200（成功）与 401/403（鉴权失败但服务可达）均视为服务在线。
     */
    private String llmStatus() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(llmBaseUrl + "/models"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + llmApiKey)
                    .GET();
            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code == 200 || code == 401 || code == 403) {
                return "UP (" + llmBaseUrl + ", HTTP " + code + ")";
            }
            return "DOWN (HTTP " + code + ")";
        } catch (Exception e) {
            log.warn("云端大模型 API 健康检查失败: {}", e.getMessage());
            return "DOWN (" + e.getMessage() + ")";
        }
    }

    private String mysqlStatus() {
        try {
            List<String> result = jdbcTemplate.queryForList("SELECT 1", String.class);
            return result != null && !result.isEmpty() ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN (" + e.getMessage() + ")";
        }
    }
}
