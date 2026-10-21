package com.example.demo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RelationshipSuggestionAction {
    private Long id;
    private Long userId;
    private Long personId;
    private String personName;
    private String priority;
    private String title;
    private String reason;
    private String action;
    private String generatedBy;
    private String suggestionKey;
    private String status;
    private String outcome;
    private String feedback;
    private Integer helpfulnessRating;
    private LocalDateTime plannedAt;
    private LocalDateTime executedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
