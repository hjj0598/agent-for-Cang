package com.example.demo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {

    private Long id;

    private Long userId;

    private Long sessionId;

    // user 表示用户消息，assistant 表示 AI 回复
    private String role;

    // 保存完整消息内容；AI 流式输出结束后，再把完整回复存进来
    private String content;

    private LocalDateTime createdAt;
}
