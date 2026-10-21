package com.example.demo.mapper;

import com.example.demo.pojo.PersonMemoryRelation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PersonMemoryRelationMapper {

    @Update("""
        create table if not exists person_memory_relation (
            id bigint primary key auto_increment,
            user_id bigint not null,
            person_id bigint not null,
            old_memory_id bigint not null,
            new_memory_id bigint not null,
            relation_type varchar(30) not null,
            relation_reason varchar(500) null,
            confidence double null,
            created_at datetime not null default current_timestamp,
            unique key uk_memory_relation_pair (user_id, old_memory_id, new_memory_id, relation_type),
            key idx_memory_relation_user_person (user_id, person_id, created_at),
            key idx_memory_relation_old_memory (old_memory_id),
            key idx_memory_relation_new_memory (new_memory_id)
        )
        """)
    void createTableIfNotExists();

    @Insert("""
        insert ignore into person_memory_relation(
            user_id, person_id, old_memory_id, new_memory_id,
            relation_type, relation_reason, confidence
        ) values(
            #{userId}, #{personId}, #{oldMemoryId}, #{newMemoryId},
            #{relationType}, #{relationReason}, #{confidence}
        )
        """)
    void insert(PersonMemoryRelation relation);

    @Select("""
        select
            r.id,
            r.user_id as userId,
            r.person_id as personId,
            r.old_memory_id as oldMemoryId,
            r.new_memory_id as newMemoryId,
            r.relation_type as relationType,
            r.relation_reason as relationReason,
            r.confidence,
            r.created_at as createdAt,
            p.name as personName,
            old_pm.memory_type as oldMemoryType,
            old_pm.content as oldMemoryContent,
            old_pm.source as oldMemorySource,
            old_pm.created_at as oldMemoryCreatedAt,
            new_pm.memory_type as newMemoryType,
            new_pm.content as newMemoryContent,
            new_pm.source as newMemorySource,
            new_pm.created_at as newMemoryCreatedAt
        from person_memory_relation r
        join person p on r.person_id = p.id and p.user_id = r.user_id
        left join person_memory old_pm on r.old_memory_id = old_pm.id and old_pm.user_id = r.user_id
        left join person_memory new_pm on r.new_memory_id = new_pm.id and new_pm.user_id = r.user_id
        where r.user_id = #{userId}
          and r.person_id = #{personId}
        order by r.created_at desc, r.id desc
        limit #{limit}
        """)
    List<PersonMemoryRelation> findByPersonId(@Param("userId") Long userId,
                                              @Param("personId") Long personId,
                                              @Param("limit") Integer limit);

    @Select("""
        select
            r.id,
            r.user_id as userId,
            r.person_id as personId,
            r.old_memory_id as oldMemoryId,
            r.new_memory_id as newMemoryId,
            r.relation_type as relationType,
            r.relation_reason as relationReason,
            r.confidence,
            r.created_at as createdAt,
            p.name as personName,
            old_pm.memory_type as oldMemoryType,
            old_pm.content as oldMemoryContent,
            old_pm.source as oldMemorySource,
            old_pm.created_at as oldMemoryCreatedAt,
            new_pm.memory_type as newMemoryType,
            new_pm.content as newMemoryContent,
            new_pm.source as newMemorySource,
            new_pm.created_at as newMemoryCreatedAt
        from person_memory_relation r
        join person p on r.person_id = p.id and p.user_id = r.user_id
        left join person_memory old_pm on r.old_memory_id = old_pm.id and old_pm.user_id = r.user_id
        left join person_memory new_pm on r.new_memory_id = new_pm.id and new_pm.user_id = r.user_id
        where r.user_id = #{userId}
          and r.person_id = #{personId}
          and (r.old_memory_id = #{memoryId} or r.new_memory_id = #{memoryId})
        order by r.created_at desc, r.id desc
        limit #{limit}
        """)
    List<PersonMemoryRelation> findByMemoryId(@Param("userId") Long userId,
                                              @Param("personId") Long personId,
                                              @Param("memoryId") Long memoryId,
                                              @Param("limit") Integer limit);
}
