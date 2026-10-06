package com.example.demo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocument {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String source;
    private String fileHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
