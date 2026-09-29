package com.fund.valuation.dto;

public record NoticeView(
        String message,
        String type,
        boolean closable,
        boolean custom
) {
}
