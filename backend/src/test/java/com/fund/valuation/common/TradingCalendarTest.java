package com.fund.valuation.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradingCalendarTest {

    @Test
    void weekdayMorningIsTrading() {
        assertTrue(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 10, 0)));
        assertTrue(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 9, 30)));
        assertTrue(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 11, 30)));
    }

    @Test
    void weekdayAfternoonIsTrading() {
        assertTrue(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 13, 0)));
        assertTrue(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 14, 59)));
        assertTrue(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 15, 0)));
    }

    @Test
    void lunchBreakNotTrading() {
        assertFalse(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 12, 0)));
        assertFalse(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 11, 31)));
        assertFalse(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 12, 59)));
    }

    @Test
    void weekendNotTrading() {
        assertFalse(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 23, 10, 0)));
        assertFalse(TradingCalendar.isTradingDay(java.time.LocalDate.of(2026, 8, 22)));
    }

    @Test
    void outOfSessionNotTrading() {
        assertFalse(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 9, 0)));
        assertFalse(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 8, 24, 15, 1)));
    }

    @Test
    void holidayNotTrading() {
        assertFalse(TradingCalendar.isTradingDay(java.time.LocalDate.of(2026, 10, 1)));
        assertFalse(TradingCalendar.isTradingTime(LocalDateTime.of(2026, 5, 1, 10, 0)));
    }
}