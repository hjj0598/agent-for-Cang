package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserResponse {

    private Long id;
    private String username;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
