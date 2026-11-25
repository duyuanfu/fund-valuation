package com.fund.valuation.dto;

import com.fund.valuation.client.TianTianFundClient.FundSearchItem;

import java.math.BigDecimal;
import java.util.List;

public record OcrFundItem(
        String fundName,
        String fundCode,
        BigDecimal holdingAmount,
        BigDecimal yesterdayIncome,
        BigDecimal holdingProfit,
        BigDecimal holdingProfitRate,
        List<FundSearchItem> candidates,
        String warning
) {
    public OcrFundItem(
            String fundName,
            String fundCode,
            BigDecimal holdingAmount,
            BigDecimal yesterdayIncome,
            BigDecimal holdingProfit,
            BigDecimal holdingProfitRate
    ) {
        this(fundName, fundCode, holdingAmount, yesterdayIncome, holdingProfit, holdingProfitRate, List.of(), null);
    }

    public OcrFundItem withCandidates(List<FundSearchItem> candidates) {
        String selectedCode = this.fundCode();
        if ((selectedCode == null || selectedCode.isBlank()) && candidates != null && !candidates.isEmpty()) {
            selectedCode = candidates.get(0).code();
        }
        String warn = this.warning();
        if (selectedCode == null || selectedCode.isBlank()) {
            warn = "未自动匹配到6位基金代码，请手动输入或选择";
        }
        return new OcrFundItem(
                this.fundName(),
                selectedCode,
                this.holdingAmount(),
                this.yesterdayIncome(),
                this.holdingProfit(),
                this.holdingProfitRate(),
                candidates != null ? candidates : List.of(),
                warn
        );
    }
}
