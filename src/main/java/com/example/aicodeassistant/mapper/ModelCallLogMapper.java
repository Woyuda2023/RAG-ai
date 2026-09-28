package com.example.aicodeassistant.mapper;

import com.example.aicodeassistant.entity.ModelCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模型调用日志 Mapper：分页查询与写入。
 */
@Mapper
public interface ModelCallLogMapper {

    @Insert("INSERT INTO model_call_log(session_id, task_id, model_name, request_messages, response_message, "
            + "tool_calls, prompt_tokens, completion_tokens, total_tokens, latency_ms, created_at) "
            + "VALUES(#{sessionId}, #{taskId}, #{modelName}, #{requestMessages}, #{responseMessage}, "
            + "#{toolCalls}, #{promptTokens}, #{completionTokens}, #{totalTokens}, #{latencyMs}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelCallLog log);

    @Select("SELECT COUNT(*) FROM model_call_log")
    long countAll();

    @Select("SELECT COUNT(*) FROM model_call_log WHERE task_id = #{taskId}")
    long countByTaskId(Long taskId);

    @Select("SELECT * FROM model_call_log ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ModelCallLog> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM model_call_log WHERE task_id = #{taskId} ORDER BY created_at DESC "
            + "LIMIT #{limit} OFFSET #{offset}")
    List<ModelCallLog> findByTaskIdPage(@Param("taskId") Long taskId,
                                        @Param("offset") int offset, @Param("limit") int limit);
}
