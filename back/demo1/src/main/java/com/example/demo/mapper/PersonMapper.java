package com.example.demo.mapper;

import com.example.demo.pojo.Person;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PersonMapper {

    @Select("select id, user_id, name, relation, description, created_at, updated_at from person where user_id = #{userId} order by id desc")
    List<Person> findByUserId(Long userId);

    @Select("""
            select id, user_id, name, relation, description, created_at, updated_at
            from person
            where user_id = #{userId}
            order by id desc
            limit #{pageSize} offset #{offset}
            """)
    List<Person> findPageByUserId(@Param("userId") Long userId,
                                  @Param("offset") Integer offset,
                                  @Param("pageSize") Integer pageSize);

    @Select("select count(*) from person where user_id = #{userId}")
    Long countByUserId(Long userId);

    @Select("select count(*) from person")
    Long countAll();

    @Select("select id, user_id, name, relation, description, created_at, updated_at from person where id = #{id} and user_id = #{userId}")
    Person findByIdAndUserId(Long id, Long userId);

    @Insert("insert into person(user_id, name, relation, description) values(#{userId}, #{name}, #{relation}, #{description})")
    void insert(Person person);

    @Update("update person set name = #{name}, relation = #{relation}, description = #{description} where id = #{id} and user_id = #{userId}")
    void update(Person person);

    @Delete("delete from person where id = #{id} and user_id = #{userId}")
    void deleteByIdAndUserId(Long id, Long userId);

    @Select("select id, user_id, name, relation, description, created_at, updated_at from person where name = #{name} limit 1")
    Person findByName(String name);

    @Select("select id, user_id, name, relation, description, created_at, updated_at from person where user_id = #{userId} and name = #{name}")
    Person findByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
}
