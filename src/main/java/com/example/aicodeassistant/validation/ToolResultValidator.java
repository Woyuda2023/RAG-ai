package com.example.aicodeassistant.validation;

import com.example.aicodeassistant.tool.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 调用校验：对模型产生的工具调用参数、以及工具返回结果做 JSON 格式校验。
 *
 * <ul>
 *   <li>参数校验：模型下发的 arguments 必须是合法 JSON 对象，且必需参数齐全；</li>
 *   <li>结果校验：工具返回必须是可解析的 JSON，且符合统一结构（ToolResult）；</li>
 *   <li>失败即拦截，带错误信息回填给模型，避免脏数据进入上下文。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ToolResultValidator {

    private final ObjectMapper objectMapper;

    /** 校验任意 JSON 字符串是否可解析。 */
    public ValidationResult validateJson(String json) {
        if (json == null || json.isBlank()) {
            return ValidationResult.invalid("内容为空，无法解析为 JSON");
        }
        try {
            JsonNode tree = objectMapper.readTree(json);
            return ValidationResult.valid(tree);
        } catch (Exception e) {
            return ValidationResult.invalid("JSON 格式校验失败: " + e.getMessage());
        }
    }

    /** 校验工具统一返回结构 ToolResult。 */
    public ValidationResult validateToolResult(ToolResult result) {
        if (result == null) {
            return ValidationResult.invalid("工具返回为空");
        }
        if (result.success() && (result.data() == null || result.data().isBlank())) {
            return ValidationResult.invalid("工具标记成功但 data 为空");
        }
        if (!result.success() && (result.error() == null || result.error().isBlank())) {
            return ValidationResult.invalid("工具标记失败但缺少 error 信息");
        }
        return ValidationResult.valid(objectMapper.valueToTree(result));
    }

    /** 校验模型生成的工具调用参数 JSON：可解析、是对象、必需参数齐全。 */
    public ValidationResult validateArguments(String argumentsJson, List<String> requiredParams) {
        ValidationResult parsed = validateJson(argumentsJson);
        if (!parsed.valid()) {
            return parsed;
        }
        JsonNode args = parsed.tree();
        if (args.isArray() || !args.isObject()) {
            return ValidationResult.invalid("工具参数必须是 JSON 对象");
        }
        if (requiredParams != null) {
            for (String required : requiredParams) {
                JsonNode value = args.get(required);
                if (value == null || value.isNull() || (value.isTextual() && value.asText().isBlank())) {
                    return ValidationResult.invalid("缺少必需参数: " + required);
                }
            }
        }
        return parsed;
    }

    /** 校验结果封装 */
    public record ValidationResult(boolean valid, JsonNode tree, String error) {

        public static ValidationResult valid(JsonNode tree) {
            return new ValidationResult(true, tree, null);
        }

        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, null, error);
        }
    }
}
