package com.example.demo.service;

import com.example.demo.dto.UserInfoResponse;

public interface UserService {
    UserInfoResponse getCurrentUser(Long userId);

    void updateNickname(Long userId, String nickname);

    void updatePassword(Long userId, String oldPassword, String newPassword);
}
