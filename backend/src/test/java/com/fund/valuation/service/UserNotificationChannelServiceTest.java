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
                "",
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

        // 更新配置为 Bark
        NotificationChannelView updateReq = new NotificationChannelView(
                "updated@example.com",
                false,
                "bark",
                "",
                "https://api.day.app/DEVICE_KEY",
                true,
                true,
                "14:45",
                "PORTFOLIO_ONLY",
                false
        );
        NotificationChannelView updated = channelService.updateChannel(userId, updateReq);
        assertEquals("updated@example.com", updated.email());
        assertFalse(updated.emailEnabled());
        assertEquals("bark", updated.wechatChannelType());
        assertEquals("https://api.day.app/DEVICE_KEY", updated.barkUrl());
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
    void testInvalidEmailThrowsException() {
        NotificationChannelView invalid = new NotificationChannelView(
                "invalid-email-format",
                true,
                "serverchan",
                "",
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
                "u1@example.com", true, "serverchan", "", "", false, true, "14:30", "ALL_WATCHLIST", false
        ));

        // 用户2：开启提醒 + 开启微信 (14:15)
        channelService.updateChannel("u2", new NotificationChannelView(
                "", false, "serverchan", "SCT2", "", true, true, "14:15", "ALL_WATCHLIST", false
        ));

        // 用户3：总开关关闭
        channelService.updateChannel("u3", new NotificationChannelView(
                "u3@example.com", true, "serverchan", "SCT3", "", true, false, "14:30", "ALL_WATCHLIST", false
        ));

        // 用户4：总开关开启但子渠道全关
        channelService.updateChannel("u4", new NotificationChannelView(
                "u4@example.com", false, "serverchan", "SCT4", "", false, true, "14:30", "ALL_WATCHLIST", false
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
