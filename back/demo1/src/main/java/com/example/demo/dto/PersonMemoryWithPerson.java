package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查询重要日期提醒时使用的轻量投影对象。
 */
@Data
public class PersonMemoryWithPerson {

    private Long id;

    private Long personId;

    private String personName;

    private String memoryType;

    private String content;

    private String source;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
