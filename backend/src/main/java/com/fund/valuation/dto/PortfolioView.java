package com.fund.valuation.dto;

import java.util.List;

public record PortfolioView(
        PortfolioSummaryView summary,
        List<PositionItemView> items
) {
}
