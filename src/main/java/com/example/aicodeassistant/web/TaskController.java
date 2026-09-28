package com.example.aicodeassistant.web;

import com.example.aicodeassistant.common.Paged;
import com.example.aicodeassistant.entity.CodeTask;
import com.example.aicodeassistant.entity.ReviewReport;
import com.example.aicodeassistant.service.CodeTaskService;
import com.example.aicodeassistant.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务与审查报告查询接口。
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final CodeTaskService taskService;
    private final ReviewReportService reviewReportService;

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        Paged<CodeTask> result = taskService.list(page, size);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", result.total());
        body.put("page", result.page());
        body.put("size", result.size());
        body.put("items", result.items());
        return body;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Optional<CodeTask> task = taskService.get(id);
        Map<String, Object> body = new LinkedHashMap<>();
        if (task.isEmpty()) {
            body.put("success", false);
            body.put("error", "任务不存在: " + id);
            return body;
        }
        body.put("success", true);
        body.put("task", task.get());
        return body;
    }

    @GetMapping("/{id}/reports")
    public Map<String, Object> reports(@PathVariable Long id) {
        List<ReviewReport> reports = reviewReportService.findByTaskId(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", id);
        body.put("count", reports.size());
        body.put("items", reports);
        return body;
    }
}
