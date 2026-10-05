package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStats {

    private Long userCount;
    private Long personCount;
    private Long knowledgeCount;
    private Long chatSessionCount;
    private Long chatMessageCount;
    private Long feedbackCount;
    private Long operationLogCount;
}
