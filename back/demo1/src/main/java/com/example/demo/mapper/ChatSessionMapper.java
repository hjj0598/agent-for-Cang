package com.example.demo.mapper;

import com.example.demo.pojo.ChatSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    @Select("""
            select id, user_id, memory_id, title, created_at, updated_at
            from chat_session
            where user_id = #{userId}
            order by updated_at desc, id desc
            """)
    List<ChatSession> findByUserId(Long userId);

    @Select("""
            select id, user_id, memory_id, title, created_at, updated_at
            from chat_session
            where user_id = #{userId}
            order by updated_at desc, id desc
            limit #{pageSize} offset #{offset}
            """)
    List<ChatSession> findPageByUserId(@Param("userId") Long userId,
                                       @Param("offset") Integer offset,
                                       @Param("pageSize") Integer pageSize);

    @Select("select count(*) from chat_session where user_id = #{userId}")
    Long countByUserId(Long userId);

    @Select("select count(*) from chat_session")
    Long countAll();

    @Select("""
            select id, user_id, memory_id, title, created_at, updated_at
            from chat_session
            where id = #{id}
              and user_id = #{userId}
            """)
    ChatSession findByIdAndUserId(@Param("id") Long id,
                                  @Param("userId") Long userId);

    @Select("""
            select id, user_id, memory_id, title, created_at, updated_at
            from chat_session
            where user_id = #{userId}
              and memory_id = #{memoryId}
            limit 1
            """)
    ChatSession findByUserIdAndMemoryId(@Param("userId") Long userId,
                                        @Param("memoryId") String memoryId);

    @Insert("""
            insert into chat_session(user_id, memory_id, title)
            values(#{userId}, #{memoryId}, #{title})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatSession session);

    @Update("""
            update chat_session
            set title = #{title}
            where id = #{id}
              and user_id = #{userId}
            """)
    void updateTitle(ChatSession session);

    @Update("""
            update chat_session
            set updated_at = now()
            where id = #{id}
              and user_id = #{userId}
            """)
    void touch(@Param("id") Long id,
               @Param("userId") Long userId);

    @Delete("""
            delete from chat_session
            where id = #{id}
              and user_id = #{userId}
            """)
    void deleteByIdAndUserId(@Param("id") Long id,
                             @Param("userId") Long userId);
}
