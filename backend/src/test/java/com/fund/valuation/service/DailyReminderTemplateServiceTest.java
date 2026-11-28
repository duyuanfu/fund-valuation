package com.fund.valuation.service;

import com.fund.valuation.dto.PortfolioSummaryView;
import com.fund.valuation.dto.PositionItemView;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DailyReminderTemplateServiceTest {

    private final DailyReminderTemplateService templateService = new DailyReminderTemplateService();

    @Test
    void testRenderHtmlEmail_WithHoldingPositions() {
        PortfolioSummaryView summary = new PortfolioSummaryView(
                BigDecimal.valueOf(10000.00),
                BigDecimal.valueOf(9500.00),
                BigDecimal.valueOf(50.00),
                BigDecimal.valueOf(500.00),
                BigDecimal.valueOf(125.00),
                BigDecimal.valueOf(1.25),
                BigDecimal.valueOf(625.00),
                BigDecimal.valueOf(6.58)
        );

        PositionItemView pos = new PositionItemView(
                1L,
                "110022",
                "易方达消费行业股票",
                "股票型",
                BigDecimal.valueOf(10000.00),
                BigDecimal.valueOf(50.00),
                BigDecimal.valueOf(500.00),
                BigDecimal.valueOf(5.00),
                BigDecimal.valueOf(9500.00),
                BigDecimal.valueOf(5000.0),
                BigDecimal.valueOf(1.9000),
                BigDecimal.valueOf(2.0000),
                LocalDate.now(),
                BigDecimal.valueOf(2.0250),
                BigDecimal.valueOf(1.25),
                LocalDateTime.now(),
                false,
                BigDecimal.valueOf(125.00),
                BigDecimal.valueOf(10125.00),
                BigDecimal.valueOf(625.00),
                BigDecimal.valueOf(6.58)
        );

        EstimateResult watch = new EstimateResult(
                "161725",
                "招商中证白酒指数",
                "指数型",
                BigDecimal.valueOf(0.8920),
                BigDecimal.valueOf(0.80),
                BigDecimal.valueOf(0.8849),
                LocalDate.now(),
                "2026Q2",
                LocalDateTime.now(),
                false,
                null,
                null
        );

        String html = templateService.renderHtmlEmail(
                "tester",
                "21:30",
                summary,
                List.of(pos),
                List.of(watch),
                false,
                "https://fund.trythis.pw/"
        );

        assertNotNull(html);
        assertTrue(html.contains("21:30 今日基金收益与持仓日报"));
        assertTrue(html.contains("持仓基金今日收益走势"), "HTML email must render holding positions section");
        assertTrue(html.contains("易方达消费行业股票"));
        assertTrue(html.contains("110022"));
        assertTrue(html.contains("+1.25%"));
        assertTrue(html.contains("+125.00元"));
        assertTrue(html.contains("招商中证白酒指数"));
        assertTrue(html.contains("https://fund.trythis.pw/"));
        assertTrue(html.contains("点击前往查看估值详情 ➔"));
        assertFalse(html.contains("¥"), "Financial display must not use currency symbol ¥");
        assertFalse(html.contains("￥"), "Financial display must not use currency symbol ￥");
    }

    @Test
    void testRenderMarkdown_WithHoldingPositions() {
        PortfolioSummaryView summary = new PortfolioSummaryView(
                BigDecimal.valueOf(20000.00),
                BigDecimal.valueOf(19000.00),
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(250.00),
                BigDecimal.valueOf(1.25),
                BigDecimal.valueOf(1250.00),
                BigDecimal.valueOf(6.58)
        );

        PositionItemView pos = new PositionItemView(
                1L,
                "000001",
                "华夏成长混合",
                "混合型",
                BigDecimal.valueOf(20000.00),
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(5.00),
                BigDecimal.valueOf(19000.00),
                BigDecimal.valueOf(10000.0),
                BigDecimal.valueOf(1.9000),
                BigDecimal.valueOf(2.0000),
                LocalDate.now(),
                BigDecimal.valueOf(2.0250),
                BigDecimal.valueOf(1.25),
                LocalDateTime.now(),
                false,
                BigDecimal.valueOf(250.00),
                BigDecimal.valueOf(20250.00),
                BigDecimal.valueOf(1250.00),
                BigDecimal.valueOf(6.58)
        );

        String md = templateService.renderMarkdown(
                "tester",
                "21:30",
                summary,
                List.of(pos),
                Collections.emptyList(),
                false,
                "https://fund.trythis.pw/"
        );

        assertNotNull(md);
        assertTrue(md.contains("21:30 今日基金收益与持仓日报"));
        assertTrue(md.contains("持仓基金今日走势与收益"), "Markdown must render holding positions section");
        assertTrue(md.contains("华夏成长混合"));
        assertTrue(md.contains("000001"));
        assertTrue(md.contains("+1.25%"));
        assertTrue(md.contains("+250.00元"));
        assertFalse(md.contains("¥"), "Financial display must not use currency symbol ¥");
        assertFalse(md.contains("￥"), "Financial display must not use currency symbol ￥");
    }

    @Test
    void testHideAmount_DoesNotDisplayMoneyValues() {
        PortfolioSummaryView summary = new PortfolioSummaryView(
                BigDecimal.valueOf(20000.00),
                BigDecimal.valueOf(19000.00),
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(250.00),
                BigDecimal.valueOf(1.25),
                BigDecimal.valueOf(1250.00),
                BigDecimal.valueOf(6.58)
        );

        PositionItemView pos = new PositionItemView(
                1L,
                "000001",
                "华夏成长混合",
                "混合型",
                BigDecimal.valueOf(20000.00),
                BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(5.00),
                BigDecimal.valueOf(19000.00),
                BigDecimal.valueOf(10000.0),
                BigDecimal.valueOf(1.9000),
                BigDecimal.valueOf(2.0000),
                LocalDate.now(),
                BigDecimal.valueOf(2.0250),
                BigDecimal.valueOf(1.25),
                LocalDateTime.now(),
                false,
                BigDecimal.valueOf(250.00),
                BigDecimal.valueOf(20250.00),
                BigDecimal.valueOf(1250.00),
                BigDecimal.valueOf(6.58)
        );

        String html = templateService.renderHtmlEmail(
                "tester",
                "21:30",
                summary,
                List.of(pos),
                Collections.emptyList(),
                true, // hideAmount
                "https://fund.trythis.pw/"
        );

        assertFalse(html.contains("250.00元"));
        assertFalse(html.contains("总资产现值"));
        assertTrue(html.contains("+1.25%"));
    }
}
