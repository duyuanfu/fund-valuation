package com.fund.valuation.scheduler;

import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.domain.User;
import com.fund.valuation.domain.UserNotificationChannel;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

/**
 * 交易日盘中关键决策半小时估值与持仓日报定时提醒调度器。
 * 支持用户自定义提醒时间 (14:00, 14:15, 14:30, 14:45, 14:50 等)。
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
     * 交易日 14:00 - 14:59 每分钟轮询，匹配用户设定的提醒时间
     */
    @Scheduled(cron = "0 * 14 * * 1-5", zone = "Asia/Shanghai")
    public void runScheduledReminder() {
        LocalTime now = LocalTime.now();
        String currentMinute = String.format("%02d:%02d", now.getHour(), now.getMinute());
        executeDailyReminder(false, currentMinute);
    }

    public int executeDailyReminder(boolean force) {
        return executeDailyReminder(force, null);
    }

    /**
     * 执行盘中推送逻辑
     * @param force 是否忽略交易日判定 (支持管理后台手动强制触发)
     * @param targetTimeMinute 指定提醒时间分秒 (如 "14:30")，若为 null 则分发给所有活跃订阅用户
     */
    public int executeDailyReminder(boolean force, String targetTimeMinute) {
        LocalDate today = LocalDate.now();

        if (!force) {
            if (!TradingCalendar.isTradingDay(today)) {
                return 0;
            }
        }

        // 1. 预热刷新盘中最新估值快照
        try {
            runner.run();
        } catch (Exception e) {
            log.warn("预热估值运行异常，继续使用缓存估值: {}", e.getMessage());
        }

        // 2. 查询目标分发用户
        List<UserNotificationChannel> channels;
        if (targetTimeMinute != null && !targetTimeMinute.isBlank()) {
            channels = channelService.listActiveRemindUsers(targetTimeMinute);
        } else {
            channels = channelService.listActiveRemindUsers();
        }

        if (channels.isEmpty()) {
            return 0;
        }

        log.info(">>> 开始执行交易日盘中估值与持仓提醒派发任务 (force={}, targetTime={}, 命中用户数={})",
                force, targetTimeMinute != null ? targetTimeMinute : "ALL", channels.size());

        int sentCount = 0;
        for (UserNotificationChannel ch : channels) {
            try {
                processSingleUserReminder(ch);
                sentCount++;
                // 发送微延时，防高频拥堵
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("向用户 [{}] 派发盘中提醒失败: {}", ch.getUserId(), e.getMessage());
            }
        }

        log.info("<<< 交易日盘中估值提醒派发完毕，成功分发用户数: {}", sentCount);
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
        String remindTime = ch.getRemindTime() != null && !ch.getRemindTime().isBlank() ? ch.getRemindTime() : "14:30";

        // 1. 发送邮件 (富文本 HTML)
        if (Boolean.TRUE.equals(ch.getEmailEnabled()) && ch.getEmail() != null && !ch.getEmail().isBlank()) {
            try {
                String subject = "【盘中快报】" + remindTime + " 基金估值与持仓日报 - " + username;
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

        // 2. 发送微信推送 (Server酱)
        if (Boolean.TRUE.equals(ch.getWechatEnabled())) {
            try {
                String title = "【" + remindTime + " 盘中估值快报】" + username;
                String markdown = templateService.renderMarkdown(
                        username,
                        portfolioView != null ? portfolioView.summary() : null,
                        portfolioView != null ? portfolioView.items() : Collections.emptyList(),
                        watchlist,
                        hideAmount
                );
                wxNotificationService.sendWechatNotification(ch, title, markdown, "http://localhost:8080");
            } catch (Exception e) {
                log.warn("用户 [{}] 移动端通知分发异常: {}", username, e.getMessage());
            }
        }
    }
}
