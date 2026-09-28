package com.example.aicodeassistant.web;

import com.example.aicodeassistant.common.Paged;
import com.example.aicodeassistant.entity.ModelCallLog;
import com.example.aicodeassistant.service.ModelCallLogService;
import lombok.RequiredArgsConstructor;
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

    private final ModelCallLogService logService;

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) Long taskId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        Paged<ModelCallLog> result = logService.list(taskId, page, size);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", result.total());
        body.put("page", result.page());
        body.put("size", result.size());
        body.put("items", result.items());
        return body;
    }
}
