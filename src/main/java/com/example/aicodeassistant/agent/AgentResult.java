package com.example.aicodeassistant.agent;

import java.util.List;

/**
 * Agent 一次执行的完整结果。
 *
 * @param success     是否成功
 * @param text        最终回复文本
 * @param toolRounds  实际工具调用轮次
 * @param invocations 全部工具调用明细
 * @param sessionId   会话 ID
 */
public record AgentResult(boolean success, String text, int toolRounds, List<ToolInvocation> invocations, String sessionId) {

    public static AgentResult fail(String error) {
        return new AgentResult(false, error, 0, List.of(), null);
    }
}
