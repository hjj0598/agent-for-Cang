package com.example.demo.controller;

import com.example.demo.annotation.AdminOnly;
import com.example.demo.dto.PageResult;
import com.example.demo.pojo.OperationLog;
import com.example.demo.pojo.Result;
import com.example.demo.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/operation-logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/recent")
    public Result<List<OperationLog>> recent(@RequestParam(defaultValue = "10") Integer limit,
                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(operationLogService.recent(userId, limit));
    }

    @AdminOnly
    @GetMapping("/admin/recent")
    public Result<List<OperationLog>> recentAll(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(operationLogService.recentAll(limit));
    }

    @AdminOnly
    @GetMapping("/admin/page")
    public Result<PageResult<OperationLog>> pageAll(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) Long userId,
                                                    @RequestParam(required = false) String keyword) {
        return Result.success(operationLogService.pageAll(page, pageSize, userId, keyword));
    }
}
