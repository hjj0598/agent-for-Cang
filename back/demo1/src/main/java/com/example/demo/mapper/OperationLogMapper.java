package com.example.demo.mapper;

import com.example.demo.pojo.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    @Insert("""
            insert into operation_log(user_id, operation_type, operation_content)
            values(#{userId}, #{operationType}, #{operationContent})
            """)
    void insert(OperationLog operationLog);

    @Select("""
            select ol.id, ol.user_id, u.username, u.nickname, ol.operation_type, ol.operation_content, ol.created_at
            from operation_log ol
            left join app_user u on ol.user_id = u.id
            where ol.user_id = #{userId}
            order by ol.id desc
            limit #{limit}
            """)
    List<OperationLog> findRecentByUserId(@Param("userId") Long userId,
                                          @Param("limit") Integer limit);

    @Select("""
            select ol.id, ol.user_id, u.username, u.nickname, ol.operation_type, ol.operation_content, ol.created_at
            from operation_log ol
            left join app_user u on ol.user_id = u.id
            order by ol.id desc
            limit #{limit}
            """)
    List<OperationLog> findRecentAll(@Param("limit") Integer limit);

    @Select("""
            select ol.id, ol.user_id, u.username, u.nickname, ol.operation_type, ol.operation_content, ol.created_at
            from operation_log ol
            left join app_user u on ol.user_id = u.id
            where (#{userId} is null or ol.user_id = #{userId})
              and (#{keyword} = ''
                   or ol.operation_type like concat('%', #{keyword}, '%')
                   or ol.operation_content like concat('%', #{keyword}, '%')
                   or u.username like concat('%', #{keyword}, '%')
                   or u.nickname like concat('%', #{keyword}, '%'))
            order by ol.id desc
            limit #{pageSize} offset #{offset}
            """)
    List<OperationLog> findPageAll(@Param("userId") Long userId,
                                   @Param("keyword") String keyword,
                                   @Param("offset") Integer offset,
                                   @Param("pageSize") Integer pageSize);

    @Select("""
            select count(*)
            from operation_log ol
            left join app_user u on ol.user_id = u.id
            where (#{userId} is null or ol.user_id = #{userId})
              and (#{keyword} = ''
                   or ol.operation_type like concat('%', #{keyword}, '%')
                   or ol.operation_content like concat('%', #{keyword}, '%')
                   or u.username like concat('%', #{keyword}, '%')
                   or u.nickname like concat('%', #{keyword}, '%'))
            """)
    Long countPageAll(@Param("userId") Long userId,
                      @Param("keyword") String keyword);

    @Select("select count(*) from operation_log")
    Long countAll();
}
