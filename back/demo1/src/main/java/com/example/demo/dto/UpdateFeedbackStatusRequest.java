package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateFeedbackStatusRequest {

    @NotBlank(message = "处理状态不能为空")
    @Pattern(regexp = "SUBMITTED|REVIEWED|DONE", message = "处理状态只能是SUBMITTED、REVIEWED或DONE")
    private String status;
}
