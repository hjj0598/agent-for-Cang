package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    private Long personCount;

    private Long knowledgeCount;

    private Long chatSessionCount;

    private Long chatMessageCount;
}
