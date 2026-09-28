package com.example.aicodeassistant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型调用日志：记录每次 LLM 调用的请求、响应、工具调用与 token 用量，方便问题追溯。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCallLog {

    private Long id;

    /** 会话 ID */
    private String sessionId;

    /** 关联任务 ID */
    private Long taskId;

    /** 模型名 */
    private String modelName;

    /** 请求消息（序列化，截断） */
    private String requestMessages;

    /** 模型响应文本 */
    private String responseMessage;

    /** 本轮工具调用请求（JSON，截断） */
    private String toolCalls;

    /** 输入 token */
    private Integer promptTokens;

    /** 输出 token */
    private Integer completionTokens;

    /** 总 token */
    private Integer totalTokens;

    /** 调用耗时（毫秒） */
    private Long latencyMs;

    private LocalDateTime createdAt;
}
