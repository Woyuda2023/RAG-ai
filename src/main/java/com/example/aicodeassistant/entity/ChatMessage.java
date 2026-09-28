package com.example.aicodeassistant.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话消息：保存用户需求与模型返回内容，按会话关联，支持问题追溯。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private Long id;

    /** 会话 ID */
    private String sessionId;

    /** 角色：USER / ASSISTANT */
    private String role;

    /** 消息内容（用户需求 / 模型返回） */
    private String content;

    /** 关联任务 ID（可空） */
    private Long taskId;

    private LocalDateTime createdAt;
}
