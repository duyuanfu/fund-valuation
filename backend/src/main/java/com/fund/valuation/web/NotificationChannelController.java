package com.fund.valuation.web;

import com.fund.valuation.config.JwtAuthFilter;
import com.fund.valuation.dto.NotificationChannelView;
import com.fund.valuation.service.UserNotificationChannelService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user/notification-channel")
@RequiredArgsConstructor
public class NotificationChannelController {

    private final UserNotificationChannelService channelService;

    @GetMapping
    public ResponseEntity<NotificationChannelView> getChannel(HttpServletRequest request) {
        String username = currentUser(request);
        return ResponseEntity.ok(channelService.getChannel(username));
    }

    @PostMapping
    public ResponseEntity<?> updateChannel(HttpServletRequest request,
                                           @RequestBody NotificationChannelView req) {
        String username = currentUser(request);
        try {
            return ResponseEntity.ok(channelService.updateChannel(username, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(HttpServletRequest request,
                                       @RequestBody(required = false) Map<String, String> body) {
        String username = currentUser(request);
        String email = body != null ? body.get("email") : null;
        try {
            channelService.testEmail(username, email);
            return ResponseEntity.ok(Map.of("success", true, "message", "测试邮件已成功发送，请注意查收收件箱或垃圾箱！"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "邮件发送异常"));
        }
    }

    @PostMapping("/test-wechat")
    public ResponseEntity<?> testWechat(HttpServletRequest request,
                                        @RequestBody(required = false) Map<String, String> body) {
        String username = currentUser(request);
        String tokenOrUrl = body != null ? body.get("wechatTarget") : null;
        String channelType = body != null ? body.get("wechatChannelType") : null;
        try {
            channelService.testWechat(username, tokenOrUrl, channelType);
            return ResponseEntity.ok(Map.of("success", true, "message", "微信/Webhook 测试消息已成功发出，请在微信中查收！"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "微信/Webhook 推送异常"));
        }
    }

    private String currentUser(HttpServletRequest request) {
        String username = (String) request.getAttribute(JwtAuthFilter.ATTR_USERNAME);
        if (username == null) {
            throw new SecurityException("请先登录账号");
        }
        return username;
    }
}
