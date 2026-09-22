package com.fund.valuation.client;

import java.math.BigDecimal;

public record HoldingRow(String stockCode, String stockName, BigDecimal weight) {
}