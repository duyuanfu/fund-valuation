package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.EstimateHistory;
import com.fund.valuation.mapper.EstimateHistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class EstimateHistoryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock TRADING_CLOCK = Clock.fixed(Instant.parse("2026-08-24T02:00:00Z"), ZONE);
    private static final Clock LUNCH_CLOCK = Clock.fixed(Instant.parse("2026-08-24T04:30:00Z"), ZONE);

    @Autowired
    private EstimateHistoryService historyService;

    @Autowired
    private EstimateHistoryMapper historyMapper;

    @BeforeEach
    void cleanDb() {
        FixedClockConfig.CLOCK.set(TRADING_CLOCK);
        historyMapper.delete(null);
    }

    @TestConfiguration
    static class FixedClockConfig {
        static final SettableClock CLOCK = new SettableClock(TRADING_CLOCK);

        @Bean
        @Primary
        Clock testClock() {
            return CLOCK;
        }
    }

    /** 可切换的委托 Clock,用于模拟不同时点。 */
    static class SettableClock extends Clock {
        private volatile Clock delegate;

        SettableClock(Clock delegate) {
            this.delegate = delegate;
        }

        void set(Clock clock) {
            this.delegate = clock;
        }

        @Override
        public ZoneId getZone() {
            return delegate.getZone();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return delegate.withZone(zone);
        }

        @Override
        public Instant instant() {
            return delegate.instant();
        }
    }

    private EstimateResult result(String code, String nav) {
        return new EstimateResult(code, "测试", "active",
                new BigDecimal(nav), BigDecimal.ONE, BigDecimal.ONE,
                LocalDateTime.now(TRADING_CLOCK).toLocalDate(), "2026-06-30",
                LocalDateTime.now(TRADING_CLOCK), false, null, "x");
    }

    @Test
    void recordDuringTradingTime() {
        FixedClockConfig.CLOCK.set(TRADING_CLOCK);
        historyService.recordAll(List.of(result("110022", "3.0300")));
        List<EstimateHistory> rows = historyMapper.selectList(null);
        assertEquals(1, rows.size());
        assertEquals("110022", rows.get(0).getFundCode());
        assertEquals(0, new BigDecimal("3.0300").compareTo(rows.get(0).getEstNav()));
    }

    @Test
    void recordAtLunchBreakSkipped() {
        FixedClockConfig.CLOCK.set(LUNCH_CLOCK);
        historyService.recordAll(List.of(result("110022", "3.0300")));
        assertTrue(historyMapper.selectList(null).isEmpty());
    }

    @Test
    void todayHistoryOrdered() {
        FixedClockConfig.CLOCK.set(TRADING_CLOCK);
        historyService.recordAll(List.of(result("110022", "3.0000")));
        historyService.recordAll(List.of(result("110022", "3.0100")));
        var history = historyService.todayHistory("110022");
        assertEquals(2, history.size());
        assertEquals(0, new BigDecimal("3.0000").compareTo(history.get(0).getEstNav()));
        assertEquals(0, new BigDecimal("3.0100").compareTo(history.get(1).getEstNav()));
    }

    @Test
    void cleanupRemovesOldRows() {
        EstimateHistory old = new EstimateHistory();
        old.setFundCode("110022");
        old.setEstTime(LocalDateTime.now(TRADING_CLOCK).minusDays(40));
        old.setEstNav(new BigDecimal("3.0000"));
        old.setEstPct(BigDecimal.ZERO);
        historyMapper.insert(old);

        historyService.cleanup();
        assertTrue(historyMapper.selectList(null).isEmpty());
    }
}