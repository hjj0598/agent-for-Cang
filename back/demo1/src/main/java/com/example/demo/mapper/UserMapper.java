package com.example.demo.mapper;

import com.example.demo.pojo.AppUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("select id, username, password_hash, nickname, role, created_at, updated_at from app_user where id = #{id}")
    AppUser findById(Long id);
    @Select("select id, username, password_hash, nickname, role, created_at, updated_at " +
            "from app_user where username = #{username}")
    AppUser findByUsername(String username);

    @Insert("insert into app_user(username, password_hash, nickname, role) " +
            "values(#{username}, #{passwordHash}, #{nickname}, #{role})")
    void insert(AppUser user);

    @Update("update app_user set password_hash = #{passwordHash} where id = #{id}")
    void updatePasswordHash(@Param("id") Long id,
                            @Param("passwordHash") String passwordHash);

    @Update("update app_user set password_hash = #{passwordHash} where username = #{username}")
    void updatePasswordHashByUsername(@Param("username") String username,
                                      @Param("passwordHash") String passwordHash);

    @Update("update app_user set nickname = #{nickname} where id = #{id}")
    void updateNickname(@Param("id") Long id,
                        @Param("nickname") String nickname);

    @Select("""
            select id, username, password_hash, nickname, role, created_at, updated_at
            from app_user
            where #{keyword} = ''
               or username like concat('%', #{keyword}, '%')
               or nickname like concat('%', #{keyword}, '%')
            order by id desc
            limit #{pageSize} offset #{offset}
            """)
    List<AppUser> findPage(@Param("keyword") String keyword,
                           @Param("offset") Integer offset,
                           @Param("pageSize") Integer pageSize);

    @Select("""
            select count(*)
            from app_user
            where #{keyword} = ''
               or username like concat('%', #{keyword}, '%')
               or nickname like concat('%', #{keyword}, '%')
            """)
    Long countPage(@Param("keyword") String keyword);

    @Select("select count(*) from app_user")
    Long countAll();

    @Update("update app_user set role = #{role} where id = #{id}")
    int updateRole(@Param("id") Long id,
                   @Param("role") String role);
}
