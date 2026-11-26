package com.fund.valuation.scheduler;

import com.fund.valuation.domain.User;
import com.fund.valuation.domain.UserNotificationChannel;
import com.fund.valuation.dto.PortfolioSummaryView;
import com.fund.valuation.dto.PortfolioView;
import com.fund.valuation.service.AuthService;
import com.fund.valuation.service.DailyReminderTemplateService;
import com.fund.valuation.service.EmailSenderService;
import com.fund.valuation.service.EstimateResult;
import com.fund.valuation.service.IntradayValuationRunner;
import com.fund.valuation.service.PortfolioService;
import com.fund.valuation.service.UserNotificationChannelService;
import com.fund.valuation.service.WatchlistService;
import com.fund.valuation.service.WxNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReminderSchedulerTest {

    @Mock
    private IntradayValuationRunner runner;

    @Mock
    private UserNotificationChannelService channelService;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private WatchlistService watchlistService;

    @Mock
    private AuthService authService;

    @Mock
    private DailyReminderTemplateService templateService;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private WxNotificationService wxNotificationService;

    @InjectMocks
    private DailyReminderScheduler scheduler;

    @Test
    void testExecuteDailyReminder_NoActiveUsers() {
        when(channelService.listActiveRemindUsers()).thenReturn(Collections.emptyList());

        int sent = scheduler.executeDailyReminder(true);

        assertEquals(0, sent);
        verify(runner).run();
        verifyNoInteractions(emailSenderService);
        verifyNoInteractions(wxNotificationService);
    }

    @Test
    void testExecuteDailyReminder_WithActiveUsers() {
        UserNotificationChannel ch = new UserNotificationChannel();
        ch.setId(1L);
        ch.setUserId("vip_user");
        ch.setEmail("vip@example.com");
        ch.setEmailEnabled(true);
        ch.setWechatEnabled(true);
        ch.setWechatChannelType("wxpusher");
        ch.setWechatUid("UID_VIP");
        ch.setRemindEnabled(true);
        ch.setPushScope("ALL_WATCHLIST");
        ch.setHideAmount(false);
        ch.setRemindTime("21:30");

        when(channelService.listActiveRemindUsers()).thenReturn(List.of(ch));

        User user = new User();
        user.setUsername("vip_user");
        user.setStatus(User.STATUS_NORMAL);
        user.setIsVip(true);
        user.setVipExpireAt(LocalDateTime.now().plusMonths(1));
        when(authService.getUser("vip_user")).thenReturn(user);

        PortfolioSummaryView summary = new PortfolioSummaryView(
                BigDecimal.valueOf(100000.0),
                BigDecimal.valueOf(95000.0),
                BigDecimal.valueOf(500.0),
                BigDecimal.valueOf(5000.0),
                BigDecimal.valueOf(1200.0),
                BigDecimal.valueOf(1.2),
                BigDecimal.valueOf(6200.0),
                BigDecimal.valueOf(6.52)
        );
        PortfolioView portfolioView = new PortfolioView(summary, Collections.emptyList());
        when(portfolioService.getPortfolio("vip_user")).thenReturn(portfolioView);

        EstimateResult est = new EstimateResult(
                "110022",
                "易方达消费行业",
                "股票型",
                BigDecimal.valueOf(2.345),
                BigDecimal.valueOf(1.25),
                BigDecimal.valueOf(2.316),
                LocalDate.now(),
                "2026Q2",
                LocalDateTime.now(),
                false,
                null,
                null
        );
        when(watchlistService.estimates("vip_user")).thenReturn(List.of(est));

        when(templateService.renderHtmlEmail(anyString(), anyString(), any(), anyList(), anyList(), anyBoolean(), anyString()))
                .thenReturn("<html>mock</html>");
        when(templateService.renderMarkdown(anyString(), anyString(), any(), anyList(), anyList(), anyBoolean()))
                .thenReturn("## mock markdown");

        int sent = scheduler.executeDailyReminder(true);

        assertEquals(1, sent);
        verify(runner).run();
        verify(emailSenderService).sendHtmlEmail(eq("vip@example.com"), contains("21:30"), eq("<html>mock</html>"));
        verify(wxNotificationService).sendWechatNotification(eq(ch), contains("21:30"), eq("## mock markdown"), anyString());
    }

    @Test
    void testExecuteDailyReminder_UserDisabled_ShouldSkip() {
        UserNotificationChannel ch = new UserNotificationChannel();
        ch.setUserId("disabled_user");
        ch.setEmail("disabled@example.com");
        ch.setEmailEnabled(true);
        ch.setRemindEnabled(true);

        when(channelService.listActiveRemindUsers()).thenReturn(List.of(ch));

        User user = new User();
        user.setUsername("disabled_user");
        user.setStatus(User.STATUS_DISABLED);
        when(authService.getUser("disabled_user")).thenReturn(user);

        int sent = scheduler.executeDailyReminder(true);

        assertEquals(1, sent);
        verifyNoInteractions(emailSenderService);
        verifyNoInteractions(wxNotificationService);
    }
}
