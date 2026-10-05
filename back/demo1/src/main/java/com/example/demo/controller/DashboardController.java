package com.example.demo.controller;

import com.example.demo.annotation.AdminOnly;
import com.example.demo.dto.AdminDashboardStats;
import com.example.demo.dto.DashboardStats;
import com.example.demo.dto.RelationshipReminder;
import com.example.demo.dto.RelationshipSuggestion;
import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.ChatSessionMapper;
import com.example.demo.mapper.CompanyFeedbackMapper;
import com.example.demo.mapper.KnowledgeDocumentMapper;
import com.example.demo.mapper.OperationLogMapper;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.pojo.Result;
import com.example.demo.service.RelationshipReminderService;
import com.example.demo.service.RelationshipSuggestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private RelationshipReminderService relationshipReminderService;

    @Autowired
    private RelationshipSuggestionService relationshipSuggestionService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PersonMapper personMapper;

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private CompanyFeedbackMapper companyFeedbackMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @GetMapping("/stats")
    public Result<DashboardStats> stats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        DashboardStats stats = new DashboardStats(
                personMapper.countByUserId(userId),
                knowledgeDocumentMapper.countByUserId(userId),
                chatSessionMapper.countByUserId(userId),
                chatMessageMapper.countByUserId(userId)
        );

        return Result.success(stats);
    }

    @GetMapping("/reminders")
    public Result<List<RelationshipReminder>> reminders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(relationshipReminderService.list(userId));
    }

    /**
     * 基于当前用户自己的关系资料生成维护建议。
     * 模型失败时服务层会返回规则兜底结果，不影响首页其他模块。
     */
    @GetMapping("/suggestions")
    public Result<List<RelationshipSuggestion>> suggestions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(relationshipSuggestionService.generate(userId));
    }

    @AdminOnly
    @GetMapping("/admin/stats")
    public Result<AdminDashboardStats> adminStats() {
        AdminDashboardStats stats = new AdminDashboardStats(
                userMapper.countAll(),
                personMapper.countAll(),
                knowledgeDocumentMapper.countAll(),
                chatSessionMapper.countAll(),
                chatMessageMapper.countAll(),
                companyFeedbackMapper.countAll(),
                operationLogMapper.countAll()
        );

        return Result.success(stats);
    }
}
