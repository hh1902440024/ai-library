package com.library.service;

import com.library.dto.LoginDTO;
import com.library.dto.LoginVO;
import com.library.dto.PageResult;
import com.library.dto.RegisterDTO;
import com.library.dto.UpdateUserDTO;
import com.library.dto.UserVO;

/**
 * 用户服务：注册、登录、个人信息与密码管理
 */
public interface UserService {

    /** 注册：唯一性双重校验（代码 + 数据库唯一索引兜底） */
    void register(RegisterDTO dto);

    /** 登录：账号（用户名/手机号）+ 密码，成功返回 token 与用户信息 */
    LoginVO login(LoginDTO dto);

    /** 获取当前登录用户信息（userId 取自 JWT） */
    UserVO getCurrentUser();

    /** 修改个人信息（邮箱/头像） */
    UserVO updateProfile(UpdateUserDTO dto);

    /** 修改密码：原密码校验通过后 BCrypt 加密入库 */
    void updatePassword(String oldPassword, String newPassword);

    /** 管理员：用户列表（keyword 模糊匹配用户名/手机号） */
    PageResult<UserVO> listUsers(String keyword, int page, int size);

    /** 管理员：禁用/启用用户（不能操作自己） */
    void updateStatus(Long operatorId, Long userId, Integer status);

    /** 管理员：调整角色（不能操作自己） */
    void updateRole(Long operatorId, Long userId, Integer role);
}
