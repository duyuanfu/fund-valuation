package com.fund.valuation.dto;

public record NotificationChannelView(
        String email,
        Boolean emailEnabled,
        String wechatChannelType,
        String serverchanKey,
        Boolean wechatEnabled,
        Boolean remindEnabled,
        String remindTime,
        String pushScope,
        Boolean hideAmount,
        Boolean eveningRemindEnabled,
        String eveningRemindTime,
        String wechatUid,
        String wechatAppToken,
        String wechatWebhookUrl,
        String systemWxpusherQrUrl,
        Boolean hasSystemWxpusherToken
) {
    public NotificationChannelView(
            String email,
            Boolean emailEnabled,
            String wechatChannelType,
            String serverchanKey,
            Boolean wechatEnabled,
            Boolean remindEnabled,
            String remindTime,
            String pushScope,
            Boolean hideAmount
    ) {
        this(email, emailEnabled, wechatChannelType != null ? wechatChannelType : "serverchan",
                serverchanKey,
                wechatEnabled != null ? wechatEnabled : true,
                remindEnabled != null ? remindEnabled : true,
                remindTime != null && !remindTime.isBlank() ? remindTime : "14:30",
                pushScope != null ? pushScope : "ALL_WATCHLIST",
                hideAmount != null ? hideAmount : false,
                true,
                "21:30",
                null, null, null, null, false);
    }

    public NotificationChannelView(
            String email,
            Boolean emailEnabled,
            String wechatChannelType,
            String serverchanKey,
            Boolean wechatEnabled,
            Boolean remindEnabled,
            String remindTime,
            String pushScope,
            Boolean hideAmount,
            Boolean eveningRemindEnabled,
            String eveningRemindTime
    ) {
        this(email, emailEnabled, wechatChannelType != null ? wechatChannelType : "serverchan",
                serverchanKey,
                wechatEnabled != null ? wechatEnabled : true,
                remindEnabled != null ? remindEnabled : true,
                remindTime != null && !remindTime.isBlank() ? remindTime : "14:30",
                pushScope != null ? pushScope : "ALL_WATCHLIST",
                hideAmount != null ? hideAmount : false,
                eveningRemindEnabled != null ? eveningRemindEnabled : true,
                eveningRemindTime != null && !eveningRemindTime.isBlank() ? eveningRemindTime : "21:30",
                null, null, null, null, false);
    }

    public NotificationChannelView(
            String email,
            Boolean emailEnabled,
            String wechatChannelType,
            String wechatUid,
            String wechatWebhookUrl,
            Boolean wechatEnabled,
            Boolean remindEnabled,
            String pushScope,
            Boolean hideAmount
    ) {
        this(email, emailEnabled, wechatChannelType, null, wechatEnabled, remindEnabled, "14:30", pushScope, hideAmount, true, "21:30", wechatUid, null, wechatWebhookUrl, null, false);
    }

    public NotificationChannelView(
            String email,
            Boolean emailEnabled,
            String wechatChannelType,
            String wechatUid,
            String wechatAppToken,
            String wechatWebhookUrl,
            Boolean wechatEnabled,
            Boolean remindEnabled,
            String pushScope,
            Boolean hideAmount,
            String systemWxpusherQrUrl,
            Boolean hasSystemWxpusherToken
    ) {
        this(email, emailEnabled, wechatChannelType, null, wechatEnabled, remindEnabled, "14:30", pushScope, hideAmount, true, "21:30", wechatUid, wechatAppToken, wechatWebhookUrl, systemWxpusherQrUrl, hasSystemWxpusherToken);
    }
}
