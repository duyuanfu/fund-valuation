package com.fund.valuation.dto;

import java.util.List;

public record ScreenshotImportResult(
        boolean success,
        String message,
        List<OcrFundItem> items,
        Integer remainingQuota
) {
}
