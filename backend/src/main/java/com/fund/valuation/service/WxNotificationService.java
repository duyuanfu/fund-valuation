package com.fund.valuation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.domain.UserNotificationChannel;
import com.fund.valuation.dto.EmailConfigView;
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
    private final SysEmailConfigService sysEmailConfigService;

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

        String type = channel.getWechatChannelType() != null ? channel.getWechatChannelType().trim().toLowerCase() : "pushplus";

        // 1. 首选: PushPlus 微信免App直推
        if ("pushplus".equals(type)) {
            String token = (channel.getWechatWebhookUrl() != null && !channel.getWechatWebhookUrl().isBlank())
                    ? channel.getWechatWebhookUrl().trim()
                    : (channel.getWechatUid() != null ? channel.getWechatUid().trim() : "");
            return sendToPushPlus(token, title, content, jumpUrl);
        }

        // 2. 自定义 Webhook 地址 (企微/钉钉/飞书/Bark/Server酱)
        if ("webhook".equals(type) && channel.getWechatWebhookUrl() != null && !channel.getWechatWebhookUrl().isBlank()) {
            return sendToWebhook(channel.getWechatWebhookUrl().trim(), title, content, jumpUrl);
        }

        // 3. WxPusher 扫码关注 (需配合官方 App)
        if ("wxpusher".equals(type) && channel.getWechatUid() != null && !channel.getWechatUid().isBlank()) {
            return sendToWxPusher(channel, title, content, jumpUrl);
        }

        // 兼容回退逻辑: 根据填写的凭证特征自动路由
        if (channel.getWechatWebhookUrl() != null && !channel.getWechatWebhookUrl().isBlank()) {
            String val = channel.getWechatWebhookUrl().trim();
            if (val.length() == 32 || val.contains("pushplus")) {
                return sendToPushPlus(val, title, content, jumpUrl);
            }
            return sendToWebhook(val, title, content, jumpUrl);
        }
        if (channel.getWechatUid() != null && !channel.getWechatUid().isBlank()) {
            String uid = channel.getWechatUid().trim();
            if (uid.startsWith("UID_")) {
                return sendToWxPusher(channel, title, content, jumpUrl);
            } else {
                return sendToPushPlus(uid, title, content, jumpUrl);
            }
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
            
            这是一条来自**基金实时估值系统**的测试消息！
            
            - **推送通道**: PushPlus 微信公众号推送
            - **推送时间**: 交易日每日 14:30 准时送达（收盘前半小时决策点）
            - **推送内容**: 今日持仓预估盈亏总览 + 自选重点基金盘中涨跌幅
            - **运行状态**: 服务已正常连通
            
            > 提示：今日 14:30 系统将为您准时推送最新估值播报，祝您投资顺利！
        """;
        return sendWechatNotification(channel, title, content, "http://localhost:8080");
    }

    /**
     * 发送消息至 PushPlus (推送加 · 微信公众号免App原生直推)
     */
    public boolean sendToPushPlus(String token, String title, String content, String jumpUrl) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("请填写您的 PushPlus Token（32位字符，在 pushplus.plus 首页复制）");
        }
        String cleanToken = token.trim();
        if (cleanToken.contains("token=")) {
            cleanToken = cleanToken.substring(cleanToken.indexOf("token=") + 6);
            if (cleanToken.contains("&")) {
                cleanToken = cleanToken.substring(0, cleanToken.indexOf("&"));
            }
        } else if (cleanToken.contains("pushplus.plus/send/")) {
            cleanToken = cleanToken.substring(cleanToken.indexOf("/send/") + 6);
            if (cleanToken.contains("?")) {
                cleanToken = cleanToken.substring(0, cleanToken.indexOf("?"));
            }
        }

        try {
            Map<String, Object> reqBody = Map.of(
                    "token", cleanToken,
                    "title", title,
                    "content", content,
                    "template", "markdown",
                    "channel", "wechat"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.pushplus.plus/send"))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reqBody)))
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("PushPlus 微信消息推送响应: code={}, body={}", resp.statusCode(), resp.body());

            if (resp.statusCode() != 200) {
                throw new RuntimeException("PushPlus 接口返回 HTTP " + resp.statusCode() + ": " + resp.body());
            }

            JsonNode root = objectMapper.readTree(resp.body());
            int code = root.path("code").asInt(-1);
            if (code != 200) {
                String msg = root.path("msg").asText("未知错误");
                log.warn("PushPlus 消息推送被拒: code={}, msg={}", code, msg);
                throw new RuntimeException("PushPlus 推送失败: " + msg + " (请核对 Token 是否正确，并确认微信已关注「pushplus推送加」公众号)");
            }

            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("向 PushPlus [{}] 推送失败: {}", cleanToken, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean sendToWxPusher(UserNotificationChannel channel, String title, String content, String jumpUrl) {
        String uid = channel.getWechatUid() != null ? channel.getWechatUid().trim() : "";
        if (uid.isBlank()) {
            throw new IllegalArgumentException("请填写您的 WxPusher UID（以 UID_ 开头）");
        }

        // 1. 确定使用的 AppToken (用户个人设置 > 系统全局邮件与提醒配置 > VIP全局配置)
        String appToken = null;
        if (channel.getWechatAppToken() != null && !channel.getWechatAppToken().isBlank()) {
            appToken = channel.getWechatAppToken().trim();
        } else {
            EmailConfigView emailCfg = sysEmailConfigService.getEmailConfig();
            if (emailCfg != null && emailCfg.wxpusherAppToken() != null && !emailCfg.wxpusherAppToken().isBlank()) {
                appToken = emailCfg.wxpusherAppToken().trim();
            } else {
                var vipCfg = sysVipConfigService.getVipConfig();
                if (vipCfg != null && vipCfg.webhookUrl() != null && vipCfg.webhookUrl().startsWith("AT_")) {
                    appToken = vipCfg.webhookUrl().trim();
                }
            }
        }

        if (appToken == null || appToken.isBlank() || "AT_DEFAULT_APP_TOKEN".equals(appToken)) {
            throw new IllegalArgumentException("未配置有效 WxPusher AppToken！WxPusher 机制要求必须由应用持有者提供 AppToken（以 AT_ 开头）。请在管理员后台配置系统 WxPusher Token，或在下方输入您个人创建的应用 AppToken。");
        }

        try {
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

            if (resp.statusCode() != 200) {
                throw new RuntimeException("WxPusher 接口返回 HTTP " + resp.statusCode() + ": " + resp.body());
            }

            JsonNode root = objectMapper.readTree(resp.body());
            int code = root.path("code").asInt(-1);
            if (code != 1000) {
                String msg = root.path("msg").asText("未知错误");
                log.warn("WxPusher 消息被拒收: code={}, msg={}", code, msg);
                throw new RuntimeException("WxPusher 拒绝发送: " + msg + " (code=" + code + ")");
            }

            JsonNode dataNode = root.path("data");
            if (dataNode.isArray() && !dataNode.isEmpty()) {
                JsonNode first = dataNode.get(0);
                int targetCode = first.path("code").asInt(1000);
                if (targetCode != 1000) {
                    String status = first.path("status").asText("未关注应用");
                    log.warn("WxPusher 目标用户推送失败: code={}, status={}", targetCode, status);
                    throw new RuntimeException("微信推送失败: " + status + "（WxPusher 规则：用户微信必须先扫码关注该 AppToken 对应的应用二维码，否则无法接收消息）");
                }
            }

            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("向 WxPusher UID [{}] 推送微信消息失败: {}", uid, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean sendToWebhook(String webhookUrl, String title, String content, String jumpUrl) {
        String url = webhookUrl.trim();
        // 智能兼容 Server酱 SendKey (如直接输入 SCTxxxx)
        if (url.startsWith("SCT") && !url.contains("/")) {
            url = "https://sctapi.ftqq.com/" + url + ".send";
        }
        // 智能兼容 PushPlus Token (如直接输入 32位 hex token)
        else if (url.length() == 32 && !url.contains("/") && !url.contains(":")) {
            url = "http://www.pushplus.plus/send?token=" + url;
        }

        try {
            String jsonBody;
            if (url.contains("pushplus.plus")) {
                // PushPlus 微信个人推送
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "title", title,
                        "content", content,
                        "template", "markdown"
                ));
            } else if (url.contains("dingtalk.com")) {
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "msgtype", "markdown",
                        "markdown", Map.of(
                                "title", title,
                                "text", "### " + title + "\n\n" + content + (jumpUrl != null ? "\n\n[点击前往查看详情](" + jumpUrl + ")" : "")
                        )
                ));
            } else if (url.contains("weixin.qq.com") || url.contains("work.weixin")) {
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "msgtype", "markdown",
                        "markdown", Map.of(
                                "content", "### " + title + "\n\n" + content + (jumpUrl != null ? "\n\n[点击前往查看详情](" + jumpUrl + ")" : "")
                        )
                ));
            } else if (url.contains("feishu.cn") || url.contains("larksuite.com")) {
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "msg_type", "text",
                        "content", Map.of(
                                "text", title + "\n\n" + content
                        )
                ));
            } else if (url.contains("day.app")) {
                // Bark
                jsonBody = objectMapper.writeValueAsString(Map.of(
                        "title", title,
                        "body", content.replaceAll("[#*`>]", "").trim(),
                        "url", jumpUrl != null ? jumpUrl : ""
                ));
            } else if (url.contains("ftqq.com")) {
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
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Webhook 推送完成: url={}, code={}", url, resp.statusCode());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("Webhook 目标服务器返回 HTTP " + resp.statusCode() + ": " + resp.body());
            }
            return true;
        } catch (Exception e) {
            log.warn("Webhook 推送失败: url={}, err={}", url, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
