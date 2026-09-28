package com.example.aicodeassistant.agent;

import com.example.aicodeassistant.config.AssistantProperties;
import com.example.aicodeassistant.processing.ResultTruncator;
import com.example.aicodeassistant.security.InputSanitizer;
import com.example.aicodeassistant.security.PromptInjectionFilter;
import com.example.aicodeassistant.service.ModelCallLogService;
import com.example.aicodeassistant.tool.ToolResult;
import com.example.aicodeassistant.validation.ToolResultValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 调度器：基于 LangChain4j 的手动工具调用循环（Tool Calling）。
 *
 * <p>流程：用户输入清洗 → 会话记忆追加 → 模型生成（带工具规格）→
 * 若模型请求调用工具：参数 JSON 校验 → 工具执行 → 返回结构 JSON 校验 →
 * 提示注入过滤 → 长度截断 → 回填工具结果 → 继续下一轮；直到模型给出最终回复或达到最大轮次。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final ChatLanguageModel chatLanguageModel;
    private final ToolRegistry toolRegistry;
    private final InputSanitizer inputSanitizer;
    private final PromptInjectionFilter injectionFilter;
    private final ToolResultValidator validator;
    private final ResultTruncator truncator;
    private final AssistantProperties properties;
    private final ModelCallLogService callLogService;
    private final ObjectMapper objectMapper;

    /** 会话 ID → 会话记忆（内存级，带上限） */
    private final Map<String, SessionMemory> sessions = new ConcurrentHashMap<>();

    /**
     * 执行一次 Agent 对话。
     *
     * @param sessionId  会话 ID（无则使用 default）
     * @param taskId     任务 ID（用于日志关联）
     * @param rawMessage 用户原始输入
     */
    public AgentResult execute(String sessionId, String taskId, String rawMessage) {
        String userText = inputSanitizer.sanitize(rawMessage);
        if (userText.isBlank()) {
            return AgentResult.fail("输入为空，请提供有效内容");
        }

        ChatMemory memory = sessionMemory(sessionId);
        memory.add(UserMessage.from(userText));

        List<ToolSpecification> specs = toolRegistry.specifications();
        int rounds = 0;
        int maxRounds = properties.getAgent().getMaxToolRounds();
        List<ToolInvocation> invocations = new ArrayList<>();
        String finalText;

        try {
            while (true) {
                Response<AiMessage> response = callModel(memory, specs, taskId, sessionId);
                AiMessage aiMessage = response.content();
                memory.add(aiMessage);

                // 模型给出最终文本回复，结束
                if (!aiMessage.hasToolExecutionRequests()) {
                    finalText = aiMessage.text() == null ? "（模型未给出文本回复）" : aiMessage.text();
                    break;
                }

                // 已达最大工具轮次，强制停止
                if (rounds >= maxRounds) {
                    finalText = "已达到工具调用最大轮次（" + maxRounds + "），已停止继续调用工具。最后一次模型输出："
                            + (aiMessage.text() == null ? "（仅工具调用请求，无文本）" : aiMessage.text());
                    break;
                }

                // 执行本轮全部工具调用
                for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                    ToolInvocation invocation = executeTool(request);
                    invocations.add(invocation);
                    String feedback;
                    if (invocation.success()) {
                        feedback = invocation.resultJson();
                    } else {
                        feedback = "{\"success\":false,\"data\":null,\"error\":\"" + escapeJson(invocation.error()) + "\",\"meta\":{}}";
                    }
                    memory.add(ToolExecutionResultMessage.from(request, feedback));
                }
                rounds++;
            }
        } catch (Exception e) {
            // 业务容错：捕获 API 限流 / 调用超时 / 其他模型调用异常，给出可读错误
            log.error("Agent 执行异常: {}", e.getMessage());
            return AgentResult.fail(classifyError(e));
        }

        return new AgentResult(true, finalText, rounds, invocations, sessionId);
    }

    /** 将模型调用异常归类为可读错误信息（限流 / 超时 / 其他） */
    private String classifyError(Throwable e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (message.contains("429") || lower.contains("rate limit") || lower.contains("too many requests")
                || lower.contains("限流") || lower.contains("访问量过大")) {
            return "模型服务触发限流（rate limit），请稍后重试。原始信息: " + message;
        }
        if (lower.contains("timeout") || lower.contains("timed out") || lower.contains("read timed out")
                || lower.contains("超时")) {
            return "模型调用超时，请稍后重试，或调大 llm.timeout 配置。原始信息: " + message;
        }
        if (lower.contains("401") || lower.contains("invalid api key") || lower.contains("unauthorized")
                || lower.contains("鉴权") || lower.contains("api key")) {
            return "模型 API 鉴权失败，请检查 llm.api-key 配置。原始信息: " + message;
        }
        return "模型调用失败: " + message;
    }

    /**
     * 执行单个工具调用，贯穿四道防线：
     * ① 参数 JSON 校验（必需参数齐全）
     * ② 工具内部权限隔离（PermissionGuard）
     * ③ 返回结构 JSON 校验（ToolResult 统一结构）
     * ④ 提示注入过滤 + 长度截断
     */
    private ToolInvocation executeTool(ToolExecutionRequest request) {
        long start = System.currentTimeMillis();
        var found = toolRegistry.find(request.name());
        if (found.isEmpty()) {
            return ToolInvocation.fail(request.name(), "工具不存在或未注册: " + request.name());
        }
        var registered = found.get();

        // ① 参数 JSON 校验
        List<String> required = registered.specification().toolParameters() == null
                ? List.of()
                : registered.specification().toolParameters().required();
        ToolResultValidator.ValidationResult argCheck = validator.validateArguments(request.arguments(), required);
        if (!argCheck.valid()) {
            return ToolInvocation.fail(request.name(), "工具参数校验失败: " + argCheck.error());
        }

        // ② 执行工具（权限隔离由工具内部自行保证）
        String rawResult;
        try {
            rawResult = registered.execute(request, null);
        } catch (Exception e) {
            log.error("工具执行异常 {}: {}", request.name(), e.getMessage());
            return ToolInvocation.fail(request.name(), "工具执行异常: " + e.getMessage());
        }

        // ③ 返回结构 JSON 校验
        ToolResultValidator.ValidationResult structureCheck = validator.validateJson(rawResult);
        if (!structureCheck.valid()) {
            return ToolInvocation.fail(request.name(), "工具返回 JSON 格式校验失败: " + structureCheck.error());
        }

        // ④ 注入过滤 + 截断，重建安全结果 JSON
        ToolResult result = toToolResult(structureCheck);
        ToolResultValidator.ValidationResult typeCheck = validator.validateToolResult(result);
        if (!typeCheck.valid()) {
            return ToolInvocation.fail(request.name(), "工具返回结构校验失败: " + typeCheck.error());
        }

        String safeData = result.success() ? injectionFilter.filter(result.data()) : null;
        String limited = truncator.truncate(safeData == null ? "" : safeData,
                properties.getAgent().getMaxResultChars()).content();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", result.success());
        payload.put("data", limited);
        payload.put("error", result.error());
        payload.put("meta", result.meta());
        String safeJson;
        try {
            safeJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return ToolInvocation.fail(request.name(), "工具结果序列化失败: " + e.getMessage());
        }
        long elapsed = System.currentTimeMillis() - start;
        return ToolInvocation.success(request.name(), safeJson, elapsed);
    }

    /** 将原始 JSON 还原为 ToolResult 记录（data/error/meta 缺失时给出可读错误） */
    private ToolResult toToolResult(ToolResultValidator.ValidationResult check) {
        try {
            var tree = check.tree();
            boolean success = tree.path("success").asBoolean(false);
            String data = tree.hasNonNull("data") ? tree.get("data").asText() : null;
            String error = tree.hasNonNull("error") ? tree.get("error").asText() : null;
            return new ToolResult(success, data, error, Map.of());
        } catch (Exception e) {
            return ToolResult.fail("工具返回结构无法解析: " + e.getMessage());
        }
    }

    /** 调用模型并记录日志 */
    private Response<AiMessage> callModel(ChatMemory memory, List<ToolSpecification> specs, String taskId, String sessionId) {
        long start = System.currentTimeMillis();
        Response<AiMessage> response = chatLanguageModel.generate(memory.messages(), specs);
        long elapsed = System.currentTimeMillis() - start;
        if (properties.getLogging().isEnabled()) {
            try {
                callLogService.record(taskId, sessionId, response, memory.messages(), elapsed,
                        properties.getLogging().getMaxLogChars());
            } catch (Exception e) {
                log.warn("记录模型调用日志失败: {}", e.getMessage());
            }
        }
        return response;
    }

    /** 获取或创建会话记忆（带会话数与单会话消息数双重上限），新会话注入系统提示词 */
    private ChatMemory sessionMemory(String sessionId) {
        String sid = (sessionId == null || sessionId.isBlank()) ? "default" : sessionId;
        SessionMemory session = sessions.computeIfAbsent(sid, k -> {
            SessionMemory created = new SessionMemory(properties.getAgent().getMemoryWindowSize());
            created.memory.add(SystemMessage.from(properties.getAgent().getSystemPrompt()));
            return created;
        });
        if (sessions.size() > properties.getAgent().getMaxSessions()) {
            sessions.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().createdAt))
                    .ifPresent(e -> sessions.remove(e.getKey()));
        }
        return session.memory;
    }

    /** 清理会话记忆 */
    public void clearSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** 会话记忆封装 */
    private static class SessionMemory {
        private final ChatMemory memory;
        private final long createdAt = System.currentTimeMillis();

        SessionMemory(int windowSize) {
            this.memory = MessageWindowChatMemory.builder().maxMessages(windowSize).build();
        }
    }
}
