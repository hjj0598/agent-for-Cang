package com.example.demo.service.impl;

import com.example.demo.dto.PageResult;
import com.example.demo.mapper.CompanyFeedbackMapper;
import com.example.demo.pojo.CompanyFeedback;
import com.example.demo.service.CompanyFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyFeedbackServiceImpl implements CompanyFeedbackService {

    @Autowired
    private CompanyFeedbackMapper companyFeedbackMapper;

    @Override
    public void submit(CompanyFeedback feedback) {
        feedback.setCategory(resolveCategory(feedback.getCategory(), feedback.getContent()));
        feedback.setStatus("SUBMITTED");
        companyFeedbackMapper.insert(feedback);
    }

    @Override
    public PageResult<CompanyFeedback> page(Long userId, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        safePageSize = Math.min(safePageSize, 50);

        int offset = (safePage - 1) * safePageSize;
        Long total = companyFeedbackMapper.countByUserId(userId);
        List<CompanyFeedback> rows = companyFeedbackMapper.findByUserId(userId, offset, safePageSize);

        return new PageResult<>(total, rows);
    }

    @Override
    public PageResult<CompanyFeedback> pageAll(Integer page, Integer pageSize, Long userId, String category, String status) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        safePageSize = Math.min(safePageSize, 50);
        String safeCategory = category == null ? "" : category.trim();
        String safeStatus = status == null ? "" : status.trim();

        int offset = (safePage - 1) * safePageSize;
        Long total = companyFeedbackMapper.countAllByCondition(userId, safeCategory, safeStatus);
        List<CompanyFeedback> rows = companyFeedbackMapper.findAll(userId, safeCategory, safeStatus, offset, safePageSize);

        return new PageResult<>(total, rows);
    }

    @Override
    public void updateStatus(Long id, String status) {
        CompanyFeedback feedback = companyFeedbackMapper.findById(id);

        if (feedback == null) {
            throw new RuntimeException("意见不存在");
        }

        int updated = companyFeedbackMapper.updateStatus(id, status);

        if (updated == 0) {
            throw new RuntimeException("修改意见状态失败");
        }
    }

    private String resolveCategory(String category, String content) {
        if (category != null && !category.trim().isEmpty() && !"AUTO".equals(category)) {
            return category.trim();
        }

        String text = content == null ? "" : content.toLowerCase();

        if (containsAny(text, "bug", "错误", "报错", "失败", "不能用", "打不开", "崩溃", "异常")) {
            return "BUG";
        }

        if (containsAny(text, "建议", "希望", "新增", "加一个", "功能", "支持")) {
            return "FEATURE";
        }

        if (containsAny(text, "界面", "ui", "不好看", "体验", "交互", "按钮", "页面")) {
            return "EXPERIENCE";
        }

        if (containsAny(text, "ai", "agent", "模型", "回答", "检索", "rag", "知识库")) {
            return "AI";
        }

        return "OTHER";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
