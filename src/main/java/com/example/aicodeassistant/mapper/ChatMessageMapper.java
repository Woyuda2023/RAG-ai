package com.example.aicodeassistant.mapper;

import com.example.aicodeassistant.entity.ChatMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 对话消息 Mapper：消息写入与按会话查询。
 */
@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_message(session_id, role, content, task_id, created_at) "
            + "VALUES(#{sessionId}, #{role}, #{content}, #{taskId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY created_at ASC, id ASC")
    List<ChatMessage> findBySessionId(String sessionId);

    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId}")
    long countBySessionId(String sessionId);

    @Delete("DELETE FROM chat_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(String sessionId);
}
