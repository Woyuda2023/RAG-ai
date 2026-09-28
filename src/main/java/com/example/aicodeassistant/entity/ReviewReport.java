package com.example.aicodeassistant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代码审查报告条目：由 CodeReviewTool 输出的 JSON 审查报告解析落库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReport {

    private Long id;

    /** 关联的任务 ID */
    private Long taskId;

    /** 被审查代码标识（文件名 / 语言 / 片段名） */
    private String fileName;

    /** 严重级别：CRITICAL / MAJOR / MINOR / INFO */
    private String severity;

    /** 问题类型（安全、性能、并发、规范等） */
    private String issueType;

    /** 行号，无法定位为 -1 */
    private Integer lineNumber;

    /** 问题描述 */
    private String description;

    /** 修改建议 */
    private String suggestion;

    private LocalDateTime createdAt;
}
