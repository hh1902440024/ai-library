package com.library.controller;

import com.library.common.Result;
import com.library.dto.PageResult;
import com.library.dto.PasswordDTO;
import com.library.dto.UpdateUserDTO;
import com.library.dto.UserVO;
import com.library.interceptor.LoginUser;
import com.library.interceptor.RequireAdmin;
import com.library.interceptor.UserContext;
import com.library.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户接口：个人信息管理 + 管理员用户管理
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.getCurrentUser());
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/me")
    public Result<UserVO> updateMe(@Valid @RequestBody UpdateUserDTO dto) {
        return Result.ok("修改成功", userService.updateProfile(dto));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordDTO dto) {
        userService.updatePassword(dto.getOldPassword(), dto.getNewPassword());
        return Result.ok("密码修改成功，请重新登录", null);
    }

    @RequireAdmin
    @Operation(summary = "用户列表（管理员）")
    @GetMapping
    public Result<PageResult<UserVO>> list(@RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(userService.listUsers(keyword, page, size));
    }

    @RequireAdmin
    @Operation(summary = "禁用/启用用户（管理员）")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("status 取值不合法（0 禁用 / 1 正常）");
        }
        userService.updateStatus(UserContext.getUserId(), id, status);
        return Result.ok("操作成功", null);
    }

    @RequireAdmin
    @Operation(summary = "调整用户角色（管理员）")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer role = body.get("role");
        if (role == null || (role != 0 && role != 1)) {
            return Result.fail("role 取值不合法（0 普通用户 / 1 管理员）");
        }
        userService.updateRole(UserContext.getUserId(), id, role);
        return Result.ok("操作成功", null);
    }
}
