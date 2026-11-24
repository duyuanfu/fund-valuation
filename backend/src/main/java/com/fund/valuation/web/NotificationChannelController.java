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
    public ResponseEntity<NotificationChannelView> updateChannel(HttpServletRequest request,
                                                                 @RequestBody NotificationChannelView req) {
        String username = currentUser(request);
        return ResponseEntity.ok(channelService.updateChannel(username, req));
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(HttpServletRequest request) {
        String username = currentUser(request);
        channelService.testEmail(username);
        return ResponseEntity.ok(Map.of("success", true, "message", "测试邮件已成功发送，请注意查收收件箱或垃圾箱！"));
    }

    @PostMapping("/test-wechat")
    public ResponseEntity<?> testWechat(HttpServletRequest request) {
        String username = currentUser(request);
        channelService.testWechat(username);
        return ResponseEntity.ok(Map.of("success", true, "message", "微信/Webhook 测试消息已成功发出！"));
    }

    private String currentUser(HttpServletRequest request) {
        String username = (String) request.getAttribute(JwtAuthFilter.ATTR_USERNAME);
        if (username == null) {
            throw new SecurityException("请先登录账号");
        }
        return username;
    }
}
