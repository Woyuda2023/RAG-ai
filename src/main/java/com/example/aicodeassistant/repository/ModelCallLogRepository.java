package com.example.aicodeassistant.repository;

import com.example.aicodeassistant.entity.ModelCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelCallLogRepository extends JpaRepository<ModelCallLog, Long> {

    Page<ModelCallLog> findByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);

    Page<ModelCallLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
