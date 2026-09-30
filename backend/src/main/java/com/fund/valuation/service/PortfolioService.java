package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.UserPosition;
import com.fund.valuation.dto.PortfolioSummaryView;
import com.fund.valuation.dto.PortfolioView;
import com.fund.valuation.dto.PositionItemView;
import com.fund.valuation.dto.PositionSaveRequest;
import com.fund.valuation.mapper.UserPositionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final UserPositionMapper positionMapper;
    private final FundCatalogService fundCatalogService;
    private final FundHoldingService fundHoldingService;
    private final ValuationEngine valuationEngine;
    private final QuoteService quoteService;

    @Autowired(required = false)
    @Qualifier("crawlerExecutor")
    private Executor crawlerExecutor;

    private void runAsyncCrawler(Runnable task) {
        if (crawlerExecutor != null) {
            CompletableFuture.runAsync(task, crawlerExecutor);
        } else {
            CompletableFuture.runAsync(task);
        }
    }

    /**
     * 获取用户持仓估值全览（包含汇总看板与各持仓明细）。
     */
    public PortfolioView getPortfolio(String userId) {
        List<UserPosition> positions = positionMapper.selectList(new LambdaQueryWrapper<UserPosition>()
                .eq(UserPosition::getUserId, userId)
                .orderByDesc(UserPosition::getHoldingAmount));

        if (positions.isEmpty()) {
            PortfolioSummaryView emptySummary = new PortfolioSummaryView(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
            return new PortfolioView(emptySummary, List.of());
        }

        List<Fund> funds = new ArrayList<>();
        for (UserPosition pos : positions) {
            Fund f = fundCatalogService.get(pos.getFundCode());
            if (f == null) {
                try {
                    f = fundCatalogService.ensureFund(pos.getFundCode());
                } catch (Exception e) {
                    log.warn("load fund failed for holding {}: {}", pos.getFundCode(), e.getMessage());
                }
            }
            if (f != null) {
                fundHoldingService.ensureHoldings(f);
                funds.add(f);
            }
        }

        Map<String, Fund> fundMap = funds.stream()
                .collect(Collectors.toMap(Fund::getCode, Function.identity(), (a, b) -> a));

        // 批量预热行情标的
        Set<String> requiredSecids = new HashSet<>();
        for (Fund f : funds) {
            Set<String> secids = valuationEngine.collectRequiredSecids(f);
            if (secids != null) {
                requiredSecids.addAll(secids);
            }
        }
        quoteService.ensureFreshQuotes(requiredSecids);

        List<PositionItemView> items = new ArrayList<>();
        BigDecimal sumMarketValue = BigDecimal.ZERO;
        BigDecimal sumHoldingAmount = BigDecimal.ZERO;
        BigDecimal sumYesterdayIncome = BigDecimal.ZERO;
        BigDecimal sumHoldingProfit = BigDecimal.ZERO;
        BigDecimal sumTodayIncome = BigDecimal.ZERO;
        BigDecimal sumCostAmount = BigDecimal.ZERO;

        for (UserPosition pos : positions) {
            Fund fund = fundMap.get(pos.getFundCode());
            EstimateResult est = null;
            if (fund != null) {
                est = valuationEngine.estimate(fund);
            }

            BigDecimal holdingAmount = pos.getHoldingAmount() != null ? pos.getHoldingAmount() : BigDecimal.ZERO;
            BigDecimal yesterdayIncome = pos.getYesterdayIncome() != null ? pos.getYesterdayIncome() : BigDecimal.ZERO;
            BigDecimal holdingProfit = pos.getHoldingProfit() != null ? pos.getHoldingProfit() : BigDecimal.ZERO;
            BigDecimal holdingProfitRate = pos.getHoldingProfitRate();
            BigDecimal costAmount = pos.getCostAmount() != null ? pos.getCostAmount() : holdingAmount.subtract(holdingProfit);

            BigDecimal estimatePct = est != null ? est.estimatePct() : null;
            BigDecimal todayIncome;
            if (estimatePct != null) {
                todayIncome = holdingAmount.multiply(estimatePct)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                todayIncome = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal dynamicMarketValue = holdingAmount.add(todayIncome).setScale(2, RoundingMode.HALF_UP);
            BigDecimal dynamicTotalProfit = holdingProfit.add(todayIncome).setScale(2, RoundingMode.HALF_UP);
            BigDecimal dynamicTotalProfitRate;
            if (costAmount.compareTo(BigDecimal.ZERO) > 0) {
                dynamicTotalProfitRate = dynamicTotalProfit.multiply(BigDecimal.valueOf(100))
                        .divide(costAmount, 2, RoundingMode.HALF_UP);
            } else {
                dynamicTotalProfitRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            sumMarketValue = sumMarketValue.add(dynamicMarketValue);
            sumHoldingAmount = sumHoldingAmount.add(holdingAmount);
            sumYesterdayIncome = sumYesterdayIncome.add(yesterdayIncome);
            sumHoldingProfit = sumHoldingProfit.add(holdingProfit);
            sumTodayIncome = sumTodayIncome.add(todayIncome);
            sumCostAmount = sumCostAmount.add(costAmount);

            items.add(new PositionItemView(
                    pos.getId(),
                    pos.getFundCode(),
                    fund != null ? fund.getName() : pos.getFundCode(),
                    fund != null ? fund.getType() : null,
                    holdingAmount.setScale(2, RoundingMode.HALF_UP),
                    yesterdayIncome.setScale(2, RoundingMode.HALF_UP),
                    holdingProfit.setScale(2, RoundingMode.HALF_UP),
                    holdingProfitRate != null ? holdingProfitRate.setScale(2, RoundingMode.HALF_UP) : null,
                    costAmount.setScale(2, RoundingMode.HALF_UP),
                    pos.getHoldingShares(),
                    pos.getCostPrice(),
                    est != null ? est.prevNav() : null,
                    est != null ? est.navDate() : null,
                    est != null ? est.estimateNav() : null,
                    est != null ? est.estimatePct() : null,
                    est != null ? est.quoteTs() : null,
                    est == null || est.stale(),
                    todayIncome,
                    dynamicMarketValue,
                    dynamicTotalProfit,
                    dynamicTotalProfitRate
            ));
        }

        BigDecimal totalTodayIncomePct;
        if (sumHoldingAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalTodayIncomePct = sumTodayIncome.multiply(BigDecimal.valueOf(100))
                    .divide(sumHoldingAmount, 2, RoundingMode.HALF_UP);
        } else {
            totalTodayIncomePct = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal dynamicTotalProfit = sumHoldingProfit.add(sumTodayIncome).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dynamicTotalProfitRate;
        if (sumCostAmount.compareTo(BigDecimal.ZERO) > 0) {
            dynamicTotalProfitRate = dynamicTotalProfit.multiply(BigDecimal.valueOf(100))
                    .divide(sumCostAmount, 2, RoundingMode.HALF_UP);
        } else {
            dynamicTotalProfitRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        PortfolioSummaryView summary = new PortfolioSummaryView(
                sumMarketValue.setScale(2, RoundingMode.HALF_UP),
                sumHoldingAmount.setScale(2, RoundingMode.HALF_UP),
                sumYesterdayIncome.setScale(2, RoundingMode.HALF_UP),
                sumHoldingProfit.setScale(2, RoundingMode.HALF_UP),
                sumTodayIncome.setScale(2, RoundingMode.HALF_UP),
                totalTodayIncomePct,
                dynamicTotalProfit,
                dynamicTotalProfitRate
        );

        return new PortfolioView(summary, items);
    }

    /**
     * 保存或更新持仓记录（支持支付宝4字段录入与双向联动计算）。
     */
    @Transactional
    public UserPosition savePosition(String userId, PositionSaveRequest req) {
        String code = req.fundCode().trim();
        Fund fund = fundCatalogService.ensureFund(code);

        BigDecimal holdingAmount = req.holdingAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal yesterdayIncome = req.yesterdayIncome() != null
                ? req.yesterdayIncome().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal holdingProfit;
        BigDecimal holdingProfitRate;
        BigDecimal costAmount;

        if (req.holdingProfit() != null) {
            holdingProfit = req.holdingProfit().setScale(2, RoundingMode.HALF_UP);
            costAmount = holdingAmount.subtract(holdingProfit).setScale(2, RoundingMode.HALF_UP);
            if (costAmount.compareTo(BigDecimal.ZERO) > 0) {
                holdingProfitRate = holdingProfit.multiply(BigDecimal.valueOf(100))
                        .divide(costAmount, 4, RoundingMode.HALF_UP);
            } else {
                holdingProfitRate = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
        } else if (req.holdingProfitRate() != null) {
            holdingProfitRate = req.holdingProfitRate().setScale(4, RoundingMode.HALF_UP);
            BigDecimal rateFraction = holdingProfitRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal onePlusRate = BigDecimal.ONE.add(rateFraction);
            costAmount = holdingAmount.divide(onePlusRate, 2, RoundingMode.HALF_UP);
            holdingProfit = holdingAmount.subtract(costAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            holdingProfit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            costAmount = holdingAmount;
            holdingProfitRate = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        // 推算份额与成本单价 (若昨日净值存在且大于0)
        BigDecimal holdingShares = null;
        BigDecimal costPrice = null;
        if (fund.getPrevNav() != null && fund.getPrevNav().compareTo(BigDecimal.ZERO) > 0) {
            holdingShares = holdingAmount.divide(fund.getPrevNav(), 4, RoundingMode.HALF_UP);
            if (holdingShares.compareTo(BigDecimal.ZERO) > 0) {
                costPrice = costAmount.divide(holdingShares, 4, RoundingMode.HALF_UP);
            }
        }

        UserPosition pos = positionMapper.selectOne(new LambdaQueryWrapper<UserPosition>()
                .eq(UserPosition::getUserId, userId)
                .eq(UserPosition::getFundCode, code));

        LocalDateTime now = LocalDateTime.now();
        if (pos == null) {
            pos = new UserPosition();
            pos.setUserId(userId);
            pos.setFundCode(code);
            pos.setHoldingAmount(holdingAmount);
            pos.setYesterdayIncome(yesterdayIncome);
            pos.setHoldingProfit(holdingProfit);
            pos.setHoldingProfitRate(holdingProfitRate);
            pos.setCostAmount(costAmount);
            pos.setHoldingShares(holdingShares);
            pos.setCostPrice(costPrice);
            pos.setCreatedAt(now);
            pos.setUpdatedAt(now);
            positionMapper.insert(pos);
        } else {
            pos.setHoldingAmount(holdingAmount);
            pos.setYesterdayIncome(yesterdayIncome);
            pos.setHoldingProfit(holdingProfit);
            pos.setHoldingProfitRate(holdingProfitRate);
            pos.setCostAmount(costAmount);
            pos.setHoldingShares(holdingShares);
            pos.setCostPrice(costPrice);
            pos.setUpdatedAt(now);
            positionMapper.updateById(pos);
        }

        // 异步预热持仓数据
        final Fund f = fund;
        runAsyncCrawler(() -> {
            try {
                fundHoldingService.refreshOne(f);
            } catch (Exception ignored) {
            }
        });

        return pos;
    }

    /**
     * 删除单条持仓记录。
     */
    @Transactional
    public void removePosition(String userId, String fundCode) {
        positionMapper.delete(new LambdaQueryWrapper<UserPosition>()
                .eq(UserPosition::getUserId, userId)
                .eq(UserPosition::getFundCode, fundCode));
    }
}
