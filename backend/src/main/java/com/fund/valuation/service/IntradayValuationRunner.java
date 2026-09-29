package com.fund.valuation.service;

import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.UserFund;
import com.fund.valuation.mapper.FundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 盘中估值流水线:拉行情 -> 计算 -> 写历史 -> SSE 推送。
 * 由定时任务驱动,也可由 admin 接口手动触发(force=true 跳过交易时段判定)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntradayValuationRunner {

    private final QuoteService quoteService;
    private final ValuationEngine valuationEngine;
    private final EstimateHistoryService historyService;
    private final WatchlistService watchlistService;
    private final SseService sseService;
    private final FundMapper fundMapper;
    private final com.fund.valuation.mapper.QuoteCacheMapper quoteCacheMapper;

    /** 互斥锁:防止定时任务与 admin 手动触发并发执行,避免历史重复写入。 */
    private final Object runLock = new Object();
    private volatile LocalDateTime lastRunTime;

    public LocalDateTime getLastRunTime() {
        return lastRunTime;
    }

    /**
     * 冷启动自愈: 新服部署上线或盘后新增自选时,若数据库中从未采集过历史快照,自动执行一次初次采样。
     */
    @jakarta.annotation.PostConstruct
    public void warmupIfEmpty() {
        try {
            Long count = quoteCacheMapper.selectCount(null);
            if (count == null || count == 0) {
                log.info("quote cache empty on startup, executing initial startup snapshot warmup");
                forceRun();
            }
        } catch (Exception e) {
            log.warn("warmup valuation snapshot failed: {}", e.getMessage());
        }
    }

    /**
     * 交易时段内执行。
     */
    public void run() {
        if (!TradingCalendar.isTradingTime(LocalDateTime.now())) {
            return;
        }
        runInternal();
    }

    /**
     * 强制执行(跳过交易时段判定),供运维/冒烟使用。
     */
    public void forceRun() {
        runInternal();
    }

    private void runInternal() {
        synchronized (runLock) {
            doRun();
        }
    }

    private void doRun() {
        lastRunTime = LocalDateTime.now();
        List<UserFund> userFunds = watchlistService.allUserFunds();
        List<String> fundCodes = userFunds.stream().map(UserFund::getFundCode).distinct().toList();
        if (fundCodes.isEmpty()) {
            return;
        }
        List<Fund> funds = fundMapper.selectBatchIds(fundCodes);

        Set<String> required = new HashSet<>();
        for (Fund f : funds) {
            required.addAll(valuationEngine.collectRequiredSecids(f));
        }
        if (required.isEmpty()) {
            return;
        }
        quoteService.refreshQuotes(required);

        Map<String, EstimateResult> byFund = new LinkedHashMap<>();
        for (Fund f : funds) {
            EstimateResult r = valuationEngine.estimate(f);
            if (r != null) {
                byFund.put(f.getCode(), r);
            }
        }
        historyService.recordAll(new ArrayList<>(byFund.values()));

        List<String> userIds = watchlistService.distinctUserIds();
        for (String userId : userIds) {
            List<EstimateResult> userEstimates = watchlistService.estimates(userId);
            sseService.push(userId, userEstimates);
        }
        log.info("intraday valuation done for {} funds, {} users", byFund.size(), userIds.size());
    }
}