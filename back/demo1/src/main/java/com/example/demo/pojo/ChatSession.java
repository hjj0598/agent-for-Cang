package com.example.demo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSession {

    private Long id;

    private Long userId;

    // 前端传给 /ai/chat 的 memoryId，也是 LangChain4j 会话记忆使用的 ID
    private String memoryId;

    // 会话标题，默认取用户第一条问题的前一小段
    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
