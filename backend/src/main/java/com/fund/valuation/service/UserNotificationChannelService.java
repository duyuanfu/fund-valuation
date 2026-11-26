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
            "serverchan",
            "",
            true,
            true,
            "14:30",
            "ALL_WATCHLIST",
            false
    );

    /**
     * 校验电子邮箱唯一性 (排除指定用户 excludeUserId，支持当前用户修改保持原邮箱)
     */
    public void checkEmailAvailable(String email, String excludeUserId) {
        if (email == null || email.isBlank()) {
            return;
        }
        String clean = email.trim();
        LambdaQueryWrapper<UserNotificationChannel> query = new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getEmail, clean);
        if (excludeUserId != null && !excludeUserId.isBlank()) {
            query.ne(UserNotificationChannel::getUserId, excludeUserId);
        }
        query.last("LIMIT 1");
        UserNotificationChannel existing = channelMapper.selectOne(query);
        if (existing != null) {
            throw new IllegalArgumentException("该电子邮箱已被其他账号使用，请更换邮箱");
        }
    }

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
            checkEmailAvailable(email, userId);
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
        entity.setWechatChannelType("serverchan");
        entity.setServerchanKey(req.serverchanKey() != null ? req.serverchanKey().trim() : "");
        entity.setWechatEnabled(req.wechatEnabled() != null ? req.wechatEnabled() : true);
        entity.setRemindEnabled(req.remindEnabled() != null ? req.remindEnabled() : true);
        entity.setRemindTime(req.remindTime() != null && !req.remindTime().isBlank() ? req.remindTime().trim() : "14:30");
        entity.setPushScope(req.pushScope() != null && !req.pushScope().isBlank() ? req.pushScope() : "ALL_WATCHLIST");
        entity.setHideAmount(req.hideAmount() != null ? req.hideAmount() : false);
        entity.setUpdatedAt(LocalDateTime.now());

        if (isNew) {
            channelMapper.insert(entity);
        } else {
            channelMapper.updateById(entity);
        }

        log.info("用户 [{}] 成功更新通知渠道设置: 提醒时间={}, 总开关={}", userId, entity.getRemindTime(), entity.getRemindEnabled());
        return toView(entity);
    }

    /**
     * 针对新用户注册或未绑定邮箱的老用户快速绑定邮箱
     */
    @Transactional
    public void bindEmail(String userId, String email) {
        if (userId == null || userId.isBlank() || email == null || email.isBlank()) {
            return;
        }
        String cleanEmail = email.trim();
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("电子邮箱格式不正确");
        }
        checkEmailAvailable(cleanEmail, userId);
        UserNotificationChannel entity = channelMapper.selectOne(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getUserId, userId));
        if (entity == null) {
            entity = new UserNotificationChannel();
            entity.setUserId(userId);
            entity.setEmail(cleanEmail);
            entity.setEmailEnabled(true);
            entity.setWechatChannelType("serverchan");
            entity.setWechatEnabled(true);
            entity.setRemindEnabled(true);
            entity.setRemindTime("21:30");
            entity.setPushScope("ALL_WATCHLIST");
            entity.setHideAmount(false);
            entity.setUpdatedAt(LocalDateTime.now());
            channelMapper.insert(entity);
        } else {
            entity.setEmail(cleanEmail);
            entity.setEmailEnabled(true);
            entity.setUpdatedAt(LocalDateTime.now());
            channelMapper.updateById(entity);
        }
        log.info("用户 [{}] 成功绑定邮箱: {}", userId, cleanEmail);
    }

    public void testEmail(String userId) {
        testEmail(userId, null);
    }

    public void testEmail(String userId, String targetEmail) {
        String emailToSend = targetEmail;
        UserNotificationChannel entity = channelMapper.selectOne(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getUserId, userId));

        if (emailToSend != null && !emailToSend.isBlank()) {
            emailToSend = emailToSend.trim();
            if (!EMAIL_PATTERN.matcher(emailToSend).matches()) {
                throw new IllegalArgumentException("电子邮箱格式不正确");
            }
            checkEmailAvailable(emailToSend, userId);
            if (entity == null) {
                entity = new UserNotificationChannel();
                entity.setUserId(userId);
                entity.setEmail(emailToSend);
                entity.setEmailEnabled(true);
                entity.setRemindEnabled(true);
                entity.setRemindTime("21:30");
                entity.setUpdatedAt(LocalDateTime.now());
                channelMapper.insert(entity);
            } else {
                entity.setEmail(emailToSend);
                entity.setEmailEnabled(true);
                entity.setUpdatedAt(LocalDateTime.now());
                channelMapper.updateById(entity);
            }
        } else {
            if (entity == null || entity.getEmail() == null || entity.getEmail().isBlank()) {
                throw new IllegalArgumentException("请先填写并保存您的电子邮箱地址");
            }
            emailToSend = entity.getEmail().trim();
        }

        emailSenderService.sendTestEmail(emailToSend);
    }

    public void testServerchan(String userId, String key) {
        UserNotificationChannel entity = channelMapper.selectOne(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getUserId, userId));

        if (key != null && !key.isBlank()) {
            boolean isNew = (entity == null);
            if (isNew) {
                entity = new UserNotificationChannel();
                entity.setUserId(userId);
                entity.setRemindEnabled(true);
                entity.setRemindTime("21:30");
                entity.setPushScope("ALL_WATCHLIST");
                entity.setHideAmount(false);
            }
            entity.setWechatChannelType("serverchan");
            entity.setServerchanKey(key.trim());
            entity.setWechatEnabled(true);
            entity.setUpdatedAt(LocalDateTime.now());
            if (isNew) {
                channelMapper.insert(entity);
            } else {
                channelMapper.updateById(entity);
            }
        } else {
            if (entity == null || entity.getServerchanKey() == null || entity.getServerchanKey().isBlank()) {
                throw new IllegalArgumentException("请先输入 Server酱 SendKey（以 SCT 开头）");
            }
        }

        wxNotificationService.sendTestWechat(entity);
    }

    public void testWechat(String userId) {
        testWechat(userId, null, null);
    }

    public void testWechat(String userId, String tokenOrUrl, String channelType) {
        testServerchan(userId, tokenOrUrl);
    }

    public List<UserNotificationChannel> listActiveRemindUsers() {
        return channelMapper.selectList(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getRemindEnabled, true)
                .and(w -> w.eq(UserNotificationChannel::getEmailEnabled, true)
                        .or().eq(UserNotificationChannel::getWechatEnabled, true)));
    }

    public List<UserNotificationChannel> listActiveRemindUsers(String remindTime) {
        if (remindTime == null || remindTime.isBlank()) {
            return listActiveRemindUsers();
        }
        return channelMapper.selectList(new LambdaQueryWrapper<UserNotificationChannel>()
                .eq(UserNotificationChannel::getRemindEnabled, true)
                .and(w -> w.eq(UserNotificationChannel::getEmailEnabled, true)
                        .or().eq(UserNotificationChannel::getWechatEnabled, true))
                .and(w -> w.eq(UserNotificationChannel::getRemindTime, remindTime)
                        .or(remindTime.equals("14:30"), o -> o.isNull(UserNotificationChannel::getRemindTime).or().eq(UserNotificationChannel::getRemindTime, ""))));
    }

    private NotificationChannelView toView(UserNotificationChannel e) {
        return new NotificationChannelView(
                e.getEmail(),
                e.getEmailEnabled(),
                "serverchan",
                e.getServerchanKey(),
                e.getWechatEnabled(),
                e.getRemindEnabled(),
                e.getRemindTime() != null && !e.getRemindTime().isBlank() ? e.getRemindTime() : "14:30",
                e.getPushScope() != null ? e.getPushScope() : "ALL_WATCHLIST",
                e.getHideAmount() != null ? e.getHideAmount() : false
        );
    }
}
