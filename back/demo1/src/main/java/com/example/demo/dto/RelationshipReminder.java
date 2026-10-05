package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 首页展示的关系维护提醒。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipReminder {

    private Long personId;

    private String personName;

    /**
     * DATE：重要日期；STALE：关系资料长期未更新。
     */
    private String reminderType;

    private String title;

    private String detail;

    private LocalDateTime referenceTime;

    /**
     * DATE 提醒的结构化结果。旧数据无法解析时保持为空，不影响原有提醒。
     */
    private String eventDate;

    private Integer daysUntil;

    /**
     * UPCOMING、TODAY、PASSED、UNPARSED。
     */
    private String dateStatus;

    private String source;

    public RelationshipReminder(Long personId,
                                String personName,
                                String reminderType,
                                String title,
                                String detail,
                                LocalDateTime referenceTime) {
        this.personId = personId;
        this.personName = personName;
        this.reminderType = reminderType;
        this.title = title;
        this.detail = detail;
        this.referenceTime = referenceTime;
    }
}
