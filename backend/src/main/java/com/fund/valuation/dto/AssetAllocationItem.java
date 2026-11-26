package com.fund.valuation.dto;

import java.math.BigDecimal;

public record AssetAllocationItem(
        String assetType,
        String assetTypeLabel,
        BigDecimal holdingAmount,
        BigDecimal ratio
) {}
