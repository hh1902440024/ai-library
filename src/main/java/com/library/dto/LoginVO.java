package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功响应：token + 用户信息
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private UserVO user;
}
