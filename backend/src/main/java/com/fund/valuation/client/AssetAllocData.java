package com.fund.valuation.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetAllocData(LocalDate reportDate, BigDecimal stockPct, BigDecimal bondPct, BigDecimal cashPct) {
}
