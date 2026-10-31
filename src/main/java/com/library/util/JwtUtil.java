package com.library.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析 token（jjwt 0.12 API）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-millis}")
    private long expireMillis;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 token：subject=userId，携带 username/role */
    public String createToken(Long userId, String username, Integer role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMillis))
                .signWith(getKey())
                .compact();
    }

    /** 解析 token，失败（过期/伪造）返回 null */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(getKey()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public long getExpireMillis() {
        return expireMillis;
    }
}
