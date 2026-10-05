package com.example.demo.mapper;

import com.example.demo.pojo.CompanyFeedback;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CompanyFeedbackMapper {

    @Insert("""
            insert into company_feedback(user_id, category, content, status)
            values(#{userId}, #{category}, #{content}, #{status})
            """)
    void insert(CompanyFeedback feedback);

    @Select("""
            select cf.id, cf.user_id, u.username, u.nickname, cf.category, cf.content, cf.status, cf.created_at, cf.updated_at
            from company_feedback cf
            left join app_user u on cf.user_id = u.id
            where cf.user_id = #{userId}
            order by cf.id desc
            limit #{offset}, #{pageSize}
            """)
    List<CompanyFeedback> findByUserId(@Param("userId") Long userId,
                                       @Param("offset") Integer offset,
                                       @Param("pageSize") Integer pageSize);

    @Select("select count(*) from company_feedback where user_id = #{userId}")
    Long countByUserId(Long userId);

    @Select("""
            select cf.id, cf.user_id, u.username, u.nickname, cf.category, cf.content, cf.status, cf.created_at, cf.updated_at
            from company_feedback cf
            left join app_user u on cf.user_id = u.id
            where (#{userId} is null or cf.user_id = #{userId})
              and (#{category} = '' or cf.category = #{category})
              and (#{status} = '' or cf.status = #{status})
            order by cf.id desc
            limit #{pageSize} offset #{offset}
            """)
    List<CompanyFeedback> findAll(@Param("userId") Long userId,
                                  @Param("category") String category,
                                  @Param("status") String status,
                                  @Param("offset") Integer offset,
                                  @Param("pageSize") Integer pageSize);

    @Select("""
            select count(*)
            from company_feedback
            where (#{userId} is null or user_id = #{userId})
              and (#{category} = '' or category = #{category})
              and (#{status} = '' or status = #{status})
            """)
    Long countAllByCondition(@Param("userId") Long userId,
                             @Param("category") String category,
                             @Param("status") String status);

    @Select("select count(*) from company_feedback")
    Long countAll();

    @Select("""
            select cf.id, cf.user_id, u.username, u.nickname, cf.category, cf.content, cf.status, cf.created_at, cf.updated_at
            from company_feedback cf
            left join app_user u on cf.user_id = u.id
            where cf.id = #{id}
            """)
    CompanyFeedback findById(Long id);

    @Update("update company_feedback set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status);
}
