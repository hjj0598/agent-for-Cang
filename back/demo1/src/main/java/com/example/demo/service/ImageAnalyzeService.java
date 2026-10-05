package com.example.demo.service;

import com.example.demo.dto.ImageAnalyzeResponse;
import com.example.demo.dto.MemoryExtractionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImageAnalyzeService {

    ImageAnalyzeResponse analyze(MultipartFile file, String prompt);

    MemoryExtractionResponse extractMemoryCandidates(MultipartFile file, String personName);
}
