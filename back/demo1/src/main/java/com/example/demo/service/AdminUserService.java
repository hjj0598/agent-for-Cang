package com.example.demo.service;

import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.PageResult;

public interface AdminUserService {

    PageResult<AdminUserResponse> page(Integer page, Integer pageSize, String keyword);

    AdminUserResponse getById(Long id);

    void updateRole(Long id, String role);
}
