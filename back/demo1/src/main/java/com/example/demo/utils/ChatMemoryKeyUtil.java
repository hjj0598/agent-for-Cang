package com.example.demo.utils;

public class ChatMemoryKeyUtil {

    private ChatMemoryKeyUtil() {
    }

    public static String build(Long userId, String memoryId) {
        if (userId == null) {
            throw new RuntimeException("无法获取当前登录用户，请重新登录后再试");
        }
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new RuntimeException("memoryId不能为空");
        }
        return userId + ":" + memoryId.trim();
    }
}
