package com.example.aicodeassistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代码任务记录：一次用户请求对应一条任务，用于问题追溯与统计。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "code_task")
public class CodeTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户原始请求 */
    @Column(name = "user_query", nullable = false, length = 2000)
    private String userQuery;

    /** 任务类型：CHAT / CODE_GENERATION / CODE_REVIEW / SCHEMA_QUERY */
    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    /** 状态：RUNNING / SUCCEEDED / FAILED */
    @Column(nullable = false, length = 16)
    private String status;

    /** 使用的模型名 */
    @Column(name = "model_name", length = 64)
    private String modelName;

    /** 结果摘要 */
    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    /** 失败原因 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 实际工具调用轮次 */
    @Column(name = "tool_rounds")
    private Integer toolRounds;

    /** 会话 ID */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
