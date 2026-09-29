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
        AuthService.LoginResult res = authService.login("alice", "password123");
        assertNotNull(res.token());
        assertEquals("alice", res.username());
        assertEquals(User.ROLE_USER, res.role());
        assertEquals(false, res.isVip());
        assertEquals("alice", authService.resolveUsername(res.token()));

        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "alice"));
        assertNotNull(u);
        assertTrue(new BCryptPasswordEncoder().matches("password123", u.getPasswordHash()));
    }

    @Test
    void adminUserAutomaticallyAssignedAdminRole() {
        authService.register("admin", "admin123");
        AuthService.LoginResult res = authService.login("admin", "admin123");
        assertEquals(User.ROLE_ADMIN, res.role());
        assertTrue(res.isVip()); // ADMIN 亦具备 VIP 特权
    }

    @Test
    void disabledUserCannotLogin() {
        authService.register("eve", "password123");
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "eve"));
        u.setStatus(User.STATUS_DISABLED);
        userMapper.updateById(u);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("eve", "password123"));
        assertTrue(ex.getMessage().contains("停用"));
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
