package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PersonMemoryEvidence {
    private Long memoryId;
    private String embeddingId;
    private Long personId;
    private String personName;
    private String relation;
    private String memoryType;
    private String content;
    private String source;
    private String sourceType;
    private String sourceReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double score;
    private Double semanticScore;
    private Double personScore;
    private Double typeScore;
    private Double trustScore;
    private Double recencyScore;
}
