package com.fund.valuation.dto;

public record EmailConfigView(
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        String smtpPassword,
        String senderName,
        String senderAddress,
        Boolean sslEnabled,
        String wxpusherAppToken,
        String wxpusherQrUrl
) {
    public EmailConfigView(
            String smtpHost,
            Integer smtpPort,
            String smtpUsername,
            String smtpPassword,
            String senderName,
            String senderAddress,
            Boolean sslEnabled
    ) {
        this(smtpHost, smtpPort, smtpUsername, smtpPassword, senderName, senderAddress, sslEnabled, null, null);
    }
}
