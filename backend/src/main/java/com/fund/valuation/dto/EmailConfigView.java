package com.fund.valuation.dto;

public record EmailConfigView(
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        String smtpPassword,
        String senderName,
        String senderAddress,
        Boolean sslEnabled
) {
}
