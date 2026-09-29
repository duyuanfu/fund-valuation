package com.fund.valuation.service;

import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.domain.CalendarHoliday;
import com.fund.valuation.mapper.CalendarHolidayMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CalendarHolidayServiceTest {

    @Autowired
    private CalendarHolidayService calendarHolidayService;

    @Autowired
    private CalendarHolidayMapper mapper;

    @BeforeEach
    void clean() {
        mapper.delete(null);
    }

    @Test
    void testAddAndRemoveHoliday() {
        // 选择一个未来的周三作为测试日期 (保证不是周末，不是元旦国庆)
        LocalDate wednesday = LocalDate.of(2027, 3, 17);
        assertTrue(TradingCalendar.isTradingDay(wednesday), "原定工作日应当为交易日");

        // 动态添加为休市日
        calendarHolidayService.addHoliday("2027-03-17", "临时特别闭市");
        assertFalse(TradingCalendar.isTradingDay(wednesday), "添加休市日后应当判定为非交易日");

        List<CalendarHolidayService.CalendarHolidayView> list = calendarHolidayService.listHolidays();
        assertEquals(1, list.size());
        assertEquals("2027-03-17", list.get(0).holidayDate());
        assertEquals("临时特别闭市", list.get(0).description());

        // 删除休市日
        calendarHolidayService.removeHoliday("2027-03-17");
        assertTrue(TradingCalendar.isTradingDay(wednesday), "删除休市日后应当恢复为交易日");
    }
}
