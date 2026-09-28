package com.example.aicodeassistant.common;

import java.util.List;

/**
 * 简单分页结果（MyBatis 手写分页，无第三方分页插件）。
 *
 * @param <T> 条目类型
 * @param items 当前页条目
 * @param total 总条数
 * @param page  页码（从 0 开始）
 * @param size  页大小
 */
public record Paged<T>(List<T> items, long total, int page, int size) {
}
