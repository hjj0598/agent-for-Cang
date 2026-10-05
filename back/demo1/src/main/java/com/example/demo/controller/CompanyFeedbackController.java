package com.example.demo.controller;

import com.example.demo.annotation.AdminOnly;
import com.example.demo.dto.CompanyFeedbackRequest;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.UpdateFeedbackStatusRequest;
import com.example.demo.pojo.CompanyFeedback;
import com.example.demo.pojo.Result;
import com.example.demo.service.CompanyFeedbackService;
import com.example.demo.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedback")
public class CompanyFeedbackController {

    @Autowired
    private CompanyFeedbackService companyFeedbackService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<CompanyFeedback>> list(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(companyFeedbackService.page(userId, page, pageSize));
    }

    @AdminOnly
    @GetMapping("/admin/all")
    public Result<PageResult<CompanyFeedback>> listAll(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) Long userId,
                                                       @RequestParam(required = false) String category,
                                                       @RequestParam(required = false) String status) {
        return Result.success(companyFeedbackService.pageAll(page, pageSize, userId, category, status));
    }

    @AdminOnly
    @PutMapping("/admin/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody @Valid UpdateFeedbackStatusRequest statusRequest,
                                     HttpServletRequest request) {
        Long adminUserId = (Long) request.getAttribute("userId");
        companyFeedbackService.updateStatus(id, statusRequest.getStatus());
        operationLogService.record(adminUserId, "管理员处理意见", "意见ID：" + id + "，新状态：" + statusRequest.getStatus());
        return Result.success();
    }

    @PostMapping
    public Result<Void> submit(@RequestBody @Valid CompanyFeedbackRequest feedbackRequest,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        CompanyFeedback feedback = new CompanyFeedback();
        feedback.setUserId(userId);
        feedback.setCategory(feedbackRequest.getCategory());
        feedback.setContent(feedbackRequest.getContent());

        companyFeedbackService.submit(feedback);
        operationLogService.record(userId, "提交意见", "提交战狼公司意见收集箱反馈");

        return Result.success();
    }
}
