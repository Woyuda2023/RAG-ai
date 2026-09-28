package com.example.aicodeassistant.service;

import com.example.aicodeassistant.common.Paged;
import com.example.aicodeassistant.entity.CodeTask;
import com.example.aicodeassistant.mapper.CodeTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 代码任务服务：任务创建、状态流转与分页查询（MyBatis）。
 */
@Service
@RequiredArgsConstructor
public class CodeTaskService {

    private final CodeTaskMapper mapper;

    @Value("${llm.model-name:qwen-plus}")
    private String modelName;

    /** 创建任务（RUNNING），返回带主键的任务实体 */
    @Transactional
    public CodeTask create(String sessionId, String query, String taskType) {
        CodeTask task = CodeTask.builder()
                .sessionId(sessionId)
                .userQuery(query)
                .taskType(taskType)
                .status("RUNNING")
                .modelName(modelName)
                .createdAt(LocalDateTime.now())
                .build();
        mapper.insert(task);
        return task;
    }

    /** 任务成功 */
    @Transactional
    public void succeed(Long id, String summary, int toolRounds) {
        mapper.markSucceeded(id, summary, toolRounds, LocalDateTime.now());
    }

    /** 任务失败 */
    @Transactional
    public void fail(Long id, String error) {
        mapper.markFailed(id, error, LocalDateTime.now());
    }

    public Optional<CodeTask> get(Long id) {
        return Optional.ofNullable(mapper.findById(id));
    }

    public Paged<CodeTask> list(int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        long total = mapper.countAll();
        List<CodeTask> items = mapper.findPage(p * s, s);
        return new Paged<>(items, total, p, s);
    }

    public List<CodeTask> bySession(String sessionId) {
        return mapper.findBySessionId(sessionId);
    }
}
