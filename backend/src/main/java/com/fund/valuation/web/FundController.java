package com.fund.valuation.web;

import com.fund.valuation.client.Quote;
import com.fund.valuation.common.FundType;
import com.fund.valuation.common.SecCodeConverter;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.FundHolding;
import com.fund.valuation.dto.BondInfo;
import com.fund.valuation.dto.EstimatePointView;
import com.fund.valuation.dto.FundDetailView;
import com.fund.valuation.dto.HoldingView;
import com.fund.valuation.service.BondDurationService;
import com.fund.valuation.service.EstimateHistoryService;
import com.fund.valuation.service.EstimateResult;
import com.fund.valuation.service.FundCatalogService;
import com.fund.valuation.service.FundHoldingService;
import com.fund.valuation.service.QuoteService;
import com.fund.valuation.service.ValuationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/fund")
@RequiredArgsConstructor
public class FundController {

    private final FundCatalogService fundCatalogService;
    private final FundHoldingService holdingService;
    private final EstimateHistoryService historyService;
    private final ValuationEngine valuationEngine;
    private final QuoteService quoteService;
    private final BondDurationService bondDurationService;
    private final AppProperties properties;

    /**
     * 单只基金详情:基本信息 + 当前估值 + 持仓明细 + 当日历史(+债券基金利率驱动信息)。
     */
    @GetMapping("/{code}")
    public ResponseEntity<FundDetailView> detail(@PathVariable String code) {
        Fund fund = fundCatalogService.get(code);
        if (fund == null) {
            return ResponseEntity.notFound().build();
        }
        java.util.Set<String> required = valuationEngine.collectRequiredSecids(fund);
        if (required != null && !required.isEmpty()) {
            java.util.Set<String> missing = new java.util.HashSet<>();
            for (String secid : required) {
                if (quoteService.getFreshQuote(secid) == null) {
                    missing.add(secid);
                }
            }
            if (!missing.isEmpty()) {
                quoteService.refreshQuotes(missing);
            }
        }
        EstimateResult estimate = valuationEngine.estimate(fund);
        List<FundHolding> holdings = holdingService.getHoldings(code);
        String reportQt = holdingService.latestReportQt(code);
        List<HoldingView> holdingViews = holdings.stream().map(h -> {
            String secid = SecCodeConverter.toSecid(h.getStockCode());
            Quote q = secid == null ? null : quoteService.getFreshQuote(secid);
            return new HoldingView(h.getStockCode(), h.getStockName(), h.getWeight(),
                    q == null ? null : q.pctChg(), reportQt);
        }).toList();
        List<EstimatePointView> history = new java.util.ArrayList<>(historyService.todayHistory(code).stream()
                .map(h -> new EstimatePointView(h.getEstTime(), h.getEstNav(), h.getEstPct()))
                .toList());
        // 当本地定时任务记录少于 3 个点时(如新加基金、冷启动或非盘中初次查看)，优先拉取交易所权威真实分时线兜底
        if (history.size() < 3) {
            List<EstimatePointView> trends = quoteService.getIntradayTrends(fund);
            if (trends != null && !trends.isEmpty()) {
                history = new java.util.ArrayList<>(trends);
            }
        }
        BondInfo bondInfo = buildBondInfo(fund);
        return ResponseEntity.ok(new FundDetailView(estimate, holdingViews, history, bondInfo));
    }

    private BondInfo buildBondInfo(Fund fund) {
        if (fund.getType() == null || !FundType.BOND.getCode().equals(fund.getType())) {
            return null;
        }
        String indexSecid = properties.getBond().getIndexSecid();
        Quote q = quoteService.getFreshQuote(indexSecid);
        if (q == null) {
            q = quoteService.getFreshQuote("1.000012");
        }
        double duration = bondDurationService.getDuration(fund.getCode(), fund.getName(), fund.getTypeRaw());
        return new BondInfo(
                q == null ? "10年期国债收益率指数" : q.name(),
                q == null ? null : q.pctChg(),
                duration,
                properties.getBond().getReferenceDuration(),
                "估算涨跌幅 = 收益率指数涨跌 × (组合久期 / 参考久期)");
    }
}
