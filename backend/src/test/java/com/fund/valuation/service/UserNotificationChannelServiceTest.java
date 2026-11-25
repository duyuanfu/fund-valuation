package com.fund.valuation.service;

import com.fund.valuation.domain.UserNotificationChannel;
import com.fund.valuation.dto.NotificationChannelView;
import com.fund.valuation.mapper.UserNotificationChannelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserNotificationChannelServiceTest {

    @Autowired
    private UserNotificationChannelService channelService;

    @Autowired
    private UserNotificationChannelMapper channelMapper;

    @BeforeEach
    void setUp() {
        channelMapper.delete(null);
    }

    @Test
    void testGetDefaultChannelWhenNotConfigured() {
        NotificationChannelView view = channelService.getChannel("non_existent_user");
        assertNotNull(view);
        assertEquals("", view.email());
        assertTrue(view.emailEnabled());
        assertEquals("serverchan", view.wechatChannelType());
        assertEquals("14:30", view.remindTime());
        assertTrue(view.remindEnabled());
    }

    @Test
    void testCreateAndUpdateChannel() {
        String userId = "test_user_channel";
        NotificationChannelView req = new NotificationChannelView(
                "tester@example.com",
                true,
                "serverchan",
                "SCT123456Key",
                true,
                true,
                "14:15",
                "ALL_WATCHLIST",
                true
        );

        NotificationChannelView created = channelService.updateChannel(userId, req);
        assertEquals("tester@example.com", created.email());
        assertEquals("serverchan", created.wechatChannelType());
        assertEquals("SCT123456Key", created.serverchanKey());
        assertEquals("14:15", created.remindTime());
        assertTrue(created.hideAmount());

        // 查询持久化结果
        NotificationChannelView fetched = channelService.getChannel(userId);
        assertEquals("tester@example.com", fetched.email());
        assertEquals("SCT123456Key", fetched.serverchanKey());
        assertEquals("14:15", fetched.remindTime());
        assertTrue(fetched.hideAmount());

        // 更新配置
        NotificationChannelView updateReq = new NotificationChannelView(
                "updated@example.com",
                false,
                "serverchan",
                "SCT_NEW_KEY",
                true,
                true,
                "14:45",
                "PORTFOLIO_ONLY",
                false
        );
        NotificationChannelView updated = channelService.updateChannel(userId, updateReq);
        assertEquals("updated@example.com", updated.email());
        assertFalse(updated.emailEnabled());
        assertEquals("serverchan", updated.wechatChannelType());
        assertEquals("SCT_NEW_KEY", updated.serverchanKey());
        assertEquals("14:45", updated.remindTime());
        assertEquals("PORTFOLIO_ONLY", updated.pushScope());
        assertFalse(updated.hideAmount());
    }

    @Test
    void testBindEmailQuickly() {
        String userId = "quick_bind_user";
        channelService.bindEmail(userId, "quick@yeah.net");

        NotificationChannelView view = channelService.getChannel(userId);
        assertEquals("quick@yeah.net", view.email());
        assertTrue(view.emailEnabled());
        assertTrue(view.remindEnabled());
        assertEquals("14:30", view.remindTime());
    }

    @Test
    void testEmailUniquenessAndModification() {
        channelService.bindEmail("user_a", "unique@example.com");

        // 同一用户允许更新配置保持原邮箱
        assertDoesNotThrow(() -> channelService.updateChannel("user_a", new NotificationChannelView(
                "unique@example.com", true, "serverchan", "", false, true, "14:30", "ALL_WATCHLIST", false
        )));

        // 同一用户修改为新邮箱
        assertDoesNotThrow(() -> channelService.bindEmail("user_a", "unique_new@example.com"));
        assertEquals("unique_new@example.com", channelService.getChannel("user_a").email());

        // 其他用户尝试绑定 user_a 已绑定的新邮箱，触发唯一性异常
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> channelService.bindEmail("user_b", "unique_new@example.com"));
        assertTrue(ex.getMessage().contains("已被其他账号使用"));

        // 原邮箱 unique@example.com 已被释放，user_b 可以绑定
        assertDoesNotThrow(() -> channelService.bindEmail("user_b", "unique@example.com"));
        assertEquals("unique@example.com", channelService.getChannel("user_b").email());
    }

    @Test
    void testInvalidEmailThrowsException() {
        NotificationChannelView invalid = new NotificationChannelView(
                "invalid-email-format",
                true,
                "serverchan",
                "",
                false,
                true,
                "14:30",
                "ALL_WATCHLIST",
                false
        );
        assertThrows(IllegalArgumentException.class, () -> channelService.updateChannel("user1", invalid));
    }

    @Test
    void testListActiveRemindUsers() {
        // 用户1：开启提醒 + 开启邮箱 (默认 14:30)
        channelService.updateChannel("u1", new NotificationChannelView(
                "u1@example.com", true, "serverchan", "", false, true, "14:30", "ALL_WATCHLIST", false
        ));

        // 用户2：开启提醒 + 开启微信 (14:15)
        channelService.updateChannel("u2", new NotificationChannelView(
                "", false, "serverchan", "SCT2", true, true, "14:15", "ALL_WATCHLIST", false
        ));

        // 用户3：总开关关闭
        channelService.updateChannel("u3", new NotificationChannelView(
                "u3@example.com", true, "serverchan", "SCT3", true, false, "14:30", "ALL_WATCHLIST", false
        ));

        // 用户4：总开关开启但子渠道全关
        channelService.updateChannel("u4", new NotificationChannelView(
                "u4@example.com", false, "serverchan", "SCT4", false, true, "14:30", "ALL_WATCHLIST", false
        ));

        List<UserNotificationChannel> activeUsers = channelService.listActiveRemindUsers();
        assertEquals(2, activeUsers.size());
        List<String> userIds = activeUsers.stream().map(UserNotificationChannel::getUserId).toList();
        assertTrue(userIds.contains("u1"));
        assertTrue(userIds.contains("u2"));
        assertFalse(userIds.contains("u3"));
        assertFalse(userIds.contains("u4"));

        // 按指定时间筛选
        List<UserNotificationChannel> usersAt1415 = channelService.listActiveRemindUsers("14:15");
        assertEquals(1, usersAt1415.size());
        assertEquals("u2", usersAt1415.get(0).getUserId());

        List<UserNotificationChannel> usersAt1430 = channelService.listActiveRemindUsers("14:30");
        assertEquals(1, usersAt1430.size());
        assertEquals("u1", usersAt1430.get(0).getUserId());
    }
}
