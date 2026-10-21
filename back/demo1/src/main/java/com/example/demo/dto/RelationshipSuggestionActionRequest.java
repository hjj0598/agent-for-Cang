package com.example.demo.dto;

import lombok.Data;

@Data
public class RelationshipSuggestionActionRequest {
    private Long personId;
    private String personName;
    private String priority;
    private String title;
    private String reason;
    private String action;
    private String generatedBy;
    private String status;
    private String outcome;
    private String feedback;
    private Integer helpfulnessRating;
}
