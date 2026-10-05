package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatHistorySearchResult {

    private Long sessionId;

    private String memoryId;

    private String sessionTitle;

    private Long messageId;

    private String role;

    private String content;

    private LocalDateTime createdAt;
}
