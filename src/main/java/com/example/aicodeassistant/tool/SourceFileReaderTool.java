package com.example.aicodeassistant.tool;

import com.example.aicodeassistant.config.AssistantProperties;
import com.example.aicodeassistant.security.PermissionGuard;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工具一：读取源码文件。
 *
 * <p>权限隔离：只允许读取配置根目录内的相对路径文件；路径穿越、绝对路径、超限文件一律拒绝。</p>
 */
@Component
@RequiredArgsConstructor
public class SourceFileReaderTool {

    private final PermissionGuard permissionGuard;
    private final AssistantProperties properties;

    @Tool("读取指定源码文件的内容。返回文件全文、行数与字节大小。仅允许读取配置的源码根目录内的文件。")
    public ToolResult readSourceFile(@P("相对源码根目录的文件路径，例如 src/main/java/com/example/Demo.java") String path) {
        Optional<Path> resolved = permissionGuard.resolveAllowedSourceFile(path);
        if (resolved.isEmpty()) {
            return ToolResult.fail("路径非法或不在允许的源码根目录内（仅接受相对路径，禁止 .. 穿越）："
                    + (path == null ? "空路径" : path));
        }
        Path file = resolved.get();
        try {
            List<String> allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int totalLines = allLines.size();
            int maxLines = properties.getSecurity().getMaxFileLines();
            boolean lineTruncated = totalLines > maxLines;

            List<String> contentLines = new ArrayList<>();
            if (lineTruncated) {
                contentLines.addAll(allLines.subList(0, maxLines));
                contentLines.add(String.format("...[文件共 %d 行，已按上限返回前 %d 行]...", totalLines, maxLines));
            } else {
                contentLines = allLines;
            }
            String content = String.join("\n", contentLines);
            long bytes = Files.size(file);

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("path", file.toAbsolutePath().toString());
            meta.put("lines", totalLines);
            meta.put("bytes", bytes);
            meta.put("truncated", lineTruncated);
            return ToolResult.ok(content, meta);
        } catch (Exception e) {
            return ToolResult.fail("读取文件失败: " + e.getMessage());
        }
    }
}
