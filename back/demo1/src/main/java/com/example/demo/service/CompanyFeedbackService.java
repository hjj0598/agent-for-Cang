package com.example.demo.service;

import com.example.demo.dto.PageResult;
import com.example.demo.pojo.CompanyFeedback;

public interface CompanyFeedbackService {

    void submit(CompanyFeedback feedback);

    PageResult<CompanyFeedback> page(Long userId, Integer page, Integer pageSize);

    PageResult<CompanyFeedback> pageAll(Integer page, Integer pageSize, Long userId, String category, String status);

    void updateStatus(Long id, String status);
}
