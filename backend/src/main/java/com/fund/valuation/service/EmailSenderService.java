package com.fund.valuation.service;

import com.fund.valuation.dto.EmailConfigView;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderService {

    private final SysEmailConfigService sysEmailConfigService;

    /**
     * 发送 HTML 格式富文本邮件
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("收件人邮箱不能为空");
        }

        EmailConfigView cfg = sysEmailConfigService.getEmailConfig();
        JavaMailSender sender = sysEmailConfigService.getMailSender();
        if (sender == null || cfg.smtpUsername() == null || cfg.smtpUsername().isBlank()) {
            throw new IllegalStateException("系统尚未配置 SMTP 发信服务，请管理员在管理后台先配置发信邮箱");
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = (cfg.senderAddress() != null && !cfg.senderAddress().isBlank())
                    ? cfg.senderAddress().trim()
                    : (cfg.smtpUsername() != null ? cfg.smtpUsername().trim() : "");
            String senderName = (cfg.senderName() != null && !cfg.senderName().isBlank())
                    ? cfg.senderName().trim()
                    : "基金实时估值系统";

            helper.setFrom(new InternetAddress(fromAddress, senderName, "UTF-8"));
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            sender.send(message);
            log.info("成功向 [{}] 发送邮件: 主题=[{}]", to, subject);
        } catch (Exception e) {
            log.warn("向 [{}] 发送邮件失败: {}", to, e.getMessage());
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送测试邮件以验证发信配置
     */
    public void sendTestEmail(String to) {
        String subject = "【基金实时估值系统】SMTP 发信配置连通性测试";
        String htmlBody = """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; borderRadius: 12px; background: #ffffff;">
                <div style="display: flex; align-items: center; margin-bottom: 16px;">
                    <div style="font-size: 20px; font-weight: 700; color: #0f172a;">🎉 恭喜！邮件服务配置成功</div>
                </div>
                <p style="color: #475569; font-size: 14px; line-height: 1.6;">
                    这是一封来自<strong>基金实时估值系统</strong>的连通性测试邮件。当您收到此邮件时，说明系统的 SMTP 发信服务已正确连通并就绪！
                </p>
                <div style="background: #f8fafc; border-left: 4px solid #2563eb; padding: 12px 16px; margin: 18px 0; border-radius: 4px; font-size: 13px; color: #334155;">
                    每个交易日下午 14:30（收盘黄金决策半小时），系统将准时向绑定的邮箱推送您的个性化持仓收益与自选基金预估播报。
                </div>
                <div style="margin-top: 24px; font-size: 12px; color: #94a3b8; border-top: 1px solid #f1f5f9; padding-top: 12px;">
                    此邮件由系统自动发送，请勿直接回复。
                </div>
            </div>
        """;
        sendHtmlEmail(to, subject, htmlBody);
    }
}
