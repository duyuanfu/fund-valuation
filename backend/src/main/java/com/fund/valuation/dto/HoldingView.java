package com.fund.valuation.dto;

import java.math.BigDecimal;

public record HoldingView(String stockCode, String stockName, BigDecimal weight, BigDecimal pctChg, String reportQt) {
}
