package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.User;
import com.fund.valuation.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void cleanDb() {
        userMapper.delete(null);
    }

    @Test
    void registerAndLogin() {
        authService.register("alice", "password123");
        String token = authService.login("alice", "password123");
        assertNotNull(token);
        assertEquals("alice", authService.resolveUsername(token));

        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "alice"));
        assertNotNull(u);
        assertTrue(new BCryptPasswordEncoder().matches("password123", u.getPasswordHash()));
    }

    @Test
    void duplicateUsernameRejected() {
        authService.register("bob", "password123");
        assertThrows(IllegalArgumentException.class, () -> authService.register("bob", "otherpass"));
    }

    @Test
    void weakPasswordRejected() {
        assertThrows(IllegalArgumentException.class, () -> authService.register("carol", "123"));
    }

    @Test
    void wrongPasswordRejected() {
        authService.register("dave", "password123");
        assertThrows(IllegalArgumentException.class, () -> authService.login("dave", "wrong"));
    }
}
