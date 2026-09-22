package com.fund.valuation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EstimatePointView(LocalDateTime estTime, BigDecimal estNav, BigDecimal estPct) {
}
