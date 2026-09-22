package com.fund.valuation.service;

import com.fund.valuation.common.FundType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FundTypeClassifierTest {

    private final FundTypeClassifier classifier = new FundTypeClassifier();

    @Test
    void classifyByOfficialType() {
        assertEquals(FundType.ACTIVE, classifier.classify("股票型", "易方达消费行业股票"));
        assertEquals(FundType.MIXED, classifier.classify("混合型", "兴全合润混合"));
        assertEquals(FundType.BOND, classifier.classify("债券型", "易方达安心回报债券"));
        assertEquals(FundType.INDEX, classifier.classify("指数型-股票", "沪深300ETF"));
    }

    @Test
    void indexTypeWithEnhancedName() {
        assertEquals(FundType.ENHANCED, classifier.classify("指数型-股票", "中证500指数增强"));
    }

    @Test
    void fallbackByNameKeywords() {
        assertEquals(FundType.INDEX, classifier.classify("", "华泰柏瑞沪深300ETF"));
        assertEquals(FundType.ENHANCED, classifier.classify("", "xx沪深300指数增强"));
        assertEquals(FundType.INDEX, classifier.classify("", "天弘中证500指数A"));
        assertEquals(FundType.MIXED, classifier.classify("", "广发稳健增长混合"));
        assertEquals(FundType.OTHER, classifier.classify("", "某FOF基金"));
    }
}