package com.example.aicodeassistant.agent;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 任务类型分类：根据用户输入关键词粗分类，用于任务记录与统计。
 */
@Component
public class TaskTypeClassifier {

    public enum TaskType {
        CHAT("普通对话"),
        CODE_GENERATION("代码生成"),
        CODE_REVIEW("代码审查"),
        SCHEMA_QUERY("表结构查询");

        private final String label;

        TaskType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public TaskType classify(String message) {
        if (message == null || message.isBlank()) {
            return TaskType.CHAT;
        }
        String m = message.toLowerCase(Locale.ROOT);
        if (containsAny(m, "审查", "评审", "review", "review code", "检查代码", "找bug", "找 bug", "代码检查")) {
            return TaskType.CODE_REVIEW;
        }
        if (containsAny(m, "表结构", "建表", "表字段", "schema", "table structure", "show tables", "查询表", "有哪些表")) {
            return TaskType.SCHEMA_QUERY;
        }
        if (containsAny(m, "生成", "写一个", "实现", "编写", "开发一个", "generate", "write code", "implement", "code gen", "帮我写")) {
            return TaskType.CODE_GENERATION;
        }
        return TaskType.CHAT;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
