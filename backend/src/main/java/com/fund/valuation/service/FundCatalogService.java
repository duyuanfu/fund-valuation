package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.client.FundBasicInfo;
import com.fund.valuation.client.TianTianFundClient;
import com.fund.valuation.common.FundType;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.mapper.FundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 基金元数据采集与维护(类型识别、净值更新、跟踪指数)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundCatalogService {

    private final FundMapper fundMapper;
    private final TianTianFundClient fundClient;
    private final FundTypeClassifier classifier;
    private final TrackIndexResolver indexResolver;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("crawlerExecutor")
    private java.util.concurrent.Executor crawlerExecutor;

    /** 正在进行异步轻量净值刷新的基金代码集合，防止高频重复向线程池提交相同任务 */
    private final java.util.Set<String> refreshingCodes = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 确保基金存在:不存在则采集元数据入库;
     * 若已存在但净值日期落后于最近交易日(如因系统离线或跨交易日未准时20:30刷新),自动执行一次单只轻量增量更新。
     */
    public Fund ensureFund(String code) {
        Fund fund = fundMapper.selectById(code);
        if (fund != null) {
            return checkAndUpdateFund(fund);
        }
        FundBasicInfo info = fundClient.fetchBasicInfo(code);
        if (info == null || info.code() == null || info.name() == null || info.name().isBlank()) {
            throw new IllegalArgumentException("基金代码无效或数据源无此基金: " + code);
        }
        FundType type = classifier.classify(info.typeRaw(), info.name());
        String trackIndex = (type == FundType.INDEX || type == FundType.ENHANCED)
                ? indexResolver.resolve(code, info.name()) : null;

        fund = new Fund();
        fund.setCode(info.code());
        fund.setName(info.name());
        fund.setType(type.getCode());
        fund.setTypeRaw(info.typeRaw());
        fund.setTrackIndex(trackIndex);
        fund.setPrevNav(info.prevNav());
        fund.setNavDate(info.navDate());
        fund.setFreshFlag(type == FundType.OTHER ? "未识别类型" : null);
        fund.setUpdatedAt(LocalDateTime.now());
        fundMapper.insert(fund);
        return fund;
    }

    public boolean isNavOutdated(java.time.LocalDate navDate) {
        if (navDate == null) {
            return true;
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        // 只要已有净值日期早于今天，均允许自检，以天天基金远端官方实际公布日期为准，避免受节假日硬编码误判阻断
        return navDate.isBefore(today);
    }

    public void refreshSingleFund(Fund fund) {
        try {
            FundBasicInfo info = fundClient.fetchBasicInfo(fund.getCode());
            if (info != null && info.prevNav() != null && info.navDate() != null) {
                // 当远端实际公布的净值日期晚于数据库现有日期时，执行更新覆盖
                if (fund.getNavDate() == null || info.navDate().isAfter(fund.getNavDate())) {
                    java.time.LocalDate oldDate = fund.getNavDate();
                    fund.setName(info.name());
                    fund.setPrevNav(info.prevNav());
                    fund.setNavDate(info.navDate());
                    FundType type = classifier.classify(info.typeRaw(), info.name());
                    if (type == FundType.INDEX || type == FundType.ENHANCED) {
                        String newIndex = indexResolver.resolve(fund.getCode(), info.name());
                        if (newIndex != null) {
                            fund.setTrackIndex(newIndex);
                        }
                    }
                    fund.setUpdatedAt(LocalDateTime.now());
                    fundMapper.updateById(fund);
                    log.info("fund {} navDate updated: {} -> {}", fund.getCode(), oldDate, info.navDate());
                }
            }
        } catch (Exception e) {
            log.debug("refresh single fund {} nav failed: {}", fund.getCode(), e.getMessage());
        }
    }

    /**
     * 刷新所有基金元数据(名称/类型/昨日净值/跟踪指数)。
     */
    public int refreshAll() {
        List<Fund> funds = fundMapper.selectList(null);
        int updated = 0;
        for (Fund fund : funds) {
            try {
                FundBasicInfo info = fundClient.fetchBasicInfo(fund.getCode());
                if (info == null) {
                    continue;
                }
                FundType type = classifier.classify(info.typeRaw(), info.name());
                fund.setName(info.name());
                fund.setType(type.getCode());
                fund.setTypeRaw(info.typeRaw());
                if (info.prevNav() != null) {
                    fund.setPrevNav(info.prevNav());
                    fund.setNavDate(info.navDate());
                }
                if (fund.getTrackIndex() == null
                        && (type == FundType.INDEX || type == FundType.ENHANCED)) {
                    fund.setTrackIndex(indexResolver.resolve(fund.getCode(), info.name()));
                }
                fund.setUpdatedAt(LocalDateTime.now());
                fundMapper.updateById(fund);
                updated++;
            } catch (Exception e) {
                log.warn("refresh fund {} failed: {}", fund.getCode(), e.getMessage());
            }
        }
        return updated;
    }

    public Fund get(String code) {
        return checkAndUpdateFund(fundMapper.selectById(code));
    }

    private Fund checkAndUpdateFund(Fund fund) {
        if (fund == null) {
            return null;
        }
        boolean updated = false;
        if (fund.getTrackIndex() == null || "1.990001".equals(fund.getTrackIndex())) {
            String newIndex = indexResolver.resolve(fund.getCode(), fund.getName());
            if (newIndex != null && !newIndex.equals(fund.getTrackIndex())) {
                fund.setTrackIndex(newIndex);
                updated = true;
            }
        }
        if (updated) {
            fundMapper.updateById(fund);
        }
        triggerAsyncRefreshIfNeeded(fund);
        return fund;
    }

    private void triggerAsyncRefreshIfNeeded(Fund fund) {
        if (fund == null || !isNavOutdated(fund.getNavDate())) {
            return;
        }
        String code = fund.getCode();
        if (refreshingCodes.add(code)) {
            Runnable task = () -> {
                try {
                    refreshSingleFund(fund);
                } finally {
                    refreshingCodes.remove(code);
                }
            };
            if (crawlerExecutor != null) {
                java.util.concurrent.CompletableFuture.runAsync(task, crawlerExecutor);
            } else {
                java.util.concurrent.CompletableFuture.runAsync(task);
            }
        }
    }
}