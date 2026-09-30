package com.fund.valuation.dto;

import java.math.BigDecimal;

public record PortfolioSummaryView(
        BigDecimal totalMarketValue,
        BigDecimal totalHoldingAmount,
        BigDecimal totalYesterdayIncome,
        BigDecimal totalHoldingProfit,
        BigDecimal totalTodayIncome,
        BigDecimal totalTodayIncomePct,
        BigDecimal dynamicTotalProfit,
        BigDecimal dynamicTotalProfitRate
) {
}
