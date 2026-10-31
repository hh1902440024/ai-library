package com.library.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码加密/比对工具
 */
public final class BcryptUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private BcryptUtil() {
    }

    /** 明文加密入库 */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /** 登录比对 */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
