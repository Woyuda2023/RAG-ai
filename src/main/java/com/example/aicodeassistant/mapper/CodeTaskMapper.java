package com.example.aicodeassistant.mapper;

import com.example.aicodeassistant.entity.CodeTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码任务 Mapper：任务 CRUD 与分页查询。
 */
@Mapper
public interface CodeTaskMapper {

    @Insert("INSERT INTO code_task(user_query, task_type, status, model_name, result_summary, error_message, "
            + "tool_rounds, session_id, created_at, finished_at) "
            + "VALUES(#{userQuery}, #{taskType}, #{status}, #{modelName}, #{resultSummary}, #{errorMessage}, "
            + "#{toolRounds}, #{sessionId}, #{createdAt}, #{finishedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CodeTask task);

    @Update("UPDATE code_task SET status='SUCCEEDED', result_summary=#{summary}, tool_rounds=#{toolRounds}, "
            + "finished_at=#{finishedAt} WHERE id=#{id}")
    int markSucceeded(@Param("id") Long id, @Param("summary") String summary,
                      @Param("toolRounds") int toolRounds, @Param("finishedAt") LocalDateTime finishedAt);

    @Update("UPDATE code_task SET status='FAILED', error_message=#{error}, finished_at=#{finishedAt} WHERE id=#{id}")
    int markFailed(@Param("id") Long id, @Param("error") String error, @Param("finishedAt") LocalDateTime finishedAt);

    @Select("SELECT * FROM code_task WHERE id = #{id}")
    CodeTask findById(Long id);

    @Select("SELECT COUNT(*) FROM code_task")
    long countAll();

    @Select("SELECT * FROM code_task ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<CodeTask> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM code_task WHERE session_id = #{sessionId} ORDER BY created_at DESC")
    List<CodeTask> findBySessionId(String sessionId);
}
