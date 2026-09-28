package com.example.aicodeassistant.entity;

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
public class CodeTask {

    private Long id;

    /** 用户原始请求 */
    private String userQuery;

    /** 任务类型：CHAT / CODE_GENERATION / CODE_REVIEW / UNIT_TEST / SCHEMA_QUERY */
    private String taskType;

    /** 状态：RUNNING / SUCCEEDED / FAILED */
    private String status;

    /** 使用的模型名 */
    private String modelName;

    /** 结果摘要 */
    private String resultSummary;

    /** 失败原因 */
    private String errorMessage;

    /** 实际工具调用轮次 */
    private Integer toolRounds;

    /** 会话 ID */
    private String sessionId;

    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;
}
