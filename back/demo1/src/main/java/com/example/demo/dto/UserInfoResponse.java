package com.example.demo.dto;

import lombok.Data;

@Data
public class UserInfoResponse {
    private Long id;
    private String username;
    private String nickname;
    private String role;
}
