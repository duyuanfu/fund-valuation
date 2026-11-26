package com.fund.valuation.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioLookThroughView(
        BigDecimal totalPortfolioAmount,
        int totalFundCount,
        List<StockExposureItem> topStockExposures,
        List<AssetAllocationItem> assetAllocations,
        List<CrossFundOverlapItem> majorOverlaps,
        String latestReportQuarter,
        BigDecimal top10StockRatioSum
) {}
