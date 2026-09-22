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

    public void register(String username, String password) {
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
        u.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u);
    }

    /**
     * 登录,成功返回 JWT;失败抛 IllegalArgumentException。
     */
    public String login(String username, String password) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (u == null || !encoder.matches(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return jwtService.generate(username);
    }

    public String resolveUsername(String token) {
        return jwtService.parseUsername(token);
    }
}
