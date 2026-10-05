package com.example.demo.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Person {
    private Long id;
    private Long userId;
    private String name;
    private String relation;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}