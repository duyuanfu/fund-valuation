package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.User;
import com.fund.valuation.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户注册与登录。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final SysVipConfigService sysVipConfigService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public RegisterResult register(String username, String password, String referralSource) {
        if (username == null || username.isBlank() || username.length() > 32) {
            throw new IllegalArgumentException("用户名不能为空且不超过32字符");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少6位");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        boolean isAdmin = "admin".equalsIgnoreCase(username);
        boolean needApproval = !isAdmin && sysVipConfigService.isRequireApproval();

        if (isAdmin) {
            // 安全防护: 仅当数据库中不存在任何超级管理员账号时，才允许首个 admin 初始化注册
            Long adminCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, User.ROLE_ADMIN));
            if (adminCount != null && adminCount > 0) {
                throw new IllegalArgumentException("超级管理员账号已存在，禁止重复注册");
            }
            u.setRole(User.ROLE_ADMIN);
            u.setStatus(User.STATUS_NORMAL);
        } else if (needApproval) {
            u.setRole(User.ROLE_USER);
            u.setStatus(User.STATUS_PENDING);
        } else {
            // 开关关闭: 免审核直接正常开通
            u.setRole(User.ROLE_USER);
            u.setStatus(User.STATUS_NORMAL);
        }
        u.setIsVip(false);
        u.setReferralSource((referralSource != null && !referralSource.isBlank()) ? referralSource.trim() : "自己搜索");
        u.setLastLoginAt(null);
        u.setLoginCount(0);
        u.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u);

        if (needApproval) {
            return new RegisterResult(
                    u.getStatus(),
                    u.getUsername(),
                    "注册申请已提交，请等待管理员审核授权",
                    true,
                    null,
                    u.getRole(),
                    false
            );
        } else {
            // 免审直接登录: 生成 JWT Token 返回
            String token = jwtService.generate(username);
            return new RegisterResult(
                    u.getStatus(),
                    u.getUsername(),
                    "注册成功，已自动开通账号",
                    false,
                    token,
                    u.getRole(),
                    u.isVipEffective()
            );
        }
    }

    public RegisterResult register(String username, String password) {
        return register(username, password, "自己搜索");
    }

    public record RegisterResult(
            String status,
            String username,
            String message,
            boolean needApproval,
            String token,
            String role,
            boolean isVip
    ) {
    }

    /**
     * 登录,成功返回 LoginResult;失败抛 IllegalArgumentException。
     */
    public LoginResult login(String username, String password) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (u == null || !encoder.matches(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (User.STATUS_PENDING.equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("您的账号正在等待管理员审核授权，暂无法登录。请联系管理员开通权限。");
        }
        if (User.STATUS_DISABLED.equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("账号已被管理员停用，请联系管理员");
        }
        // 更新登录活跃时间与累计登录频次
        u.setLastLoginAt(LocalDateTime.now());
        u.setLoginCount((u.getLoginCount() == null ? 0 : u.getLoginCount()) + 1);
        userMapper.updateById(u);

        String token = jwtService.generate(username);
        String role = (u.getRole() == null || u.getRole().isBlank()) ? User.ROLE_USER : u.getRole();
        return new LoginResult(token, u.getUsername(), role, u.isVipEffective());
    }

    public User getUser(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    public record LoginResult(String token, String username, String role, boolean isVip) {
    }

    public String resolveUsername(String token) {
        return jwtService.parseUsername(token);
    }
}
