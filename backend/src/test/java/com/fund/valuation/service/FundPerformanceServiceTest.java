package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fund.valuation.client.TianTianFundClient;
import com.fund.valuation.domain.EstimateHistory;
import com.fund.valuation.dto.FundPerformanceView;
import com.fund.valuation.dto.NavPointView;
import com.fund.valuation.dto.ValuationAccuracyView;
import com.fund.valuation.mapper.EstimateHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FundPerformanceServiceTest {

    private TianTianFundClient tianTianFundClient;
    private EstimateHistoryMapper estimateHistoryMapper;
    private FundPerformanceService service;

    @BeforeEach
    void setUp() {
        tianTianFundClient = Mockito.mock(TianTianFundClient.class);
        estimateHistoryMapper = Mockito.mock(EstimateHistoryMapper.class);
        service = new FundPerformanceService(tianTianFundClient, estimateHistoryMapper);
    }

    @Test
    void testMapToApiRange() {
        assertEquals("y", FundPerformanceService.mapToApiRange("1m"));
        assertEquals("y", FundPerformanceService.mapToApiRange("y"));
        assertEquals("3y", FundPerformanceService.mapToApiRange("3m"));
        assertEquals("6y", FundPerformanceService.mapToApiRange("6m"));
        assertEquals("6y", FundPerformanceService.mapToApiRange("6y"));
        assertEquals("n", FundPerformanceService.mapToApiRange("1y"));
        assertEquals("3n", FundPerformanceService.mapToApiRange("3y"));
        assertEquals("3n", FundPerformanceService.mapToApiRange("3n"));
        assertEquals("5n", FundPerformanceService.mapToApiRange("5y"));
        assertEquals("5n", FundPerformanceService.mapToApiRange("5n"));
        assertEquals("all", FundPerformanceService.mapToApiRange("all"));
        assertEquals("y", FundPerformanceService.mapToApiRange(null));
        assertEquals("y", FundPerformanceService.mapToApiRange("unknown"));
    }

    @Test
    void testCalculateAccuracy_withMatchingEstimates() {
        String code = "110022";
        List<NavPointView> navPoints = List.of(
                new NavPointView("2026-10-20", new BigDecimal("3.00"), new BigDecimal("3.00"), new BigDecimal("1.00"), new BigDecimal("0.00")),
                new NavPointView("2026-10-21", new BigDecimal("3.03"), new BigDecimal("3.03"), new BigDecimal("1.00"), new BigDecimal("1.00")),
                new NavPointView("2026-10-22", new BigDecimal("3.00"), new BigDecimal("3.00"), new BigDecimal("-1.00"), new BigDecimal("0.00"))
        );

        // 模拟本地历史估值点:
        // 2026-10-20 预估 1.10% (实际 1.00%, diff = +0.10, 同向)
        // 2026-10-21 预估 0.80% (实际 1.00%, diff = -0.20, 同向)
        // 2026-10-22 预估 0.50% (实际 -1.00%, diff = +1.50, 反向)
        EstimateHistory h1 = new EstimateHistory();
        h1.setFundCode(code);
        h1.setEstTime(LocalDateTime.of(2026, 10, 20, 14, 55));
        h1.setEstPct(new BigDecimal("1.10"));

        EstimateHistory h2 = new EstimateHistory();
        h2.setFundCode(code);
        h2.setEstTime(LocalDateTime.of(2026, 10, 21, 14, 58));
        h2.setEstPct(new BigDecimal("0.80"));

        EstimateHistory h3 = new EstimateHistory();
        h3.setFundCode(code);
        h3.setEstTime(LocalDateTime.of(2026, 10, 22, 14, 59));
        h3.setEstPct(new BigDecimal("0.50"));

        when(estimateHistoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(h1, h2, h3));

        ValuationAccuracyView accuracy = service.calculateAccuracy(code, navPoints);
        assertNotNull(accuracy);
        assertTrue(accuracy.hasData());
        assertEquals(3, accuracy.sampleCount());
        assertEquals(3, accuracy.points().size());

        // MAE = (|0.10| + |-0.20| + |1.50|) / 3 = 1.80 / 3 = 0.6000
        assertEquals(new BigDecimal("0.6000"), accuracy.mae());

        // 方向吻合 2/3 = 66.67%
        assertEquals(new BigDecimal("66.67"), accuracy.directionMatchRate());
    }

    @Test
    void testCalculateAccuracy_noLocalHistory() {
        String code = "110022";
        List<NavPointView> navPoints = List.of(
                new NavPointView("2026-10-20", new BigDecimal("3.00"), new BigDecimal("3.00"), new BigDecimal("1.00"), new BigDecimal("0.00"))
        );
        when(estimateHistoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        ValuationAccuracyView accuracy = service.calculateAccuracy(code, navPoints);
        assertNotNull(accuracy);
        assertFalse(accuracy.hasData());
        assertEquals(0, accuracy.sampleCount());
        assertTrue(accuracy.points().isEmpty());
    }

    @Test
    void testGetPerformanceTrend_integration() {
        String code = "110022";
        List<NavPointView> navPoints = List.of(
                new NavPointView("2026-10-20", new BigDecimal("3.00"), new BigDecimal("3.00"), new BigDecimal("0.00"), new BigDecimal("0.00")),
                new NavPointView("2026-10-21", new BigDecimal("3.06"), new BigDecimal("3.06"), new BigDecimal("2.00"), new BigDecimal("2.00"))
        );
        when(tianTianFundClient.fetchPerformanceTrend(code, "y")).thenReturn(navPoints);
        when(estimateHistoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        FundPerformanceView view = service.getPerformanceTrend(code, "1m");
        assertNotNull(view);
        assertEquals(code, view.fundCode());
        assertEquals("1m", view.range());
        assertEquals(new BigDecimal("2.00"), view.totalReturnPct());
        assertEquals(2, view.navPoints().size());
        assertFalse(view.accuracy().hasData());
    }
}
