package com.fund.valuation.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * A股交易时段判定。参考级实现:
 * - 周一至周五
 * - 交易时段 9:30-11:30、13:00-15:00(午间休市跳过)
 * - 节假日采用静态清单(中国法定假日及调休日),按年维护
 */
public final class TradingCalendar {

    private static final LocalTime MORNING_START = LocalTime.of(9, 30);
    private static final LocalTime MORNING_END = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(15, 0);

    /**
     * 中国法定节假日(非交易日)。按年维护,格式 yyyy-MM-dd。
     */
    private static final Set<String> HOLIDAYS = Set.of(
            "2026-01-01", "2026-01-02",
            "2026-02-16", "2026-02-17", "2026-02-18", "2026-02-19", "2026-02-20",
            "2026-04-06", "2026-04-07", "2026-04-08",
            "2026-05-01", "2026-05-04", "2026-05-05",
            "2026-06-19",
            "2026-09-25", "2026-09-28", "2026-09-29", "2026-09-30",
            "2026-10-01", "2026-10-02", "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08"
    );

    private TradingCalendar() {
    }

    public static boolean isTradingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        return !HOLIDAYS.contains(date.toString());
    }

    public static boolean isTradingTime(LocalDateTime now) {
        if (!isTradingDay(now.toLocalDate())) {
            return false;
        }
        LocalTime t = now.toLocalTime();
        return !t.isBefore(MORNING_START) && !t.isAfter(MORNING_END)
                || !t.isBefore(AFTERNOON_START) && !t.isAfter(AFTERNOON_END);
    }
}
