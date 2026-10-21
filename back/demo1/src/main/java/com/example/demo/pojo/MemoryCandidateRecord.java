package com.example.demo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemoryCandidateRecord {
    private Long id;
    private Long userId;
    private Long personId;
    private String personName;
    private String memoryType;
    private String content;
    private String sourceType;
    private String sourceReference;
    private String source;
    private Double confidence;
    private String factType;
    private String trustLevel;
    private String status;
    private String rawEvidence;
    private String rejectReason;
    private Long confirmedMemoryId;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
