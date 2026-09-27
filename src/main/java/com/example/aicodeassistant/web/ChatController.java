package com.example.aicodeassistant.web;

import com.example.aicodeassistant.agent.AgentOrchestrator;
import com.example.aicodeassistant.agent.AgentResult;
import com.example.aicodeassistant.agent.TaskTypeClassifier;
import com.example.aicodeassistant.agent.ToolInvocation;
import com.example.aicodeassistant.entity.CodeTask;
import com.example.aicodeassistant.service.CodeTaskService;
import com.example.aicodeassistant.service.ReviewReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 对话入口：接收用户自然语言请求，交由 Agent 编排器处理。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final AgentOrchestrator orchestrator;
    private final CodeTaskService taskService;
    private final ReviewReportService reviewReportService;
    private final TaskTypeClassifier classifier;

    @PostMapping("/chat")
    public Map<String, Object> chat(@Valid @RequestBody ChatRequest request) {
        String sessionId = (request.sessionId() == null || request.sessionId().isBlank())
                ? UUID.randomUUID().toString()
                : request.sessionId();
        String taskType = classifier.classify(request.message()).name();

        CodeTask task = taskService.create(sessionId, request.message(), taskType);

        AgentResult result;
        try {
            result = orchestrator.execute(sessionId, String.valueOf(task.getId()), request.message());
        } catch (Exception e) {
            taskService.fail(task.getId(), e.getMessage());
            return simpleResponse(false, task.getId(), "服务处理异常: " + e.getMessage());
        }

        if (!result.success()) {
            taskService.fail(task.getId(), result.text());
            return simpleResponse(false, task.getId(), result.text());
        }

        String reply = result.text() == null ? "（无文本回复）" : result.text();
        taskService.succeed(task.getId(), reply.length() > 2000 ? reply.substring(0, 2000) : reply, result.toolRounds());
        reviewReportService.saveFromAgentResult(task.getId(), result);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("taskId", task.getId());
        body.put("sessionId", sessionId);
        body.put("reply", reply);
        body.put("toolRounds", result.toolRounds());
        body.put("tools", ToolInvocation.names(result.invocations()));
        return body;
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, Object> clearSession(@PathVariable String sessionId) {
        orchestrator.clearSession(sessionId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("sessionId", sessionId);
        return body;
    }

    private Map<String, Object> simpleResponse(boolean success, Long taskId, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", success);
        body.put("taskId", taskId);
        body.put("error", message);
        return body;
    }
}
