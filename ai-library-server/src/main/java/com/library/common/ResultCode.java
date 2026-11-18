package com.library.common;

/**
 * 统一错误码：4xx/5xx 语义自定义
 */
public final class ResultCode {

    public static final int SUCCESS = 200;       // 成功
    public static final int BAD_REQUEST = 400;   // 参数错误
    public static final int UNAUTHORIZED = 401;  // 未登录 / token 失效
    public static final int FORBIDDEN = 403;     // 无权限
    public static final int NOT_FOUND = 404;     // 资源不存在
    public static final int SERVER_ERROR = 500;  // 系统错误

    private ResultCode() {
    }
}
