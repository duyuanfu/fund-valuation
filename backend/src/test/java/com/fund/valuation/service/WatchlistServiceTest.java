package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.UserFund;
import com.fund.valuation.mapper.FundMapper;
import com.fund.valuation.mapper.UserFundMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class WatchlistServiceTest {

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private FundMapper fundMapper;

    @Autowired
    private UserFundMapper userFundMapper;

    @MockBean
    private FundCatalogService fundCatalogService;

    @MockBean
    private FundHoldingService fundHoldingService;

    @MockBean
    private ValuationEngine valuationEngine;

    @BeforeEach
    void setUp() {
        userFundMapper.delete(null);
        fundMapper.delete(null);
        when(fundCatalogService.ensureFund(anyString())).thenAnswer(inv -> {
            Fund f = fundMapper.selectById(inv.getArgument(0));
            if (f == null) {
                f = new Fund();
                f.setCode(inv.getArgument(0));
                f.setName("测试基金");
                f.setType("active");
                f.setUpdatedAt(java.time.LocalDateTime.now());
                fundMapper.insert(f);
            }
            return f;
        });
        when(fundCatalogService.get(anyString())).thenAnswer(inv -> fundMapper.selectById(inv.getArgument(0)));
        when(valuationEngine.estimate(org.mockito.ArgumentMatchers.any(Fund.class))).thenReturn(
                new EstimateResult("110022", "测试基金", "active",
                        new BigDecimal("3.0300"), new BigDecimal("1.0000"),
                        new BigDecimal("3.0000"), LocalDate.of(2026, 8, 20),
                        "2026-06-30", java.time.LocalDateTime.now(), false, null, "估值仅供参考,不构成投资建议"));
    }

    @Test
    void addAndList() {
        watchlistService.add("u1", "110022");
        assertEquals(1, watchlistService.fundCodesOf("u1").size());
        assertEquals("110022", watchlistService.fundCodesOf("u1").get(0));
    }

    @Test
    void duplicateAddRejected() {
        watchlistService.add("u1", "110022");
        assertThrows(IllegalArgumentException.class, () -> watchlistService.add("u1", "110022"));
    }

    @Test
    void remove() {
        watchlistService.add("u1", "110022");
        watchlistService.remove("u1", "110022");
        assertTrue(watchlistService.fundCodesOf("u1").isEmpty());
    }

    @Test
    void multiUserIsolation() {
        watchlistService.add("u1", "110022");
        watchlistService.add("u2", "110022");
        watchlistService.remove("u1", "110022");
        assertTrue(watchlistService.fundCodesOf("u1").isEmpty());
        assertEquals(1, watchlistService.fundCodesOf("u2").size());
    }

    @Test
    void reorder() {
        watchlistService.add("u1", "110022");
        watchlistService.add("u1", "161725");
        watchlistService.reorder("u1", java.util.List.of("161725", "110022"));
        assertEquals("161725", watchlistService.fundCodesOf("u1").get(0));
        assertEquals("110022", watchlistService.fundCodesOf("u1").get(1));
    }

    @Test
    void estimatesAssembled() {
        watchlistService.add("u1", "110022");
        var list = watchlistService.estimates("u1");
        assertEquals(1, list.size());
        assertEquals("110022", list.get(0).fundCode());
        assertEquals(0, new BigDecimal("3.0300").compareTo(list.get(0).estimateNav()));
    }
}