package com.example.aicodeassistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "model_call_log", indexes = {
        @Index(name = "idx_log_task", columnList = "task_id"),
        @Index(name = "idx_log_session", columnList = "session_id")
})
public class ModelCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话 ID */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /** 关联任务 ID */
    @Column(name = "task_id")
    private Long taskId;

    /** 模型名 */
    @Column(name = "model_name", length = 64)
    private String modelName;

    /** 请求消息（序列化，截断） */
    @Column(name = "request_messages", columnDefinition = "LONGTEXT")
    private String requestMessages;

    /** 模型响应文本 */
    @Column(name = "response_message", columnDefinition = "LONGTEXT")
    private String responseMessage;

    /** 本轮工具调用请求（JSON，截断） */
    @Column(name = "tool_calls", columnDefinition = "TEXT")
    private String toolCalls;

    /** 输入 token */
    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    /** 输出 token */
    @Column(name = "completion_tokens")
    private Integer completionTokens;

    /** 总 token */
    @Column(name = "total_tokens")
    private Integer totalTokens;

    /** 调用耗时（毫秒） */
    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
