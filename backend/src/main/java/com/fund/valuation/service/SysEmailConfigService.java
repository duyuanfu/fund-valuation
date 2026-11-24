package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.SysEmailConfig;
import com.fund.valuation.dto.EmailConfigView;
import com.fund.valuation.mapper.SysEmailConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class SysEmailConfigService {

    private final SysEmailConfigMapper emailConfigMapper;
    private final AtomicReference<EmailConfigView> currentConfig = new AtomicReference<>();
    private final AtomicReference<JavaMailSenderImpl> mailSenderRef = new AtomicReference<>();

    private static final EmailConfigView DEFAULT_CONFIG = new EmailConfigView(
            "smtp.163.com",
            465,
            "",
            "",
            "基金实时估值系统",
            "",
            true
    );

    @Autowired
    public SysEmailConfigService(@Autowired(required = false) SysEmailConfigMapper emailConfigMapper) {
        this.emailConfigMapper = emailConfigMapper;
        this.currentConfig.set(DEFAULT_CONFIG);
    }

    @PostConstruct
    public void init() {
        if (emailConfigMapper != null) {
            try {
                SysEmailConfig persistent = emailConfigMapper.selectOne(new LambdaQueryWrapper<SysEmailConfig>()
                        .orderByDesc(SysEmailConfig::getId)
                        .last("limit 1"));
                if (persistent != null) {
                    EmailConfigView view = new EmailConfigView(
                            persistent.getSmtpHost() != null ? persistent.getSmtpHost() : DEFAULT_CONFIG.smtpHost(),
                            persistent.getSmtpPort() != null ? persistent.getSmtpPort() : DEFAULT_CONFIG.smtpPort(),
                            persistent.getSmtpUsername(),
                            persistent.getSmtpPassword(),
                            persistent.getSenderName() != null ? persistent.getSenderName() : DEFAULT_CONFIG.senderName(),
                            persistent.getSenderAddress(),
                            persistent.getSslEnabled() != null ? persistent.getSslEnabled() : DEFAULT_CONFIG.sslEnabled()
                    );
                    currentConfig.set(view);
                    rebuildMailSender(view);
                    log.info("loaded persistent sys_email_config from database, smtpHost={}", view.smtpHost());
                }
            } catch (Exception e) {
                log.debug("could not load email config from db: {}", e.getMessage());
            }
        }
    }

    public EmailConfigView getEmailConfig() {
        EmailConfigView cfg = currentConfig.get();
        return cfg != null ? cfg : DEFAULT_CONFIG;
    }

    public JavaMailSender getMailSender() {
        return mailSenderRef.get();
    }

    @Transactional
    public EmailConfigView updateConfig(EmailConfigView req) {
        EmailConfigView view = new EmailConfigView(
                req.smtpHost() != null && !req.smtpHost().isBlank() ? req.smtpHost().trim() : DEFAULT_CONFIG.smtpHost(),
                req.smtpPort() != null ? req.smtpPort() : DEFAULT_CONFIG.smtpPort(),
                req.smtpUsername() != null ? req.smtpUsername().trim() : "",
                req.smtpPassword() != null ? req.smtpPassword().trim() : "",
                req.senderName() != null && !req.senderName().isBlank() ? req.senderName().trim() : DEFAULT_CONFIG.senderName(),
                req.senderAddress() != null ? req.senderAddress().trim() : "",
                req.sslEnabled() != null ? req.sslEnabled() : true
        );

        if (emailConfigMapper != null) {
            try {
                emailConfigMapper.delete(null);
                SysEmailConfig entity = new SysEmailConfig();
                entity.setSmtpHost(view.smtpHost());
                entity.setSmtpPort(view.smtpPort());
                entity.setSmtpUsername(view.smtpUsername());
                entity.setSmtpPassword(view.smtpPassword());
                entity.setSenderName(view.senderName());
                entity.setSenderAddress(view.senderAddress());
                entity.setSslEnabled(view.sslEnabled());
                entity.setUpdatedAt(LocalDateTime.now());
                emailConfigMapper.insert(entity);
                log.info("persisted sys_email_config to database");
            } catch (Exception e) {
                log.warn("failed to persist sys_email_config to db: {}", e.getMessage());
            }
        }

        currentConfig.set(view);
        rebuildMailSender(view);
        return view;
    }

    private void rebuildMailSender(EmailConfigView view) {
        if (view.smtpHost() == null || view.smtpHost().isBlank()) {
            mailSenderRef.set(null);
            return;
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(view.smtpHost());
        sender.setPort(view.smtpPort() != null ? view.smtpPort() : 465);
        sender.setUsername(view.smtpUsername());
        sender.setPassword(view.smtpPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (Boolean.TRUE.equals(view.sslEnabled())) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.connectiontimeout", "8000");

        mailSenderRef.set(sender);
    }
}
