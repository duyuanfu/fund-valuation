package com.fund.valuation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 单只基金的估值结果。stale 表示存在行情缺失/降级。
 */
public record EstimateResult(
        String fundCode,
        String fundName,
        String fundType,
        BigDecimal estimateNav,
        BigDecimal estimatePct,
        BigDecimal prevNav,
        LocalDate navDate,
        String reportQt,
        LocalDateTime quoteTs,
        boolean stale,
        String message,
        String disclaimer
) {
}
