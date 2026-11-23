package com.fund.valuation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 基金历史估值准确度度量与明细。
 */
public record ValuationAccuracyView(
        boolean hasData,               // 是否具有本地历史比对数据
        Integer sampleCount,           // 样本交易日天数
        BigDecimal mae,                // 平均绝对误差 Mean Absolute Error (%)
        BigDecimal directionMatchRate, // 涨跌方向吻合率 (0.0 ~ 100.0%)
        List<AccuracyPointView> points // 近期每日对比明细点(按日期升序)
) {
}
