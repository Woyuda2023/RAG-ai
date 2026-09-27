package com.example.aicodeassistant.repository;

import com.example.aicodeassistant.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    List<ReviewReport> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    List<ReviewReport> findByTaskIdAndSeverityOrderByCreatedAtAsc(Long taskId, String severity);
}
