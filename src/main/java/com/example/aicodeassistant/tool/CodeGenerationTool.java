package com.example.aicodeassistant.tool;

import com.example.aicodeassistant.security.InputSanitizer;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具三：代码生成。
 *
 * <p>通过 LLM 按结构化提示生成代码，输入经输入过滤清洗，输出从 Markdown 代码块中提取。</p>
 */
@Component
@RequiredArgsConstructor
public class CodeGenerationTool {

    private static final Pattern CODE_BLOCK = Pattern.compile("```[A-Za-z0-9_+.#-]*\\s*\\n(.*?)```", Pattern.DOTALL);

    private final ChatLanguageModel chatLanguageModel;
    private final InputSanitizer inputSanitizer;

    @Tool("根据需求生成代码。返回生成的代码（从代码块中提取的纯代码文本）。")
    public ToolResult generateCode(
            @P("代码需求描述，例如：用 Java 写一个读取 CSV 文件的工具类") String requirement,
            @P("目标编程语言，例如 Java / Python / SQL / Go") String language) {
        String sanitized = inputSanitizer.sanitize(requirement);
        if (sanitized.isBlank()) {
            return ToolResult.fail("需求描述为空");
        }
        String lang = (language == null || language.isBlank()) ? "Java" : language.trim();

        String prompt = "请根据以下需求生成完整、可直接运行的代码。\n"
                + "目标语言: " + lang + "\n"
                + "需求: " + sanitized + "\n"
                + "要求：只输出代码，代码放在 ```语言 代码块中，不要输出任何解释文字。";
        try {
            var response = chatLanguageModel.generate(
                    SystemMessage.from("你是资深软件工程师，输出高质量、可运行的代码。"),
                    UserMessage.from(prompt));
            String raw = response.content().text();
            String code = extractCode(raw);
            if (code.isBlank()) {
                return ToolResult.fail("模型未返回有效代码");
            }
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("language", lang);
            meta.put("rawLength", raw == null ? 0 : raw.length());
            meta.put("codeLength", code.length());
            return ToolResult.ok(code, meta);
        } catch (Exception e) {
            return ToolResult.fail("代码生成失败: " + e.getMessage());
        }
    }

    /** 从模型输出中提取第一个 Markdown 代码块；没有代码块则原样返回。 */
    private String extractCode(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = CODE_BLOCK.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return text.trim();
    }
}
