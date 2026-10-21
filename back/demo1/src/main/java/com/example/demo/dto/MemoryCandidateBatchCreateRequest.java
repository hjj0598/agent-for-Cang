package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MemoryCandidateBatchCreateRequest {

    private Long personId;

    @Size(max = 50, message = "人物姓名不能超过50个字符")
    private String personName;

    @Size(max = 30, message = "来源类型不能超过30个字符")
    private String sourceType;

    @Size(max = 255, message = "来源引用不能超过255个字符")
    private String sourceReference;

    @Size(max = 10000, message = "原始证据不能超过10000个字符")
    private String rawEvidence;

    @Valid
    @NotEmpty(message = "候选记忆列表不能为空")
    private List<MemoryCandidateCreateRequest> candidates;
}
