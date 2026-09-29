package com.fund.valuation.service;

import com.fund.valuation.domain.User;
import com.fund.valuation.mapper.UserFundMapper;
import com.fund.valuation.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserManagementServiceTest {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserFundMapper userFundMapper;

    @BeforeEach
    void clean() {
        userFundMapper.delete(null);
        userMapper.delete(null);
    }

    @Test
    void testUserManagementLifecycle() {
        authService.register("tester1", "password123");
        authService.register("tester2", "password123");

        List<UserManagementService.UserView> users = userManagementService.listUsers();
        assertEquals(2, users.size());

        // 测试状态切换
        userManagementService.updateStatus("tester1", User.STATUS_DISABLED);
        User u1 = authService.getUser("tester1");
        assertEquals(User.STATUS_DISABLED, u1.getStatus());

        // 测试开通 VIP
        LocalDateTime expire = LocalDateTime.now().plusMonths(1);
        userManagementService.updateVip("tester1", true, expire);
        u1 = authService.getUser("tester1");
        assertTrue(u1.getIsVip());
        assertEquals(expire.getHour(), u1.getVipExpireAt().getHour());

        // 测试角色提升
        userManagementService.updateRole("tester2", User.ROLE_ADMIN);
        User u2 = authService.getUser("tester2");
        assertEquals(User.ROLE_ADMIN, u2.getRole());

        // 测试删除用户
        userManagementService.deleteUser("tester1");
        assertNull(authService.getUser("tester1"), "用户已被成功物理删除");

        // 禁止删除内置 admin
        assertThrows(IllegalArgumentException.class, () -> userManagementService.deleteUser("admin"));
    }

    @Test
    void testKeywordSearchAndFundWatchers() {
        authService.register("alice", "pass123456", "闲鱼");
        authService.register("bob", "pass123456", "B站");

        // 关键词搜索匹配
        List<UserManagementService.UserView> searchAlice = userManagementService.listUsers("alice");
        assertEquals(1, searchAlice.size());
        assertEquals("alice", searchAlice.get(0).username());

        List<UserManagementService.UserView> searchSource = userManagementService.listUsers("B站");
        assertEquals(1, searchSource.size());
        assertEquals("bob", searchSource.get(0).username());

        // 模拟自选添加
        com.fund.valuation.domain.UserFund uf = new com.fund.valuation.domain.UserFund();
        uf.setUserId("alice");
        uf.setFundCode("001186");
        uf.setSortNo(0);
        userFundMapper.insert(uf);

        List<UserManagementService.UserView> watchers = userManagementService.listFundWatchers("001186");
        assertEquals(1, watchers.size());
        assertEquals("alice", watchers.get(0).username());

        List<UserManagementService.UserView> emptyWatchers = userManagementService.listFundWatchers("999999");
        assertTrue(emptyWatchers.isEmpty());
    }
}
