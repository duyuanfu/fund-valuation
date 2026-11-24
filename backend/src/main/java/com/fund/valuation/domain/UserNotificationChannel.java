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

    /** 微信通道类型: wxpusher / webhook */
    private String wechatChannelType;

    /** WxPusher 用户 UID (例如 UID_xxx) */
    private String wechatUid;

    /** 自定义 Webhook 地址 (企微/钉钉/飞书/Bark/Server酱) */
    private String wechatWebhookUrl;

    /** 是否启用微信推送 */
    private Boolean wechatEnabled;

    /** 每日14:30盘中提醒总开关 */
    private Boolean remindEnabled;

    /** 推送内容范围: ALL_WATCHLIST(全部自选) / PORTFOLIO_ONLY(仅持仓) */
    private String pushScope;

    /** 是否隐藏具体资产金额 (隐私脱敏，仅展示百分比涨跌与收益率) */
    private Boolean hideAmount;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
