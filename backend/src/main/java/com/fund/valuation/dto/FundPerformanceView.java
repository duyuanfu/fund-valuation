package com.fund.valuation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 基金历史业绩走势综合视图。
 */
public record FundPerformanceView(
        String fundCode,
        String range,
        BigDecimal totalReturnPct,
        List<NavPointView> navPoints,
        ValuationAccuracyView accuracy
) {
}
