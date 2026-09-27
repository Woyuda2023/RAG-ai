package com.example.aicodeassistant.agent;

import com.example.aicodeassistant.tool.CodeGenerationTool;
import com.example.aicodeassistant.tool.CodeReviewTool;
import com.example.aicodeassistant.tool.DbSchemaQueryTool;
import com.example.aicodeassistant.tool.SourceFileReaderTool;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工具注册表：通过反射收集所有 @Tool 方法，构建工具规格与执行器。
 *
 * <p>编排器依据这里的规格列表让模型做工具选择，并依据名称定位执行器执行。</p>
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, RegisteredTool> tools = new LinkedHashMap<>();
    private final List<ToolSpecification> specifications = new ArrayList<>();

    public ToolRegistry(SourceFileReaderTool sourceFileReaderTool,
                        DbSchemaQueryTool dbSchemaQueryTool,
                        CodeGenerationTool codeGenerationTool,
                        CodeReviewTool codeReviewTool) {
        register(sourceFileReaderTool);
        register(dbSchemaQueryTool);
        register(codeGenerationTool);
        register(codeReviewTool);
        log.info("已注册工具 {} 个: {}", specifications.size(), tools.keySet());
    }

    private void register(Object toolObject) {
        for (Method method : toolObject.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Tool.class)) {
                continue;
            }
            try {
                method.setAccessible(true);
                ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(method);
                RegisteredTool registered = new RegisteredTool(
                        specification.name(),
                        specification.description(),
                        specification,
                        new DefaultToolExecutor(toolObject, method));
                tools.put(specification.name(), registered);
                specifications.add(specification);
            } catch (Exception e) {
                log.error("注册工具方法失败 {}#{}: {}", toolObject.getClass().getSimpleName(), method.getName(), e.getMessage());
            }
        }
    }

    /** 全部工具规格（供模型选择） */
    public List<ToolSpecification> specifications() {
        return specifications;
    }

    /** 按工具名定位 */
    public Optional<RegisteredTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /** 已注册工具：规格 + 执行器 */
    public record RegisteredTool(String name, String description, ToolSpecification specification,
                                 dev.langchain4j.service.tool.ToolExecutor executor) {

        /** 执行工具，返回序列化后的 JSON 结果字符串 */
        public String execute(ToolExecutionRequest request, Object memoryId) {
            return executor.execute(request, memoryId);
        }
    }
}
