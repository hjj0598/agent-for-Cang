package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonRequest {

    @NotBlank(message = "人物姓名不能为空")
    @Size(max = 50, message = "人物姓名不能超过50个字符")
    private String name;

    @Size(max = 50, message = "人物关系不能超过50个字符")
    private String relation;

    @Size(max = 1000, message = "人物备注不能超过1000个字符")
    private String description;
}
