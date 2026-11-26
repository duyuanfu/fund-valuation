package com.fund.valuation.dto;

import java.math.BigDecimal;
import java.util.List;

public record StockExposureItem(
        String stockCode,
        String stockName,
        BigDecimal holdingAmount,
        BigDecimal portfolioRatio,
        List<StockContributor> contributors
) {
    public record StockContributor(
            String fundCode,
            String fundName,
            BigDecimal fundHoldingAmount,
            BigDecimal weightInFund,
            BigDecimal effectiveAmount
    ) {}
}
