package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemoryCandidateCreateRequest {

    private Long personId;

    @Size(max = 50, message = "人物姓名不能超过50个字符")
    private String personName;

    @Size(max = 30, message = "记忆类型不能超过30个字符")
    private String memoryType;

    @NotBlank(message = "候选记忆内容不能为空")
    @Size(max = 5000, message = "候选记忆内容不能超过5000个字符")
    private String content;

    @Size(max = 30, message = "来源类型不能超过30个字符")
    private String sourceType;

    @Size(max = 255, message = "来源引用不能超过255个字符")
    private String sourceReference;

    private Double confidence;

    @Size(max = 30, message = "事实属性不能超过30个字符")
    private String factType;

    @Size(max = 20, message = "可信等级不能超过20个字符")
    private String trustLevel;

    @Size(max = 10000, message = "原始证据不能超过10000个字符")
    private String rawEvidence;
}
