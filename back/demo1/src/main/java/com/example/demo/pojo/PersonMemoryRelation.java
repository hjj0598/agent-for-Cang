package com.example.demo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PersonMemoryRelation {
    private Long id;
    private Long userId;
    private Long personId;
    private Long oldMemoryId;
    private Long newMemoryId;
    private String relationType;
    private String relationReason;
    private Double confidence;
    private LocalDateTime createdAt;

    private String personName;
    private String oldMemoryType;
    private String oldMemoryContent;
    private String oldMemorySource;
    private LocalDateTime oldMemoryCreatedAt;
    private String newMemoryType;
    private String newMemoryContent;
    private String newMemorySource;
    private LocalDateTime newMemoryCreatedAt;
}
