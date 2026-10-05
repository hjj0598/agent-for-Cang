package com.example.demo.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLog {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private String operationType;

    private String operationContent;

    private LocalDateTime createdAt;
}
