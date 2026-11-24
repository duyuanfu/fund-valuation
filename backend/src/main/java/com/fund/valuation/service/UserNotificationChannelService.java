package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.UserNotificationChannel;
import com.fund.valuation.dto.NotificationChannelView;
import com.fund.valuation.mapper.UserNotificationChannelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationChannelService {

    private final UserNotificationChannelMapper channelMapper;
    private final EmailSenderService emailSenderService;
    private final WxNotificationService wxNotificationService;
    private final SysEmailConfigService sysEmailConfigService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final NotificationChannelView DEFAULT_VIEW = new NotificationChannelView(
            "",
            true,
            "wxpusher",
            "",
            "",
            "",
            true,
            true,
            "ALL_WATCHLIST",
            false,
            "",
            false
    );

    public NotificationChannelView getChannel(String userId) {
        if (userId == null || userId.isBlank()) {
            return DEFAULT_VIEW;
        }
        UserNotificationChannel entity = channelMapper.selectOne(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getUserId, userId));
        if (entity == null) {
            return DEFAULT_VIEW;
        }
        return toView(entity);
    }

    @Transactional
    public NotificationChannelView updateChannel(String userId, NotificationChannelView req) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("用户未登录");
        }

        if (req.email() != null && !req.email().isBlank()) {
            String email = req.email().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("电子邮箱格式不正确");
            }
        }

        UserNotificationChannel entity = channelMapper.selectOne(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getUserId, userId));
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new UserNotificationChannel();
            entity.setUserId(userId);
        }

        entity.setEmail(req.email() != null ? req.email().trim() : "");
        entity.setEmailEnabled(req.emailEnabled() != null ? req.emailEnabled() : true);
        entity.setWechatChannelType(req.wechatChannelType() != null && !req.wechatChannelType().isBlank() ? req.wechatChannelType() : "wxpusher");
        entity.setWechatUid(req.wechatUid() != null ? req.wechatUid().trim() : "");
        entity.setWechatAppToken(req.wechatAppToken() != null ? req.wechatAppToken().trim() : "");
        entity.setWechatWebhookUrl(req.wechatWebhookUrl() != null ? req.wechatWebhookUrl().trim() : "");
        entity.setWechatEnabled(req.wechatEnabled() != null ? req.wechatEnabled() : true);
        entity.setRemindEnabled(req.remindEnabled() != null ? req.remindEnabled() : true);
        entity.setPushScope(req.pushScope() != null && !req.pushScope().isBlank() ? req.pushScope() : "ALL_WATCHLIST");
        entity.setHideAmount(req.hideAmount() != null ? req.hideAmount() : false);
        entity.setUpdatedAt(LocalDateTime.now());

        if (isNew) {
            channelMapper.insert(entity);
        } else {
            channelMapper.updateById(entity);
        }

        log.info("用户 [{}] 成功更新通知渠道设置: 提醒总开关={}", userId, entity.getRemindEnabled());
        return toView(entity);
    }

    public void testEmail(String userId) {
        UserNotificationChannel entity = channelMapper.selectOne(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getUserId, userId));
        if (entity == null || entity.getEmail() == null || entity.getEmail().isBlank()) {
            throw new IllegalArgumentException("请先填写并保存您的电子邮箱地址");
        }
        emailSenderService.sendTestEmail(entity.getEmail().trim());
    }

    public void testWechat(String userId) {
        UserNotificationChannel entity = channelMapper.selectOne(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getUserId, userId));
        if (entity == null || ((entity.getWechatUid() == null || entity.getWechatUid().isBlank())
                && (entity.getWechatWebhookUrl() == null || entity.getWechatWebhookUrl().isBlank()))) {
            throw new IllegalArgumentException("请先绑定微信扫码 UID 或填写 Webhook 地址");
        }
        wxNotificationService.sendTestWechat(entity);
    }

    public List<UserNotificationChannel> listActiveRemindUsers() {
        return channelMapper.selectList(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getRemindEnabled, true)
                .and(w -> w.eq(UserNotificationChannel::getEmailEnabled, true)
                        .or().eq(UserNotificationChannel::getWechatEnabled, true)));
    }

    private NotificationChannelView toView(UserNotificationChannel e) {
        com.fund.valuation.dto.EmailConfigView emailCfg = sysEmailConfigService != null ? sysEmailConfigService.getEmailConfig() : null;
        String systemQr = (emailCfg != null && emailCfg.wxpusherQrUrl() != null) ? emailCfg.wxpusherQrUrl() : "";
        boolean hasToken = (emailCfg != null && emailCfg.wxpusherAppToken() != null && !emailCfg.wxpusherAppToken().isBlank());

        return new NotificationChannelView(
                e.getEmail(),
                e.getEmailEnabled(),
                e.getWechatChannelType(),
                e.getWechatUid(),
                e.getWechatAppToken(),
                e.getWechatWebhookUrl(),
                e.getWechatEnabled(),
                e.getRemindEnabled(),
                e.getPushScope(),
                e.getHideAmount(),
                systemQr,
                hasToken
        );
    }
}
