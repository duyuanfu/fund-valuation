package com.fund.valuation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.domain.User;
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
 * JWT 鉴权与安全访问控制过滤器。
 * - 放行: /api/auth/**、/api/fund/**、/api/notice/**、非 /api/** 静态资源路径
 * - 鉴权:
 *   1. /api/watchlist/** 需有效 token 且账号状态为 NORMAL
 *   2. /api/admin/** 强制校验有效 token，且要求角色为 ADMIN 且账号未被禁用
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

        // 1. 管理员接口管控
        if (path.startsWith("/api/admin")) {
            String token = extractToken(request);
            if (token == null) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录管理员账号");
                return;
            }
            String username = authService.resolveUsername(token);
            if (username == null) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "登录已过期，请重新登录");
                return;
            }
            User u = authService.getUser(username);
            if (u == null || !User.ROLE_ADMIN.equalsIgnoreCase(u.getRole())) {
                reject(response, HttpServletResponse.SC_FORBIDDEN, "无管理员访问权限");
                return;
            }
            if (User.STATUS_DISABLED.equalsIgnoreCase(u.getStatus())) {
                reject(response, HttpServletResponse.SC_FORBIDDEN, "管理员账号已被禁用");
                return;
            }
            request.setAttribute(ATTR_USERNAME, username);
        }

        // 2. 自选列表、个人持仓及个人数据管控 (放行 /api/portfolio/vip-config 公共价格与收款配置)
        if (path.startsWith("/api/watchlist") || (path.startsWith("/api/portfolio") && !path.equals("/api/portfolio/vip-config"))) {
            String token = extractToken(request);
            String username = token == null ? null : authService.resolveUsername(token);
            if (username == null) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
                return;
            }
            User u = authService.getUser(username);
            if (u == null) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "用户不存在或已失效");
                return;
            }
            if (User.STATUS_DISABLED.equalsIgnoreCase(u.getStatus())) {
                reject(response, HttpServletResponse.SC_FORBIDDEN, "账号已被管理员停用，请联系管理员");
                return;
            }
            request.setAttribute(ATTR_USERNAME, username);
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("error", msg)));
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

