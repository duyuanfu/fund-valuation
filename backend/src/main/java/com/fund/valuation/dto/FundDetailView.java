package com.fund.valuation.dto;

import com.fund.valuation.service.EstimateResult;

import java.util.List;

public record FundDetailView(EstimateResult estimate, List<HoldingView> holdings, List<EstimatePointView> history, BondInfo bondInfo) {
}
