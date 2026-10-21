package com.example.demo.mapper;

import com.example.demo.pojo.MemoryCandidateRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MemoryCandidateMapper {

    @Update("""
        create table if not exists memory_candidate (
            id bigint primary key auto_increment,
            user_id bigint not null,
            person_id bigint null,
            person_name varchar(50) null,
            memory_type varchar(30) not null default 'NOTE',
            content text not null,
            source_type varchar(30) not null default '手动记录',
            source_reference varchar(255) null,
            source varchar(255) null,
            confidence double null,
            fact_type varchar(30) not null default 'AI_EXTRACTED_FACT',
            trust_level varchar(20) not null default 'MEDIUM',
            status varchar(20) not null default 'PENDING',
            raw_evidence text null,
            reject_reason varchar(255) null,
            confirmed_memory_id bigint null,
            confirmed_at datetime null,
            created_at datetime not null default current_timestamp,
            updated_at datetime not null default current_timestamp on update current_timestamp,
            key idx_memory_candidate_user_status (user_id, status),
            key idx_memory_candidate_user_person (user_id, person_id),
            key idx_memory_candidate_created_at (created_at)
        )
        """)
    void createTableIfNotExists();

    @Select("""
        select id, user_id, person_id, person_name, memory_type, content, source_type,
               source_reference, source, confidence, fact_type, trust_level, status,
               raw_evidence, reject_reason, confirmed_memory_id, confirmed_at,
               created_at, updated_at
        from memory_candidate
        where user_id = #{userId}
          and (#{status} is null or #{status} = '' or status = #{status})
          and (#{personId} is null or person_id = #{personId})
        order by updated_at desc, id desc
        limit #{limit}
        """)
    List<MemoryCandidateRecord> findByUserId(@Param("userId") Long userId,
                                             @Param("status") String status,
                                             @Param("personId") Long personId,
                                             @Param("limit") Integer limit);

    @Select("""
        select id, user_id, person_id, person_name, memory_type, content, source_type,
               source_reference, source, confidence, fact_type, trust_level, status,
               raw_evidence, reject_reason, confirmed_memory_id, confirmed_at,
               created_at, updated_at
        from memory_candidate
        where id = #{id}
          and user_id = #{userId}
        limit 1
        """)
    MemoryCandidateRecord findByIdAndUserId(@Param("id") Long id,
                                            @Param("userId") Long userId);

    @Insert("""
        insert into memory_candidate(
            user_id, person_id, person_name, memory_type, content, source_type,
            source_reference, source, confidence, fact_type, trust_level, status,
            raw_evidence
        ) values(
            #{userId}, #{personId}, #{personName}, #{memoryType}, #{content}, #{sourceType},
            #{sourceReference}, #{source}, #{confidence}, #{factType}, #{trustLevel}, #{status},
            #{rawEvidence}
        )
        """)
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(MemoryCandidateRecord candidate);

    @Update("""
        update memory_candidate
        set person_id = #{personId},
            person_name = #{personName},
            memory_type = #{memoryType},
            content = #{content},
            source_type = #{sourceType},
            source_reference = #{sourceReference},
            source = #{source},
            confidence = #{confidence},
            fact_type = #{factType},
            trust_level = #{trustLevel},
            raw_evidence = #{rawEvidence}
        where id = #{id}
          and user_id = #{userId}
          and status = 'PENDING'
        """)
    int updatePending(MemoryCandidateRecord candidate);

    @Update("""
        update memory_candidate
        set status = 'CONFIRMED',
            confirmed_memory_id = #{confirmedMemoryId},
            confirmed_at = now()
        where id = #{id}
          and user_id = #{userId}
          and status = 'PENDING'
        """)
    int markConfirmed(@Param("id") Long id,
                      @Param("userId") Long userId,
                      @Param("confirmedMemoryId") Long confirmedMemoryId);

    @Update("""
        update memory_candidate
        set status = 'REJECTED',
            reject_reason = #{reason}
        where id = #{id}
          and user_id = #{userId}
          and status = 'PENDING'
        """)
    int markRejected(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("reason") String reason);

    @Delete("delete from memory_candidate where id = #{id} and user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id,
                            @Param("userId") Long userId);
}
