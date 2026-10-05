package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeTextRequest {

    @NotBlank(message = "知识标题不能为空")
    @Size(max = 100, message = "知识标题不能超过100个字符")
    private String title;

    @NotBlank(message = "知识内容不能为空")
    @Size(max = 20000, message = "知识内容不能超过20000个字符")
    private String content;
}
