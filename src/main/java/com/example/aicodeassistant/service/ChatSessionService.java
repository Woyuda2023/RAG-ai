package com.example.aicodeassistant.service;

import com.example.aicodeassistant.entity.ChatMessage;
import com.example.aicodeassistant.entity.ChatSession;
import com.example.aicodeassistant.mapper.ChatMessageMapper;
import com.example.aicodeassistant.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话会话服务：会话与消息的持久化（MyBatis）。
 *
 * <p>保存用户需求、模型返回内容与创建时间；会话按 session_id 唯一，
 * 首条消息截断生成会话标题；删除会话时级联删除消息。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final int TITLE_MAX_CHARS = 60;

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    /** 确保会话存在（不存在则创建） */
    @Transactional
    public String ensure(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("会话 ID 不能为空");
        }
        if (sessionMapper.findBySessionId(sessionId) == null) {
            LocalDateTime now = LocalDateTime.now();
            sessionMapper.insert(ChatSession.builder()
                    .sessionId(sessionId)
                    .title("")
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        return sessionId;
    }

    /** 会话列表（含消息条数） */
    public List<Map<String, Object>> list() {
        List<ChatSession> sessions = sessionMapper.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession session : sessions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", session.getSessionId());
            item.put("title", session.getTitle());
            item.put("createdAt", session.getCreatedAt());
            item.put("updatedAt", session.getUpdatedAt());
            item.put("messageCount", messageMapper.countBySessionId(session.getSessionId()));
            result.add(item);
        }
        return result;
    }

    /** 会话历史消息 */
    public List<ChatMessage> messages(String sessionId) {
        return messageMapper.findBySessionId(sessionId);
    }

    /** 保存一条消息（用户需求 / 模型返回），并更新会话活跃时间 */
    @Transactional
    public void saveMessage(String sessionId, String role, String content, Long taskId) {
        if (content == null || content.isBlank()) {
            return;
        }
        messageMapper.insert(ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .taskId(taskId)
                .createdAt(LocalDateTime.now())
                .build());
        sessionMapper.touch(sessionId, LocalDateTime.now());
    }

    /** 会话标题为空时用首条用户消息截断生成 */
    @Transactional
    public void updateTitleIfBlank(String sessionId, String userMessage) {
        ChatSession session = sessionMapper.findBySessionId(sessionId);
        if (session == null || (session.getTitle() != null && !session.getTitle().isBlank())) {
            return;
        }
        String title = userMessage == null ? "" : userMessage.trim().replaceAll("\\s+", " ");
        if (title.length() > TITLE_MAX_CHARS) {
            title = title.substring(0, TITLE_MAX_CHARS);
        }
        sessionMapper.updateTitle(sessionId, title, LocalDateTime.now());
    }

    /** 删除会话及其全部消息 */
    @Transactional
    public void delete(String sessionId) {
        messageMapper.deleteBySessionId(sessionId);
        sessionMapper.deleteBySessionId(sessionId);
    }
}
