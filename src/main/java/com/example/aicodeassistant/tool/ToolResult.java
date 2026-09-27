package com.example.aicodeassistant.tool;

import java.util.Map;

/**
 * 工具统一返回结构。
 *
 * <p>所有工具输出固定为该结构，保证下游可以做统一的 JSON 格式校验、
 * 提示注入过滤与长度截断，避免脏数据进入 LLM 上下文。</p>
 *
 * @param success 是否成功
 * @param data    成功时的数据（文本）
 * @param error   失败时的错误信息
 * @param meta    附加元信息（来源、行数、是否截断等）
 */
public record ToolResult(boolean success, String data, String error, Map<String, Object> meta) {

    public static ToolResult ok(String data, Map<String, Object> meta) {
        return new ToolResult(true, data, null, meta == null ? Map.of() : meta);
    }

    public static ToolResult fail(String error) {
        return new ToolResult(false, null, error == null ? "未知错误" : error, Map.of());
    }
}
