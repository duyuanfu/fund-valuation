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
 * 交易日定时提醒调度器：
 * 1. 交易日盘中决策提醒 (14:00-14:59，默认 14:30): 发送盘中实时估值与预估涨跌，辅助尾盘申赎调仓决策。
 * 2. 交易日盘后收益提醒 (21:00-21:59，默认 21:30): 发送官方收盘净值确认后的当日实际收益与持仓累计盈亏日报。
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
     * 1. 交易日盘中提醒调度 (14:00-14:59 每分钟轮询，默认 14:30 触发)
     */
    @Scheduled(cron = "0 * 14 * * 1-5", zone = "Asia/Shanghai")
    public void runIntradayScheduledReminder() {
        LocalTime now = LocalTime.now();
        String currentMinute = String.format("%02d:%02d", now.getHour(), now.getMinute());
        executeIntradayReminder(false, currentMinute);
    }

    /**
     * 2. 交易日盘后官方收益提醒调度 (21:00-21:59 每分钟轮询，默认 21:30 触发)
     */
    @Scheduled(cron = "0 * 21 * * 1-5", zone = "Asia/Shanghai")
    public void runEveningScheduledReminder() {
        LocalTime now = LocalTime.now();
        String currentMinute = String.format("%02d:%02d", now.getHour(), now.getMinute());
        executeEveningReminder(false, currentMinute);
    }

    public int executeDailyReminder(boolean force) {
        return executeDailyReminder(force, null);
    }

    /**
     * 兼容/统一执行入口 (支持管理后台手动强制分发或测试)
     */
    public int executeDailyReminder(boolean force, String targetTimeMinute) {
        if (targetTimeMinute != null && (targetTimeMinute.startsWith("2") || targetTimeMinute.startsWith("19"))) {
            return executeEveningReminder(force, targetTimeMinute);
        }
        return executeIntradayReminder(force, targetTimeMinute);
    }

    /**
     * 执行盘中估值提醒推送 (14:30 等时段)
     */
    public int executeIntradayReminder(boolean force, String targetTimeMinute) {
        LocalDate today = LocalDate.now();
        if (!force && !TradingCalendar.isTradingDay(today)) {
            return 0;
        }

        // 预热刷新盘中最新估值快照
        try {
            runner.run();
        } catch (Exception e) {
            log.warn("预热估值运行异常，继续使用缓存估值: {}", e.getMessage());
        }

        List<UserNotificationChannel> channels = channelService.listActiveIntradayRemindUsers(targetTimeMinute);
        if (channels.isEmpty()) {
            return 0;
        }

        log.info(">>> 开始执行交易日【盘中估值提醒】任务 (force={}, targetTime={}, 命中用户数={})",
                force, targetTimeMinute != null ? targetTimeMinute : "ALL", channels.size());

        int sentCount = 0;
        for (UserNotificationChannel ch : channels) {
            try {
                processSingleUserReminder(ch, false, targetTimeMinute != null ? targetTimeMinute : ch.getRemindTime());
                sentCount++;
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("向用户 [{}] 派发盘中提醒失败: {}", ch.getUserId(), e.getMessage());
            }
        }

        log.info("<<< 交易日【盘中估值提醒】派发完毕，成功分发用户数: {}", sentCount);
        return sentCount;
    }

    /**
     * 执行盘后官方确认收益提醒推送 (21:30 等时段)
     */
    public int executeEveningReminder(boolean force, String targetTimeMinute) {
        LocalDate today = LocalDate.now();
        if (!force && !TradingCalendar.isTradingDay(today)) {
            return 0;
        }

        List<UserNotificationChannel> channels = channelService.listActiveEveningRemindUsers(targetTimeMinute);
        if (channels.isEmpty()) {
            return 0;
        }

        log.info(">>> 开始执行交易日【盘后收益提醒】任务 (force={}, targetTime={}, 命中用户数={})",
                force, targetTimeMinute != null ? targetTimeMinute : "ALL", channels.size());

        int sentCount = 0;
        for (UserNotificationChannel ch : channels) {
            try {
                processSingleUserReminder(ch, true, targetTimeMinute != null ? targetTimeMinute : ch.getEveningRemindTime());
                sentCount++;
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("向用户 [{}] 派发盘后收益提醒失败: {}", ch.getUserId(), e.getMessage());
            }
        }

        log.info("<<< 交易日【盘后收益提醒】派发完毕，成功分发用户数: {}", sentCount);
        return sentCount;
    }

    private void processSingleUserReminder(UserNotificationChannel ch, boolean isEvening, String remindTime) {
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
        String displayTime = (remindTime != null && !remindTime.isBlank())
                ? remindTime
                : (isEvening ? "21:30" : "14:30");

        // 1. 发送邮件 (富文本 HTML)
        if (Boolean.TRUE.equals(ch.getEmailEnabled()) && ch.getEmail() != null && !ch.getEmail().isBlank()) {
            try {
                String subject = (isEvening ? "【收益日报】" : "【盘中快报】") + displayTime
                        + (isEvening ? " 今日基金收益与持仓日报 - " : " 基金估值与持仓日报 - ") + username;
                String html = templateService.renderHtmlEmail(
                        username,
                        displayTime,
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
                String title = "【" + displayTime + (isEvening ? " 基金收益快报】" : " 盘中估值快报】") + username;
                String markdown = templateService.renderMarkdown(
                        username,
                        displayTime,
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
