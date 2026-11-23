package com.fund.valuation.dto;

import java.math.BigDecimal;

public record VipOrderNotifyRequest(
        String planKey,
        String planName,
        BigDecimal amount,
        String payMethod
) {
}
