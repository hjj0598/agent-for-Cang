package com.example.demo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeChunk {
    private Long id;
    private Long userId;
    private Long documentId;
    private String embeddingId;
    private Integer chunkIndex;
    private String content;
    private LocalDateTime createdAt;
}