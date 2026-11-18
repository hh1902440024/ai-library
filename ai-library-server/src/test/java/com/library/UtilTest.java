package com.library;

import com.library.util.BcryptUtil;
import com.library.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工具类单元测试（不依赖 Spring 上下文与数据库）
 */
class UtilTest {

    /** ≥ 32 字节的测试密钥（HS512 要求） */
    private static final String SECRET = "unit-test-secret-key-0123456789abcdef-0123456789abcdef";

    private JwtUtil newJwt(long expireMillis) {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", SECRET);
        ReflectionTestUtils.setField(jwt, "expireMillis", expireMillis);
        return jwt;
    }

    @Test
    void jwt_createAndParse_roundtrip() {
        JwtUtil jwt = newJwt(3600_000L);
        String token = jwt.createToken(42L, "alice", 1);

        Claims claims = jwt.parseToken(token);
        assertNotNull(claims);
        assertEquals("42", claims.getSubject());
        assertEquals("alice", claims.get("username", String.class));
        assertEquals(1, claims.get("role", Integer.class));
    }

    @Test
    void jwt_expiredToken_returnsNull() {
        // 有效期 -1s：签发即过期
        JwtUtil expired = newJwt(-1000L);
        String token = expired.createToken(1L, "bob", 0);
        assertNull(expired.parseToken(token), "过期 token 应解析失败返回 null");
    }

    @Test
    void jwt_forgedToken_returnsNull() {
        JwtUtil jwt = newJwt(3600_000L);
        // 篡改 payload 的伪造 token
        String forged = jwt.createToken(1L, "alice", 0);
        forged = forged.substring(0, forged.length() - 2) + "xx";
        assertNull(jwt.parseToken(forged), "伪造 token 应解析失败返回 null");
    }

    @Test
    void bcrypt_encodeAndMatch() {
        String hash = BcryptUtil.encode("123456");
        assertTrue(hash.startsWith("$2a$"), "BCrypt 哈希格式");
        assertTrue(BcryptUtil.matches("123456", hash));
        assertFalse(BcryptUtil.matches("wrong-password", hash));
        // 同一明文两次加密产生不同盐值
        assertFalse(hash.equals(BcryptUtil.encode("123456")));
    }
}
