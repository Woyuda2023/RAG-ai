package com.example.aicodeassistant.web;

import com.example.aicodeassistant.entity.ModelCallLog;
import com.example.aicodeassistant.repository.ModelCallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型调用日志查询接口，方便问题追溯。
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final ModelCallLogRepository repository;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) Long taskId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<ModelCallLog> result = taskId == null
                ? repository.findAllByOrderByCreatedAtDesc(pageable)
                : repository.findByTaskIdOrderByCreatedAtDesc(taskId, pageable);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", result.getTotalElements());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("items", result.getContent());
        return body;
    }
}
