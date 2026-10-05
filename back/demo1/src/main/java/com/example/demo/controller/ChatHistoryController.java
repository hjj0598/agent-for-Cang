package com.example.demo.controller;

import com.example.demo.dto.ChatHistorySearchResult;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.UpdateChatSessionTitleRequest;
import com.example.demo.pojo.ChatMessage;
import com.example.demo.pojo.ChatSession;
import com.example.demo.pojo.Result;
import com.example.demo.service.ChatHistoryService;
import com.example.demo.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat-sessions")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<ChatSession>> listSessions(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatHistoryService.pageSessions(userId, page, pageSize));
    }

    @GetMapping("/search")
    public Result<List<ChatHistorySearchResult>> search(@RequestParam String keyword,
                                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatHistoryService.searchMessages(userId, keyword));
    }

    @GetMapping("/{sessionId}/messages")
    public Result<List<ChatMessage>> listMessages(@PathVariable Long sessionId,
                                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatHistoryService.listMessages(userId, sessionId));
    }

    @PutMapping("/{sessionId}/title")
    public Result<Void> updateTitle(@PathVariable Long sessionId,
                                    @RequestBody @Valid UpdateChatSessionTitleRequest updateChatSessionTitleRequest,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatHistoryService.updateTitle(userId, sessionId, updateChatSessionTitleRequest.getTitle());
        operationLogService.record(userId, "重命名会话", "重命名会话：" + updateChatSessionTitleRequest.getTitle());
        return Result.success();
    }

    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatHistoryService.deleteSession(userId, sessionId);
        operationLogService.record(userId, "删除聊天会话", "删除会话ID：" + sessionId);
        return Result.success();
    }
}
