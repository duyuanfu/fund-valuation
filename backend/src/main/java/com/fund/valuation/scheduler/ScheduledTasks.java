package com.fund.valuation.scheduler;

import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.service.EstimateHistoryService;
import com.fund.valuation.service.FundCatalogService;
import com.fund.valuation.service.FundHoldingService;
import com.fund.valuation.service.IntradayValuationRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时任务:净值更新、持仓更新、盘中估值、历史清理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final FundCatalogService fundCatalogService;
    private final FundHoldingService fundHoldingService;
    private final IntradayValuationRunner intradayRunner;
    private final EstimateHistoryService historyService;

    /**
     * 每日净值更新(默认 20:30)。
     */
    @Scheduled(cron = "${fund.schedule.nav-cron}")
    public void refreshNav() {
        log.info("scheduled nav refresh start");
        int updated = fundCatalogService.refreshAll();
        log.info("scheduled nav refresh done, updated {}", updated);
    }

    /**
     * 每日持仓更新(默认 21:00)。
     */
    @Scheduled(cron = "${fund.schedule.holding-cron}")
    public void refreshHoldings() {
        log.info("scheduled holdings refresh start");
        fundHoldingService.refreshAll();
        log.info("scheduled holdings refresh done");
    }

    /**
     * 盘中估值(由 runner 内部权威判定交易时段)。
     */
    @Scheduled(fixedDelayString = "${fund.schedule.intraday-interval-ms}")
    public void intradayValuation() {
        try {
            intradayRunner.run();
        } catch (Exception e) {
            log.error("intraday valuation failed", e);
        }
    }

    /**
     * 历史数据清理。
     */
    @Scheduled(cron = "0 20 3 * * *")
    public void cleanupHistory() {
        historyService.cleanup();
    }
}
