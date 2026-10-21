package com.example.demo.controller;

import com.example.demo.dto.RelationshipSuggestionActionRequest;
import com.example.demo.dto.RelationshipSuggestionActionUpdateRequest;
import com.example.demo.pojo.RelationshipSuggestionAction;
import com.example.demo.pojo.Result;
import com.example.demo.service.OperationLogService;
import com.example.demo.service.RelationshipSuggestionActionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/relationship-suggestion-actions")
public class RelationshipSuggestionActionController {

    @Autowired
    private RelationshipSuggestionActionService actionService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public Result<List<RelationshipSuggestionAction>> list(@RequestParam(required = false) String status,
                                                           @RequestParam(required = false) Long personId,
                                                           @RequestParam(required = false) Integer limit,
                                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(actionService.list(userId, status, personId, limit));
    }

    @GetMapping("/feedback")
    public Result<List<RelationshipSuggestionAction>> recentFeedback(@RequestParam(required = false) Integer limit,
                                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(actionService.recentFeedback(userId, limit));
    }

    @PostMapping
    public Result<RelationshipSuggestionAction> createOrUpdate(@RequestBody RelationshipSuggestionActionRequest actionRequest,
                                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        RelationshipSuggestionAction action = actionService.createOrUpdate(userId, actionRequest);
        operationLogService.record(userId, "记录关系建议行动", action.getPersonName() + " - " + action.getTitle());
        return Result.success(action);
    }

    @PutMapping("/{id}")
    public Result<RelationshipSuggestionAction> update(@PathVariable Long id,
                                                       @RequestBody RelationshipSuggestionActionUpdateRequest updateRequest,
                                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        RelationshipSuggestionAction action = actionService.update(userId, id, updateRequest);
        operationLogService.record(userId, "更新关系建议行动", action.getPersonName() + " - " + action.getStatus());
        return Result.success(action);
    }
}
