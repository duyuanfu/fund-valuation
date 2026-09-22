package com.fund.valuation.service;

import com.fund.valuation.client.Quote;
import com.fund.valuation.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 纯债(利率债)估值模型:
 * 利率债估算涨跌幅(%) = 收益率指数涨跌幅 × (组合久期 / 参考久期)
 */
@Component
@RequiredArgsConstructor
public class BondEstimator {

    private final QuoteService quoteService;
    private final BondDurationService durationService;
    private final AppProperties properties;

    /**
     * 估算利率债部分的当日涨跌幅。返回 null 表示利率数据缺失(应降级为 0)。
     */
    public BigDecimal estimateRateSleeve(String fundCode, String fundName, String typeRaw) {
        String indexSecid = properties.getBond().getIndexSecid();
        Quote q = quoteService.getFreshQuote(indexSecid);
        if (q == null) {
            // 东财 103.TY00Y 若由于网络限制获取不到，自动平滑降级至国债指数(1.000012)
            q = quoteService.getFreshQuote("1.000012");
        }
        if (q == null) {
            return null;
        }
        double duration = durationService.getDuration(fundCode, fundName, typeRaw);
        double reference = properties.getBond().getReferenceDuration();
        if (reference <= 0) {
            reference = 10.0;
        }
        return q.pctChg()
                .multiply(BigDecimal.valueOf(duration / reference))
                .setScale(4, RoundingMode.HALF_UP);
    }
}