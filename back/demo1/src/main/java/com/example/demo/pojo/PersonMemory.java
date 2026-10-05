package com.example.demo.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PersonMemory {
    private Long id;
    private Long userId;
    private Long personId;
    private String memoryType;
    private String content;
    private String source;

    /**
     * 由 source 推导出的来源类型和来源引用。
     * 不新增数据库字段，兼容已有 person_memory 表。
     */
    private String sourceType;
    private String sourceReference;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
