package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fund.valuation.client.TianTianFundClient;
import com.fund.valuation.domain.EstimateHistory;
import com.fund.valuation.dto.AccuracyPointView;
import com.fund.valuation.dto.FundPerformanceView;
import com.fund.valuation.dto.NavPointView;
import com.fund.valuation.dto.ValuationAccuracyView;
import com.fund.valuation.mapper.EstimateHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 基金历史业绩走势拉取、多级缓存与估值准确性分析服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundPerformanceService {

    private final TianTianFundClient tianTianFundClient;
    private final EstimateHistoryMapper estimateHistoryMapper;

    // 历史不可变数据缓存 12 小时
    private final Cache<String, List<NavPointView>> navCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(12))
            .maximumSize(1000)
            .build();

    /**
     * 将前端传入的时间范围映射为天天基金的 API range 参数。
     */
    public static String mapToApiRange(String clientRange) {
        if (clientRange == null || clientRange.isBlank()) {
            return "y";
        }
        return switch (clientRange.trim().toLowerCase()) {
            case "1m", "y" -> "y";
            case "3m" -> "3y";
            case "6m", "6y" -> "6y";
            case "1y", "n" -> "n";
            case "3y", "3n" -> "3n";
            case "5y", "5n" -> "5n";
            case "all" -> "all";
            default -> "y";
        };
    }

    /**
     * 获取指定基金在指定时间范围内的历史业绩走势与估值准确度分析。
     *
     * @param fundCode 基金代码
     * @param range    时间周期 (1m, 3m, 6m, 1y, 3y, 5y, all)
     */
    public FundPerformanceView getPerformanceTrend(String fundCode, String range) {
        String normalizedRange = (range == null || range.isBlank()) ? "1m" : range.trim().toLowerCase();
        String apiRange = mapToApiRange(normalizedRange);
        String cacheKey = fundCode + ":" + apiRange;

        // 1. 获取历史净值点集 (带缓存)
        List<NavPointView> navPoints = navCache.get(cacheKey, k -> tianTianFundClient.fetchPerformanceTrend(fundCode, apiRange));
        if (navPoints == null) {
            navPoints = List.of();
        }

        // 2. 计算区间总收益率
        BigDecimal totalReturnPct = BigDecimal.ZERO;
        if (!navPoints.isEmpty()) {
            totalReturnPct = navPoints.get(navPoints.size() - 1).accumReturnPct();
            if (totalReturnPct == null) {
                totalReturnPct = BigDecimal.ZERO;
            }
        }

        // 3. 计算估值准确度对比与统计
        ValuationAccuracyView accuracy = calculateAccuracy(fundCode, navPoints);

        return new FundPerformanceView(fundCode, normalizedRange, totalReturnPct, navPoints, accuracy);
    }

    /**
     * 对齐本地收盘前最终预估点与官方实际涨跌幅，计算准确度指标。
     */
    public ValuationAccuracyView calculateAccuracy(String fundCode, List<NavPointView> navPoints) {
        if (navPoints == null || navPoints.isEmpty()) {
            return new ValuationAccuracyView(false, 0, null, null, List.of());
        }

        // 提取本地该基金的历史估值记录
        List<EstimateHistory> historyList = estimateHistoryMapper.selectList(
                new LambdaQueryWrapper<EstimateHistory>()
                        .eq(EstimateHistory::getFundCode, fundCode)
                        .orderByAsc(EstimateHistory::getEstTime)
        );

        if (historyList == null || historyList.isEmpty()) {
            return new ValuationAccuracyView(false, 0, null, null, List.of());
        }

        // 按日期提取当日最后一个收盘估算点 (以 yyyy-MM-dd 为 key)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, BigDecimal> dailyClosingEst = new HashMap<>();
        for (EstimateHistory h : historyList) {
            if (h.getEstTime() != null && h.getEstPct() != null) {
                String dateStr = h.getEstTime().format(dtf);
                // 列表按时间升序排列，后面时间会覆盖前面，最终保留的是该日最晚估值点
                dailyClosingEst.put(dateStr, h.getEstPct());
            }
        }

        List<AccuracyPointView> points = new ArrayList<>();
        BigDecimal sumAbsDiff = BigDecimal.ZERO;
        int matchCount = 0;

        for (NavPointView navPoint : navPoints) {
            String dateStr = navPoint.date();
            if (dailyClosingEst.containsKey(dateStr)) {
                BigDecimal actualPct = navPoint.dailyGrowthPct() != null ? navPoint.dailyGrowthPct() : BigDecimal.ZERO;
                BigDecimal estPct = dailyClosingEst.get(dateStr);
                BigDecimal diffPct = estPct.subtract(actualPct).setScale(4, RoundingMode.HALF_UP);

                // 判断涨跌方向是否一致 (同正或同负，或其中一个为0也算方向不冲突)
                boolean directionMatched = isDirectionMatched(actualPct, estPct);
                if (directionMatched) {
                    matchCount++;
                }
                sumAbsDiff = sumAbsDiff.add(diffPct.abs());

                points.add(new AccuracyPointView(dateStr, actualPct, estPct, diffPct, directionMatched));
            }
        }

        if (points.isEmpty()) {
            return new ValuationAccuracyView(false, 0, null, null, List.of());
        }

        int sampleCount = points.size();
        BigDecimal mae = sumAbsDiff.divide(BigDecimal.valueOf(sampleCount), 4, RoundingMode.HALF_UP);
        BigDecimal directionMatchRate = BigDecimal.valueOf(matchCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sampleCount), 2, RoundingMode.HALF_UP);

        return new ValuationAccuracyView(true, sampleCount, mae, directionMatchRate, points);
    }

    private boolean isDirectionMatched(BigDecimal actual, BigDecimal est) {
        if (actual.compareTo(BigDecimal.ZERO) == 0 || est.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        return (actual.compareTo(BigDecimal.ZERO) > 0 && est.compareTo(BigDecimal.ZERO) > 0)
                || (actual.compareTo(BigDecimal.ZERO) < 0 && est.compareTo(BigDecimal.ZERO) < 0);
    }

    public void clearCache(String fundCode) {
        if (fundCode == null || fundCode.isBlank()) {
            navCache.invalidateAll();
        } else {
            for (String range : List.of("y", "3y", "6m", "n", "3n", "5n", "all")) {
                navCache.invalidate(fundCode + ":" + range);
            }
        }
    }
}
