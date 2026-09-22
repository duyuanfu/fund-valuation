package com.fund.valuation.service;

import com.fund.valuation.client.Quote;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.FundAssetAlloc;
import com.fund.valuation.domain.FundHolding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValuationEngineTest {

    @Mock
    private FundHoldingService holdingService;
    @Mock
    private QuoteService quoteService;
    @Mock
    private BondEstimator bondEstimator;

    private AppProperties properties;
    private ValuationEngine engine;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        engine = new ValuationEngine(holdingService, quoteService, bondEstimator, properties);
    }

    private Fund fund(String type, BigDecimal prevNav) {
        Fund f = new Fund();
        f.setCode("110022");
        f.setName("测试基金");
        f.setType(type);
        f.setPrevNav(prevNav);
        f.setNavDate(LocalDateTime.now().toLocalDate());
        return f;
    }

    private FundHolding holding(String code, String name, String weight) {
        FundHolding h = new FundHolding();
        h.setFundCode("110022");
        h.setStockCode(code);
        h.setStockName(name);
        h.setWeight(new BigDecimal(weight));
        h.setReportQt("2026-06-30");
        return h;
    }

    private Quote quote(String secid, String pct) {
        return new Quote(secid, "x", BigDecimal.ONE, new BigDecimal(pct), LocalDateTime.now(), "test");
    }

    @Test
    void missingPrevNavReturnsNull() {
        Fund f = fund("active", null);
        assertNull(engine.estimate(f));
    }

    @Test
    void indexFundUsesTrackIndexPct() {
        Fund f = fund("index", new BigDecimal("1.5000"));
        f.setTrackIndex("1.000300");
        when(quoteService.getFreshQuote("1.000300")).thenReturn(quote("1.000300", "1.50"));
        when(holdingService.getHoldings("110022")).thenReturn(List.of());
        when(holdingService.latestReportQt("110022")).thenReturn(null);

        EstimateResult r = engine.estimate(f);
        assertNotNull(r);
        assertEquals(0, new BigDecimal("1.50").compareTo(r.estimatePct()));
        assertEquals(0, new BigDecimal("1.5225").compareTo(r.estimateNav()));
    }

    @Test
    void indexFundWithoutTrackIndexFallsBackToHoldings() {
        Fund f = fund("index", new BigDecimal("2.0000"));
        when(holdingService.getHoldings("110022")).thenReturn(List.of(holding("600519", "贵州茅台", "50")));
        when(holdingService.latestReportQt("110022")).thenReturn("2026-06-30");
        when(quoteService.getFreshQuote("1.600519")).thenReturn(quote("1.600519", "2.00"));

        EstimateResult r = engine.estimate(f);
        assertNotNull(r);
        assertEquals(0, new BigDecimal("1.00").compareTo(r.estimatePct()));
        assertEquals(0, new BigDecimal("2.0200").compareTo(r.estimateNav()));
    }

    @Test
    void activeFundWeightedPct() {
        Fund f = fund("active", new BigDecimal("1.0000"));
        when(holdingService.getHoldings("110022")).thenReturn(List.of(
                holding("600519", "贵州茅台", "50"),
                holding("000333", "美的集团", "30")));
        when(holdingService.latestReportQt("110022")).thenReturn("2026-06-30");
        when(quoteService.getFreshQuote("1.600519")).thenReturn(quote("1.600519", "2.00"));
        when(quoteService.getFreshQuote("0.000333")).thenReturn(quote("0.000333", "-1.00"));

        EstimateResult r = engine.estimate(f);
        assertNotNull(r);
        // (50*2 + 30*(-1))/100 = 0.7
        assertEquals(0, new BigDecimal("0.7000").compareTo(r.estimatePct()));
        assertEquals(0, new BigDecimal("1.0070").compareTo(r.estimateNav()));
    }

    @Test
    void mixedFundUsesHoldingsWithAllocFlag() {
        Fund f = fund("mixed", new BigDecimal("1.0000"));
        when(holdingService.getHoldings("110022")).thenReturn(List.of(holding("600519", "贵州茅台", "40")));
        when(holdingService.latestReportQt("110022")).thenReturn("2026-06-30");
        FundAssetAlloc alloc = new FundAssetAlloc();
        alloc.setStockPct(new BigDecimal("80"));
        alloc.setBondPct(new BigDecimal("10"));
        alloc.setCashPct(new BigDecimal("10"));
        when(holdingService.latestAlloc("110022")).thenReturn(alloc);
        when(quoteService.getFreshQuote("1.600519")).thenReturn(quote("1.600519", "2.50"));

        EstimateResult r = engine.estimate(f);
        assertNotNull(r);
        assertEquals(0, new BigDecimal("1.0000").compareTo(r.estimatePct()));
    }

    @Test
    void bondFundUsesConvertibleBondRealtime() {
        Fund f = fund("bond", new BigDecimal("1.5000"));
        when(holdingService.getHoldings("110022")).thenReturn(List.of(holding("113050", "龙净转债", "30")));
        when(holdingService.latestReportQt("110022")).thenReturn("2026-06-30");
        when(quoteService.getFreshQuote("1.113050")).thenReturn(quote("1.113050", "-1.00"));
        when(bondEstimator.estimateRateSleeve(anyString(), anyString(), any())).thenReturn(BigDecimal.ZERO);

        EstimateResult r = engine.estimate(f);
        assertNotNull(r);
        // 30*(-1)/100 = -0.3
        assertEquals(0, new BigDecimal("-0.3000").compareTo(r.estimatePct()));
        assertEquals(0, new BigDecimal("1.4955").compareTo(r.estimateNav()));
    }

    @Test
    void pureBondFundDrivenByRateSleeve() {
        Fund f = fund("bond", new BigDecimal("1.5000"));
        when(holdingService.getHoldings("110022")).thenReturn(List.of());
        when(holdingService.latestReportQt("110022")).thenReturn("2026-06-30");
        // 久期2.0 / 参考久期10 × 收益率指数涨跌1.60% = 0.32%
        when(bondEstimator.estimateRateSleeve(anyString(), anyString(), any())).thenReturn(new BigDecimal("0.3200"));

        EstimateResult r = engine.estimate(f);
        assertNotNull(r);
        assertEquals(0, new BigDecimal("0.3200").compareTo(r.estimatePct()));
        assertEquals(0, new BigDecimal("1.5048").compareTo(r.estimateNav()));
        assertEquals(false, r.stale());
    }

    @Test
    void pureBondFundRateMissingDegradesToZero() {
        Fund f = fund("bond", new BigDecimal("1.5000"));
        when(holdingService.getHoldings("110022")).thenReturn(List.of());
        when(holdingService.latestReportQt("110022")).thenReturn("2026-06-30");
        when(bondEstimator.estimateRateSleeve(anyString(), anyString(), any())).thenReturn(null);

        EstimateResult r = engine.estimate(f);
        assertNotNull(r);
        assertEquals(0, new BigDecimal("0.0000").compareTo(r.estimatePct()));
        assertEquals(true, r.stale());
        assertEquals("利率数据缺失,按0近似", r.message());
    }

    @Test
    void enhancedFundBlendsIndexAndHoldings() {
        Fund f = fund("enhanced", new BigDecimal("1.0000"));
        f.setTrackIndex("1.000905");
        when(holdingService.getHoldings("110022")).thenReturn(List.of(holding("600519", "贵州茅台", "50")));
        when(holdingService.latestReportQt("110022")).thenReturn("2026-06-30");
        when(quoteService.getFreshQuote("1.000905")).thenReturn(quote("1.000905", "1.00"));
        when(quoteService.getFreshQuote("1.600519")).thenReturn(quote("1.600519", "2.00"));

        EstimateResult r = engine.estimate(f);
        assertNotNull(r);
        // 0.8*1.0 + 0.2*1.0 = 1.0
        assertEquals(0, new BigDecimal("1.0000").compareTo(r.estimatePct()));
    }

    @Test
    void missingQuoteMarksStale() {
        Fund f = fund("active", new BigDecimal("1.0000"));
        when(holdingService.getHoldings("110022")).thenReturn(List.of(
                holding("600519", "贵州茅台", "50"),
                holding("000333", "美的集团", "30")));
        when(holdingService.latestReportQt("110022")).thenReturn("2026-06-30");
        when(quoteService.getFreshQuote("1.600519")).thenReturn(quote("1.600519", "2.00"));
        when(quoteService.getFreshQuote("0.000333")).thenReturn(null);

        EstimateResult r = engine.estimate(f);
        assertNotNull(r);
        assertEquals(true, r.stale());
        assertEquals(0, new BigDecimal("1.0000").compareTo(r.estimatePct()));
    }
}