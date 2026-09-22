package com.fund.valuation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * JWT 鉴权过滤器。
 * - 放行: /api/auth/**、/api/fund/**、/api/admin/**、非 /api/** 路径
 * - 鉴权: /api/watchlist/** 需有效 token(Authorization Bearer 或 SSE token 查询参数)
 * 校验通过后将 username 写入 request attribute "username"。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USERNAME = "username";

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/api/watchlist")) {
            String token = extractToken(request);
            String username = token == null ? null : authService.resolveUsername(token);
            if (username == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        Map.of("error", "未登录或登录已过期")));
                return;
            }
            request.setAttribute(ATTR_USERNAME, username);
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        // SSE 无法带 header,支持 token 查询参数
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken;
        }
        return null;
    }
}
