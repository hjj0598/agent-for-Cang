package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图片记忆提取结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryExtractionResponse {

    private String personName;

    private List<MemoryCandidate> candidates;

    private String model;

    private String filename;

    /**
     * 当模型没有严格返回 JSON 时保留原始结果，方便用户手动参考。
     */
    private String rawAnswer;
}
