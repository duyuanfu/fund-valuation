package com.fund.valuation.service;

import com.fund.valuation.client.Quote;
import com.fund.valuation.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BondEstimatorTest {

    @Mock
    private QuoteService quoteService;
    @Mock
    private BondDurationService durationService;

    private AppProperties properties;
    private BondEstimator estimator;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        estimator = new BondEstimator(quoteService, durationService, properties);
    }

    @Test
    void rateSleeveCalc() {
        Quote q = new Quote("103.TY00Y", "10年期国债收益率", new BigDecimal("108.4375"),
                new BigDecimal("1.60"), LocalDateTime.now(), "eastmoney");
        when(quoteService.getFreshQuote("103.TY00Y")).thenReturn(q);
        when(durationService.getDuration("008559", "xx纯债", "债券型-长债")).thenReturn(2.0);

        BigDecimal pct = estimator.estimateRateSleeve("008559", "xx纯债", "债券型-长债");
        // 1.60 × (2.0/10) = 0.32
        assertEquals(0, new BigDecimal("0.3200").compareTo(pct));
    }

    @Test
    void rateSleeveWithLongDuration() {
        Quote q = new Quote("103.TY00Y", "10年期国债收益率", new BigDecimal("108.4375"),
                new BigDecimal("-0.50"), LocalDateTime.now(), "eastmoney");
        when(quoteService.getFreshQuote("103.TY00Y")).thenReturn(q);
        when(durationService.getDuration("008559", "xx长债", "债券型-长债")).thenReturn(4.5);

        BigDecimal pct = estimator.estimateRateSleeve("008559", "xx长债", "债券型-长债");
        // -0.50 × (4.5/10) = -0.225
        assertEquals(0, new BigDecimal("-0.2250").compareTo(pct));
    }

    @Test
    void missingQuoteReturnsNull() {
        when(quoteService.getFreshQuote("103.TY00Y")).thenReturn(null);
        assertNull(estimator.estimateRateSleeve("008559", "xx纯债", "债券型-长债"));
    }
}