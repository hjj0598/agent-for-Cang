package com.example.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemoryCandidateRejectRequest {

    @Size(max = 255, message = "拒绝原因不能超过255个字符")
    private String reason;
}
