package com.example.aicodeassistant.repository;

import com.example.aicodeassistant.entity.CodeTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeTaskRepository extends JpaRepository<CodeTask, Long> {

    Page<CodeTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<CodeTask> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}
