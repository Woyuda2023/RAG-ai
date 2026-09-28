package com.example.aicodeassistant.mapper;

import com.example.aicodeassistant.entity.ChatSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话会话 Mapper：会话 CRUD。
 */
@Mapper
public interface ChatSessionMapper {

    @Insert("INSERT INTO chat_session(session_id, title, created_at, updated_at) "
            + "VALUES(#{sessionId}, #{title}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSession session);

    @Select("SELECT * FROM chat_session WHERE session_id = #{sessionId}")
    ChatSession findBySessionId(String sessionId);

    @Select("SELECT * FROM chat_session ORDER BY updated_at DESC")
    List<ChatSession> findAll();

    @Update("UPDATE chat_session SET title = #{title}, updated_at = #{updatedAt} WHERE session_id = #{sessionId}")
    int updateTitle(@Param("sessionId") String sessionId, @Param("title") String title,
                    @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE chat_session SET updated_at = #{updatedAt} WHERE session_id = #{sessionId}")
    int touch(@Param("sessionId") String sessionId, @Param("updatedAt") LocalDateTime updatedAt);

    @Delete("DELETE FROM chat_session WHERE session_id = #{sessionId}")
    int deleteBySessionId(String sessionId);
}
