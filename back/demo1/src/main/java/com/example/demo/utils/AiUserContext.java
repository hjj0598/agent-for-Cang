package com.example.demo.utils;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AiUserContext {

    private static final Map<String, ContextEntry> CONTEXTS = new ConcurrentHashMap<>();
    private static final long CONTEXT_TTL_SECONDS = 300;

    private AiUserContext() {
    }

    public static void register(String contextKey, Long userId) {
        if (contextKey == null || contextKey.trim().isEmpty()) {
            throw new RuntimeException("工具上下文不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("无法获取当前登录用户，请重新登录后再试");
        }
        CONTEXTS.put(contextKey, new ContextEntry(userId, Instant.now().plusSeconds(CONTEXT_TTL_SECONDS).toEpochMilli()));
    }

    public static Long requireUserId(String contextKey) {
        cleanupExpiredContexts();

        ContextEntry entry = contextKey == null ? null : CONTEXTS.get(contextKey);
        if (entry == null) {
            throw new RuntimeException("无法获取当前登录用户，请重新登录后再试");
        }
        return entry.userId;
    }

    public static void clear(String contextKey) {
        if (contextKey != null) {
            CONTEXTS.remove(contextKey);
        }
    }

    private static void cleanupExpiredContexts() {
        long now = Instant.now().toEpochMilli();
        CONTEXTS.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
    }

    private static class ContextEntry {
        private final Long userId;
        private final long expiresAt;

        private ContextEntry(Long userId, long expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }
}
