package com.fund.valuation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PositionSaveRequest(
        @NotBlank(message = "基金代码不能为空")
        String fundCode,
        @NotNull(message = "持有金额不能为空")
        @Positive(message = "持有金额必须大于0")
        BigDecimal holdingAmount,
        BigDecimal yesterdayIncome,
        BigDecimal holdingProfit,
        BigDecimal holdingProfitRate
) {
}
