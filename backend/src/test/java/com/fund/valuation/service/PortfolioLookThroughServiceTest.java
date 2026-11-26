package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.FundHolding;
import com.fund.valuation.domain.UserPosition;
import com.fund.valuation.dto.PortfolioLookThroughView;
import com.fund.valuation.dto.StockExposureItem;
import com.fund.valuation.mapper.UserPositionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioLookThroughServiceTest {

    @Mock
    private UserPositionMapper positionMapper;

    @Mock
    private FundCatalogService fundCatalogService;

    @Mock
    private FundHoldingService fundHoldingService;

    @InjectMocks
    private PortfolioLookThroughService lookThroughService;

    @Test
    void testCalculateLookThrough_EmptyPositions() {
        when(positionMapper.selectList(any())).thenReturn(List.of());

        PortfolioLookThroughView result = lookThroughService.calculateLookThrough("test_user");

        assertNotNull(result);
        assertEquals(0, result.totalFundCount());
        assertTrue(result.topStockExposures().isEmpty());
    }

    @Test
    void testCalculateLookThrough_MultiFundAggregation() {
        UserPosition pos1 = new UserPosition();
        pos1.setFundCode("110022");
        pos1.setHoldingAmount(BigDecimal.valueOf(10000.00));

        UserPosition pos2 = new UserPosition();
        pos2.setFundCode("000001");
        pos2.setHoldingAmount(BigDecimal.valueOf(10000.00));

        when(positionMapper.selectList(any())).thenReturn(List.of(pos1, pos2));

        Fund fund1 = new Fund();
        fund1.setCode("110022");
        fund1.setName("易方达消费行业");
        fund1.setType("ACTIVE");
        when(fundCatalogService.get("110022")).thenReturn(fund1);

        Fund fund2 = new Fund();
        fund2.setCode("000001");
        fund2.setName("华夏成长混合");
        fund2.setType("MIXED");
        when(fundCatalogService.get("000001")).thenReturn(fund2);

        // 基金1持有茅台 10%
        FundHolding h1 = new FundHolding();
        h1.setFundCode("110022");
        h1.setStockCode("600519");
        h1.setStockName("贵州茅台");
        h1.setWeight(BigDecimal.valueOf(10.0));
        h1.setReportQt("2026Q2");

        // 基金2也持有茅台 8%
        FundHolding h2 = new FundHolding();
        h2.setFundCode("000001");
        h2.setStockCode("600519");
        h2.setStockName("贵州茅台");
        h2.setWeight(BigDecimal.valueOf(8.0));
        h2.setReportQt("2026Q2");

        when(fundHoldingService.ensureHoldings(fund1)).thenReturn(List.of(h1));
        when(fundHoldingService.ensureHoldings(fund2)).thenReturn(List.of(h2));

        PortfolioLookThroughView view = lookThroughService.calculateLookThrough("test_user");

        assertNotNull(view);
        assertEquals(2, view.totalFundCount());
        assertEquals(BigDecimal.valueOf(20000.00).setScale(2), view.totalPortfolioAmount());

        assertFalse(view.topStockExposures().isEmpty());
        StockExposureItem topStock = view.topStockExposures().get(0);
        assertEquals("600519", topStock.stockCode());
        assertEquals("贵州茅台", topStock.stockName());
        // 10000 * 10% = 1000, 10000 * 8% = 800 => 1800
        assertEquals(BigDecimal.valueOf(1800.00).setScale(2), topStock.holdingAmount());
        // 1800 / 20000 = 9.00%
        assertEquals(BigDecimal.valueOf(9.00).setScale(2), topStock.portfolioRatio());
        assertEquals(2, topStock.contributors().size());
    }
}
