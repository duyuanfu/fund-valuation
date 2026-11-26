package com.fund.valuation.dto;

import java.math.BigDecimal;
import java.util.List;

public record CrossFundOverlapItem(
        String fundCodeA,
        String fundNameA,
        String fundCodeB,
        String fundNameB,
        BigDecimal overlapRatio,
        List<String> commonStockNames
) {}
