package com.example.demo.mapper;

import com.example.demo.dto.PersonMemoryWithPerson;
import com.example.demo.pojo.PersonMemory;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PersonMemoryMapper {

    @Select("select id, user_id, person_id, memory_type, content, source, created_at, updated_at from person_memory where user_id = #{userId} and person_id = #{personId} order by id desc")
    List<PersonMemory> findByPersonId(Long userId, Long personId);

    @Insert("insert into person_memory(user_id, person_id, memory_type, content, source) values(#{userId}, #{personId}, #{memoryType}, #{content}, #{source})")
    void insert(PersonMemory memory);

    @Delete("delete from person_memory where id = #{memoryId} and person_id = #{personId} and user_id = #{userId}")
    void deleteById(Long memoryId, Long personId, Long userId);

    @Select("""
        select pm.id, pm.user_id, pm.person_id, pm.memory_type, pm.content, pm.source, pm.created_at, pm.updated_at
        from person_memory pm
        join person p on pm.person_id = p.id
        where p.name = #{personName}
        order by pm.id desc
        """)
    List<PersonMemory> findByPersonName(String personName);


    @Select("""
        select pm.id, pm.user_id, pm.person_id, pm.memory_type, pm.content, pm.source, pm.created_at, pm.updated_at
        from person_memory pm
        join person p on pm.person_id = p.id
        where pm.user_id = #{userId}
          and p.user_id = #{userId}
          and p.name = #{personName}
        order by pm.id desc
        """)
    List<PersonMemory> findByUserIdAndPersonName(@Param("userId") Long userId,
                                                 @Param("personName") String personName);

    @Select("""
        select pm.id, pm.user_id, pm.person_id, pm.memory_type, pm.content, pm.source, pm.created_at, pm.updated_at
        from person_memory pm
        join person p on pm.person_id = p.id
        where pm.user_id = #{userId}
          and p.user_id = #{userId}
          and p.name like concat('%', #{personName}, '%')
        order by pm.id desc
        limit 100
        """)
    List<PersonMemory> searchByUserIdAndPersonName(@Param("userId") Long userId,
                                                   @Param("personName") String personName);

    @Select("""
        select max(updated_at)
        from person_memory
        where user_id = #{userId}
          and person_id = #{personId}
        """)
    LocalDateTime findLatestUpdatedAtByPersonId(@Param("userId") Long userId,
                                                @Param("personId") Long personId);

    @Select("""
        select
            pm.id,
            pm.person_id as personId,
            p.name as personName,
            pm.memory_type as memoryType,
            pm.content,
            pm.source,
            pm.created_at as createdAt,
            pm.updated_at as updatedAt
        from person_memory pm
        join person p on pm.person_id = p.id
        where pm.user_id = #{userId}
          and p.user_id = #{userId}
          and pm.memory_type = 'DATE'
        order by pm.updated_at desc, pm.id desc
        """)
    List<PersonMemoryWithPerson> findDateMemoriesByUserId(@Param("userId") Long userId);
}
