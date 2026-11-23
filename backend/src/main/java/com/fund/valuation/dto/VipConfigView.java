package com.fund.valuation.dto;

import java.math.BigDecimal;

public record VipConfigView(
        BigDecimal monthlyPrice,
        BigDecimal quarterlyPrice,
        BigDecimal quarterlyOrigPrice,
        BigDecimal yearlyPrice,
        BigDecimal yearlyOrigPrice,
        String wechatQrUrl,
        String alipayQrUrl,
        String payeeName,
        String paymentTip,
        Boolean requireApproval,
        String webhookUrl
) {
    public VipConfigView(
            BigDecimal monthlyPrice,
            BigDecimal quarterlyPrice,
            BigDecimal quarterlyOrigPrice,
            BigDecimal yearlyPrice,
            BigDecimal yearlyOrigPrice,
            String wechatQrUrl,
            String alipayQrUrl,
            String payeeName,
            String paymentTip,
            Boolean requireApproval
    ) {
        this(monthlyPrice, quarterlyPrice, quarterlyOrigPrice, yearlyPrice, yearlyOrigPrice,
                wechatQrUrl, alipayQrUrl, payeeName, paymentTip, requireApproval, null);
    }
}
