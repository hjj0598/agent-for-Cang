package com.example.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemoryCandidateConfirmRequest {

    private Long personId;

    @Size(max = 50, message = "人物姓名不能超过50个字符")
    private String personName;

    @Size(max = 30, message = "记忆类型不能超过30个字符")
    private String memoryType;

    @Size(max = 5000, message = "记忆内容不能超过5000个字符")
    private String content;

    @Size(max = 100, message = "记忆来源不能超过100个字符")
    private String source;

    private String conflictAction;

    private Long conflictMemoryId;
}
