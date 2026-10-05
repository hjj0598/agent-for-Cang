package com.example.demo.controller;

import com.example.demo.dto.UpdateNicknameRequest;
import com.example.demo.dto.UpdatePasswordRequest;
import com.example.demo.dto.UserInfoResponse;
import com.example.demo.pojo.Result;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public Result<UserInfoResponse> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        UserInfoResponse userInfo = userService.getCurrentUser(userId);

        return Result.success(userInfo);
    }

    @PutMapping("/nickname")
    public Result<Void> updateNickname(@RequestBody @Valid UpdateNicknameRequest updateNicknameRequest,
                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        userService.updateNickname(userId, updateNicknameRequest.getNickname());

        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody @Valid UpdatePasswordRequest updatePasswordRequest,
                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        userService.updatePassword(
                userId,
                updatePasswordRequest.getOldPassword(),
                updatePasswordRequest.getNewPassword()
        );

        return Result.success();
    }
}
