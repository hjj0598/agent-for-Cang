package com.example.demo.filter;

import com.example.demo.annotation.AdminOnly;
import com.example.demo.pojo.Result;
import com.example.demo.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error("没有登录")));
            return false;
        }

        try {
            String token = authorization.substring(7);
            Claims claims = JwtUtil.parseToken(token);

            Long userId = Long.valueOf(claims.get("userId").toString());
            String role = claims.get("role", String.class);
            if (role == null || role.trim().isEmpty()) {
                role = "USER";
            }

            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            if (handler instanceof HandlerMethod handlerMethod
                    && handlerMethod.hasMethodAnnotation(AdminOnly.class)
                    && !"ADMIN".equals(role)) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(Result.error("无权限访问")));
                return false;
            }

            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error("登录已过期")));
            return false;
        }
    }
}
