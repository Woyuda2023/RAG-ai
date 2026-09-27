package com.example.aicodeassistant.security;

import com.example.aicodeassistant.config.AssistantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 权限隔离：为工具访问划定边界。
 *
 * <ul>
 *   <li>源码文件读取：仅允许配置的根目录内的相对路径，禁止绝对路径与 .. 穿越；</li>
 *   <li>数据库工具：表名白名单 + 只读 SQL 检查（仅 information_schema 查询）。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PermissionGuard {

    private final AssistantProperties properties;

    /**
     * 解析并校验源码文件路径。
     * 规则：仅接受相对路径（无盘符、无绝对路径、无 .. 段），且最终解析结果必须落在允许根目录内。
     *
     * @return 校验通过且存在的文件路径；否则返回 empty。
     */
    public Optional<Path> resolveAllowedSourceFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return Optional.empty();
        }
        String trimmed = rawPath.trim();
        if (Path.of(trimmed).isAbsolute() || trimmed.contains("..")) {
            return Optional.empty();
        }
        Path normalized;
        try {
            normalized = Path.of(trimmed).normalize();
        } catch (Exception e) {
            return Optional.empty();
        }
        for (String root : properties.getSecurity().getAllowedSourceRoots()) {
            if (root == null || root.isBlank()) {
                continue;
            }
            Path rootPath = Path.of(root).toAbsolutePath().normalize();
            Path resolved = rootPath.resolve(normalized).normalize();
            if (!resolved.startsWith(rootPath)) {
                continue; // 路径穿越到根目录之外
            }
            if (!Files.isRegularFile(resolved)) {
                continue;
            }
            long maxBytes = properties.getSecurity().getMaxSourceFileSizeKb() * 1024L;
            try {
                if (Files.size(resolved) > maxBytes) {
                    continue; // 超过大小上限，拒绝
                }
            } catch (Exception e) {
                return Optional.empty();
            }
            return Optional.of(resolved);
        }
        return Optional.empty();
    }

    /** 表名白名单：仅允许普通标识符，从根上杜绝 SQL 拼接注入。 */
    public boolean isSafeTableName(String table) {
        return table != null && table.matches("[A-Za-z_][A-Za-z0-9_]{0,63}");
    }

    /** 数据库只读检查：仅允许以 SELECT / SHOW / DESC / EXPLAIN 开头的查询语句。 */
    public boolean isReadOnlySql(String sql) {
        if (sql == null) {
            return false;
        }
        String s = sql.trim().toLowerCase();
        return s.startsWith("select") || s.startsWith("show") || s.startsWith("desc")
                || s.startsWith("describe") || s.startsWith("explain");
    }
}
