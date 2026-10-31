package com.library.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.Result;
import com.library.common.ResultCode;
import com.library.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * JWT 登录拦截器：
 * 1. @Anonymous 注解的接口放行（注册/登录/公开查询/文档）
 * 2. 其余 /api/** 接口校验 Authorization: Bearer <token>，失败返回 401
 * 3. @RequireAdmin 注解的接口额外校验 role=1，失败返回 403
 * 4. 校验通过后写入 UserContext，供业务层获取当前用户
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 非 Controller 方法（静态资源等）放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean anonymous = handlerMethod.hasMethodAnnotation(Anonymous.class)
                || handlerMethod.getBeanType().isAnnotationPresent(Anonymous.class);
        if (anonymous) {
            return true;
        }

        // 解析 Authorization: Bearer <token>
        String token = resolveToken(request);
        Claims claims = token == null ? null : jwtUtil.parseToken(token);
        if (claims == null) {
            writeJson(response, Result.fail(ResultCode.UNAUTHORIZED, "未登录或登录已过期，请重新登录"));
            return false;
        }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        Integer role = claims.get("role", Integer.class);
        UserContext.set(new LoginUser(userId, username, role));

        // 管理员接口校验角色
        boolean requireAdmin = handlerMethod.hasMethodAnnotation(RequireAdmin.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAdmin.class);
        if (requireAdmin && (role == null || role != 1)) {
            writeJson(response, Result.fail(ResultCode.FORBIDDEN, "无权限访问，请联系管理员"));
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private void writeJson(HttpServletResponse response, Result<Void> result) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
