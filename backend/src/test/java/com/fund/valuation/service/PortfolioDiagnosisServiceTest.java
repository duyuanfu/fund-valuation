package com.fund.valuation.service;

import com.fund.valuation.client.AiAdvisorClient;
import com.fund.valuation.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioDiagnosisServiceTest {

    @Mock
    private PortfolioLookThroughService lookThroughService;

    @Mock
    private AiAdvisorClient aiAdvisorClient;

    @InjectMocks
    private PortfolioDiagnosisService diagnosisService;

    @Test
    void testGetDiagnosis_EmptyPortfolio() {
        PortfolioLookThroughView emptyLookThrough = new PortfolioLookThroughView(
                BigDecimal.ZERO, 0, List.of(), List.of(), List.of(), "-", BigDecimal.ZERO
        );
        when(lookThroughService.calculateLookThrough("user1")).thenReturn(emptyLookThrough);

        PortfolioDiagnosisView view = diagnosisService.getDiagnosis("user1", false);

        assertNotNull(view);
        assertEquals(100, view.healthScore());
        assertEquals("空仓待配置", view.scoreLevel());
    }

    @Test
    void testGetDiagnosis_HighConcentrationDetection() {
        StockExposureItem topStock = new StockExposureItem(
                "600519", "贵州茅台",
                BigDecimal.valueOf(3000.0),
                BigDecimal.valueOf(30.0), // 30% concentration
                List.of()
        );

        AssetAllocationItem alloc1 = new AssetAllocationItem("ACTIVE", "偏股混合 / 股票型", BigDecimal.valueOf(10000.0), BigDecimal.valueOf(100.0));

        PortfolioLookThroughView mockLookThrough = new PortfolioLookThroughView(
                BigDecimal.valueOf(10000.0),
                2,
                List.of(topStock),
                List.of(alloc1),
                List.of(),
                "2026Q2",
                BigDecimal.valueOf(30.0)
        );
        when(lookThroughService.calculateLookThrough("user2")).thenReturn(mockLookThrough);

        AiDiagnosisReport mockReport = new AiDiagnosisReport(
                "测试诊断综述", List.of("亮点"), List.of("隐患"), List.of("调仓建议"), "2026-11-26 12:00"
        );
        when(aiAdvisorClient.generateReport(anyInt(), anyString(), any(), anyList())).thenReturn(mockReport);

        PortfolioDiagnosisView view = diagnosisService.getDiagnosis("user2", false);

        assertNotNull(view);
        assertTrue(view.healthScore() < 85);
        assertTrue(view.riskTags().stream().anyMatch(t -> "DANGER".equals(t.type()) && t.title().contains("单一重仓股")));
        assertNotNull(view.aiReport());
        assertEquals("测试诊断综述", view.aiReport().summary());
    }
}
