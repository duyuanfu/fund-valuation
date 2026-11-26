package com.fund.valuation.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioDiagnosisView(
        int healthScore,
        String scoreLevel,
        BigDecimal concentrationScore,
        BigDecimal assetBalanceScore,
        BigDecimal stockOverlapScore,
        List<DiagnosisRiskTag> riskTags,
        PortfolioLookThroughView lookThrough,
        AiDiagnosisReport aiReport
) {}
