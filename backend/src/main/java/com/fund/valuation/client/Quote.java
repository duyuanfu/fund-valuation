package com.fund.valuation.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Quote(String secid, String name, BigDecimal price, BigDecimal pctChg, LocalDateTime quoteTs, String source) {
}
