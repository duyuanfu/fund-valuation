package com.fund.valuation.dto;

public record NotificationChannelView(
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
}
