package com.example.demo.controller;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PersonMemory {
    private Long id;
    private Long userId;
    private Long personId;
    private String memoryType;
    private String content;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}