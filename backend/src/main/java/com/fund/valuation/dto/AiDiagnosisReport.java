package com.fund.valuation.dto;

import java.util.List;

public record AiDiagnosisReport(
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> suggestions,
        String generatedAt
) {}
