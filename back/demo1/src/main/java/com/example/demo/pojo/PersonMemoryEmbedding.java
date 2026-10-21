package com.example.demo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PersonMemoryEmbedding {
    private Long id;
    private Long userId;
    private Long personId;
    private Long memoryId;
    private String embeddingId;
    private String embeddingText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
