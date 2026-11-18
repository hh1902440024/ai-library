package com.library.interceptor;

/**
 * 当前登录用户（从 JWT 解析，请求内有效）
 */
public record LoginUser(Long userId, String username, Integer role) {
}
