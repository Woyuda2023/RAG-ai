package com.example.aicodeassistant.service;

import com.example.aicodeassistant.agent.AgentResult;
import com.example.aicodeassistant.agent.ToolInvocation;
import com.example.aicodeassistant.entity.ReviewReport;
import com.example.aicodeassistant.mapper.ReviewReportMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码审查报告服务：将 CodeReviewTool 输出的 JSON 审查报告解析并落库（MyBatis）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewReportService {

    private final ReviewReportMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * 从一次 Agent 执行结果中提取审查报告并落库。
     * 工具调用记录中的 resultJson 为统一包装结构 {"success":true,"data":"<审查JSON>","meta":{...}}，
     * 需先取出 data 字段中的审查 JSON 再解析。
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
                saved += saveFromWrappedJson(taskId, invocation.resultJson());
            }
        }
        return saved;
    }

    /** 从统一包装结构中提取 data 字段（审查 JSON）并落库 */
    private int saveFromWrappedJson(Long taskId, String wrappedJson) {
        if (wrappedJson == null || wrappedJson.isBlank()) {
            return 0;
        }
        try {
            JsonNode wrapper = objectMapper.readTree(wrappedJson);
            JsonNode dataNode = wrapper.get("data");
            if (dataNode == null || !dataNode.isTextual() || dataNode.asText().isBlank()) {
                return 0;
            }
            return saveFromJson(taskId, dataNode.asText());
        } catch (Exception e) {
            log.warn("审查报告包装结构解析失败: {}", e.getMessage());
            return 0;
        }
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
                mapper.insert(report);
                saved++;
            }
            return saved;
        } catch (Exception e) {
            log.warn("审查报告解析落库失败: {}", e.getMessage());
            return 0;
        }
    }

    public List<ReviewReport> findByTaskId(Long taskId) {
        return mapper.findByTaskId(taskId);
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
