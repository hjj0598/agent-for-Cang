package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;

public interface AuthService {

    void register(LoginRequest request);

    LoginResponse login(LoginRequest request);

    String generateResetCode(String username);

    void resetPassword(String username, String code, String newPassword);
}
