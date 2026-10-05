package com.example.demo.service;

import com.example.demo.dto.ChatHistorySearchResult;
import com.example.demo.dto.PageResult;
import com.example.demo.pojo.ChatMessage;
import com.example.demo.pojo.ChatSession;

import java.util.List;

public interface ChatHistoryService {

    ChatSession getOrCreateSession(Long userId, String memoryId, String firstMessage);

    List<ChatSession> listSessions(Long userId);

    PageResult<ChatSession> pageSessions(Long userId, Integer page, Integer pageSize);

    List<ChatMessage> listMessages(Long userId, Long sessionId);

    List<ChatHistorySearchResult> searchMessages(Long userId, String keyword);

    void addMessage(Long userId, Long sessionId, String role, String content);

    void updateTitle(Long userId, Long sessionId, String title);

    void deleteSession(Long userId, Long sessionId);
}
