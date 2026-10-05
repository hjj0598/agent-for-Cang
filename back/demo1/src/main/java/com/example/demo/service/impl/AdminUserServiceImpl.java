package com.example.demo.service.impl;

import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.PageResult;
import com.example.demo.mapper.UserMapper;
import com.example.demo.pojo.AppUser;
import com.example.demo.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult<AdminUserResponse> page(Integer page, Integer pageSize, String keyword) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        safePageSize = Math.min(safePageSize, 50);
        String safeKeyword = keyword == null ? "" : keyword.trim();

        int offset = (safePage - 1) * safePageSize;
        Long total = userMapper.countPage(safeKeyword);
        List<AdminUserResponse> rows = userMapper.findPage(safeKeyword, offset, safePageSize)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageResult<>(total, rows);
    }

    @Override
    public AdminUserResponse getById(Long id) {
        AppUser user = userMapper.findById(id);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return toResponse(user);
    }

    @Override
    public void updateRole(Long id, String role) {
        AppUser user = userMapper.findById(id);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int updated = userMapper.updateRole(id, role);

        if (updated == 0) {
            throw new RuntimeException("修改用户角色失败");
        }
    }

    private AdminUserResponse toResponse(AppUser user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
