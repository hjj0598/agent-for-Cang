package com.example.demo.controller;

import com.example.demo.dto.ImageAnalyzeResponse;
import com.example.demo.dto.MemoryExtractionResponse;
import com.example.demo.pojo.Result;
import com.example.demo.service.ImageAnalyzeService;
import com.example.demo.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
public class ImageAnalyzeController {

    @Autowired
    private ImageAnalyzeService imageAnalyzeService;

    @Autowired
    private OperationLogService operationLogService;

    @PostMapping("/analyze")
    public Result<ImageAnalyzeResponse> analyze(@RequestParam("file") MultipartFile file,
                                                @RequestParam(required = false) String prompt,
                                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ImageAnalyzeResponse response = imageAnalyzeService.analyze(file, prompt);
        operationLogService.record(userId, "图片分析", "分析图片：" + response.getFilename());
        return Result.success(response);
    }

    /**
     * 图片分析的增强入口：只提取候选记忆，不直接写入 person_memory。
     * 用户确认后由前端复用已有的人物记忆保存接口。
     */
    @PostMapping("/memory-candidates")
    public Result<MemoryExtractionResponse> memoryCandidates(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String personName,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryExtractionResponse response = imageAnalyzeService.extractMemoryCandidates(file, personName);
        operationLogService.record(userId, "图片记忆提取", "提取图片候选记忆：" + response.getFilename());
        return Result.success(response);
    }
}
