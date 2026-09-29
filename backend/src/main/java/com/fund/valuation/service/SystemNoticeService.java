package com.fund.valuation.service;

import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.dto.NoticeView;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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
public class SystemNoticeService {

    private final AppProperties properties;
    private final DataSource dataSource;

    private final AtomicReference<NoticeState> currentNotice = new AtomicReference<>();

    public record NoticeState(String message, String type, boolean enabled, boolean closable) {}

    public SystemNoticeService(AppProperties properties) {
        this(properties, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SystemNoticeService(AppProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        // 1. 默认尝试从 application.yml 加载初始公告
        AppProperties.Notice n = properties.getNotice();
        if (n != null && n.getMessage() != null && !n.getMessage().isBlank()) {
            currentNotice.set(new NoticeState(n.getMessage(), n.getType(), n.isEnabled(), n.isClosable()));
        } else {
            currentNotice.set(new NoticeState(null, "info", false, true));
        }

        // 2. 检查数据库持久化配置，若存在数据库自定义公告则覆盖内存默认值
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT notice_message, notice_type, is_enabled, is_closable FROM sys_notice ORDER BY id DESC LIMIT 1")) {
                if (rs.next()) {
                    String msg = rs.getString("notice_message");
                    String type = rs.getString("notice_type");
                    boolean enabled = rs.getBoolean("is_enabled");
                    boolean closable = rs.getBoolean("is_closable");
                    currentNotice.set(new NoticeState(msg, type, enabled, closable));
                    log.info("loaded persistent system notice from database: enabled={}, type={}, message={}", enabled, type, msg);
                }
            } catch (Exception e) {
                log.debug("could not load notice from sys_notice: {}", e.getMessage());
            }
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
     * Admin 动态配置/更新公告并持久化至数据库。
     */
    public NoticeView updateNotice(String message, String type, Boolean enabled, Boolean closable) {
        boolean isEnabled = enabled == null ? (message != null && !message.isBlank()) : enabled;
        boolean isClosable = closable == null || closable;
        String noticeType = (type == null || type.isBlank()) ? "info" : type;
        NoticeState newState = new NoticeState(message, noticeType, isEnabled, isClosable);
        currentNotice.set(newState);

        // 持久化至数据库
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection()) {
                try (Statement del = conn.createStatement()) {
                    del.executeUpdate("DELETE FROM sys_notice");
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO sys_notice (notice_message, notice_type, is_enabled, is_closable) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, message);
                    ps.setString(2, noticeType);
                    ps.setBoolean(3, isEnabled);
                    ps.setBoolean(4, isClosable);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                log.warn("failed to persist system notice to db: {}", e.getMessage());
            }
        }

        log.info("system notice updated: enabled={}, type={}, message={}", isEnabled, noticeType, message);
        return getActiveNotice();
    }
}
