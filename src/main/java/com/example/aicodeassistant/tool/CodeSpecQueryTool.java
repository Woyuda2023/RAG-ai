package com.example.aicodeassistant.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 工具五：查询代码规范。
 *
 * <p>内置常见语言编码规范要点（命名、注释、异常、安全、性能等），按语言返回；
 * 纯规则库实现，不调用 LLM，结果稳定可校验。</p>
 */
@Component
public class CodeSpecQueryTool {

    private static final Map<String, String> SPECS = new LinkedHashMap<>();

    static {
        SPECS.put("java", """
                【Java 编码规范要点】
                1. 命名：类/接口使用 UpperCamelCase；方法/字段使用 lowerCamelCase；常量使用 UPPER_SNAKE_CASE；包名全小写。
                2. 注释：公共 API 必须写 Javadoc（@param/@return/@throws）；复杂逻辑写行内注释说明意图。
                3. 异常：优先使用特定异常类型；不吞异常（禁止空 catch）；finally 释放资源，或使用 try-with-resources。
                4. 空值：对外入参做空值/边界校验；返回集合不返回 null（返回空集合）。
                5. 安全：SQL 一律使用 PreparedStatement 参数化，禁止字符串拼接；输出转义防 XSS；密码等敏感信息不入日志。
                6. 性能：循环内不创建大对象/不查询数据库；字符串拼接用 StringBuilder；批量操作用批量 API。
                7. 并发：共享可变状态加锁或用并发容器；避免在持有锁时做 IO/远程调用。
                8. 格式：缩进 4 空格；行宽不超过 120 字符；import 不出现通配符 *；类/方法间留一空行。
                9. 测试：核心逻辑必须配套单元测试（JUnit 5），测试命名 testXxx_should_xxx_when_xxx。
                """);
        SPECS.put("python", """
                【Python 编码规范要点（PEP 8）】
                1. 命名：模块/函数/变量小写下划线；类 UpperCamelCase；常量全大写；私有成员加单下划线前缀。
                2. 缩进：统一 4 空格，禁止混用 Tab 与空格。
                3. 行宽：不超过 79 字符（文档/注释 72）。
                4. 空行：顶层函数/类定义前后空两行；类内方法间空一行。
                5. 导入：标准库 → 第三方 → 本地模块，三组之间空一行；禁止通配符导入。
                6. 异常：捕获具体异常类型，禁止裸 except；用 with 语句管理资源。
                7. 类型：公共函数建议加类型注解（typing）；文档字符串使用 docstring。
                8. 安全：SQL 用参数化查询（cursor.execute(sql, params)）；不 eval 用户输入；敏感信息不入日志。
                """);
        SPECS.put("sql", """
                【SQL 编码规范要点】
                1. 关键字（SELECT/INSERT/UPDATE/DELETE/FROM/WHERE）统一大写。
                2. 表名/字段名使用小写加下划线；保留字不得用作标识符。
                3. 每张表必须有主键；关联查询显式写 JOIN ON，禁止逗号隐式连接。
                4. WHERE 条件避免对字段做函数包裹（导致索引失效）；大表查询必须走索引。
                5. 禁止 SELECT *，显式列出所需字段。
                6. 更新/删除必须先确认影响范围（WHERE 必填），生产环境禁止不带条件的 DELETE/UPDATE。
                7. 事务：多步写操作必须包裹在事务中；避免长事务。
                8. 敏感数据查询结果不得直接暴露到日志与接口。
                """);
        SPECS.put("go", """
                【Go 编码规范要点（Effective Go）】
                1. 命名：导出标识符首字母大写（PascalCase），包内私有 camelCase；包名小写单词，不用下划线。
                2. 错误：错误必须显式检查；不要用 panic 处理常规错误；错误信息小写开头、不带句号。
                3. 格式化：一律 gofmt；Tab 缩进；行尾不加分号。
                4. 资源：文件/网络连接用 defer Close；注意 defer 在循环中的资源释放。
                5. 并发：优先 channel 与 goroutine 协调；共享变量用 sync.Mutex/atomic；注意 goroutine 泄漏。
                6. 性能：避免不必要的内存分配；热路径避免反射与 fmt 序列化。
                7. 测试：测试文件 *_test.go，表驱动测试（table-driven tests）。
                """);
    }

    @Tool("查询指定语言的编码规范要点（命名、注释、异常处理、安全、性能等），返回规范文本。")
    public ToolResult querySpec(
            @P("编程语言，例如 Java / Python / SQL / Go / JavaScript") String language) {
        String lang = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        String spec = SPECS.get(lang);
        if (spec == null) {
            if (lang.isBlank()) {
                return ToolResult.fail("请指定要查询规范的语言");
            }
            // 未内置的语言返回通用规范
            return ToolResult.ok("暂无「" + language + "」专属规范条目。通用要点：命名清晰一致；代码可读性优先；"
                    + "输入必须校验；错误必须显式处理；敏感信息不落日志；核心逻辑配套单元测试。", Map.of("language", language));
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("language", language);
        meta.put("length", spec.length());
        return ToolResult.ok(spec, meta);
    }
}
