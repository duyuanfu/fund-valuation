package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_notification_channel")
public class UserNotificationChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户名 */
    private String userId;

    /** 电子邮箱 */
    private String email;

    /** 是否启用邮箱推送 */
    private Boolean emailEnabled;

    /** 移动端/微信通道类型: serverchan */
    private String wechatChannelType;

    /** Server酱 SendKey (微信推送) */
    private String serverchanKey;

    /** 历史兼容 WxPusher UID */
    private String wechatUid;

    /** 历史兼容 AppToken */
    private String wechatAppToken;

    /** 历史兼容 Webhook URL */
    private String wechatWebhookUrl;

    /** 是否启用微信/移动端推送 */
    private Boolean wechatEnabled;

    /** 盘中提醒总开关 */
    private Boolean remindEnabled;

    /** 每日提醒时间点，默认 '14:30'，可选 '14:00', '14:15', '14:30', '14:45', '14:50' */
    private String remindTime;

    /** 推送内容范围: ALL_WATCHLIST(全部自选) / PORTFOLIO_ONLY(仅持仓) */
    private String pushScope;

    /** 是否隐藏具体资产金额 (隐私脱敏，仅展示百分比涨跌与收益率) */
    private Boolean hideAmount;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
