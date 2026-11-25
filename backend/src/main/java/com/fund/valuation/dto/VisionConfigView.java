package com.fund.valuation.dto;

public record VisionConfigView(
        Boolean enabled,
        String apiBaseUrl,
        String apiKey,
        String modelName,
        Integer dailyUserLimit,
        Boolean hasApiKey
) {
    public VisionConfigView(Boolean enabled, String apiBaseUrl, String apiKey, String modelName, Integer dailyUserLimit) {
        this(
                enabled,
                apiBaseUrl,
                apiKey,
                modelName,
                dailyUserLimit,
                apiKey != null && !apiKey.isBlank()
        );
    }
}
