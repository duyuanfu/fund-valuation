package com.fund.valuation.service;

import com.fund.valuation.client.AiAdvisorClient;
import com.fund.valuation.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基金持仓智能健康体检与AI投顾诊断服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioDiagnosisService {

    private final PortfolioLookThroughService lookThroughService;
    private final AiAdvisorClient aiAdvisorClient;

    // 缓存已生成的 AI 诊断报告: "userId:fingerprint" -> AiDiagnosisReport
    private final ConcurrentHashMap<String, AiDiagnosisReport> reportCache = new ConcurrentHashMap<>();

    public PortfolioDiagnosisView getDiagnosis(String userId, boolean forceRefreshAi) {
        PortfolioLookThroughView lookThrough = lookThroughService.calculateLookThrough(userId);

        if (lookThrough.totalFundCount() == 0) {
            return new PortfolioDiagnosisView(
                    100,
                    "空仓待配置",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(100),
                    List.of(new DiagnosisRiskTag("INFO", "暂无持仓", "当前未添加任何基金持仓，添加后即可一键生成体检报告。")),
                    lookThrough,
                    null
            );
        }

        List<DiagnosisRiskTag> riskTags = new ArrayList<>();

        // 1. 集中度评分 (Concentration Score, 0 - 100)
        BigDecimal concentrationScore = BigDecimal.valueOf(100);
        List<StockExposureItem> topStocks = lookThrough.topStockExposures();

        if (!topStocks.isEmpty()) {
            StockExposureItem top1 = topStocks.get(0);
            BigDecimal top1Ratio = top1.portfolioRatio(); // 占组合穿透比 (%)

            if (top1Ratio.compareTo(BigDecimal.valueOf(25.0)) > 0) {
                concentrationScore = concentrationScore.subtract(BigDecimal.valueOf(45));
                riskTags.add(new DiagnosisRiskTag(
                        "DANGER",
                        "单一重仓股严重超标",
                        String.format("穿透第一大重仓股【%s】在组合中占比达 %s%%，已突破25%%安全红线，个股黑天鹅风险极高。", top1.stockName(), top1Ratio)
                ));
            } else if (top1Ratio.compareTo(BigDecimal.valueOf(15.0)) > 0) {
                concentrationScore = concentrationScore.subtract(BigDecimal.valueOf(20));
                riskTags.add(new DiagnosisRiskTag(
                        "WARNING",
                        "重仓个股集中度偏高",
                        String.format("穿透第一大重仓股【%s】在组合中占比为 %s%%，建议关注个股回调对组合净值的冲击。", top1.stockName(), top1Ratio)
                ));
            } else {
                riskTags.add(new DiagnosisRiskTag(
                        "SUCCESS",
                        "个股权重合理分散",
                        String.format("第一大重仓股票【%s】穿透占比 %s%%，未见单一个股过度暴露风险。", top1.stockName(), top1Ratio)
                ));
            }

            BigDecimal top10Sum = lookThrough.top10StockRatioSum();
            if (top10Sum.compareTo(BigDecimal.valueOf(60.0)) > 0) {
                concentrationScore = concentrationScore.subtract(BigDecimal.valueOf(20));
                riskTags.add(new DiagnosisRiskTag(
                        "WARNING",
                        "前十大重仓合计占比较高",
                        String.format("前十大穿透重仓股合计占比达 %s%%，持仓风格呈现明显的高集中进攻型。", top10Sum)
                ));
            }
        }

        if (concentrationScore.compareTo(BigDecimal.valueOf(20)) < 0) {
            concentrationScore = BigDecimal.valueOf(20);
        }

        // 2. 资产配置平衡评分 (Asset Balance Score, 0 - 100)
        BigDecimal assetBalanceScore = BigDecimal.valueOf(100);
        int fundCount = lookThrough.totalFundCount();

        if (fundCount == 1) {
            assetBalanceScore = BigDecimal.valueOf(50);
            riskTags.add(new DiagnosisRiskTag(
                    "WARNING",
                    "持仓基金单一",
                    "当前仅持有 1 只基金，难以抵御特定行业周期与基金经理风格漂移风险。"
            ));
        } else if (fundCount >= 2 && fundCount <= 8) {
            riskTags.add(new DiagnosisRiskTag(
                    "SUCCESS",
                    "持仓只数适中",
                    String.format("持有 %d 只基金，组合具备良好的核心-卫星配置基底。", fundCount)
            ));
        } else {
            assetBalanceScore = assetBalanceScore.subtract(BigDecimal.valueOf(15));
            riskTags.add(new DiagnosisRiskTag(
                    "INFO",
                    "基金只数偏多",
                    String.format("持有 %d 只基金，持仓过于分散容易变成“指数化持有”，收益易被费用摊薄，建议适当归并。", fundCount)
            ));
        }

        BigDecimal equityRatio = BigDecimal.ZERO;
        BigDecimal bondRatio = BigDecimal.ZERO;

        for (AssetAllocationItem alloc : lookThrough.assetAllocations()) {
            if ("ACTIVE".equalsIgnoreCase(alloc.assetType()) || "MIXED".equalsIgnoreCase(alloc.assetType()) || "INDEX".equalsIgnoreCase(alloc.assetType())) {
                equityRatio = equityRatio.add(alloc.ratio());
            } else if ("BOND".equalsIgnoreCase(alloc.assetType())) {
                bondRatio = bondRatio.add(alloc.ratio());
            }
        }

        if (equityRatio.compareTo(BigDecimal.valueOf(90.0)) >= 0 && fundCount > 1) {
            assetBalanceScore = assetBalanceScore.subtract(BigDecimal.valueOf(15));
            riskTags.add(new DiagnosisRiskTag(
                    "INFO",
                    "全权益进攻型配置",
                    "权益类基金占比超过90%，无固收防守垫，在牛市弹性大，但熊市需承受较大回撤压力。"
            ));
        } else if (equityRatio.compareTo(BigDecimal.valueOf(40.0)) >= 0 && bondRatio.compareTo(BigDecimal.valueOf(20.0)) >= 0) {
            riskTags.add(new DiagnosisRiskTag(
                    "SUCCESS",
                    "股债搭配科学平衡",
                    String.format("权益占比 %s%%、固收占比 %s%%，具备典型的股债平衡防御韧性。", equityRatio, bondRatio)
            ));
        }

        if (assetBalanceScore.compareTo(BigDecimal.valueOf(20)) < 0) {
            assetBalanceScore = BigDecimal.valueOf(20);
        }

        // 3. 基金底层重合度评分 (Stock Overlap Score, 0 - 100)
        BigDecimal stockOverlapScore = BigDecimal.valueOf(100);
        List<CrossFundOverlapItem> majorOverlaps = lookThrough.majorOverlaps();

        if (!majorOverlaps.isEmpty()) {
            CrossFundOverlapItem highestOverlap = majorOverlaps.get(0);
            if (highestOverlap.overlapRatio().compareTo(BigDecimal.valueOf(25.0)) > 0) {
                stockOverlapScore = stockOverlapScore.subtract(BigDecimal.valueOf(40));
                riskTags.add(new DiagnosisRiskTag(
                        "DANGER",
                        "存在显著重仓同质化",
                        String.format("【%s】与【%s】重仓股重合度达 %s%%（共同重仓 %s），存在明显的伪分散隐患。",
                                highestOverlap.fundNameA(), highestOverlap.fundNameB(),
                                highestOverlap.overlapRatio(),
                                String.join("、", highestOverlap.commonStockNames().subList(0, Math.min(3, highestOverlap.commonStockNames().size()))))
                ));
            } else {
                stockOverlapScore = stockOverlapScore.subtract(BigDecimal.valueOf(15));
                riskTags.add(new DiagnosisRiskTag(
                        "WARNING",
                        "部分基金持仓存在重合",
                        String.format("【%s】与【%s】重合度为 %s%%，建议评估两只基金的收益弹性差异。",
                                highestOverlap.fundNameA(), highestOverlap.fundNameB(), highestOverlap.overlapRatio())
                ));
            }
        } else if (fundCount > 1) {
            riskTags.add(new DiagnosisRiskTag(
                    "SUCCESS",
                    "底层持仓互补性好",
                    "各持仓基金底层前十大重仓股票未见高度雷同，分散化配置效果显著。"
            ));
        }

        if (stockOverlapScore.compareTo(BigDecimal.valueOf(20)) < 0) {
            stockOverlapScore = BigDecimal.valueOf(20);
        }

        // 4. 综合健康得分与等级计算
        BigDecimal composite = concentrationScore.multiply(BigDecimal.valueOf(0.40))
                .add(assetBalanceScore.multiply(BigDecimal.valueOf(0.35)))
                .add(stockOverlapScore.multiply(BigDecimal.valueOf(0.25)))
                .setScale(0, RoundingMode.HALF_UP);

        int healthScore = composite.intValue();
        healthScore = Math.max(10, Math.min(100, healthScore));

        String scoreLevel;
        if (healthScore >= 90) {
            scoreLevel = "极佳";
        } else if (healthScore >= 75) {
            scoreLevel = "良好";
        } else if (healthScore >= 60) {
            scoreLevel = "需关注";
        } else {
            scoreLevel = "风险偏高";
        }

        // 5. AI 诊断报告缓存与组装
        String fingerprint = buildFingerprint(userId, lookThrough, healthScore);
        String cacheKey = userId + ":" + fingerprint;
        AiDiagnosisReport aiReport = reportCache.get(cacheKey);

        if (aiReport == null || forceRefreshAi) {
            aiReport = aiAdvisorClient.generateReport(healthScore, scoreLevel, lookThrough, riskTags);
            reportCache.put(cacheKey, aiReport);
        }

        return new PortfolioDiagnosisView(
                healthScore,
                scoreLevel,
                concentrationScore.setScale(1, RoundingMode.HALF_UP),
                assetBalanceScore.setScale(1, RoundingMode.HALF_UP),
                stockOverlapScore.setScale(1, RoundingMode.HALF_UP),
                riskTags,
                lookThrough,
                aiReport
        );
    }

    private String buildFingerprint(String userId, PortfolioLookThroughView lookThrough, int healthScore) {
        String topStock = lookThrough.topStockExposures().isEmpty() ? "none" : lookThrough.topStockExposures().get(0).stockCode();
        return String.format("%d_%s_%s_%d",
                lookThrough.totalFundCount(),
                lookThrough.totalPortfolioAmount(),
                topStock,
                healthScore
        );
    }
}
