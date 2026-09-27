package com.example.aicodeassistant.tool;

import com.example.aicodeassistant.security.PermissionGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具二：查询数据库表结构。
 *
 * <p>权限隔离：只读 information_schema 元数据，表名走白名单校验，全程无写操作、无表名拼接注入。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbSchemaQueryTool {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionGuard permissionGuard;
    private final ObjectMapper objectMapper;

    @Tool("查询指定数据库表的结构信息（字段名、类型、是否可空、主键、默认值、注释及索引），返回 JSON。仅支持查询，不做任何写操作。")
    public ToolResult queryTableStructure(
            @P("要查询的数据库表名，例如 user") String tableName,
            @P("数据库名（schema），例如 mydb；缺省使用当前连接默认库") String schema) {
        if (!permissionGuard.isSafeTableName(tableName)) {
            return ToolResult.fail("非法表名（仅允许字母、数字、下划线组成的标识符）: " + tableName);
        }
        String db = (schema == null || schema.isBlank()) ? currentDatabase() : schema.trim();
        if (db == null) {
            return ToolResult.fail("无法确定数据库名，请显式传入 schema 参数");
        }

        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME AS columnName, COLUMN_TYPE AS columnType, DATA_TYPE AS dataType, "
                        + "IS_NULLABLE AS nullable, COLUMN_KEY AS columnKey, COLUMN_DEFAULT AS columnDefault, "
                        + "COLUMN_COMMENT AS comment "
                        + "FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                db, tableName);

        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME AS indexName, NON_UNIQUE AS nonUnique, COLUMN_NAME AS columnName, "
                        + "SEQ_IN_INDEX AS seq "
                        + "FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY INDEX_NAME, SEQ_IN_INDEX",
                db, tableName);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schema", db);
        payload.put("table", tableName);
        payload.put("columns", columns);
        payload.put("indexes", indexes);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("schema", db);
        meta.put("table", tableName);
        meta.put("columnCount", columns.size());
        meta.put("indexCount", indexes.size());
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(payload), meta);
        } catch (Exception e) {
            return ToolResult.fail("序列化表结构失败: " + e.getMessage());
        }
    }

    @Tool("列出指定数据库（schema）下的全部业务表名，返回 JSON 数组。仅支持查询。")
    public ToolResult listTables(@P("数据库名（schema），例如 mydb") String schema) {
        String db = (schema == null || schema.isBlank()) ? currentDatabase() : schema.trim();
        if (db == null) {
            return ToolResult.fail("无法确定数据库名，请显式传入 schema 参数");
        }
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? ORDER BY TABLE_NAME",
                String.class, db);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schema", db);
        payload.put("tables", tables);
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(payload), Map.of("schema", db, "tableCount", tables.size()));
        } catch (Exception e) {
            return ToolResult.fail("序列化表清单失败: " + e.getMessage());
        }
    }

    private String currentDatabase() {
        try {
            List<String> result = jdbcTemplate.queryForList("SELECT DATABASE()", String.class);
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            log.warn("获取当前数据库失败: {}", e.getMessage());
            return null;
        }
    }
}
