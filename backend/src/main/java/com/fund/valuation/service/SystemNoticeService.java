package com.fund.valuation.service;

import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.dto.NoticeView;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通用系统公告与消息区服务:
 * 1. 优先展示由 Admin 或配置维护的系统通知公告 (如新功能上线、系统维护等);
 * 2. 未启用自定义公告时，在非交易时段自动展示默认的行情快照提示;
 * 3. 盘中交易时段且无公告时自动隐藏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemNoticeService {

    private final AppProperties properties;

    private final AtomicReference<NoticeState> currentNotice = new AtomicReference<>();

    public record NoticeState(String message, String type, boolean enabled, boolean closable) {}

    @PostConstruct
    public void init() {
        AppProperties.Notice n = properties.getNotice();
        if (n != null && n.getMessage() != null && !n.getMessage().isBlank()) {
            currentNotice.set(new NoticeState(n.getMessage(), n.getType(), n.isEnabled(), n.isClosable()));
        } else {
            currentNotice.set(new NoticeState(null, "info", false, true));
        }
    }

    /**
     * 获取当前有效公告或市场状态提示。
     */
    public NoticeView getActiveNotice() {
        NoticeState state = currentNotice.get();
        if (state != null && state.enabled() && state.message() != null && !state.message().isBlank()) {
            return new NoticeView(state.message(), state.type(), state.closable(), true);
        }
        // 若无自定义公告，且处于非交易时段，自动展示默认的非交易时段提示
        if (!TradingCalendar.isTradingTime(LocalDateTime.now())) {
            return new NoticeView("当前为非交易时段，展示最新行情快照", "info", false, false);
        }
        return null;
    }

    /**
     * Admin 动态配置/更新公告。
     */
    public NoticeView updateNotice(String message, String type, Boolean enabled, Boolean closable) {
        boolean isEnabled = enabled == null ? (message != null && !message.isBlank()) : enabled;
        boolean isClosable = closable == null || closable;
        String noticeType = (type == null || type.isBlank()) ? "info" : type;
        NoticeState newState = new NoticeState(message, noticeType, isEnabled, isClosable);
        currentNotice.set(newState);
        log.info("system notice updated: enabled={}, type={}, message={}", isEnabled, noticeType, message);
        return getActiveNotice();
    }
}
