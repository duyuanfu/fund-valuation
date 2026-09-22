package com.fund.valuation.dto;

import java.math.BigDecimal;

public record BondInfo(
        String indexName,
        BigDecimal indexPct,
        double duration,
        double referenceDuration,
        String formula
) {
}
