package com.example.aicodeassistant.service;

import com.example.aicodeassistant.common.Paged;
import com.example.aicodeassistant.entity.ModelCallLog;
import com.example.aicodeassistant.mapper.ModelCallLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型调用日志服务：写入每次 LLM 调用的请求/响应/token 用量，供问题追溯；支持分页查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelCallLogService {

    private final ModelCallLogMapper mapper;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${llm.model-name:qwen-plus}")
    private String modelName;

    /**
     * 记录一次模型调用。
     *
     * @param taskId   关联任务 ID（字符串形式）
     * @param sessionId 会话 ID
     * @param response 模型响应
     * @param messages 请求消息（含系统提示词与历史上下文）
     * @param elapsed  调用耗时（毫秒）
     * @param maxChars 请求/响应文本最大记录长度，超出截断
     */
    public void record(String taskId, String sessionId, Response<AiMessage> response,
                       List<ChatMessage> messages, long elapsed, int maxChars) {
        try {
            AiMessage aiMessage = response.content();
            String responseText = truncate(aiMessage.text(), maxChars);

            // 工具调用请求（JSON 文本）
            String toolCalls = null;
            if (aiMessage.hasToolExecutionRequests()) {
                List<Map<String, String>> calls = new ArrayList<>();
                for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                    Map<String, String> call = new LinkedHashMap<>();
                    call.put("name", request.name());
                    call.put("arguments", truncate(request.arguments(), maxChars));
                    calls.add(call);
                }
                toolCalls = objectMapper.writeValueAsString(calls);
            }

            Integer promptTokens = response.tokenUsage() == null ? null : response.tokenUsage().inputTokenCount();
            Integer completionTokens = response.tokenUsage() == null ? null : response.tokenUsage().outputTokenCount();

            ModelCallLog logEntry = ModelCallLog.builder()
                    .sessionId(sessionId)
                    .taskId(taskId == null ? null : parseLong(taskId))
                    .modelName(modelName)
                    .requestMessages(truncate(toRequestText(messages), maxChars))
                    .responseMessage(responseText)
                    .toolCalls(toolCalls)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(promptTokens != null && completionTokens != null
                            ? promptTokens + completionTokens : null)
                    .latencyMs(elapsed)
                    .createdAt(LocalDateTime.now())
                    .build();
            mapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("记录模型调用日志失败: {}", e.getMessage());
        }
    }

    /** 请求消息序列化为可读文本（角色 + 文本摘要） */
    private String toRequestText(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage message : messages) {
            sb.append('[').append(message.type().name()).append("] ");
            if (message instanceof AiMessage ai) {
                sb.append(ai.text() == null ? "（工具调用）" : ai.text());
            } else {
                sb.append(message.text());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }

    public Paged<ModelCallLog> list(Long taskId, int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        long total = taskId == null ? mapper.countAll() : mapper.countByTaskId(taskId);
        List<ModelCallLog> items = taskId == null
                ? mapper.findPage(p * s, s)
                : mapper.findByTaskIdPage(taskId, p * s, s);
        return new Paged<>(items, total, p, s);
    }
}
