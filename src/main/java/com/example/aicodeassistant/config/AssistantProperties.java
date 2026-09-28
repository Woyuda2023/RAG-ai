package com.example.aicodeassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 助手行为配置（对应 application.yml 中 assistant.* 前缀）。
 */
@Data
@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {

    /** Agent 调度配置 */
    private Agent agent = new Agent();

    /** 安全与权限配置 */
    private Security security = new Security();

    /** 日志记录配置 */
    private Logging logging = new Logging();

    @Data
    public static class Agent {
        /** 工具调用最大轮次（防止 Agent 无限循环调用工具） */
        private int maxToolRounds = 5;
        /** 工具返回结果最大字符数，超过则截断 */
        private int maxResultChars = 4000;
        /** 截断后是否调用 LLM 生成摘要 */
        private boolean summarizeWhenTruncated = false;
        /** 系统提示词：约束 Agent 行为与代码输出格式 */
        private String systemPrompt = "你是「AI 代码助手」，一个基于 Agent + Tool Calling 架构、面向开发人员的智能编程助手。\n"
                + "你可以根据用户需求自主判断是否调用工具：读取源码文件、查询数据库表结构、生成代码、审查代码、编写单元测试、查询代码规范、校验代码格式。\n"
                + "输出规范：\n"
                + "1) 代码一律放在 Markdown 代码块中并标注语言（如 ```java）；\n"
                + "2) 先给出结论再给细节，语言简洁；代码生成与单元测试任务只输出代码和必要说明，不输出无关内容；\n"
                + "3) 代码审查任务输出结构化结论与问题清单；\n"
                + "4) 回答用户问题时使用用户使用的语言。";
        /** 单会话记忆窗口保留的最大消息条数 */
        private int memoryWindowSize = 20;
        /** 内存中保留的最大会话数（LRU 近似淘汰） */
        private int maxSessions = 100;
    }

    @Data
    public static class Security {
        /** 允许读取的源码根目录（相对路径必须落在这些目录内） */
        private List<String> allowedSourceRoots = new ArrayList<>();
        /** 单个源码文件大小上限（KB），超过拒绝读取 */
        private long maxSourceFileSizeKb = 512;
        /** 数据库工具只读开关（本实现只查询 information_schema，天然只读） */
        private boolean readOnlyDb = true;
        /** 是否启用提示注入防护（对工具返回内容做注入过滤） */
        private boolean injectionGuardEnabled = true;
        /** 文件读取工具单次返回的最大行数 */
        private int maxFileLines = 2000;
    }

    @Data
    public static class Logging {
        /** 是否记录模型调用日志到 MySQL */
        private boolean enabled = true;
        /** 日志中请求/响应文本最大长度（字符） */
        private int maxLogChars = 6000;
    }
}
