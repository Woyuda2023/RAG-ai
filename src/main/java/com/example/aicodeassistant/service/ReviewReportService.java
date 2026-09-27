package com.example.aicodeassistant.service;

import com.example.aicodeassistant.agent.AgentResult;
import com.example.aicodeassistant.agent.ToolInvocation;
import com.example.aicodeassistant.entity.ReviewReport;
import com.example.aicodeassistant.repository.ReviewReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码审查报告服务：将 CodeReviewTool 输出的 JSON 审查报告解析并落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewReportService {

    private final ReviewReportRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 从一次 Agent 执行结果中提取审查报告并落库。
     *
     * @return 落库的审查问题条数
     */
    @Transactional
    public int saveFromAgentResult(Long taskId, AgentResult result) {
        if (taskId == null || result == null || result.invocations() == null) {
            return 0;
        }
        int saved = 0;
        for (ToolInvocation invocation : result.invocations()) {
            if ("reviewCode".equals(invocation.name()) && invocation.success()) {
                saved += saveFromJson(taskId, invocation.resultJson());
            }
        }
        return saved;
    }

    /**
     * 解析审查 JSON（结构：{"summary":"...","issues":[{severity,line,type,description,suggestion}]}）并落库。
     */
    @Transactional
    public int saveFromJson(Long taskId, String reviewJson) {
        if (reviewJson == null || reviewJson.isBlank()) {
            return 0;
        }
        try {
            JsonNode root = objectMapper.readTree(reviewJson);
            JsonNode issues = root.get("issues");
            if (issues == null || !issues.isArray()) {
                return 0;
            }
            int saved = 0;
            for (JsonNode issue : issues) {
                ReviewReport report = ReviewReport.builder()
                        .taskId(taskId)
                        .fileName(text(issue, "file"))
                        .severity(text(issue, "severity"))
                        .issueType(text(issue, "type"))
                        .lineNumber(issue.hasNonNull("line") && issue.get("line").canConvertToInt()
                                ? issue.get("line").asInt() : -1)
                        .description(text(issue, "description"))
                        .suggestion(text(issue, "suggestion"))
                        .createdAt(LocalDateTime.now())
                        .build();
                repository.save(report);
                saved++;
            }
            return saved;
        } catch (Exception e) {
            log.warn("审查报告解析落库失败: {}", e.getMessage());
            return 0;
        }
    }

    public List<ReviewReport> findByTaskId(Long taskId) {
        return repository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String s = value.asText();
        return s.isBlank() ? null : s;
    }
}
