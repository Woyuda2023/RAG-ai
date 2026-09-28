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
 * 工具七：单元测试编写。
 *
 * <p>通过 LLM 按结构化提示生成单元测试代码，输入经输入过滤清洗，
 * 输出从 Markdown 代码块中提取；默认按语言推断主流测试框架。</p>
 */
@Component
@RequiredArgsConstructor
public class UnitTestGenerationTool {

    private static final Pattern CODE_BLOCK = Pattern.compile("```[A-Za-z0-9_+.#-]*\\s*\\n(.*?)```", Pattern.DOTALL);

    private final ChatLanguageModel chatLanguageModel;
    private final InputSanitizer inputSanitizer;

    @Tool("根据需求编写单元测试代码。返回生成的测试代码（从代码块中提取的纯代码文本）。")
    public ToolResult generateUnitTest(
            @P("被测代码或单测需求描述，例如：为 CsvReader 类的 parse 方法编写 JUnit 5 测试") String requirement,
            @P("被测代码语言，例如 Java / Python / Go") String language,
            @P("测试框架（可选），例如 JUnit 5 / pytest / go test；不填则按语言默认") String framework) {
        String sanitized = inputSanitizer.sanitize(requirement);
        if (sanitized.isBlank()) {
            return ToolResult.fail("单测需求描述为空");
        }
        String lang = (language == null || language.isBlank()) ? "Java" : language.trim();
        String fw = (framework == null || framework.isBlank()) ? "（按语言默认框架）" : framework.trim();

        String prompt = "请根据以下需求编写完整、可直接运行的单元测试代码。\n"
                + "被测语言: " + lang + "\n"
                + "测试框架: " + fw + "\n"
                + "需求: " + sanitized + "\n"
                + "要求：覆盖正常路径、边界值与异常路径；测试方法命名清晰表达意图；"
                + "只输出测试代码，代码放在 ```语言 代码块中，不要输出任何解释文字。";
        try {
            var response = chatLanguageModel.generate(
                    SystemMessage.from("你是资深软件测试工程师，编写高质量、覆盖全面的单元测试。"),
                    UserMessage.from(prompt));
            String raw = response.content().text();
            String code = extractCode(raw);
            if (code.isBlank()) {
                return ToolResult.fail("模型未返回有效测试代码");
            }
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("language", lang);
            meta.put("framework", framework == null || framework.isBlank() ? "default" : framework.trim());
            meta.put("rawLength", raw == null ? 0 : raw.length());
            meta.put("codeLength", code.length());
            return ToolResult.ok(code, meta);
        } catch (Exception e) {
            return ToolResult.fail("单元测试生成失败: " + e.getMessage());
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
