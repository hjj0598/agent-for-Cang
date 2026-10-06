package com.example.demo.mapper;

import com.example.demo.pojo.KnowledgeDocument;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {

    @Select("select id, user_id, title, content, source, file_hash, created_at, updated_at from knowledge_document where user_id = #{userId} order by id desc")
    List<KnowledgeDocument> findByUserId(Long userId);

    @Select("""
            select id, user_id, title, content, source, file_hash, created_at, updated_at
            from knowledge_document
            where user_id = #{userId}
            order by id desc
            limit #{pageSize} offset #{offset}
            """)
    List<KnowledgeDocument> findPageByUserId(@Param("userId") Long userId,
                                             @Param("offset") Integer offset,
                                             @Param("pageSize") Integer pageSize);

    @Select("select count(*) from knowledge_document where user_id = #{userId}")
    Long countByUserId(Long userId);

    @Select("select count(*) from knowledge_document")
    Long countAll();

    @Select("""
            select count(*)
            from knowledge_document
            where user_id = #{userId}
              and (#{keyword} = '' or title like concat('%', #{keyword}, '%'))
            """)
    Long countByUserIdAndTitle(@Param("userId") Long userId,
                               @Param("keyword") String keyword);

    @Select("""
            select id, user_id, title, content, source, file_hash, created_at, updated_at
            from knowledge_document
            where user_id = #{userId}
              and (#{keyword} = '' or title like concat('%', #{keyword}, '%'))
            order by id desc
            limit #{pageSize} offset #{offset}
            """)
    List<KnowledgeDocument> findPageByUserIdAndTitle(@Param("userId") Long userId,
                                                     @Param("keyword") String keyword,
                                                     @Param("offset") Integer offset,
                                                     @Param("pageSize") Integer pageSize);

    @Select("select id, user_id, title, content, source, file_hash, created_at, updated_at from knowledge_document where id = #{id} and user_id = #{userId}")
    KnowledgeDocument findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("select id, user_id, title, content, source, file_hash, created_at, updated_at from knowledge_document where user_id = #{userId} and file_hash = #{fileHash} limit 1")
    KnowledgeDocument findByUserIdAndFileHash(@Param("userId") Long userId,
                                              @Param("fileHash") String fileHash);

    @Insert("insert into knowledge_document(user_id, title, content, source, file_hash) values(#{userId}, #{title}, #{content}, #{source}, #{fileHash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(KnowledgeDocument document);

    @Update("""
            update knowledge_document
            set title = #{title},
                content = #{content}
            where id = #{id}
              and user_id = #{userId}
            """)
    void update(KnowledgeDocument document);

    @Delete("delete from knowledge_document where id = #{id} and user_id = #{userId}")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
