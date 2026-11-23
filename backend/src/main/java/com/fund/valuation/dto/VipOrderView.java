package com.fund.valuation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VipOrderView(
        Long id,
        String username,
        String planKey,
        String planName,
        BigDecimal amount,
        String payMethod,
        String status,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
}
