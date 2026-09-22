package com.fund.valuation.service;

import com.fund.valuation.client.Quote;
import com.fund.valuation.common.FundType;
import com.fund.valuation.common.SecCodeConverter;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.FundAssetAlloc;
import com.fund.valuation.domain.FundHolding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 估值引擎:按基金类型分派模型,输出估算净值与估算涨跌幅。
 * 估算净值 = 昨日净值 × (1 + 估算涨跌幅)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValuationEngine {

    public static final String DISCLAIMER = "估值仅供参考,不构成投资建议";

    private final FundHoldingService holdingService;
    private final QuoteService quoteService;
    private final BondEstimator bondEstimator;
    private final AppProperties properties;

    /**
     * 估算单只基金,无净值基准时返回 null。
     */
    public EstimateResult estimate(Fund fund) {
        if (fund.getPrevNav() == null) {
            return null;
        }
        FundType type = FundType.fromCode(fund.getType()).orElse(FundType.OTHER);

        // 1. 场内基金校验: 属于交易所场内号段且非场外联接时，直接拉取二级市场真实秒级成交行情
        // 不单独设置"etf"分类，保持基金真实的自然分类(如index/bond)，保证分类严谨统一
        if (SecCodeConverter.isOnMarket(fund.getCode(), fund.getName())) {
            String onMarketSecid = SecCodeConverter.toSecid(fund.getCode());
            Quote q = onMarketSecid == null ? null : quoteService.getFreshQuote(onMarketSecid);
            if (q != null && q.pctChg() != null) {
                BigDecimal pct = q.pctChg();
                BigDecimal estNav = q.price() != null ? q.price() : fund.getPrevNav();
                return new EstimateResult(
                        fund.getCode(), fund.getName(), type.getCode(),
                        estNav, pct.setScale(4, RoundingMode.HALF_UP),
                        fund.getPrevNav(), fund.getNavDate(),
                        holdingService.latestReportQt(fund.getCode()),
                        q.quoteTs(), false, null, "场内实时交易行情");
            }
        }

        // 2. 场外基金校验: 严格按照系统资产类别算法模型估算
        List<FundHolding> holdings = holdingService.getHoldings(fund.getCode());
        FundAssetAlloc alloc = holdingService.latestAlloc(fund.getCode());
        String reportQt = holdingService.latestReportQt(fund.getCode());

        BigDecimal pct;
        boolean stale;
        String message = null;
        LocalDateTime quoteTs = LocalDateTime.now();
        switch (type) {
            case INDEX -> {
                var r = indexStrategy(fund, holdings);
                pct = r.pct();
                stale = r.stale();
                message = r.message();
                quoteTs = r.quoteTs();
            }
            case ENHANCED -> {
                var r = enhancedStrategy(fund, holdings);
                pct = r.pct();
                stale = r.stale();
                message = r.message();
                quoteTs = r.quoteTs();
            }
            case MIXED -> {
                var r = mixedStrategy(holdings, alloc);
                pct = r.pct();
                stale = r.stale();
                message = r.message();
                quoteTs = r.quoteTs();
            }
            case BOND -> {
                var r = bondStrategy(fund, holdings);
                pct = r.pct();
                stale = r.stale();
                message = r.message();
                quoteTs = r.quoteTs();
            }
            case ACTIVE -> {
                var r = holdingsStrategy(holdings, false);
                pct = r.pct();
                stale = r.stale();
                quoteTs = r.quoteTs();
            }
            default -> {
                pct = BigDecimal.ZERO;
                stale = true;
                message = "未支持类型,估算为0";
            }
        }

        BigDecimal estNav = fund.getPrevNav().multiply(BigDecimal.ONE.add(
                pct.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)))
                .setScale(4, RoundingMode.HALF_UP);
        return new EstimateResult(
                fund.getCode(), fund.getName(), type.getCode(),
                estNav, pct.setScale(4, RoundingMode.HALF_UP),
                fund.getPrevNav(), fund.getNavDate(),
                reportQt, quoteTs, stale, message, DISCLAIMER);
    }

    /**
     * 指数型:估算涨跌幅 = 跟踪指数涨跌幅。
     */
    private Result indexStrategy(Fund fund, List<FundHolding> holdings) {
        if (fund.getTrackIndex() == null) {
            log.warn("fund {} index type but no track index, fallback to holdings", fund.getCode());
            return holdingsStrategy(holdings, true);
        }
        Quote q = quoteService.getFreshQuote(fund.getTrackIndex());
        if (q == null) {
            // 指数行情暂时缺失时，自动平滑回退至重仓持仓估测兜底
            if (holdings != null && !holdings.isEmpty()) {
                Result h = holdingsStrategy(holdings, false);
                if (!h.stale()) {
                    return h;
                }
            }
            return new Result(BigDecimal.ZERO, true, "跟踪指数行情缺失", LocalDateTime.now());
        }
        return new Result(q.pctChg(), false, null, q.quoteTs());
    }

    /**
     * 指数增强:指数涨跌幅为主体(0.8),结合重仓股加权修正(0.2)。
     */
    private Result enhancedStrategy(Fund fund, List<FundHolding> holdings) {
        BigDecimal indexPct = BigDecimal.ZERO;
        boolean stale = false;
        LocalDateTime quoteTs = LocalDateTime.now();
        String message = null;
        if (fund.getTrackIndex() != null) {
            Quote q = quoteService.getFreshQuote(fund.getTrackIndex());
            if (q != null) {
                indexPct = q.pctChg();
                quoteTs = q.quoteTs();
            } else {
                stale = true;
                message = "跟踪指数行情缺失";
            }
        } else {
            stale = true;
            message = "无跟踪指数";
        }
        Result h = holdingsStrategy(holdings, false);
        BigDecimal pct = indexPct.multiply(BigDecimal.valueOf(0.8))
                .add(h.pct().multiply(BigDecimal.valueOf(0.2)))
                .setScale(4, RoundingMode.HALF_UP);
        quoteTs = h.quoteTs().isAfter(quoteTs) ? h.quoteTs() : quoteTs;
        return new Result(pct, stale || h.stale(), message != null ? message : h.message(), quoteTs);
    }

    /**
     * 主动股票型:Σ(重仓股权重% × 涨跌幅%)/100,未披露仓位按0。
     */
    private Result holdingsStrategy(List<FundHolding> holdings, boolean markMissing) {
        BigDecimal sum = BigDecimal.ZERO;
        int used = 0;
        LocalDateTime latest = LocalDateTime.now();
        for (FundHolding h : holdings) {
            // 防御性安全过滤: 任何权重超过 100% 或为负的异常脏数据直接排除
            if (h.getWeight() == null || h.getWeight().compareTo(BigDecimal.valueOf(100)) > 0
                    || h.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String secid = SecCodeConverter.toSecid(h.getStockCode());
            Quote q = secid == null ? null : quoteService.getFreshQuote(secid);
            if (q == null) {
                continue;
            }
            sum = sum.add(h.getWeight().multiply(q.pctChg()));
            used++;
            if (q.quoteTs().isAfter(latest)) {
                latest = q.quoteTs();
            }
        }
        BigDecimal pct = sum.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        boolean stale = used < holdings.size();
        String message = markMissing && holdings.isEmpty() ? "无持仓数据" : null;
        return new Result(pct, stale, message, latest);
    }

    /**
     * 混合型:分段加权(股票段 + 债券段 + 现金段)。
     * 重仓股权重为占净值比例,其加权和即对净值的贡献;债券段/现金段参考级按 0 近似。
     */
    private Result mixedStrategy(List<FundHolding> holdings, FundAssetAlloc alloc) {
        Result h = holdingsStrategy(holdings, false);
        boolean stale = h.stale() || alloc == null;
        String message = alloc == null ? "资产配置缺失,按100%股票处理" : null;
        return new Result(h.pct(), stale, message, h.quoteTs());
    }

    /**
     * 债券型:重仓可转债实时行情 + 利率债"久期×收益率指数"驱动。
     * 纯债基金(无持仓)由利率驱动,不再恒等于昨日净值。
     */
    private Result bondStrategy(Fund fund, List<FundHolding> holdings) {
        BigDecimal sum = BigDecimal.ZERO;
        int total = holdings.size();
        int used = 0;
        LocalDateTime latest = LocalDateTime.now();
        for (FundHolding h : holdings) {
            String secid = SecCodeConverter.toSecid(h.getStockCode());
            Quote q = secid == null ? null : quoteService.getFreshQuote(secid);
            if (q == null) {
                continue;
            }
            sum = sum.add(h.getWeight().multiply(q.pctChg()));
            used++;
            if (q.quoteTs().isAfter(latest)) {
                latest = q.quoteTs();
            }
        }
        BigDecimal pct = sum.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal rateSleeve = bondEstimator.estimateRateSleeve(fund.getCode(), fund.getName(), fund.getTypeRaw());
        boolean rateMissing = rateSleeve == null;
        String message = null;
        if (rateMissing) {
            rateSleeve = BigDecimal.ZERO;
            message = "利率数据缺失,按0近似";
        }
        pct = pct.add(rateSleeve).setScale(4, RoundingMode.HALF_UP);
        boolean stale = used < total || rateMissing;
        return new Result(pct, stale, message, latest);
    }

    /**
     * 估值所需的全部行情 secid(供行情采集去重)。
     */
    public java.util.Set<String> collectRequiredSecids(Fund fund) {
        java.util.Set<String> secids = new java.util.HashSet<>();
        FundType type = FundType.fromCode(fund.getType()).orElse(FundType.OTHER);
        if ((type == FundType.INDEX || type == FundType.ENHANCED) && fund.getTrackIndex() != null) {
            secids.add(fund.getTrackIndex());
        }
        if (type != FundType.INDEX) {
            for (FundHolding h : holdingService.getHoldings(fund.getCode())) {
                String secid = SecCodeConverter.toSecid(h.getStockCode());
                if (secid != null) {
                    secids.add(secid);
                }
            }
        }
        if (type == FundType.BOND) {
            secids.add(properties.getBond().getIndexSecid());
            secids.add("1.000012");
        }
        // 场内基金: 采集该基金本身的交易所二级市场行情
        if (SecCodeConverter.isOnMarket(fund.getCode(), fund.getName())) {
            String onMarketSecid = SecCodeConverter.toSecid(fund.getCode());
            if (onMarketSecid != null) {
                secids.add(onMarketSecid);
            }
        }
        return secids;
    }

    private record Result(BigDecimal pct, boolean stale, String message, LocalDateTime quoteTs) {
    }
}
