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
 * 健康检查：Ollama 服务与 MySQL 连接状态。
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${langchain4j.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ollama", ollamaStatus());
        body.put("mysql", mysqlStatus());
        return body;
    }

    private String ollamaStatus() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return "UP (" + ollamaBaseUrl + ")";
            }
            return "DOWN (HTTP " + response.statusCode() + ")";
        } catch (Exception e) {
            log.warn("Ollama 健康检查失败: {}", e.getMessage());
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
