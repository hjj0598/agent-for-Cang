package com.example.demo.mapper;

import com.example.demo.pojo.RelationshipSuggestionAction;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RelationshipSuggestionActionMapper {

    @Update("""
        create table if not exists relationship_suggestion_action (
            id bigint primary key auto_increment,
            user_id bigint not null,
            person_id bigint null,
            person_name varchar(50) not null,
            priority varchar(20) not null default 'MEDIUM',
            title varchar(120) not null,
            reason text null,
            action text not null,
            generated_by varchar(20) not null default 'RULE',
            suggestion_key varchar(64) not null,
            status varchar(20) not null default 'PLANNED',
            outcome text null,
            feedback text null,
            helpfulness_rating int null,
            planned_at datetime null,
            executed_at datetime null,
            created_at datetime not null default current_timestamp,
            updated_at datetime not null default current_timestamp on update current_timestamp,
            unique key uk_suggestion_action_key (user_id, suggestion_key),
            key idx_suggestion_action_user_status (user_id, status, updated_at),
            key idx_suggestion_action_user_person (user_id, person_id, updated_at)
        )
        """)
    void createTableIfNotExists();

    @Insert("""
        insert into relationship_suggestion_action(
            user_id, person_id, person_name, priority, title, reason, action,
            generated_by, suggestion_key, status, outcome, feedback,
            helpfulness_rating, planned_at, executed_at
        ) values(
            #{userId}, #{personId}, #{personName}, #{priority}, #{title}, #{reason}, #{action},
            #{generatedBy}, #{suggestionKey}, #{status}, #{outcome}, #{feedback},
            #{helpfulnessRating}, #{plannedAt}, #{executedAt}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RelationshipSuggestionAction action);

    @Select("""
        select id, user_id as userId, person_id as personId, person_name as personName,
               priority, title, reason, action, generated_by as generatedBy,
               suggestion_key as suggestionKey, status, outcome, feedback,
               helpfulness_rating as helpfulnessRating, planned_at as plannedAt,
               executed_at as executedAt, created_at as createdAt, updated_at as updatedAt
        from relationship_suggestion_action
        where id = #{id}
          and user_id = #{userId}
        limit 1
        """)
    RelationshipSuggestionAction findByIdAndUserId(@Param("id") Long id,
                                                   @Param("userId") Long userId);

    @Select("""
        select id, user_id as userId, person_id as personId, person_name as personName,
               priority, title, reason, action, generated_by as generatedBy,
               suggestion_key as suggestionKey, status, outcome, feedback,
               helpfulness_rating as helpfulnessRating, planned_at as plannedAt,
               executed_at as executedAt, created_at as createdAt, updated_at as updatedAt
        from relationship_suggestion_action
        where user_id = #{userId}
          and suggestion_key = #{suggestionKey}
        limit 1
        """)
    RelationshipSuggestionAction findBySuggestionKey(@Param("userId") Long userId,
                                                     @Param("suggestionKey") String suggestionKey);

    @Select("""
        select id, user_id as userId, person_id as personId, person_name as personName,
               priority, title, reason, action, generated_by as generatedBy,
               suggestion_key as suggestionKey, status, outcome, feedback,
               helpfulness_rating as helpfulnessRating, planned_at as plannedAt,
               executed_at as executedAt, created_at as createdAt, updated_at as updatedAt
        from relationship_suggestion_action
        where user_id = #{userId}
          and (#{status} is null or #{status} = '' or status = #{status})
          and (#{personId} is null or person_id = #{personId})
        order by updated_at desc, id desc
        limit #{limit}
        """)
    List<RelationshipSuggestionAction> findByUserId(@Param("userId") Long userId,
                                                    @Param("status") String status,
                                                    @Param("personId") Long personId,
                                                    @Param("limit") Integer limit);

    @Select("""
        select id, user_id as userId, person_id as personId, person_name as personName,
               priority, title, reason, action, generated_by as generatedBy,
               suggestion_key as suggestionKey, status, outcome, feedback,
               helpfulness_rating as helpfulnessRating, planned_at as plannedAt,
               executed_at as executedAt, created_at as createdAt, updated_at as updatedAt
        from relationship_suggestion_action
        where user_id = #{userId}
          and (outcome is not null or feedback is not null or helpfulness_rating is not null)
        order by updated_at desc, id desc
        limit #{limit}
        """)
    List<RelationshipSuggestionAction> findRecentFeedback(@Param("userId") Long userId,
                                                          @Param("limit") Integer limit);

    @Update("""
        update relationship_suggestion_action
        set person_id = #{personId},
            person_name = #{personName},
            priority = #{priority},
            title = #{title},
            reason = #{reason},
            action = #{action},
            generated_by = #{generatedBy},
            status = #{status},
            outcome = #{outcome},
            feedback = #{feedback},
            helpfulness_rating = #{helpfulnessRating},
            planned_at = #{plannedAt},
            executed_at = #{executedAt}
        where id = #{id}
          and user_id = #{userId}
        """)
    int update(RelationshipSuggestionAction action);
}
