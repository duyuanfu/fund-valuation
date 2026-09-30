package com.fund.valuation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PositionItemView(
        Long id,
        String fundCode,
        String fundName,
        String fundType,
        BigDecimal holdingAmount,
        BigDecimal yesterdayIncome,
        BigDecimal holdingProfit,
        BigDecimal holdingProfitRate,
        BigDecimal costAmount,
        BigDecimal holdingShares,
        BigDecimal costPrice,
        BigDecimal prevNav,
        LocalDate navDate,
        BigDecimal estimateNav,
        BigDecimal estimatePct,
        LocalDateTime quoteTs,
        boolean stale,
        BigDecimal todayIncome,
        BigDecimal dynamicMarketValue,
        BigDecimal dynamicTotalProfit,
        BigDecimal dynamicTotalProfitRate
) {
}
