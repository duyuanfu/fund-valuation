package com.fund.valuation.common;

import java.util.Arrays;
import java.util.Optional;

public enum FundType {
    INDEX("index"),
    ENHANCED("enhanced"),
    ACTIVE("active"),
    MIXED("mixed"),
    BOND("bond"),
    OTHER("other");

    private final String code;

    FundType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<FundType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equals(code))
                .findFirst();
    }
}
