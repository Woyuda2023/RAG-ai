package com.example.aicodeassistant.tool;

import com.example.aicodeassistant.security.InputSanitizer;
import com.example.aicodeassistant.validation.ToolResultValidator;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具四：代码审查。
 *
 * <p>通过 LLM 对代码做结构化审查，强制模型输出 JSON 报告，并做 JSON 格式校验；
 * 审查报告在编排层统一落库（见 ReviewReportService）。</p>
 */
@Component
@RequiredArgsConstructor
public class CodeReviewTool {

    private final ChatLanguageModel chatLanguageModel;
    private final InputSanitizer inputSanitizer;
    private final ToolResultValidator validator;

    @Tool("对指定代码进行审查，返回 JSON 格式审查报告：{\"summary\":\"总体结论\",\"issues\":[{\"severity\":\"CRITICAL|MAJOR|MINOR|INFO\",\"line\":行号或-1,\"type\":\"问题类型\",\"description\":\"问题描述\",\"suggestion\":\"修改建议\"}]}")
    public ToolResult reviewCode(
            @P("待审查的代码内容") String code,
            @P("代码语言，例如 Java / Python / SQL") String language,
            @P("审查重点（可选），例如 安全性、性能、并发、代码规范，多个用逗号分隔") String focus) {
        String sanitizedCode = inputSanitizer.sanitize(code);
        if (sanitizedCode.isBlank()) {
            return ToolResult.fail("代码为空，无法审查");
        }
        String lang = (language == null || language.isBlank()) ? "未知" : language.trim();
        String focusText = (focus == null || focus.isBlank()) ? "正确性、安全、性能、可读性" : focus.trim();

        String prompt = "请审查以下代码。\n语言: " + lang + "\n审查重点: " + focusText + "\n"
                + "代码:\n```\n" + sanitizedCode + "\n```\n"
                + "要求：只输出 JSON，不要输出其他任何内容。JSON 结构："
                + "{\"summary\":\"总体结论与亮点\",\"issues\":[{\"severity\":\"CRITICAL|MAJOR|MINOR|INFO\","
                + "\"line\":行号(无法定位填-1),\"type\":\"问题类型\",\"description\":\"问题描述\",\"suggestion\":\"修改建议\"}]}";
        try {
            var response = chatLanguageModel.generate(
                    SystemMessage.from("你是资深代码审查专家。只输出 JSON，不要输出任何解释。"),
                    UserMessage.from(prompt));
            String raw = response.content().text();
            String json = extractJson(raw);
            ToolResultValidator.ValidationResult check = validator.validateJson(json);
            if (!check.valid()) {
                return ToolResult.fail("审查结果 JSON 格式校验失败: " + check.error());
            }
            int issueCount = 0;
            JsonNode issues = check.tree().get("issues");
            if (issues != null && issues.isArray()) {
                issueCount = issues.size();
            }
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("language", lang);
            meta.put("issueCount", issueCount);
            meta.put("rawLength", raw == null ? 0 : raw.length());
            return ToolResult.ok(json, meta);
        } catch (Exception e) {
            return ToolResult.fail("代码审查失败: " + e.getMessage());
        }
    }

    /** 从模型输出中提取 JSON 对象：优先取 ```json 代码块，否则取首尾花括号之间的内容。 */
    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim();
        // 去掉 ```json 围栏
        trimmed = trimmed.replaceAll("(?s)^```(json)?\\s*", "").replaceAll("(?s)\\s*```$", "");
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
