package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyFeedbackRequest {

    @Size(max = 30, message = "分类长度不能超过30个字符")
    private String category;

    @NotBlank(message = "意见内容不能为空")
    @Size(max = 2000, message = "意见内容不能超过2000个字符")
    private String content;
}
