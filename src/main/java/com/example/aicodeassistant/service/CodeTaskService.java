package com.example.aicodeassistant.service;

import com.example.aicodeassistant.entity.CodeTask;
import com.example.aicodeassistant.repository.CodeTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 代码任务服务：任务创建、状态流转与查询。
 */
@Service
@RequiredArgsConstructor
public class CodeTaskService {

    private final CodeTaskRepository repository;

    @Value("${llm.model-name:deepseek-chat}")
    private String modelName;

    /** 创建任务（RUNNING） */
    @Transactional
    public CodeTask create(String sessionId, String query, String taskType) {
        return repository.save(CodeTask.builder()
                .sessionId(sessionId)
                .userQuery(query)
                .taskType(taskType)
                .status("RUNNING")
                .modelName(modelName)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /** 任务成功 */
    @Transactional
    public void succeed(Long id, String summary, int toolRounds) {
        repository.findById(id).ifPresent(task -> {
            task.setStatus("SUCCEEDED");
            task.setResultSummary(summary);
            task.setToolRounds(toolRounds);
            task.setFinishedAt(LocalDateTime.now());
            repository.save(task);
        });
    }

    /** 任务失败 */
    @Transactional
    public void fail(Long id, String error) {
        repository.findById(id).ifPresent(task -> {
            task.setStatus("FAILED");
            task.setErrorMessage(error);
            task.setFinishedAt(LocalDateTime.now());
            repository.save(task);
        });
    }

    public Optional<CodeTask> get(Long id) {
        return repository.findById(id);
    }

    public Page<CodeTask> list(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<CodeTask> bySession(String sessionId) {
        return repository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }
}
