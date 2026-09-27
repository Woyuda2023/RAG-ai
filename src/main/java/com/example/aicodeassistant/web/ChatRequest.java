package com.example.aicodeassistant.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求体。
 *
 * @param sessionId 会话 ID（为空时服务端自动生成）
 * @param message   用户消息
 */
public record ChatRequest(
        @Size(max = 64) String sessionId,
        @NotBlank(message = "消息不能为空") @Size(max = 8000, message = "消息过长") String message
) {
}
