package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.domain.EstimateHistory;
import com.fund.valuation.mapper.EstimateHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 估值历史:盘中落库、按基金+时间查询、保留期清理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EstimateHistoryService {

    private final EstimateHistoryMapper historyMapper;
    private final AppProperties properties;
    private final Clock clock;

    /**
     * 落库一批估值结果(仅交易时段)。
     */
    @Transactional
    public void recordAll(List<EstimateResult> results) {
        if (results == null || results.isEmpty() || !TradingCalendar.isTradingTime(LocalDateTime.now(clock))) {
            return;
        }
        for (EstimateResult r : results) {
            if (r == null || r.estimateNav() == null || r.estimatePct() == null) {
                continue;
            }
            EstimateHistory h = new EstimateHistory();
            h.setFundCode(r.fundCode());
            h.setEstTime(LocalDateTime.now(clock));
            h.setEstNav(r.estimateNav());
            h.setEstPct(r.estimatePct());
            historyMapper.insert(h);
        }
    }

    /**
     * 查询某基金当日历史(时间升序)。
     */
    public List<EstimateHistory> todayHistory(String fundCode) {
        LocalDate today = LocalDate.now(clock);
        return historyMapper.selectList(new LambdaQueryWrapper<EstimateHistory>()
                .eq(EstimateHistory::getFundCode, fundCode)
                .ge(EstimateHistory::getEstTime, today.atStartOfDay())
                .lt(EstimateHistory::getEstTime, today.plusDays(1).atStartOfDay())
                .orderByAsc(EstimateHistory::getEstTime));
    }

    /**
     * 清理超过保留期的历史。
     */
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(properties.getHistory().getRetainDays());
        long deleted = historyMapper.delete(new LambdaQueryWrapper<EstimateHistory>()
                .lt(EstimateHistory::getEstTime, cutoff));
        if (deleted > 0) {
            log.info("cleaned {} estimate history rows older than {} days", deleted, properties.getHistory().getRetainDays());
        }
    }
}