package com.example.demo.dto;

import lombok.Data;

@Data
public class RelationshipSuggestionActionUpdateRequest {
    private String status;
    private String outcome;
    private String feedback;
    private Integer helpfulnessRating;
}
