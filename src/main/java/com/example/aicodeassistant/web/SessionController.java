package com.example.aicodeassistant.web;

import com.example.aicodeassistant.agent.AgentOrchestrator;
import com.example.aicodeassistant.entity.ChatMessage;
import com.example.aicodeassistant.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 会话管理接口：创建 / 列表 / 历史消息 / 删除（持久化到 MySQL）。
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ChatSessionService chatSessionService;
    private final AgentOrchestrator orchestrator;

    /** 创建会话 */
    @PostMapping
    public Map<String, Object> create() {
        String sessionId = UUID.randomUUID().toString();
        chatSessionService.ensure(sessionId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("sessionId", sessionId);
        return body;
    }

    /** 会话列表（含消息条数） */
    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> items = chatSessionService.list();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("total", items.size());
        body.put("items", items);
        return body;
    }

    /** 会话历史消息 */
    @GetMapping("/{sessionId}/messages")
    public Map<String, Object> messages(@PathVariable String sessionId) {
        List<ChatMessage> messages = chatSessionService.messages(sessionId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("sessionId", sessionId);
        body.put("count", messages.size());
        body.put("items", messages);
        return body;
    }

    /** 删除会话（含历史消息与内存记忆） */
    @DeleteMapping("/{sessionId}")
    public Map<String, Object> delete(@PathVariable String sessionId) {
        orchestrator.clearSession(sessionId);
        chatSessionService.delete(sessionId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("sessionId", sessionId);
        return body;
    }
}
