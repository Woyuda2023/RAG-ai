package com.example.aicodeassistant.service;

import com.example.aicodeassistant.entity.ModelCallLog;
import com.example.aicodeassistant.repository.ModelCallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型调用日志服务：每次 LLM 调用落一条日志，记录请求、响应、工具调用与 token 用量。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelCallLogService {

    private final ModelCallLogRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${langchain4j.ollama.chat-model.model-name:qwen2.5-coder:14b}")
    private String modelName;

    /**
     * 记录一次模型调用。
     *
     * @param taskId          关联任务 ID（可空）
     * @param sessionId       会话 ID
     * @param response        模型响应
     * @param requestMessages 请求消息列表
     * @param latencyMs       调用耗时
     * @param maxChars        日志文本最大长度
     */
    public void record(String taskId, String sessionId, Response<AiMessage> response,
                       List<ChatMessage> requestMessages, long latencyMs, int maxChars) {
        try {
            ModelCallLog logEntry = ModelCallLog.builder()
                    .sessionId(sessionId)
                    .taskId(taskId == null || taskId.isBlank() ? null : Long.valueOf(taskId))
                    .modelName(modelName)
                    .requestMessages(truncate(serializeMessages(requestMessages), maxChars))
                    .responseMessage(truncate(response.content().text(), maxChars))
                    .toolCalls(truncate(serializeToolCalls(response.content()), maxChars))
                    .latencyMs(latencyMs)
                    .build();
            TokenUsage usage = response.tokenUsage();
            if (usage != null) {
                logEntry.setPromptTokens(usage.inputTokenCount());
                logEntry.setCompletionTokens(usage.outputTokenCount());
                logEntry.setTotalTokens(usage.totalTokenCount());
            }
            repository.save(logEntry);
        } catch (Exception e) {
            log.warn("模型调用日志写入失败: {}", e.getMessage());
        }
    }

    private String serializeMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage message : messages) {
            sb.append(roleOf(message)).append(": ");
            if (message instanceof SystemMessage system) {
                sb.append(system.text());
            } else if (message instanceof UserMessage user) {
                sb.append(user.text());
            } else if (message instanceof AiMessage ai) {
                sb.append(ai.text());
                if (ai.hasToolExecutionRequests()) {
                    sb.append(" [工具调用: ")
                            .append(ai.toolExecutionRequests().stream()
                                    .map(r -> r.name() + "(" + r.arguments() + ")")
                                    .toList())
                            .append("]");
                }
            } else if (message instanceof ToolExecutionResultMessage tool) {
                sb.append(tool.toolName()).append(" => ").append(tool.text());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String serializeToolCalls(AiMessage aiMessage) {
        if (aiMessage == null || !aiMessage.hasToolExecutionRequests()) {
            return "";
        }
        try {
            List<String> calls = aiMessage.toolExecutionRequests().stream()
                    .map(r -> "{\"name\":\"" + r.name() + "\",\"arguments\":" + r.arguments() + "}")
                    .toList();
            return "[" + String.join(",", calls) + "]";
        } catch (Exception e) {
            return "[工具调用序列化失败]";
        }
    }

    private String roleOf(ChatMessage message) {
        return switch (message.type()) {
            case SYSTEM -> "system";
            case USER -> "user";
            case AI -> "assistant";
            case TOOL_EXECUTION_RESULT -> "tool";
        };
    }

    private String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "...[日志截断]";
    }
}
