package com.example.demo.service.impl;

import com.example.demo.dto.UserInfoResponse;
import com.example.demo.mapper.UserMapper;
import com.example.demo.pojo.AppUser;
import com.example.demo.service.UserService;
import com.example.demo.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserInfoResponse getCurrentUser(Long userId) {
        AppUser user = userMapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRole(user.getRole());

        return response;
    }

    @Override
    public void updateNickname(Long userId, String nickname) {
        AppUser user = userMapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        userMapper.updateNickname(userId, nickname);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        AppUser user = userMapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!checkPassword(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("原密码错误");
        }

        userMapper.updatePasswordHash(userId, PasswordUtil.encode(newPassword));
    }

    private boolean checkPassword(String rawPassword, String passwordHash) {
        if (PasswordUtil.isEncoded(passwordHash)) {
            return PasswordUtil.matches(rawPassword, passwordHash);
        }

        return passwordHash != null && passwordHash.equals(rawPassword);
    }
}
