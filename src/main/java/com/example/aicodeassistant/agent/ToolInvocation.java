package com.example.aicodeassistant.agent;

import java.util.List;
import java.util.Map;

/**
 * 单次工具调用记录（用于回传前端展示与日志追踪）。
 *
 * @param name        工具名
 * @param success     是否成功
 * @param resultJson  已校验、已过滤、已截断的安全结果 JSON（仅成功时非空）
 * @param error       错误信息（仅失败时非空）
 * @param latencyMs   执行耗时
 */
public record ToolInvocation(String name, boolean success, String resultJson, String error, long latencyMs) {

    public static ToolInvocation success(String name, String resultJson, long latencyMs) {
        return new ToolInvocation(name, true, resultJson, null, latencyMs);
    }

    public static ToolInvocation fail(String name, String error) {
        return new ToolInvocation(name, false, null, error, 0L);
    }

    /** 便捷：调用链上的工具名列表 */
    public static List<String> names(List<ToolInvocation> invocations) {
        return invocations.stream().map(ToolInvocation::name).toList();
    }
}
