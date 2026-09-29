package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.domain.SysNotice;
import com.fund.valuation.dto.NoticeView;
import com.fund.valuation.mapper.SysNoticeMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通用系统公告与消息区服务:
 * 1. 优先展示由 Admin 或配置维护的系统通知公告 (如新功能上线、系统维护等);
 * 2. Admin 每次保存公告都会覆盖替换旧文案，并通过 MyBatis-Plus 持久化至数据库，重启不丢失;
 * 3. Admin 在控制台关闭自定义公告后，前端不再向用户展示任何公告。
 */
@Slf4j
@Service
public class SystemNoticeService {

    private final AppProperties properties;
    private final SysNoticeMapper noticeMapper;
    private final AtomicReference<NoticeState> currentNotice = new AtomicReference<>();

    public record NoticeState(String message, String type, boolean enabled, boolean closable) {}

    public SystemNoticeService(AppProperties properties) {
        this(properties, null);
    }

    @Autowired
    public SystemNoticeService(AppProperties properties, @Autowired(required = false) SysNoticeMapper noticeMapper) {
        this.properties = properties;
        this.noticeMapper = noticeMapper;
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
        if (noticeMapper != null) {
            try {
                SysNotice persistent = noticeMapper.selectOne(new LambdaQueryWrapper<SysNotice>()
                        .orderByDesc(SysNotice::getId)
                        .last("limit 1"));
                if (persistent != null) {
                    currentNotice.set(new NoticeState(
                            persistent.getNoticeMessage(),
                            persistent.getNoticeType(),
                            Boolean.TRUE.equals(persistent.getIsEnabled()),
                            Boolean.TRUE.equals(persistent.getIsClosable())
                    ));
                    log.info("loaded persistent system notice from database: enabled={}, type={}, message={}",
                            persistent.getIsEnabled(), persistent.getNoticeType(), persistent.getNoticeMessage());
                }
            } catch (Exception e) {
                log.debug("could not load notice from sys_notice: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取当前有效公告。
     * 未启用自定义公告时返回 null，即不再向用户展示任何公告。
     */
    public NoticeView getActiveNotice() {
        NoticeState state = currentNotice.get();
        if (state != null && state.enabled() && state.message() != null && !state.message().isBlank()) {
            return new NoticeView(state.message(), state.type(), state.closable(), true);
        }
        return null;
    }

    /**
     * 获取后台配置的公告 (含已关闭状态)，用于管理控制台表单回显。
     */
    public NoticeView getConfiguredNotice() {
        NoticeState state = currentNotice.get();
        if (state == null || state.message() == null || state.message().isBlank()) {
            return null;
        }
        return new NoticeView(state.message(), state.type(), state.closable(), state.enabled());
    }

    /**
     * Admin 动态配置/更新公告并持久化至数据库 (覆盖替换旧文案)。
     */
    @Transactional
    public NoticeView updateNotice(String message, String type, Boolean enabled, Boolean closable) {
        boolean isEnabled = enabled == null ? (message != null && !message.isBlank()) : enabled;
        boolean isClosable = closable == null || closable;
        String noticeType = (type == null || type.isBlank()) ? "info" : type;
        NoticeState newState = new NoticeState(message, noticeType, isEnabled, isClosable);

        if (noticeMapper != null) {
            try {
                noticeMapper.delete(null);
                SysNotice entity = new SysNotice();
                entity.setNoticeMessage(message);
                entity.setNoticeType(noticeType);
                entity.setIsEnabled(isEnabled);
                entity.setIsClosable(isClosable);
                entity.setUpdatedAt(LocalDateTime.now());
                noticeMapper.insert(entity);
                log.info("system notice persisted to database (replaced previous), enabled={}", isEnabled);
            } catch (Exception e) {
                log.warn("failed to persist system notice to db: {}", e.getMessage());
            }
        }

        currentNotice.set(newState);
        log.info("system notice updated: enabled={}, type={}, message={}", isEnabled, noticeType, message);
        return getConfiguredNotice();
    }
}
