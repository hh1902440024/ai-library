package com.library.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
public class User {

    private Long id;
    private String username;
    private String phone;
    /** BCrypt 加密后的密码 */
    private String password;
    /** 0 普通用户 1 管理员 */
    private Integer role;
    private String email;
    private String avatar;
    /** 1 正常 0 禁用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
