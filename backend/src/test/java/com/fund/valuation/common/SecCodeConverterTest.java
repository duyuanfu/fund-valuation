package com.fund.valuation.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecCodeConverterTest {

    @Test
    void shStockToSecid() {
        assertEquals("1.600519", SecCodeConverter.toSecid("600519"));
        assertEquals("1.600519", SecCodeConverter.toSecid("SH600519"));
        assertEquals("1.600519", SecCodeConverter.toSecid("sh600519"));
    }

    @Test
    void szStockToSecid() {
        assertEquals("0.000001", SecCodeConverter.toSecid("000001"));
        assertEquals("0.000333", SecCodeConverter.toSecid("SZ000333"));
        assertEquals("0.300750", SecCodeConverter.toSecid("300750"));
    }

    @Test
    void indexAndBondToSecid() {
        // 000xxx/300xxx 等持仓代码按深市股票处理;指数使用完整 secid(如 1.000300)不经过本转换
        assertEquals("0.000300", SecCodeConverter.toSecid("000300"));
        assertEquals("0.399006", SecCodeConverter.toSecid("399006"));
        assertEquals("1.113050", SecCodeConverter.toSecid("113050"));
        assertEquals("0.123775", SecCodeConverter.toSecid("123775"));
    }

    @Test
    void invalidInput() {
        assertNull(SecCodeConverter.toSecid(null));
    }

    @Test
    void onMarketFundValidation() {
        // 场内基金 (ETF / LOF)
        org.junit.jupiter.api.Assertions.assertTrue(SecCodeConverter.isOnMarket("510300", "华泰柏瑞沪深300ETF"));
        org.junit.jupiter.api.Assertions.assertTrue(SecCodeConverter.isOnMarket("159919", "嘉实沪深300ETF"));
        org.junit.jupiter.api.Assertions.assertTrue(SecCodeConverter.isOnMarket("161725", "招商中证白酒指数(LOF)"));
        org.junit.jupiter.api.Assertions.assertTrue(SecCodeConverter.isOnMarket("511010", "国债ETF"));

        // 场外基金 (即使名称包含 ETF 关键字，但含有"联接"或是场外号段，一律为场外基金)
        org.junit.jupiter.api.Assertions.assertFalse(SecCodeConverter.isOnMarket("008888", "国泰国证半导体芯片ETF联接C"));
        org.junit.jupiter.api.Assertions.assertFalse(SecCodeConverter.isOnMarket("001594", "天弘中证500ETF联接A"));
        org.junit.jupiter.api.Assertions.assertFalse(SecCodeConverter.isOnMarket("110022", "易方达消费行业股票"));
        org.junit.jupiter.api.Assertions.assertFalse(SecCodeConverter.isOnMarket("008559", "永赢邦利债券C"));
    }
}