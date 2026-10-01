package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.SysVipConfig;
import com.fund.valuation.dto.VipConfigView;
import com.fund.valuation.mapper.SysVipConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class SysVipConfigService {

    private final SysVipConfigMapper vipConfigMapper;
    private final AtomicReference<VipConfigView> currentConfig = new AtomicReference<>();

    private static final VipConfigView DEFAULT_CONFIG = new VipConfigView(
            new BigDecimal("2.90"),
            new BigDecimal("6.90"),
            new BigDecimal("8.70"),
            new BigDecimal("19.90"),
            new BigDecimal("34.80"),
            "/wechat-pay.png",
            "/alipay.jpg",
            "管理员",
            "付款请务必备注用户名",
            true
    );

    @Autowired
    public SysVipConfigService(@Autowired(required = false) SysVipConfigMapper vipConfigMapper) {
        this.vipConfigMapper = vipConfigMapper;
        this.currentConfig.set(DEFAULT_CONFIG);
    }

    @PostConstruct
    public void init() {
        if (vipConfigMapper != null) {
            try {
                SysVipConfig persistent = vipConfigMapper.selectOne(new LambdaQueryWrapper<SysVipConfig>()
                        .orderByDesc(SysVipConfig::getId)
                        .last("limit 1"));
                if (persistent != null) {
                    currentConfig.set(new VipConfigView(
                            persistent.getMonthlyPrice() != null ? persistent.getMonthlyPrice() : DEFAULT_CONFIG.monthlyPrice(),
                            persistent.getQuarterlyPrice() != null ? persistent.getQuarterlyPrice() : DEFAULT_CONFIG.quarterlyPrice(),
                            persistent.getQuarterlyOrigPrice() != null ? persistent.getQuarterlyOrigPrice() : DEFAULT_CONFIG.quarterlyOrigPrice(),
                            persistent.getYearlyPrice() != null ? persistent.getYearlyPrice() : DEFAULT_CONFIG.yearlyPrice(),
                            persistent.getYearlyOrigPrice() != null ? persistent.getYearlyOrigPrice() : DEFAULT_CONFIG.yearlyOrigPrice(),
                            persistent.getWechatQrUrl(),
                            persistent.getAlipayQrUrl(),
                            persistent.getPayeeName() != null ? persistent.getPayeeName() : DEFAULT_CONFIG.payeeName(),
                            persistent.getPaymentTip() != null ? persistent.getPaymentTip() : DEFAULT_CONFIG.paymentTip(),
                            persistent.getRequireApproval() != null ? persistent.getRequireApproval() : DEFAULT_CONFIG.requireApproval()
                    ));
                    log.info("loaded persistent sys_vip_config from database");
                }
            } catch (Exception e) {
                log.debug("could not load vip config from db: {}", e.getMessage());
            }
        }
    }

    public VipConfigView getVipConfig() {
        VipConfigView cfg = currentConfig.get();
        return cfg != null ? cfg : DEFAULT_CONFIG;
    }

    @Transactional
    public VipConfigView updateVipConfig(VipConfigView req) {
        VipConfigView view = new VipConfigView(
                req.monthlyPrice() != null ? req.monthlyPrice() : DEFAULT_CONFIG.monthlyPrice(),
                req.quarterlyPrice() != null ? req.quarterlyPrice() : DEFAULT_CONFIG.quarterlyPrice(),
                req.quarterlyOrigPrice() != null ? req.quarterlyOrigPrice() : DEFAULT_CONFIG.quarterlyOrigPrice(),
                req.yearlyPrice() != null ? req.yearlyPrice() : DEFAULT_CONFIG.yearlyPrice(),
                req.yearlyOrigPrice() != null ? req.yearlyOrigPrice() : DEFAULT_CONFIG.yearlyOrigPrice(),
                req.wechatQrUrl(),
                req.alipayQrUrl(),
                req.payeeName() != null && !req.payeeName().isBlank() ? req.payeeName() : DEFAULT_CONFIG.payeeName(),
                req.paymentTip() != null && !req.paymentTip().isBlank() ? req.paymentTip() : DEFAULT_CONFIG.paymentTip(),
                req.requireApproval() != null ? req.requireApproval() : getVipConfig().requireApproval()
        );

        if (vipConfigMapper != null) {
            try {
                vipConfigMapper.delete(null);
                SysVipConfig entity = new SysVipConfig();
                entity.setMonthlyPrice(view.monthlyPrice());
                entity.setQuarterlyPrice(view.quarterlyPrice());
                entity.setQuarterlyOrigPrice(view.quarterlyOrigPrice());
                entity.setYearlyPrice(view.yearlyPrice());
                entity.setYearlyOrigPrice(view.yearlyOrigPrice());
                entity.setWechatQrUrl(view.wechatQrUrl());
                entity.setAlipayQrUrl(view.alipayQrUrl());
                entity.setPayeeName(view.payeeName());
                entity.setPaymentTip(view.paymentTip());
                entity.setRequireApproval(view.requireApproval());
                entity.setUpdatedAt(LocalDateTime.now());
                vipConfigMapper.insert(entity);
                log.info("persisted sys_vip_config to database");
            } catch (Exception e) {
                log.warn("failed to persist sys_vip_config to db: {}", e.getMessage());
            }
        }

        currentConfig.set(view);
        return view;
    }

    public boolean isRequireApproval() {
        VipConfigView cfg = getVipConfig();
        return cfg.requireApproval() == null || Boolean.TRUE.equals(cfg.requireApproval());
    }

    @Transactional
    public void updateRequireApproval(boolean requireApproval) {
        VipConfigView current = getVipConfig();
        VipConfigView updated = new VipConfigView(
                current.monthlyPrice(),
                current.quarterlyPrice(),
                current.quarterlyOrigPrice(),
                current.yearlyPrice(),
                current.yearlyOrigPrice(),
                current.wechatQrUrl(),
                current.alipayQrUrl(),
                current.payeeName(),
                current.paymentTip(),
                requireApproval
        );
        updateVipConfig(updated);
        log.info("user registration requireApproval updated to {}", requireApproval);
    }
}
