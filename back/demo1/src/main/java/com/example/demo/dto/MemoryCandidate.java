package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视觉模型从图片中提取出的候选记忆。
 *
 * <p>候选记忆不会直接写入数据库，前端需要先让用户确认或编辑，
 * 再复用人物记忆接口保存。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryCandidate {

    private String memoryType;

    private String content;

    private Double confidence;
}
