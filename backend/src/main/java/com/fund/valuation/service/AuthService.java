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
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public void register(String username, String password, String referralSource) {
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
        u.setRole(isAdmin ? User.ROLE_ADMIN : User.ROLE_USER);
        // 新用户注册后默认状态为 PENDING (待管理员审核授权)，初始 admin 直接生效
        u.setStatus(isAdmin ? User.STATUS_NORMAL : User.STATUS_PENDING);
        u.setIsVip(false);
        u.setReferralSource((referralSource != null && !referralSource.isBlank()) ? referralSource.trim() : "自己搜索");
        u.setLastLoginAt(null);
        u.setLoginCount(0);
        u.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u);
    }

    public void register(String username, String password) {
        register(username, password, "自己搜索");
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
