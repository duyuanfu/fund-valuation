package com.fund.valuation.dto;

import java.math.BigDecimal;

/**
 * 基金历史净值与收益率数据点。
 */
public record NavPointView(
        String date,               // yyyy-MM-dd
        BigDecimal nav,            // 单位净值
        BigDecimal cumulativeNav,  // 累计净值
        BigDecimal dailyGrowthPct, // 当日涨跌幅(%)
        BigDecimal accumReturnPct  // 区间累计收益率(%)
) {
}
