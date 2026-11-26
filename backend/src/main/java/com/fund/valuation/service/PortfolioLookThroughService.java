package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.FundHolding;
import com.fund.valuation.domain.UserPosition;
import com.fund.valuation.dto.AssetAllocationItem;
import com.fund.valuation.dto.CrossFundOverlapItem;
import com.fund.valuation.dto.PortfolioLookThroughView;
import com.fund.valuation.dto.StockExposureItem;
import com.fund.valuation.dto.StockExposureItem.StockContributor;
import com.fund.valuation.mapper.UserPositionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基金持仓组合底层资产穿透与重仓股透视计算引擎
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioLookThroughService {

    private final UserPositionMapper positionMapper;
    private final FundCatalogService fundCatalogService;
    private final FundHoldingService fundHoldingService;

    private static final Map<String, String> TYPE_LABEL_MAP = Map.of(
            "ACTIVE", "偏股混合 / 股票型",
            "MIXED", "平衡混合型",
            "INDEX", "指数型 / ETF",
            "BOND", "债券型 / 固收",
            "QDII", "海外 QDII"
    );

    public PortfolioLookThroughView calculateLookThrough(String userId) {
        List<UserPosition> positions = positionMapper.selectList(new LambdaQueryWrapper<UserPosition>()
                .eq(UserPosition::getUserId, userId)
                .orderByDesc(UserPosition::getHoldingAmount));

        if (positions.isEmpty()) {
            return new PortfolioLookThroughView(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0,
                    List.of(),
                    List.of(),
                    List.of(),
                    "-",
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        BigDecimal totalAmount = positions.stream()
                .map(UserPosition::getHoldingAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            totalAmount = BigDecimal.ONE;
        }

        // 1. 资产大类配置分布计算
        Map<String, BigDecimal> assetTypeAmounts = new LinkedHashMap<>();
        Map<String, List<FundHolding>> fundHoldingsMap = new HashMap<>();
        Map<String, Fund> fundMap = new HashMap<>();
        String latestReportQuarter = null;

        for (UserPosition p : positions) {
            String code = p.getFundCode();
            Fund fund = fundCatalogService.get(code);
            fundMap.put(code, fund);

            String type = (fund != null && fund.getType() != null) ? fund.getType() : "OTHER";
            BigDecimal curAmt = p.getHoldingAmount() != null ? p.getHoldingAmount() : BigDecimal.ZERO;
            assetTypeAmounts.put(type, assetTypeAmounts.getOrDefault(type, BigDecimal.ZERO).add(curAmt));

            List<FundHolding> holdings = fundHoldingService.ensureHoldings(fund);
            fundHoldingsMap.put(code, holdings);

            if (!holdings.isEmpty() && latestReportQuarter == null) {
                latestReportQuarter = holdings.get(0).getReportQt();
            }
        }

        final BigDecimal finalTotal = totalAmount;
        List<AssetAllocationItem> assetAllocations = assetTypeAmounts.entrySet().stream()
                .map(e -> {
                    String type = e.getKey();
                    BigDecimal amt = e.getValue().setScale(2, RoundingMode.HALF_UP);
                    BigDecimal ratio = amt.multiply(BigDecimal.valueOf(100))
                            .divide(finalTotal, 2, RoundingMode.HALF_UP);
                    String label = TYPE_LABEL_MAP.getOrDefault(type, type);
                    return new AssetAllocationItem(type, label, amt, ratio);
                })
                .sorted((a, b) -> b.holdingAmount().compareTo(a.holdingAmount()))
                .collect(Collectors.toList());

        // 2. 底层股票穿透聚合计算
        Map<String, StockAccumulator> stockMap = new HashMap<>();

        for (UserPosition p : positions) {
            String fundCode = p.getFundCode();
            BigDecimal fundAmt = p.getHoldingAmount() != null ? p.getHoldingAmount() : BigDecimal.ZERO;
            if (fundAmt.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Fund fund = fundMap.get(fundCode);
            String fundName = fund != null ? fund.getName() : fundCode;
            List<FundHolding> holdings = fundHoldingsMap.getOrDefault(fundCode, List.of());

            for (FundHolding h : holdings) {
                String stockCode = h.getStockCode();
                String stockName = h.getStockName();
                BigDecimal weightInFund = h.getWeight() != null ? h.getWeight() : BigDecimal.ZERO;

                // 穿透金额 = 该基金持仓市值 * (该股票在基金中权重 / 100)
                BigDecimal effectiveAmt = fundAmt.multiply(weightInFund)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                StockAccumulator acc = stockMap.computeIfAbsent(stockCode, k -> new StockAccumulator(stockCode, stockName));
                acc.add(fundCode, fundName, fundAmt, weightInFund, effectiveAmt);
            }
        }

        List<StockExposureItem> topStockExposures = stockMap.values().stream()
                .sorted((a, b) -> b.totalEffectiveAmount.compareTo(a.totalEffectiveAmount))
                .limit(10)
                .map(acc -> {
                    BigDecimal ratio = acc.totalEffectiveAmount.multiply(BigDecimal.valueOf(100))
                            .divide(finalTotal, 2, RoundingMode.HALF_UP);
                    return new StockExposureItem(
                            acc.stockCode,
                            acc.stockName,
                            acc.totalEffectiveAmount.setScale(2, RoundingMode.HALF_UP),
                            ratio,
                            acc.contributors
                    );
                })
                .collect(Collectors.toList());

        BigDecimal top10Sum = topStockExposures.stream()
                .map(StockExposureItem::portfolioRatio)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // 3. 跨基金重合度计算 (选出重合度高或有共同重仓股票的基金对)
        List<CrossFundOverlapItem> majorOverlaps = new ArrayList<>();
        List<UserPosition> validPositions = positions.stream()
                .filter(p -> !fundHoldingsMap.getOrDefault(p.getFundCode(), List.of()).isEmpty())
                .collect(Collectors.toList());

        for (int i = 0; i < validPositions.size(); i++) {
            for (int j = i + 1; j < validPositions.size(); j++) {
                UserPosition posA = validPositions.get(i);
                UserPosition posB = validPositions.get(j);
                String codeA = posA.getFundCode();
                String codeB = posB.getFundCode();

                List<FundHolding> listA = fundHoldingsMap.get(codeA);
                List<FundHolding> listB = fundHoldingsMap.get(codeB);

                Map<String, BigDecimal> weightMapB = listB.stream()
                        .collect(Collectors.toMap(FundHolding::getStockCode, FundHolding::getWeight, (v1, v2) -> v1));

                BigDecimal overlapWeight = BigDecimal.ZERO;
                List<String> commonStockNames = new ArrayList<>();

                for (FundHolding hA : listA) {
                    BigDecimal wB = weightMapB.get(hA.getStockCode());
                    if (wB != null) {
                        overlapWeight = overlapWeight.add(hA.getWeight().min(wB));
                        commonStockNames.add(hA.getStockName());
                    }
                }

                if (overlapWeight.compareTo(BigDecimal.valueOf(8.0)) >= 0 || commonStockNames.size() >= 3) {
                    Fund fundA = fundMap.get(codeA);
                    Fund fundB = fundMap.get(codeB);
                    majorOverlaps.add(new CrossFundOverlapItem(
                            codeA,
                            fundA != null ? fundA.getName() : codeA,
                            codeB,
                            fundB != null ? fundB.getName() : codeB,
                            overlapWeight.setScale(2, RoundingMode.HALF_UP),
                            commonStockNames
                    ));
                }
            }
        }

        majorOverlaps.sort((a, b) -> b.overlapRatio().compareTo(a.overlapRatio()));

        return new PortfolioLookThroughView(
                totalAmount,
                positions.size(),
                topStockExposures,
                assetAllocations,
                majorOverlaps,
                latestReportQuarter != null ? latestReportQuarter : "最新披露季报",
                top10Sum
        );
    }

    private static class StockAccumulator {
        final String stockCode;
        final String stockName;
        BigDecimal totalEffectiveAmount = BigDecimal.ZERO;
        final List<StockContributor> contributors = new ArrayList<>();

        StockAccumulator(String stockCode, String stockName) {
            this.stockCode = stockCode;
            this.stockName = stockName;
        }

        void add(String fundCode, String fundName, BigDecimal fundAmount, BigDecimal weightInFund, BigDecimal effectiveAmt) {
            this.totalEffectiveAmount = this.totalEffectiveAmount.add(effectiveAmt);
            this.contributors.add(new StockContributor(fundCode, fundName, fundAmount, weightInFund, effectiveAmt));
        }
    }
}
