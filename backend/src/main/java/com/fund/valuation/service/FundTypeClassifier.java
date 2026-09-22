package com.fund.valuation.service;

import com.fund.valuation.common.FundType;
import org.springframework.stereotype.Component;

/**
 * 基金类型识别:优先使用官方类型字段,名称关键词兜底。
 */
@Component
public class FundTypeClassifier {

    public FundType classify(String typeRaw, String name) {
        FundType t = byTypeRaw(typeRaw);
        if (t != null) {
            // 官方类型为指数型但名称含"增强" -> 指数增强
            if (t == FundType.INDEX && containsAny(name, "增强")) {
                return FundType.ENHANCED;
            }
            return t;
        }
        return byName(name);
    }

    private FundType byTypeRaw(String typeRaw) {
        if (typeRaw == null || typeRaw.isBlank()) {
            return null;
        }
        if (typeRaw.contains("指数")) {
            return FundType.INDEX;
        }
        if (typeRaw.contains("股票")) {
            return FundType.ACTIVE;
        }
        if (typeRaw.contains("混合")) {
            return FundType.MIXED;
        }
        if (typeRaw.contains("债券")) {
            return FundType.BOND;
        }
        return null;
    }

    private FundType byName(String name) {
        if (name == null || name.isBlank()) {
            return FundType.OTHER;
        }
        if (containsAny(name, "增强")) {
            return FundType.ENHANCED;
        }
        if (containsAny(name, "ETF", "LOF", "联接", "指数")) {
            return FundType.INDEX;
        }
        if (containsAny(name, "混合")) {
            return FundType.MIXED;
        }
        if (containsAny(name, "债券")) {
            return FundType.BOND;
        }
        if (containsAny(name, "股票")) {
            return FundType.ACTIVE;
        }
        return FundType.OTHER;
    }

    private boolean containsAny(String s, String... keywords) {
        for (String k : keywords) {
            if (s.contains(k)) {
                return true;
            }
        }
        return false;
    }
}
