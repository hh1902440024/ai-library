package com.library.exception;

import com.library.common.ResultCode;

/**
 * 业务异常：Service 层主动抛出，由全局异常处理器统一转为 Result
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(ResultCode.BAD_REQUEST, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
