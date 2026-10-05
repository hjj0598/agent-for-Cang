package com.example.demo.service.impl;

import com.example.demo.dto.ChatHistorySearchResult;
import com.example.demo.dto.PageResult;
import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.ChatSessionMapper;
import com.example.demo.pojo.ChatMessage;
import com.example.demo.pojo.ChatSession;
import com.example.demo.service.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    public ChatSession getOrCreateSession(Long userId, String memoryId, String firstMessage) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new RuntimeException("memoryId不能为空");
        }

        ChatSession session = chatSessionMapper.findByUserIdAndMemoryId(userId, memoryId);

        if (session != null) {
            return session;
        }

        session = new ChatSession();
        session.setUserId(userId);
        session.setMemoryId(memoryId);
        session.setTitle(buildTitle(firstMessage));

        chatSessionMapper.insert(session);

        return session;
    }

    @Override
    public List<ChatSession> listSessions(Long userId) {
        return chatSessionMapper.findByUserId(userId);
    }

    @Override
    public PageResult<ChatSession> pageSessions(Long userId, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        safePageSize = Math.min(safePageSize, 50);

        int offset = (safePage - 1) * safePageSize;

        Long total = chatSessionMapper.countByUserId(userId);
        List<ChatSession> rows = chatSessionMapper.findPageByUserId(userId, offset, safePageSize);

        return new PageResult<>(total, rows);
    }

    @Override
    public List<ChatMessage> listMessages(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.findByIdAndUserId(sessionId, userId);

        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        return chatMessageMapper.findBySessionIdAndUserId(sessionId, userId);
    }

    @Override
    public List<ChatHistorySearchResult> searchMessages(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new RuntimeException("搜索关键词不能为空");
        }

        return chatMessageMapper.searchByKeyword(userId, keyword.trim());
    }

    @Override
    public void addMessage(Long userId, Long sessionId, String role, String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);

        chatMessageMapper.insert(message);
        chatSessionMapper.touch(sessionId, userId);
    }

    @Override
    public void updateTitle(Long userId, Long sessionId, String title) {
        ChatSession session = chatSessionMapper.findByIdAndUserId(sessionId, userId);

        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        session.setTitle(title);
        chatSessionMapper.updateTitle(session);
    }

    @Transactional
    @Override
    public void deleteSession(Long userId, Long sessionId) {
        chatMessageMapper.deleteBySessionIdAndUserId(sessionId, userId);
        chatSessionMapper.deleteByIdAndUserId(sessionId, userId);
    }

    private String buildTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.trim().isEmpty()) {
            return "新会话";
        }

        String title = firstMessage.trim().replaceAll("\\s+", " ");

        if (title.length() > 30) {
            return title.substring(0, 30);
        }

        return title;
    }
}
