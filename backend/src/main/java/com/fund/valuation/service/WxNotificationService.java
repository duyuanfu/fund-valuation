package com.fund.valuation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.domain.UserNotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxNotificationService {

    private final ObjectMapper objectMapper;

    @Value("${fund.website-url:https://fund.trythis.pw/}")
    private String defaultWebsiteUrl = "https://fund.trythis.pw/";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    /**
     * 向用户的移动端通道 (Server酱微信) 发送估值提醒
     */
    public boolean sendWechatNotification(UserNotificationChannel channel, String title, String content, String jumpUrl) {
        if (channel == null || Boolean.FALSE.equals(channel.getWechatEnabled())) {
            return false;
        }

        // Server酱 (个人微信公众号推送)
        String serverchanKey = channel.getServerchanKey() != null && !channel.getServerchanKey().isBlank()
                ? channel.getServerchanKey().trim()
                : (channel.getWechatWebhookUrl() != null ? channel.getWechatWebhookUrl().trim() : "");
        return sendToServerChan(serverchanKey, title, content, jumpUrl);
    }

    /**
     * 发送测试消息验证通道连通性
     */
    public boolean sendTestWechat(UserNotificationChannel channel) {
        String channelName = "Server酱 (微信公众号推送)";
        String timeStr = channel.getRemindTime() != null ? channel.getRemindTime() : "14:30";

        String title = "【基金盘中估值】推送测试成功";
        String content = String.format("""
            ### 🎉 盘中提醒通知绑定成功
            
            这是一条来自**基金实时估值系统**的测试消息！
            
            - **推送方式**: %s
            - **设定时间**: 交易日每日 %s 准时送达
            - **推送内容**: 今日持仓预估盈亏总览 + 自选重点基金盘中涨跌幅
            - **连通状态**: 连通正常已就绪
            
            > 提示：系统将在交易日指定时间为您准时推送最新估值播报，祝您投资顺利！
        """, channelName, timeStr);

        return sendWechatNotification(channel, title, content, defaultWebsiteUrl);
    }

    /**
     * 发送消息至 Server酱 (微信直接接收)
     */
    public boolean sendToServerChan(String keyOrUrl, String title, String content, String jumpUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            throw new IllegalArgumentException("请填写 Server酱 SendKey（以 SCT 开头）");
        }
        String clean = keyOrUrl.trim();
        String url;
        if (clean.startsWith("http")) {
            url = clean;
        } else {
            url = "https://sctapi.ftqq.com/" + clean + ".send";
        }

        String targetUrl = (jumpUrl != null && !jumpUrl.isBlank()) ? jumpUrl.trim() : defaultWebsiteUrl;

        try {
            Map<String, Object> reqBody = Map.of(
                    "title", title,
                    "desp", content + (targetUrl != null && !targetUrl.isBlank() ? "\n\n[点击前往查看估值详情](" + targetUrl + ")" : "")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reqBody)))
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Server酱微信推送响应: code={}, body={}", resp.statusCode(), resp.body());

            if (resp.statusCode() != 200) {
                throw new RuntimeException("Server酱接口返回 HTTP " + resp.statusCode() + ": " + resp.body());
            }

            JsonNode root = objectMapper.readTree(resp.body());
            int code = root.path("code").asInt(0);
            if (code != 0 && code != 200) {
                String msg = root.path("message").asText(root.path("info").asText("发送受阻"));
                throw new RuntimeException("Server酱拒绝发送: " + msg);
            }
            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("向 Server酱 [{}] 推送失败: {}", clean, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
