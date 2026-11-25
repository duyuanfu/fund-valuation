package com.fund.valuation.service;

import com.fund.valuation.client.TianTianFundClient.FundSearchItem;
import com.fund.valuation.client.VisionOcrClient;
import com.fund.valuation.dto.OcrFundItem;
import com.fund.valuation.dto.ScreenshotImportResult;
import com.fund.valuation.dto.VisionConfigView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioScreenshotServiceTest {

    @Mock
    private VisionOcrClient visionOcrClient;

    @Mock
    private FundSearchService fundSearchService;

    @Mock
    private SysVisionConfigService visionConfigService;

    private PortfolioScreenshotService screenshotService;

    @BeforeEach
    void setUp() {
        screenshotService = new PortfolioScreenshotService(visionOcrClient, fundSearchService, visionConfigService);
    }

    @Test
    void rejectsEmptyOrOversizedImage() {
        ScreenshotImportResult r1 = screenshotService.processScreenshot("user1", new byte[0], "image/jpeg");
        assertFalse(r1.success());
        assertTrue(r1.message().contains("为空"));

        byte[] big = new byte[6 * 1024 * 1024];
        ScreenshotImportResult r2 = screenshotService.processScreenshot("user1", big, "image/jpeg");
        assertFalse(r2.success());
        assertTrue(r2.message().contains("超过 5MB"));
    }

    @Test
    void rejectsWhenVisionDisabled() {
        when(visionConfigService.getConfig()).thenReturn(new VisionConfigView(
                false, "http://api.test", "key", "glm", 20
        ));
        byte[] valid = new byte[]{1, 2, 3};
        ScreenshotImportResult r = screenshotService.processScreenshot("user1", valid, "image/jpeg");
        assertFalse(r.success());
        assertTrue(r.message().contains("未启用"));
    }

    @Test
    void successfullyExtractsHoldingsAndEnrichesCandidates() {
        when(visionConfigService.getConfig()).thenReturn(new VisionConfigView(
                true, "http://api.test", "key", "glm-4v-flash", 20
        ));

        OcrFundItem item1 = new OcrFundItem(
                "广发稳健增长A", null, new BigDecimal("50201.26"),
                new BigDecimal("-450.34"), new BigDecimal("-1048.74"), new BigDecimal("-2.05")
        );
        when(visionOcrClient.extractHoldings(any(), anyString())).thenReturn(List.of(item1));

        when(fundSearchService.search(eq("广发稳健增长A"), anyInt())).thenReturn(List.of(
                new FundSearchItem("270002", "广发稳健增长混合A", "混合型")
        ));

        byte[] img = new byte[]{1, 2, 3, 4};
        ScreenshotImportResult result = screenshotService.processScreenshot("vip_user", img, "image/jpeg");

        assertTrue(result.success());
        assertEquals(1, result.items().size());
        OcrFundItem enriched = result.items().get(0);
        assertEquals("270002", enriched.fundCode());
        assertEquals(new BigDecimal("50201.26"), enriched.holdingAmount());
        assertEquals(new BigDecimal("-450.34"), enriched.yesterdayIncome());
        assertNotNull(result.remainingQuota());
    }

    @Test
    void interceptsWhenDailyQuotaExceeded() {
        when(visionConfigService.getConfig()).thenReturn(new VisionConfigView(
                true, "http://api.test", "key", "glm-4v-flash", 1
        ));

        OcrFundItem item1 = new OcrFundItem(
                "基金A", "110022", new BigDecimal("1000"), null, null, null
        );
        when(visionOcrClient.extractHoldings(any(), anyString())).thenReturn(List.of(item1));
        when(fundSearchService.search(anyString(), anyInt())).thenReturn(List.of(
                new FundSearchItem("110022", "基金A", "股票型")
        ));

        byte[] img = new byte[]{1, 2, 3};
        // 第一次成功
        ScreenshotImportResult r1 = screenshotService.processScreenshot("quota_user", img, "image/jpeg");
        assertTrue(r1.success());

        // 第二次超出限额
        ScreenshotImportResult r2 = screenshotService.processScreenshot("quota_user", img, "image/jpeg");
        assertFalse(r2.success());
        assertTrue(r2.message().contains("配额已用完"));
    }

    @Test
    void hallucinatedCodeFromModelIsOverriddenByOfficialSearchCatalog() {
        when(visionConfigService.getConfig()).thenReturn(new VisionConfigView(
                true, "http://api.test", "key", "glm-4v-flash", 20
        ));

        // 模型幻觉出了 001005 (中海合鑫混合)，但实际基金是广发稳健增长A (270002)
        OcrFundItem hallucinatedItem = new OcrFundItem(
                "广发稳健增长A", "001005", new BigDecimal("50201.26"),
                new BigDecimal("-450.34"), new BigDecimal("-1048.74"), new BigDecimal("-2.05")
        );
        when(visionOcrClient.extractHoldings(any(), anyString())).thenReturn(List.of(hallucinatedItem));

        when(fundSearchService.search(eq("广发稳健增长A"), anyInt())).thenReturn(List.of(
                new FundSearchItem("270002", "广发稳健增长混合A", "混合型")
        ));

        byte[] img = new byte[]{1, 2, 3, 4};
        ScreenshotImportResult result = screenshotService.processScreenshot("vip_user", img, "image/jpeg");

        assertTrue(result.success());
        assertEquals(1, result.items().size());
        OcrFundItem item = result.items().get(0);
        // 校验：幻觉出的 001005 被正确纠偏为权威目录代码 270002
        assertEquals("270002", item.fundCode());
    }
}
