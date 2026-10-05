package com.example.demo.controller;

import com.example.demo.annotation.AdminOnly;
import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.UpdateUserRoleRequest;
import com.example.demo.pojo.Result;
import com.example.demo.service.AdminUserService;
import com.example.demo.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private OperationLogService operationLogService;

    @AdminOnly
    @GetMapping
    public Result<PageResult<AdminUserResponse>> page(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                                      @RequestParam(required = false) String keyword) {
        return Result.success(adminUserService.page(page, pageSize, keyword));
    }

    @AdminOnly
    @GetMapping("/{id}")
    public Result<AdminUserResponse> getById(@PathVariable Long id) {
        return Result.success(adminUserService.getById(id));
    }

    @AdminOnly
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id,
                                   @RequestBody @Valid UpdateUserRoleRequest roleRequest,
                                   HttpServletRequest request) {
        Long adminUserId = (Long) request.getAttribute("userId");
        adminUserService.updateRole(id, roleRequest.getRole());
        operationLogService.record(adminUserId, "管理员修改用户角色", "用户ID：" + id + "，新角色：" + roleRequest.getRole());
        return Result.success();
    }
}
