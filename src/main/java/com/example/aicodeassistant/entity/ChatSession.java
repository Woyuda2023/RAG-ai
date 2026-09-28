package com.example.aicodeassistant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话会话：一次对话会话一条记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    private Long id;

    /** 会话 ID（唯一，由调用方生成 UUID） */
    private String sessionId;

    /** 会话标题（由首条用户消息截断生成） */
    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
