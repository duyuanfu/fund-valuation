package com.fund.valuation.dto;

import java.math.BigDecimal;

/**
 * 单日模型估算与官方真实涨跌幅对比点。
 */
public record AccuracyPointView(
        String date,              // yyyy-MM-dd
        BigDecimal actualPct,     // 官方确认的实际涨跌幅(%)
        BigDecimal estPct,        // 盘中收盘前最终预估涨跌幅(%)
        BigDecimal diffPct,       // 偏差值: estPct - actualPct (%)
        boolean directionMatched  // 涨跌方向是否一致
) {
}
