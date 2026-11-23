package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.domain.SysVipOrder;
import com.fund.valuation.domain.User;
import com.fund.valuation.dto.VipConfigView;
import com.fund.valuation.dto.VipOrderNotifyRequest;
import com.fund.valuation.dto.VipOrderView;
import com.fund.valuation.mapper.SysVipOrderMapper;
import com.fund.valuation.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipOrderService {

    private final SysVipOrderMapper vipOrderMapper;
    private final UserMapper userMapper;
    private final UserManagementService userManagementService;
    private final SysVipConfigService sysVipConfigService;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 用户提交 VIP 充值通知申请
     */
    @Transactional
    public VipOrderView submitOrder(String username, VipOrderNotifyRequest req) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (req.amount() == null || req.amount().doubleValue() <= 0) {
            throw new IllegalArgumentException("充值金额必须大于0");
        }

        SysVipOrder order = new SysVipOrder();
        order.setUsername(username);
        order.setPlanKey(req.planKey() != null ? req.planKey() : "quarter");
        order.setPlanName(req.planName() != null ? req.planName() : "VIP会员");
        order.setAmount(req.amount());
        order.setPayMethod(req.payMethod() != null ? req.payMethod() : "wechat");
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        vipOrderMapper.insert(order);

        VipOrderView view = toView(order);
        log.info("用户 [{}] 提交 VIP 充值通知: 订单ID={}, 套餐={}, 金额={}, 支付方式={}",
                username, order.getId(), order.getPlanName(), order.getAmount(), order.getPayMethod());

        // 1. 向所有在线管理员推送 SSE 实时事件
        notifyAdminsViaSse(view);

        // 2. 若配置了 Webhook 机器人，异步发起外部推送
        VipConfigView vipConfig = sysVipConfigService.getVipConfig();
        if (vipConfig != null && vipConfig.webhookUrl() != null && !vipConfig.webhookUrl().isBlank()) {
            sendWebhookNotificationAsync(vipConfig.webhookUrl().trim(), order);
        }

        return view;
    }

    /**
     * 管理员调阅 VIP 充值订单列表
     */
    public List<VipOrderView> listOrders() {
        List<SysVipOrder> orders = vipOrderMapper.selectList(new LambdaQueryWrapper<SysVipOrder>()
                .orderByDesc(SysVipOrder::getId));
        return orders.stream().map(this::toView).toList();
    }

    /**
     * 管理员一键审批开通 VIP
     */
    @Transactional
    public void approveOrder(Long orderId) {
        SysVipOrder order = vipOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("充值记录不存在");
        }
        if ("APPROVED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("该充值记录此前已被开通，请勿重复操作");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, order.getUsername()));
        if (user == null) {
            throw new IllegalArgumentException("目标用户 [" + order.getUsername() + "] 不存在");
        }

        // 根据充值套餐确定延长时间
        int days = switch (order.getPlanKey().toLowerCase()) {
            case "month" -> 30;
            case "year" -> 365;
            default -> 90; // quarter
        };

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime baseTime = (user.isVipEffective() && user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(now))
                ? user.getVipExpireAt()
                : now;
        LocalDateTime newExpireAt = baseTime.plusDays(days);

        // 更新用户 VIP 权限及有效期
        userManagementService.updateVip(order.getUsername(), true, newExpireAt);

        // 更新订单状态
        order.setStatus("APPROVED");
        order.setProcessedAt(now);
        vipOrderMapper.updateById(order);

        log.info("管理员已为用户 [{}] 成功开通 VIP (订单号: {}), 有效期延长至: {}",
                order.getUsername(), orderId, newExpireAt);

        // 向该用户推送 SSE 会员开通即时生效事件
        sseService.pushEvent(order.getUsername(), "vip_activated", Map.of(
                "success", true,
                "isVip", true,
                "expireAt", newExpireAt.toString(),
                "message", "恭喜您，管理员已为您审核开通 VIP 会员！有效期至 " + newExpireAt.toLocalDate()
        ));
    }

    private void notifyAdminsViaSse(VipOrderView view) {
        try {
            List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getRole, User.ROLE_ADMIN));
            for (User admin : admins) {
                sseService.pushEvent(admin.getUsername(), "vip_recharge", view);
            }
        } catch (Exception e) {
            log.warn("向管理员推送 VIP 充值 SSE 消息失败: {}", e.getMessage());
        }
    }

    private void sendWebhookNotificationAsync(String webhookUrl, SysVipOrder order) {
        CompletableFuture.runAsync(() -> {
            try {
                String payMethodName = "wechat".equalsIgnoreCase(order.getPayMethod()) ? "微信支付" : "支付宝";
                String timeStr = order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String title = "【基金估值】收到新的 VIP 充值申请";
                String text = String.format("### %s\n\n- **用户名**: `%s`\n- **充值方案**: %s\n- **支付金额**: ¥%s\n- **支付方式**: %s\n- **提交时间**: %s\n\n> 用户已点击“我已完成支付”，请管理员前往后台核对后一键开通。",
                        title, order.getUsername(), order.getPlanName(), order.getAmount(), payMethodName, timeStr);

                String jsonBody;
                if (webhookUrl.contains("dingtalk.com")) {
                    jsonBody = objectMapper.writeValueAsString(Map.of(
                            "msgtype", "markdown",
                            "markdown", Map.of(
                                    "title", title,
                                    "text", text
                            )
                    ));
                } else if (webhookUrl.contains("weixin.qq.com") || webhookUrl.contains("work.weixin")) {
                    jsonBody = objectMapper.writeValueAsString(Map.of(
                            "msgtype", "markdown",
                            "markdown", Map.of(
                                    "content", text
                            )
                    ));
                } else if (webhookUrl.contains("feishu.cn") || webhookUrl.contains("larksuite.com")) {
                    jsonBody = objectMapper.writeValueAsString(Map.of(
                            "msg_type", "text",
                            "content", Map.of(
                                    "text", title + "\n\n用户: " + order.getUsername() + "\n套餐: " + order.getPlanName() + "\n金额: ¥" + order.getAmount() + "\n方式: " + payMethodName + "\n时间: " + timeStr
                            )
                    ));
                } else if (webhookUrl.contains("day.app")) {
                    // Bark
                    jsonBody = objectMapper.writeValueAsString(Map.of(
                            "title", title,
                            "body", "用户 " + order.getUsername() + " 充值 " + order.getPlanName() + " (¥" + order.getAmount() + ")"
                    ));
                } else {
                    // 通用 Webhook
                    jsonBody = objectMapper.writeValueAsString(Map.of(
                            "title", title,
                            "username", order.getUsername(),
                            "planName", order.getPlanName(),
                            "amount", order.getAmount(),
                            "payMethod", payMethodName,
                            "createdAt", timeStr,
                            "content", text
                    ));
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .timeout(Duration.ofSeconds(6))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("VIP 充值 Webhook 推送完成, 响应码: {}, 返回: {}", response.statusCode(), response.body());
            } catch (Exception e) {
                log.warn("VIP 充值 Webhook 推送异常: {}", e.getMessage());
            }
        });
    }

    private VipOrderView toView(SysVipOrder o) {
        return new VipOrderView(
                o.getId(),
                o.getUsername(),
                o.getPlanKey(),
                o.getPlanName(),
                o.getAmount(),
                o.getPayMethod(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getProcessedAt()
        );
    }
}
