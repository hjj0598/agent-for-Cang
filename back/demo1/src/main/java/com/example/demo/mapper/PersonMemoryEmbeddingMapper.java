package com.example.demo.mapper;

import com.example.demo.dto.PersonMemoryEvidence;
import com.example.demo.pojo.PersonMemoryEmbedding;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PersonMemoryEmbeddingMapper {

    @Update("""
        create table if not exists person_memory_embedding (
            id bigint primary key auto_increment,
            user_id bigint not null,
            person_id bigint not null,
            memory_id bigint not null,
            embedding_id varchar(100) not null,
            embedding_text text not null,
            created_at datetime not null default current_timestamp,
            updated_at datetime not null default current_timestamp on update current_timestamp,
            unique key uk_person_memory_embedding_memory (memory_id),
            key idx_person_memory_embedding_user (user_id),
            key idx_person_memory_embedding_embedding (embedding_id)
        )
        """)
    void createTableIfNotExists();

    @Select("""
        select id, user_id, person_id, memory_id, embedding_id, embedding_text, created_at, updated_at
        from person_memory_embedding
        where memory_id = #{memoryId}
        limit 1
        """)
    PersonMemoryEmbedding findByMemoryId(@Param("memoryId") Long memoryId);

    @Select("""
        select
            pm.id as memoryId,
            pme.embedding_id as embeddingId,
            pm.person_id as personId,
            p.name as personName,
            p.relation,
            pm.memory_type as memoryType,
            pm.content,
            pm.source,
            pm.created_at as createdAt,
            pm.updated_at as updatedAt
        from person_memory_embedding pme
        join person_memory pm on pme.memory_id = pm.id
        join person p on pm.person_id = p.id
        where pme.embedding_id = #{embeddingId}
          and pme.user_id = #{userId}
          and pm.user_id = #{userId}
          and p.user_id = #{userId}
        limit 1
        """)
    PersonMemoryEvidence findEvidenceByEmbeddingIdAndUserId(@Param("embeddingId") String embeddingId,
                                                            @Param("userId") Long userId);

    @Insert("""
        insert into person_memory_embedding(user_id, person_id, memory_id, embedding_id, embedding_text)
        values(#{userId}, #{personId}, #{memoryId}, #{embeddingId}, #{embeddingText})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PersonMemoryEmbedding embedding);
}
