package com.example.demo.dto;

import lombok.Data;

@Data
public class KnowledgeHit {

    private Long documentId;

    private String documentTitle;

    private String documentSource;

    private String content;

    private Double score;
}
