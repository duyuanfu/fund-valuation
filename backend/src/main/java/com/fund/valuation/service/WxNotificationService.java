package com.fund.valuation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.domain.UserNotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxNotificationService {

    private final ObjectMapper objectMapper;
    private final SysVipConfigService sysVipConfigService;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    /**
     * 向用户的微信通道或自定义 Webhook 发送通知
     */
    public boolean sendWechatNotification(UserNotificationChannel channel, String title, String content, String jumpUrl) {
        if (channel == null || Boolean.FALSE.equals(channel.getWechatEnabled())) {
            return false;
        }

        // 1. 若配置了自定义 Webhook 地址 (企微/钉钉/飞书/Bark/Server酱)
        if (channel.getWechatWebhookUrl() != null && !channel.getWechatWebhookUrl().isBlank()) {
            return sendToWebhook(channel.getWechatWebhookUrl().trim(), title, content, jumpUrl);
        }

        // 2. 若配置了 WxPusher UID (微信扫码个人关注推送)
        if ("wxpusher".equalsIgnoreCase(channel.getWechatChannelType())
                && channel.getWechatUid() != null && !channel.getWechatUid().isBlank()) {
            return sendToWxPusher(channel.getWechatUid().trim(), title, content, jumpUrl);
        }

        return false;
    }

    /**
     * 发送测试消息验证微信/Webhook连通性
     */
    public boolean sendTestWechat(UserNotificationChannel channel) {
        String title = "【基金实时估值】微信通知绑定测试成功";
        String content = """
            ### 🎉 微信通知绑定成功
            
            这是一条连通性测试消息！
            
            - **推送时间**: 交易日每日 14:30（收盘前半小时）
            - **推送内容**: 今日持仓预估盈亏总览 + 自选重点基金盘中走势
            - **服务状态**: 正常运行中
            
            > 感谢使用基金实时估值，祝您投资顺利！
        """;
        return sendWechatNotification(channel, title, content, "http://localhost:8080");
    }

    private boolean sendToWxPusher(String uid, String title, String content, String jumpUrl) {
        try {
            // 获取系统全局配置或默认 WxPusher appToken
            String appToken = "AT_DEFAULT_APP_TOKEN";
            var vipCfg = sysVipConfigService.getVipConfig();
            if (vipCfg != null && vipCfg.webhookUrl() != null && vipCfg.webhookUrl().startsWith("AT_")) {
                appToken = vipCfg.webhookUrl().trim();
            }

            Map<String, Object> reqBody = Map.of(
                    "appToken", appToken,
                    "content", content,
                    "summary", title,
                    "contentType", 3, // 3: Markdown
                    "uids", List.of(uid),
                    "url", jumpUrl != null ? jumpUrl : ""
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://wxpusher.zjiecode.com/api/send/message"))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reqBody)))
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("WxPusher 微信消息推送响应: code={}, body={}", resp.statusCode(), resp.body());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.warn("向 WxPusher UID [{}] 推送微信消息失败: {}", uid, e.getMessage());
            return false;
        }
    }

    private boolean sendToWebhook(String webhookUrl, String title, String content, String jumpUrl) {
        try {
            String jsonBody;
            if (webhookUrl.contains("dingtalk.com")) {
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "msgtype", "markdown",
                        "markdown", Map.of(
                                "title", title,
                                "text", "### " + title + "\n\n" + content + (jumpUrl != null ? "\n\n[点击前往查看详情](" + jumpUrl + ")" : "")
                        )
                ));
            } else if (webhookUrl.contains("weixin.qq.com") || webhookUrl.contains("work.weixin")) {
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "msgtype", "markdown",
                        "markdown", Map.of(
                                "content", "### " + title + "\n\n" + content + (jumpUrl != null ? "\n\n[点击前往查看详情](" + jumpUrl + ")" : "")
                        )
                ));
            } else if (webhookUrl.contains("feishu.cn") || webhookUrl.contains("larksuite.com")) {
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "msg_type", "text",
                        "content", Map.of(
                                "text", title + "\n\n" + content
                        )
                ));
            } else if (webhookUrl.contains("day.app")) {
                // Bark
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "title", title,
                        "body", content.replaceAll("[#*`>]", "").trim(),
                        "url", jumpUrl != null ? jumpUrl : ""
                ));
            } else if (webhookUrl.contains("ftqq.com")) {
                // Server酱
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "title", title,
                        "desp", content
                ));
            } else {
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "title", title,
                        "content", content,
                        "url", jumpUrl != null ? jumpUrl : ""
                ));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Webhook 推送完成: url={}, code={}", webhookUrl, resp.statusCode());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Webhook 推送失败: url={}, err={}", webhookUrl, e.getMessage());
            return false;
        }
    }
}
