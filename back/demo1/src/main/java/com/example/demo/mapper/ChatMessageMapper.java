package com.example.demo.mapper;

import com.example.demo.dto.ChatHistorySearchResult;
import com.example.demo.pojo.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Select("""
            select id, user_id, session_id, role, content, created_at
            from chat_message
            where user_id = #{userId}
              and session_id = #{sessionId}
            order by id asc
            """)
    List<ChatMessage> findBySessionIdAndUserId(@Param("sessionId") Long sessionId,
                                               @Param("userId") Long userId);

    @Insert("""
            insert into chat_message(user_id, session_id, role, content)
            values(#{userId}, #{sessionId}, #{role}, #{content})
            """)
    void insert(ChatMessage message);

    @Select("""
            select
                cs.id as sessionId,
                cs.memory_id as memoryId,
                cs.title as sessionTitle,
                cm.id as messageId,
                cm.role as role,
                cm.content as content,
                cm.created_at as createdAt
            from chat_message cm
            join chat_session cs on cm.session_id = cs.id
            where cm.user_id = #{userId}
              and cs.user_id = #{userId}
              and (
                  cm.content like concat('%', #{keyword}, '%')
                  or cs.title like concat('%', #{keyword}, '%')
              )
            order by cm.created_at desc, cm.id desc
            limit 100
            """)
    List<ChatHistorySearchResult> searchByKeyword(@Param("userId") Long userId,
                                                  @Param("keyword") String keyword);

    @Delete("""
            delete from chat_message
            where session_id = #{sessionId}
              and user_id = #{userId}
            """)
    void deleteBySessionIdAndUserId(@Param("sessionId") Long sessionId,
                                    @Param("userId") Long userId);

    @Select("select count(*) from chat_message where user_id = #{userId}")
    Long countByUserId(Long userId);

    @Select("select count(*) from chat_message")
    Long countAll();
}
