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
 * 代码审查报告条目：由 CodeReviewTool 输出的 JSON 审查报告解析落库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_report", indexes = {
        @Index(name = "idx_review_task", columnList = "task_id")
})
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的任务 ID */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** 被审查代码标识（文件名 / 语言 / 片段名） */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /** 严重级别：CRITICAL / MAJOR / MINOR / INFO */
    @Column(length = 16)
    private String severity;

    /** 问题类型（安全、性能、并发、规范等） */
    @Column(name = "issue_type", length = 64)
    private String issueType;

    /** 行号，无法定位为 -1 */
    @Column(name = "line_number")
    private Integer lineNumber;

    /** 问题描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 修改建议 */
    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
