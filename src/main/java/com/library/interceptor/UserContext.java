package com.library.interceptor;

/**
 * 登录用户上下文：拦截器写入、Controller/Service 读取、请求结束清理
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
