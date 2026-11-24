package com.fund.valuation.web;

import com.fund.valuation.dto.EmailConfigView;
import com.fund.valuation.service.EmailSenderService;
import com.fund.valuation.service.SysEmailConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/email-config")
@RequiredArgsConstructor
public class AdminEmailController {

    private final SysEmailConfigService sysEmailConfigService;
    private final EmailSenderService emailSenderService;

    @GetMapping
    public ResponseEntity<EmailConfigView> getEmailConfig() {
        return ResponseEntity.ok(sysEmailConfigService.getEmailConfig());
    }

    @PostMapping
    public ResponseEntity<EmailConfigView> updateEmailConfig(@RequestBody EmailConfigView req) {
        return ResponseEntity.ok(sysEmailConfigService.updateConfig(req));
    }

    @PostMapping("/test")
    public ResponseEntity<?> testSend(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入测试收件人邮箱"));
        }
        try {
            emailSenderService.sendTestEmail(to.trim());
            return ResponseEntity.ok(Map.of("success", true, "message", "测试邮件发送成功！请检查收件箱或垃圾邮件。"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "邮件发送异常"));
        }
    }
}
