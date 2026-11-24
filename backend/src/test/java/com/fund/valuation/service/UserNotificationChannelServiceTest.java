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
        assertEquals("wxpusher", view.wechatChannelType());
        assertTrue(view.remindEnabled());
    }

    @Test
    void testCreateAndUpdateChannel() {
        String userId = "test_user_channel";
        NotificationChannelView req = new NotificationChannelView(
                "tester@example.com",
                true,
                "wxpusher",
                "UID_123456",
                "",
                true,
                true,
                "ALL_WATCHLIST",
                true
        );

        NotificationChannelView created = channelService.updateChannel(userId, req);
        assertEquals("tester@example.com", created.email());
        assertEquals("UID_123456", created.wechatUid());
        assertTrue(created.hideAmount());

        // 查询持久化结果
        NotificationChannelView fetched = channelService.getChannel(userId);
        assertEquals("tester@example.com", fetched.email());
        assertEquals("UID_123456", fetched.wechatUid());
        assertTrue(fetched.hideAmount());

        // 更新配置
        NotificationChannelView updateReq = new NotificationChannelView(
                "updated@example.com",
                false,
                "webhook",
                "",
                "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc",
                true,
                true,
                "PORTFOLIO_ONLY",
                false
        );
        NotificationChannelView updated = channelService.updateChannel(userId, updateReq);
        assertEquals("updated@example.com", updated.email());
        assertFalse(updated.emailEnabled());
        assertEquals("webhook", updated.wechatChannelType());
        assertEquals("PORTFOLIO_ONLY", updated.pushScope());
        assertFalse(updated.hideAmount());
    }

    @Test
    void testInvalidEmailThrowsException() {
        NotificationChannelView invalid = new NotificationChannelView(
                "invalid-email-format",
                true,
                "wxpusher",
                "",
                "",
                false,
                true,
                "ALL_WATCHLIST",
                false
        );
        assertThrows(IllegalArgumentException.class, () -> channelService.updateChannel("user1", invalid));
    }

    @Test
    void testListActiveRemindUsers() {
        // 用户1：开启提醒 + 开启邮箱
        channelService.updateChannel("u1", new NotificationChannelView(
                "u1@example.com", true, "wxpusher", "", "", false, true, "ALL_WATCHLIST", false
        ));

        // 用户2：开启提醒 + 开启微信
        channelService.updateChannel("u2", new NotificationChannelView(
                "", false, "wxpusher", "UID_2", "", true, true, "ALL_WATCHLIST", false
        ));

        // 用户3：总开关关闭
        channelService.updateChannel("u3", new NotificationChannelView(
                "u3@example.com", true, "wxpusher", "UID_3", "", true, false, "ALL_WATCHLIST", false
        ));

        // 用户4：总开关开启但子渠道全关
        channelService.updateChannel("u4", new NotificationChannelView(
                "u4@example.com", false, "wxpusher", "UID_4", "", false, true, "ALL_WATCHLIST", false
        ));

        List<UserNotificationChannel> activeUsers = channelService.listActiveRemindUsers();
        assertEquals(2, activeUsers.size());
        List<String> userIds = activeUsers.stream().map(UserNotificationChannel::getUserId).toList();
        assertTrue(userIds.contains("u1"));
        assertTrue(userIds.contains("u2"));
        assertFalse(userIds.contains("u3"));
        assertFalse(userIds.contains("u4"));
    }
}
