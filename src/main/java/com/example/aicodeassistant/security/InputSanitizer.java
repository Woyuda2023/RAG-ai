package com.example.aicodeassistant.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入过滤：对用户输入与工具内容做统一清洗。
 *
 * <ul>
 *   <li>去除控制字符，限制输入长度上限，防止超长上下文注入；</li>
 *   <li>检测常见的提示注入 / 越权指令特征（中英文），命中即标记可疑。</li>
 * </ul>
 */
@Component
public class InputSanitizer {

    /** 单次输入最大长度（字符） */
    private static final int MAX_INPUT_LEN = 8000;

    /** 提示注入 / 越权指令特征库 */
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior|above|earlier|past)\\s+(instructions|prompts|directives|messages|context)"),
            Pattern.compile("(?i)(forget|disregard|override|bypass)\\s+(your|the|all)\\s+(instructions|rules|prompts|system\\s*prompt)"),
            Pattern.compile("(?i)(reveal|show|print|output|leak|repeat)\\s+(your|the)\\s+(system\\s*)?(prompt|instructions|rules)"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(acting\\s+as|a|an)"),
            Pattern.compile("(?i)(jailbreak|dan\\s*mode|do\\s+anything\\s+now|developer\\s+mode|unfiltered\\s+mode)"),
            Pattern.compile("忽略(之前|以上|所有)?(指令|提示|规则|要求)"),
            Pattern.compile("(输出|展示|泄露|透露)(你的|系统)?(提示词|指令|规则|system\\s*prompt)")
    );

    /** 命中注入特征时使用的统一拦截提示 */
    public static final String INJECTION_WARNING = "[安全拦截] 检测到疑似提示注入/越权指令内容，已隔离处理。";

    /** 清洗：去控制字符 + 长度上限。 */
    public String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replaceAll("[\\p{Cc}\\p{Cf}]", " ").trim();
        if (text.length() > MAX_INPUT_LEN) {
            text = text.substring(0, MAX_INPUT_LEN) + "\n[输入过长，已截断]";
        }
        return text;
    }

    /** 检测文本是否命中提示注入特征。 */
    public boolean isSuspicious(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
