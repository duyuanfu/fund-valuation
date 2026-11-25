package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.client.TianTianFundClient;
import com.fund.valuation.client.TianTianFundClient.FundSearchItem;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.mapper.FundMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundSearchServiceTest {

    @Mock
    private FundMapper fundMapper;

    @Mock
    private TianTianFundClient tianTianFundClient;

    private FundSearchService fundSearchService;

    @BeforeEach
    void setUp() {
        fundSearchService = new FundSearchService(fundMapper, tianTianFundClient);
    }

    @Test
    void searchReturnsEmptyForBlankQuery() {
        List<FundSearchItem> res = fundSearchService.search("   ", 10);
        assertTrue(res.isEmpty());
    }

    @Test
    void cleanQueryStripsParenthesesAndSuffixes() {
        assertEquals("招商中证白酒指数", fundSearchService.cleanQuery("招商中证白酒指数(LOF)"));
        assertEquals("广发稳健增长", fundSearchService.cleanQuery("广发稳健增长A"));
        assertEquals("诺安成长", fundSearchService.cleanQuery("诺安成长（LOF）"));
        assertEquals("易方达蓝筹精选", fundSearchService.cleanQuery("易方达蓝筹精选混合"));
    }

    @Test
    void searchPrefersExactMatchAndRankACorrectly() {
        Fund f1 = new Fund();
        f1.setCode("270002");
        f1.setName("广发稳健增长混合A");
        f1.setType("mixed");

        when(fundMapper.selectList(any())).thenReturn(List.of(f1));
        when(tianTianFundClient.searchFundsByName("广发稳健增长A")).thenReturn(List.of(
                new FundSearchItem("009326", "广发稳健增长混合C", "混合型"),
                new FundSearchItem("270002", "广发稳健增长混合A", "混合型")
        ));

        List<FundSearchItem> items = fundSearchService.search("广发稳健增长A", 5);

        assertFalse(items.isEmpty());
        // A类关键词优先排在最前
        assertEquals("270002", items.get(0).code());
        assertTrue(items.get(0).name().contains("A"));
    }

    @Test
    void searchFallsBackToRemoteWhenLocalEmpty() {
        when(fundMapper.selectList(any())).thenReturn(List.of());
        when(tianTianFundClient.searchFundsByName(eq("诺安成长混合"))).thenReturn(List.of(
                new FundSearchItem("320007", "诺安成长混合A", "混合型"),
                new FundSearchItem("025333", "诺安成长混合C", "混合型")
        ));

        List<FundSearchItem> items = fundSearchService.search("诺安成长混合", 5);
        assertEquals(2, items.size());
        assertEquals("320007", items.get(0).code());
    }
}
