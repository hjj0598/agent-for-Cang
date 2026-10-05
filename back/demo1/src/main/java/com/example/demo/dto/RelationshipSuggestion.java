package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 或规则引擎生成的关系维护建议。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipSuggestion {

    private Long personId;

    private String personName;

    /**
     * HIGH、MEDIUM、LOW。
     */
    private String priority;

    private String title;

    private String reason;

    private String action;

    /**
     * AI：模型生成；RULE：模型不可用时的规则兜底。
     */
    private String generatedBy;
}
