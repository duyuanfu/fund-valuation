package com.fund.valuation.dto;

public record NotificationChannelView(
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
        this(email, emailEnabled, wechatChannelType, wechatUid, null, wechatWebhookUrl, wechatEnabled, remindEnabled, pushScope, hideAmount, null, false);
    }
}
