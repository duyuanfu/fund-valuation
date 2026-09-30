package com.fund.valuation.service;

import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.UserPosition;
import com.fund.valuation.dto.PortfolioView;
import com.fund.valuation.dto.PositionSaveRequest;
import com.fund.valuation.mapper.FundMapper;
import com.fund.valuation.mapper.UserPositionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class PortfolioServiceTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserPositionMapper positionMapper;

    @Autowired
    private FundMapper fundMapper;

    @MockBean
    private FundCatalogService fundCatalogService;

    @MockBean
    private FundHoldingService fundHoldingService;

    @MockBean
    private ValuationEngine valuationEngine;

    @MockBean
    private QuoteService quoteService;

    private static final String USER = "vip_trader";

    @BeforeEach
    void setUp() {
        positionMapper.delete(null);
        fundMapper.delete(null);

        Fund f1 = new Fund();
        f1.setCode("110022");
        f1.setName("易方达消费行业股票");
        f1.setType("active");
        f1.setPrevNav(new BigDecimal("2.5000"));
        f1.setNavDate(LocalDate.now().minusDays(1));
        f1.setUpdatedAt(LocalDateTime.now());
        fundMapper.insert(f1);

        when(fundCatalogService.ensureFund(anyString())).thenAnswer(inv -> {
            Fund f = fundMapper.selectById((String) inv.getArgument(0));
            return f != null ? f : f1;
        });
        when(fundCatalogService.get(anyString())).thenAnswer(inv -> fundMapper.selectById((String) inv.getArgument(0)));
    }

    @Test
    void savePositionWithProfitCalculatesRateAndCost() {
        PositionSaveRequest req = new PositionSaveRequest(
                "110022",
                new BigDecimal("10000.00"),
                new BigDecimal("50.00"),
                new BigDecimal("1000.00"),
                null
        );

        UserPosition pos = portfolioService.savePosition(USER, req);
        assertNotNull(pos.getId());
        assertEquals(new BigDecimal("10000.00"), pos.getHoldingAmount());
        assertEquals(new BigDecimal("50.00"), pos.getYesterdayIncome());
        assertEquals(new BigDecimal("1000.00"), pos.getHoldingProfit());
        assertEquals(new BigDecimal("9000.00"), pos.getCostAmount());
        // 1000 / 9000 * 100 = 11.1111%
        assertEquals(new BigDecimal("11.1111"), pos.getHoldingProfitRate());
        // shares = 10000 / 2.5 = 4000
        assertEquals(new BigDecimal("4000.0000"), pos.getHoldingShares());
    }

    @Test
    void getPortfolioAggregatesTodayIncomeAccurately() {
        PositionSaveRequest req = new PositionSaveRequest(
                "110022",
                new BigDecimal("10000.00"),
                new BigDecimal("80.00"),
                new BigDecimal("500.00"),
                null
        );
        portfolioService.savePosition(USER, req);

        // 模拟估值：涨幅 +2.00%
        EstimateResult mockEst = new EstimateResult(
                "110022",
                "易方达消费行业股票",
                "active",
                new BigDecimal("2.5500"),
                new BigDecimal("2.00"),
                new BigDecimal("2.5000"),
                LocalDate.now().minusDays(1),
                "2026-06-30",
                LocalDateTime.now(),
                false,
                null,
                "免责声明"
        );
        when(valuationEngine.estimate(any(Fund.class))).thenReturn(mockEst);

        PortfolioView view = portfolioService.getPortfolio(USER);
        assertNotNull(view);
        assertEquals(1, view.items().size());

        // 今日预估收益: 10000 * 2% = 200.00
        assertEquals(new BigDecimal("200.00"), view.items().get(0).todayIncome());
        // 动态市值: 10000 + 200 = 10200.00
        assertEquals(new BigDecimal("10200.00"), view.items().get(0).dynamicMarketValue());
        // 动态最新总盈亏: 500 + 200 = 700.00
        assertEquals(new BigDecimal("700.00"), view.items().get(0).dynamicTotalProfit());

        // 汇总看板
        assertEquals(new BigDecimal("10200.00"), view.summary().totalMarketValue());
        assertEquals(new BigDecimal("200.00"), view.summary().totalTodayIncome());
        assertEquals(new BigDecimal("2.00"), view.summary().totalTodayIncomePct());
        assertEquals(new BigDecimal("700.00"), view.summary().dynamicTotalProfit());
    }

    @Test
    void removePositionDeletesSuccessfully() {
        PositionSaveRequest req = new PositionSaveRequest(
                "110022",
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        portfolioService.savePosition(USER, req);
        assertEquals(1, portfolioService.getPortfolio(USER).items().size());

        portfolioService.removePosition(USER, "110022");
        assertEquals(0, portfolioService.getPortfolio(USER).items().size());
    }
}
