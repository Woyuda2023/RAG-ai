package com.example.aicodeassistant.security;

import com.example.aicodeassistant.config.AssistantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示注入防护：工具返回的内容（尤其是源码文件内容）在回填给 LLM 之前，
 * 若检测到“忽略指令 / 输出系统提示词”等注入特征，将其替换为安全提示，
 * 避免恶意文件内容劫持模型行为。
 */
@Component
@RequiredArgsConstructor
public class PromptInjectionFilter {

    private final AssistantProperties properties;
    private final InputSanitizer inputSanitizer;

    /** 匹配文件内容中出现的指令覆盖类注入片段 */
    private static final Pattern INSTRUCTION_IN_TEXT = Pattern.compile(
            "(?is)((ignore|forget|disregard|override|bypass)\\s+(all\\s+)?(previous|prior|above)\\s+(instructions|prompts|directives)"
                    + "|忽略(之前|以上|所有)?(指令|提示|规则|要求))[^\\n。；;]{0,120}");

    /**
     * 过滤工具返回内容。
     *
     * @param toolContent 工具返回的文本（如文件内容）
     * @return 净化后的文本；命中注入特征时，注入片段被替换为安全提示。
     */
    public String filter(String toolContent) {
        if (!properties.getSecurity().isInjectionGuardEnabled() || toolContent == null || toolContent.isEmpty()) {
            return toolContent;
        }
        if (!inputSanitizer.isSuspicious(toolContent)) {
            return toolContent;
        }
        Matcher matcher = INSTRUCTION_IN_TEXT.matcher(toolContent);
        return matcher.replaceAll(InputSanitizer.INJECTION_WARNING);
    }
}
