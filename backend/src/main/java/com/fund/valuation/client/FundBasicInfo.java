package com.fund.valuation.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FundBasicInfo(String code, String name, String typeRaw, BigDecimal prevNav, LocalDate navDate) {
}
