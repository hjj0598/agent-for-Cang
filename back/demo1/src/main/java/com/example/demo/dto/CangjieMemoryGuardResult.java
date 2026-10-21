package com.example.demo.dto;

import lombok.Data;

@Data
public class CangjieMemoryGuardResult {
    private String memoryType;
    private String trustLevel;
    private String factType;
    private Double confidence;
    private String riskLevel;
    private Boolean needsReview;
    private Boolean hasTemporalChange;
    private Boolean hasConflictRisk;
    private String reason;
}
