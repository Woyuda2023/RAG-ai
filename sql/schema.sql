-- =====================================================================
-- AI 代码助手 数据库初始化脚本（MySQL 8.0+）
-- 说明：MyBatis 不自动建表，首次部署需先执行本脚本初始化。
--       执行前请先创建数据库：
--       CREATE DATABASE ai_code_assistant DEFAULT CHARACTER SET utf8mb4;
-- =====================================================================

USE ai_code_assistant;

-- 代码任务表：一次用户请求对应一条任务
CREATE TABLE IF NOT EXISTS code_task (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_query     VARCHAR(2000) NOT NULL                COMMENT '用户原始请求',
    task_type      VARCHAR(32)  NOT NULL                COMMENT '任务类型: CHAT/CODE_GENERATION/CODE_REVIEW/SCHEMA_QUERY',
    status         VARCHAR(16)  NOT NULL DEFAULT 'RUNNING' COMMENT '状态: RUNNING/SUCCEEDED/FAILED',
    model_name     VARCHAR(64)                           COMMENT '使用的模型名',
    result_summary TEXT                                  COMMENT '结果摘要',
    error_message  TEXT                                  COMMENT '失败原因',
    tool_rounds    INT                                   COMMENT '实际工具调用轮次',
    session_id     VARCHAR(64)                           COMMENT '会话 ID',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finished_at    DATETIME                              COMMENT '完成时间',
    PRIMARY KEY (id),
    KEY idx_task_session (session_id),
    KEY idx_task_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '代码任务记录';

-- 代码审查报告表：CodeReviewTool 输出的 JSON 审查报告逐条落库
CREATE TABLE IF NOT EXISTS review_report (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id      BIGINT       NOT NULL                COMMENT '关联任务 ID',
    file_name    VARCHAR(255)                          COMMENT '被审查代码标识',
    severity     VARCHAR(16)                           COMMENT '严重级别: CRITICAL/MAJOR/MINOR/INFO',
    issue_type   VARCHAR(64)                           COMMENT '问题类型（安全/性能/并发/规范等）',
    line_number  INT                                   COMMENT '行号，无法定位为 -1',
    description  TEXT                                  COMMENT '问题描述',
    suggestion   TEXT                                  COMMENT '修改建议',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_review_task (task_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '代码审查报告条目';

-- 模型调用日志表：每次 LLM 调用的请求/响应/工具调用与 token 用量
CREATE TABLE IF NOT EXISTS model_call_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id       VARCHAR(64)                           COMMENT '会话 ID',
    task_id          BIGINT                               COMMENT '关联任务 ID',
    model_name       VARCHAR(64)                           COMMENT '模型名',
    request_messages LONGTEXT                              COMMENT '请求消息（序列化）',
    response_message LONGTEXT                              COMMENT '模型响应文本',
    tool_calls       TEXT                                  COMMENT '本轮工具调用请求（JSON）',
    prompt_tokens    INT                                   COMMENT '输入 token',
    completion_tokens INT                                 COMMENT '输出 token',
    total_tokens     INT                                   COMMENT '总 token',
    latency_ms       BIGINT                                COMMENT '调用耗时（毫秒）',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_log_task (task_id),
    KEY idx_log_session (session_id),
    KEY idx_log_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模型调用日志';

-- 会话表：一次对话会话一条记录
CREATE TABLE IF NOT EXISTS chat_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id  VARCHAR(64)  NOT NULL                COMMENT '会话 ID（唯一）',
    title       VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '会话标题（首条消息截断生成）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近活跃时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_session_updated (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '对话会话';

-- 对话消息表：保存用户需求与模型返回内容，按会话关联，支持问题追溯
CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id  VARCHAR(64)  NOT NULL                COMMENT '会话 ID',
    role        VARCHAR(16)  NOT NULL                COMMENT '角色: USER/ASSISTANT',
    content     TEXT                                  COMMENT '消息内容（用户需求 / 模型返回）',
    task_id     BIGINT                               COMMENT '关联任务 ID',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_msg_session (session_id, created_at),
    KEY idx_msg_task (task_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '对话消息';
