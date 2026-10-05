package com.example.demo.mapper;

import com.example.demo.dto.KnowledgeHit;
import com.example.demo.pojo.KnowledgeChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {

    @Insert("""
            insert into knowledge_chunk(user_id, document_id, embedding_id, chunk_index, content)
            values(#{userId}, #{documentId}, #{embeddingId}, #{chunkIndex}, #{content})
            """)
    void insert(KnowledgeChunk chunk);

    @Select("""
            select id, user_id, document_id, embedding_id, chunk_index, content, created_at
            from knowledge_chunk
            where document_id = #{documentId}
              and user_id = #{userId}
            order by chunk_index asc, id asc
            """)
    List<KnowledgeChunk> findByDocumentIdAndUserId(@Param("documentId") Long documentId,
                                                   @Param("userId") Long userId);

    @Delete("""
            delete from knowledge_chunk
            where document_id = #{documentId}
              and user_id = #{userId}
            """)
    void deleteByDocumentIdAndUserId(@Param("documentId") Long documentId,
                                     @Param("userId") Long userId);

    @Select("""
            select id, user_id, document_id, embedding_id, chunk_index, content, created_at
            from knowledge_chunk
            where embedding_id = #{embeddingId}
              and user_id = #{userId}
            """)
    KnowledgeChunk findByEmbeddingIdAndUserId(@Param("embeddingId") String embeddingId,
                                              @Param("userId") Long userId);

    @Select("""
            select
                kc.document_id as documentId,
                kd.title as documentTitle,
                kd.source as documentSource,
                kc.content as content
            from knowledge_chunk kc
            join knowledge_document kd on kc.document_id = kd.id
            where kc.embedding_id = #{embeddingId}
              and kc.user_id = #{userId}
              and kd.user_id = #{userId}
            """)
    KnowledgeHit findHitByEmbeddingIdAndUserId(@Param("embeddingId") String embeddingId,
                                               @Param("userId") Long userId);
}
