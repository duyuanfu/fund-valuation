package com.fund.valuation.dto;

public record NotificationChannelView(
        String email,
        Boolean emailEnabled,
        String wechatChannelType,
        String serverchanKey,
        String barkUrl,
        Boolean wechatEnabled,
        Boolean remindEnabled,
        String remindTime,
        String pushScope,
        Boolean hideAmount,
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
            String barkUrl,
            Boolean wechatEnabled,
            Boolean remindEnabled,
            String remindTime,
            String pushScope,
            Boolean hideAmount
    ) {
        this(email, emailEnabled, wechatChannelType != null ? wechatChannelType : "serverchan",
                serverchanKey, barkUrl,
                wechatEnabled != null ? wechatEnabled : true,
                remindEnabled != null ? remindEnabled : true,
                remindTime != null && !remindTime.isBlank() ? remindTime : "14:30",
                pushScope != null ? pushScope : "ALL_WATCHLIST",
                hideAmount != null ? hideAmount : false,
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
        this(email, emailEnabled, wechatChannelType, null, null, wechatEnabled, remindEnabled, "14:30", pushScope, hideAmount, wechatUid, null, wechatWebhookUrl, null, false);
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
        this(email, emailEnabled, wechatChannelType, null, null, wechatEnabled, remindEnabled, "14:30", pushScope, hideAmount, wechatUid, wechatAppToken, wechatWebhookUrl, systemWxpusherQrUrl, hasSystemWxpusherToken);
    }
}
