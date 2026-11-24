package com.fund.valuation.scheduler;

import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.domain.User;
import com.fund.valuation.domain.UserNotificationChannel;
import com.fund.valuation.dto.PortfolioView;
import com.fund.valuation.service.AuthService;
import com.fund.valuation.service.CalendarHolidayService;
import com.fund.valuation.service.DailyReminderTemplateService;
import com.fund.valuation.service.EmailSenderService;
import com.fund.valuation.service.EstimateResult;
import com.fund.valuation.service.IntradayValuationRunner;
import com.fund.valuation.service.PortfolioService;
import com.fund.valuation.service.UserNotificationChannelService;
import com.fund.valuation.service.WatchlistService;
import com.fund.valuation.service.WxNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 交易日 14:30 盘中关键决策半小时估值与持仓日报定时提醒调度器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReminderScheduler {

    private final IntradayValuationRunner runner;
    private final UserNotificationChannelService channelService;
    private final PortfolioService portfolioService;
    private final WatchlistService watchlistService;
    private final AuthService authService;
    private final DailyReminderTemplateService templateService;
    private final EmailSenderService emailSenderService;
    private final WxNotificationService wxNotificationService;

    /**
     * 每个交易日 14:30 准时触发
     */
    @Scheduled(cron = "0 30 14 * * 1-5", zone = "Asia/Shanghai")
    public void runScheduledReminder() {
        executeDailyReminder(false);
    }

    /**
     * 执行每日 14:30 推送逻辑 (支持管理后台 force 手动测试触发)
     */
    public int executeDailyReminder(boolean force) {
        LocalDate today = LocalDate.now();

        if (!force) {
            if (!TradingCalendar.isTradingDay(today)) {
                log.info("今日 [{}] 为法定休市日/周末/节假日，跳过 14:30 盘中估值提醒推送", today);
                return 0;
            }
        }

        log.info(">>> 开始执行交易日 14:30 盘中估值与持仓提醒派发任务 (force={})", force);

        // 1. 预热刷新盘中最新估值快照
        try {
            runner.run();
        } catch (Exception e) {
            log.warn("14:30 预热估值运行异常，继续使用缓存估值: {}", e.getMessage());
        }

        // 2. 查询所有启用了每日提醒的用户
        List<UserNotificationChannel> channels = channelService.listActiveRemindUsers();
        if (channels.isEmpty()) {
            log.info("当前无开启 14:30 提醒的活跃订阅用户，任务结束");
            return 0;
        }

        int sentCount = 0;
        for (UserNotificationChannel ch : channels) {
            try {
                processSingleUserReminder(ch);
                sentCount++;
                // 发送微延时，防高并发频控
                Thread.sleep(150);
            } catch (Exception e) {
                log.warn("向用户 [{}] 派发 14:30 提醒失败: {}", ch.getUserId(), e.getMessage());
            }
        }

        log.info("<<< 交易日 14:30 盘中估值提醒派发完毕，成功分发用户数: {}", sentCount);
        return sentCount;
    }

    private void processSingleUserReminder(UserNotificationChannel ch) {
        String username = ch.getUserId();
        User user = authService.getUser(username);
        if (user == null || User.STATUS_DISABLED.equalsIgnoreCase(user.getStatus())) {
            return;
        }

        // 查询用户持仓数据 (VIP 用户有效持仓)
        PortfolioView portfolioView = null;
        if (user.isVipEffective()) {
            try {
                portfolioView = portfolioService.getPortfolio(username);
            } catch (Exception e) {
                log.debug("获取用户 [{}] 持仓失败: {}", username, e.getMessage());
            }
        }

        // 查询用户自选列表
        List<EstimateResult> watchlist = Collections.emptyList();
        boolean portfolioOnly = "PORTFOLIO_ONLY".equalsIgnoreCase(ch.getPushScope());
        if (!portfolioOnly) {
            try {
                watchlist = watchlistService.estimates(username);
            } catch (Exception e) {
                log.debug("获取用户 [{}] 自选失败: {}", username, e.getMessage());
            }
        }

        boolean hideAmount = Boolean.TRUE.equals(ch.getHideAmount());

        // 1. 发送邮件
        if (Boolean.TRUE.equals(ch.getEmailEnabled()) && ch.getEmail() != null && !ch.getEmail().isBlank()) {
            try {
                String subject = "【盘中快报】14:30 基金估值与持仓日报 - " + username;
                String html = templateService.renderHtmlEmail(
                        username,
                        portfolioView != null ? portfolioView.summary() : null,
                        portfolioView != null ? portfolioView.items() : Collections.emptyList(),
                        watchlist,
                        hideAmount,
                        "http://localhost:8080"
                );
                emailSenderService.sendHtmlEmail(ch.getEmail().trim(), subject, html);
            } catch (Exception e) {
                log.warn("用户 [{}] 邮件分发异常: {}", username, e.getMessage());
            }
        }

        // 2. 发送微信 / Webhook
        if (Boolean.TRUE.equals(ch.getWechatEnabled())) {
            try {
                String title = "【14:30 盘中估值快报】" + username;
                String markdown = templateService.renderMarkdown(
                        username,
                        portfolioView != null ? portfolioView.summary() : null,
                        portfolioView != null ? portfolioView.items() : Collections.emptyList(),
                        watchlist,
                        hideAmount
                );
                wxNotificationService.sendWechatNotification(ch, title, markdown, "http://localhost:8080");
            } catch (Exception e) {
                log.warn("用户 [{}] 微信/Webhook 分发异常: {}", username, e.getMessage());
            }
        }
    }
}
