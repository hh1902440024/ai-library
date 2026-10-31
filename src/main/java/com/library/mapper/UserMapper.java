package com.library.mapper;

import com.library.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 Mapper
 */
public interface UserMapper {

    /** 新增用户（注册） */
    int insert(User user);

    /** 按用户名查询（注册查重 / 登录） */
    User findByUsername(@Param("username") String username);

    /** 按手机号查询（注册查重） */
    User findByPhone(@Param("phone") String phone);

    /** 按主键查询（获取当前用户信息） */
    User findById(@Param("id") Long id);

    /** 修改个人信息（邮箱/头像） */
    int updateProfile(@Param("id") Long id,
                      @Param("email") String email,
                      @Param("avatar") String avatar);

    /** 修改密码 */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 管理员：用户列表（keyword 模糊匹配用户名/手机号，分页由 PageHelper 拦截） */
    List<User> findByPage(@Param("keyword") String keyword);

    /** 管理员：禁用/启用 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 管理员：调整角色 */
    int updateRole(@Param("id") Long id, @Param("role") Integer role);
}
