package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.SysVisionConfig;
import com.fund.valuation.dto.VisionConfigView;
import com.fund.valuation.mapper.SysVisionConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class SysVisionConfigService {

    private final SysVisionConfigMapper visionConfigMapper;
    private final AtomicReference<VisionConfigView> currentConfig = new AtomicReference<>();

    public static final VisionConfigView DEFAULT_CONFIG = new VisionConfigView(
            true,
            "https://open.bigmodel.cn/api/paas/v4",
            "fe28700189fe48688c7e7f0ea80bd1ee.YEkUKFIP3hassaov",
            "glm-4v-flash",
            20
    );

    @Autowired
    public SysVisionConfigService(@Autowired(required = false) SysVisionConfigMapper visionConfigMapper) {
        this.visionConfigMapper = visionConfigMapper;
        this.currentConfig.set(DEFAULT_CONFIG);
    }

    @PostConstruct
    public void init() {
        if (visionConfigMapper != null) {
            try {
                SysVisionConfig persistent = visionConfigMapper.selectOne(new LambdaQueryWrapper<SysVisionConfig>()
                        .orderByDesc(SysVisionConfig::getId)
                        .last("limit 1"));
                if (persistent != null) {
                    VisionConfigView view = new VisionConfigView(
                            persistent.getEnabled() != null ? persistent.getEnabled() : DEFAULT_CONFIG.enabled(),
                            persistent.getApiBaseUrl() != null && !persistent.getApiBaseUrl().isBlank()
                                    ? persistent.getApiBaseUrl().trim() : DEFAULT_CONFIG.apiBaseUrl(),
                            persistent.getApiKey() != null ? persistent.getApiKey().trim() : "",
                            persistent.getModelName() != null && !persistent.getModelName().isBlank()
                                    ? persistent.getModelName().trim() : DEFAULT_CONFIG.modelName(),
                            persistent.getDailyUserLimit() != null && persistent.getDailyUserLimit() > 0
                                    ? persistent.getDailyUserLimit() : DEFAULT_CONFIG.dailyUserLimit()
                    );
                    currentConfig.set(view);
                    log.info("loaded persistent sys_vision_config from database, modelName={}", view.modelName());
                } else {
                    // 数据库中无记录，写入初始默认配置
                    saveToDb(DEFAULT_CONFIG);
                }
            } catch (Exception e) {
                log.debug("could not load vision config from db: {}", e.getMessage());
            }
        }
    }

    public VisionConfigView getConfig() {
        VisionConfigView cfg = currentConfig.get();
        return cfg != null ? cfg : DEFAULT_CONFIG;
    }

    /**
     * 获取供前端展示的配置（API Key 脱敏）
     */
    public VisionConfigView getMaskedConfig() {
        VisionConfigView cfg = getConfig();
        String maskedKey = maskKey(cfg.apiKey());
        return new VisionConfigView(
                cfg.enabled(),
                cfg.apiBaseUrl(),
                maskedKey,
                cfg.modelName(),
                cfg.dailyUserLimit(),
                cfg.apiKey() != null && !cfg.apiKey().isBlank()
        );
    }

    @Transactional
    public VisionConfigView updateConfig(VisionConfigView req) {
        VisionConfigView existing = getConfig();
        String apiKeyToSave = req.apiKey();

        // 若前端传入的是脱敏掩码或为空，则保留已有 Key
        if (apiKeyToSave == null || apiKeyToSave.isBlank() || apiKeyToSave.contains("****")) {
            apiKeyToSave = existing.apiKey();
        } else {
            apiKeyToSave = apiKeyToSave.trim();
        }

        VisionConfigView updated = new VisionConfigView(
                req.enabled() != null ? req.enabled() : existing.enabled(),
                req.apiBaseUrl() != null && !req.apiBaseUrl().isBlank() ? req.apiBaseUrl().trim() : existing.apiBaseUrl(),
                apiKeyToSave,
                req.modelName() != null && !req.modelName().isBlank() ? req.modelName().trim() : existing.modelName(),
                req.dailyUserLimit() != null && req.dailyUserLimit() > 0 ? req.dailyUserLimit() : existing.dailyUserLimit()
        );

        saveToDb(updated);
        currentConfig.set(updated);
        log.info("updated sys_vision_config, enabled={}, modelName={}, limit={}",
                updated.enabled(), updated.modelName(), updated.dailyUserLimit());

        return getMaskedConfig();
    }

    private void saveToDb(VisionConfigView view) {
        if (visionConfigMapper != null) {
            try {
                visionConfigMapper.delete(null);
                SysVisionConfig entity = new SysVisionConfig();
                entity.setEnabled(view.enabled());
                entity.setApiBaseUrl(view.apiBaseUrl());
                entity.setApiKey(view.apiKey());
                entity.setModelName(view.modelName());
                entity.setDailyUserLimit(view.dailyUserLimit());
                entity.setUpdatedAt(LocalDateTime.now());
                visionConfigMapper.insert(entity);
            } catch (Exception e) {
                log.warn("failed to persist sys_vision_config: {}", e.getMessage());
            }
        }
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        if (key.length() <= 8) {
            return "******";
        }
        return key.substring(0, 4) + "******" + key.substring(key.length() - 4);
    }
}
