package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.domain.CalendarHoliday;
import com.fund.valuation.mapper.CalendarHolidayMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@DependsOn("databaseInitializer")
@RequiredArgsConstructor
public class CalendarHolidayService {

    private final CalendarHolidayMapper mapper;
    private final AppProperties properties;

    @PostConstruct
    public void init() {
        refreshTradingCalendar();
    }

    public synchronized void refreshTradingCalendar() {
        Set<String> holidays = new HashSet<>();
        if (properties.getCalendar() != null && properties.getCalendar().getHolidays() != null) {
            holidays.addAll(properties.getCalendar().getHolidays());
        }
        try {
            List<CalendarHoliday> list = mapper.selectList(null);
            for (CalendarHoliday h : list) {
                if (h.getHolidayDate() != null) {
                    holidays.add(h.getHolidayDate().toString());
                }
            }
        } catch (Exception e) {
            log.warn("load calendar holidays from db failed: {}", e.getMessage());
        }
        TradingCalendar.setHolidays(holidays);
        log.info("trading calendar holidays reloaded, total active dynamic holidays: {}", holidays.size());
    }

    public List<CalendarHolidayView> listHolidays() {
        List<CalendarHoliday> dbList = mapper.selectList(new LambdaQueryWrapper<CalendarHoliday>()
                .orderByAsc(CalendarHoliday::getHolidayDate));
        return dbList.stream().map(h -> new CalendarHolidayView(
                h.getId(),
                h.getHolidayDate().toString(),
                h.getDescription(),
                h.getCreatedAt()
        )).toList();
    }

    public void addHoliday(String dateStr, String description) {
        LocalDate date = LocalDate.parse(dateStr);
        CalendarHoliday exist = mapper.selectOne(new LambdaQueryWrapper<CalendarHoliday>()
                .eq(CalendarHoliday::getHolidayDate, date));
        if (exist != null) {
            exist.setDescription(description);
            mapper.updateById(exist);
        } else {
            CalendarHoliday h = new CalendarHoliday();
            h.setHolidayDate(date);
            h.setDescription(description);
            h.setCreatedAt(LocalDateTime.now());
            mapper.insert(h);
        }
        refreshTradingCalendar();
    }

    public void removeHoliday(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        mapper.delete(new LambdaQueryWrapper<CalendarHoliday>()
                .eq(CalendarHoliday::getHolidayDate, date));
        refreshTradingCalendar();
    }

    public record CalendarHolidayView(Long id, String holidayDate, String description, LocalDateTime createdAt) {}
}
