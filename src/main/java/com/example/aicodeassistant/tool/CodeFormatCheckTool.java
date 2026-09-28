package com.example.aicodeassistant.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具六：代码格式校验。
 *
 * <p>基于规则的启发式格式检查：行尾空格、Tab/空格缩进混用、超长行、连续空行、
 * 缺少文件结尾换行、Java 通配符导入等。不调用 LLM，结果确定、可校验，返回 JSON 报告。</p>
 */
@Component
@RequiredArgsConstructor
public class CodeFormatCheckTool {

    private static final int MAX_LINE_LENGTH = 120;
    private static final int MAX_INPUT_CHARS = 8000;

    private final ObjectMapper objectMapper;

    @Tool("对代码做格式校验，返回 JSON 格式报告：{\"formatted\":bool,\"issueCount\":n,\"issues\":[{\"type\":\"问题类型\",\"line\":行号,\"message\":\"问题描述\"}]}")
    public ToolResult formatCheck(
            @P("待校验的代码内容") String code,
            @P("代码语言，例如 Java / Python / Go（用于判定缩进风格）") String language) {
        // 格式校验必须保留换行信息，因此不做全量 sanitize（会把换行替换为空格），
        // 仅去除危险控制字符并限制长度，防止超长内容进入上下文。
        String sanitized = cleanForInspection(code);
        if (sanitized.isBlank()) {
            return ToolResult.fail("代码为空，无法校验");
        }

        List<Map<String, Object>> issues = new ArrayList<>();
        String[] lines = sanitized.split("\n", -1);
        boolean seenTab = false;
        boolean seenSpace = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNo = i + 1;

            // 行尾空格
            if (line.length() != line.replaceAll("[ \t]+$", "").length()) {
                issues.add(issue("trailing-whitespace", lineNo, "行尾存在多余空格"));
            }
            // 超长行
            if (line.length() > MAX_LINE_LENGTH) {
                issues.add(issue("line-too-long", lineNo, "行长度 " + line.length() + " 超过 " + MAX_LINE_LENGTH));
            }
            // 缩进风格混用检测
            if (line.startsWith("\t")) {
                seenTab = true;
            }
            if (line.matches("^( {2,}).*") && line.charAt(0) == ' ') {
                seenSpace = true;
            }
            // Java 通配符导入
            if (line.trim().matches("^import\\s+[\\w.*]+\\s*;\\s*$") && line.contains(".*")) {
                issues.add(issue("wildcard-import", lineNo, "禁止通配符导入（import xxx.*）"));
            }
        }

        if (seenTab && seenSpace) {
            issues.add(issue("mixed-indent", -1, "代码同时使用 Tab 与空格缩进，请统一"));
        }

        // 连续空行（超过 2 行）
        int blankRun = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                blankRun++;
                if (blankRun > 2) {
                    issues.add(issue("blank-lines", i + 1, "连续空行超过 2 行"));
                    blankRun = 1;
                }
            } else {
                blankRun = 0;
            }
        }

        // 文件结尾缺换行
        if (sanitized.endsWith("\n") == false && sanitized.length() > 0) {
            issues.add(issue("no-final-newline", lines.length, "文件末尾缺少换行符"));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("formatted", issues.isEmpty());
        body.put("issueCount", issues.size());
        body.put("issues", issues);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("language", language == null ? "" : language);
        meta.put("lineCount", lines.length);
        try {
            String json = objectMapper.writeValueAsString(body);
            return ToolResult.ok(json, meta);
        } catch (Exception e) {
            return ToolResult.fail("格式校验结果序列化失败: " + e.getMessage());
        }
    }

    /** 清洗用于格式检查的代码：去除危险控制字符（保留 \n / \t）并限制长度 */
    private String cleanForInspection(String code) {
        if (code == null) {
            return "";
        }
        String cleaned = code.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ");
        if (cleaned.length() > MAX_INPUT_CHARS) {
            cleaned = cleaned.substring(0, MAX_INPUT_CHARS) + "\n[输入过长，已截断]";
        }
        return cleaned;
    }

    private Map<String, Object> issue(String type, int line, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("line", line);
        map.put("message", message);
        return map;
    }
}
