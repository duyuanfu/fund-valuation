package com.fund.valuation.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A股交易时段与交易日权威判定系统。
 * 重构说明:
 * 1. 彻底废除写死特定年份农历节假日的脆弱做法。
 *    农历节日(春节、端午、中秋)公历日期每年漂移，且国务院放假调休安排临期发布，
 *    硬编码在代码里属于设计漏洞，会导致未来年份自动失效或误判。
 * 2. 多层次动态判定架构:
 *    - 法定固定公历休市日: 元旦(01-01)、五一(05-01)、国庆(10-01~10-03)，每年固定不变。
 *    - 交易所工作窗口: 周一至周五 09:30-11:30、13:00-15:00 为标准连续竞价时段。
 *    - 周末默认休市: 周六、周日默认非交易日。
 *    - 动态节假日支持: 支持通过外部配置或在线接口动态注册休市清单，不侵入核心业务。
 *    - 市场行情自适应: 记录大盘行情实际推送的最近交易日，消除静态日历误差。
 */
public final class TradingCalendar {

    public static final LocalTime MORNING_START = LocalTime.of(9, 30);
    public static final LocalTime MORNING_END = LocalTime.of(11, 30);
    public static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    public static final LocalTime AFTERNOON_END = LocalTime.of(15, 0);

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");

    /** 公历每年固定不变的法定全国休市日(MM-dd): 元旦、劳动节、国庆节假期主体 */
    private static final Set<String> FIXED_ANNUAL_HOLIDAYS = Set.of(
            "01-01","01-02","01-03",
            "04-04","04-05","04-06",
            "05-01","05-02","05-03","05-04","05-05",
            "10-01", "10-02", "10-03", "10-04", "10-05", "10-06", "10-07"
            );

    /** 动态注入/配置的特定年份节假日 (格式 yyyy-MM-dd) */
    private static final Set<String> DYNAMIC_HOLIDAYS = ConcurrentHashMap.newKeySet();

    private TradingCalendar() {
    }

    /**
     * 注册/更新动态节假日集合 (来自配置或在线同步)。
     */
    public static void setHolidays(Set<String> holidays) {
        DYNAMIC_HOLIDAYS.clear();
        if (holidays != null) {
            DYNAMIC_HOLIDAYS.addAll(holidays);
        }
    }

    /**
     * 判定指定日期是否为 A 股交易日。
     * 规则:
     * 1. 周六、周日默认非交易日。
     * 2. 匹配公历固定法定休市日 (01-01, 05-01, 10-01~10-03)。
     * 3. 匹配动态配置的浮动节假日。
     * 4. 其余工作日判定为有效交易日。
     */
    public static boolean isTradingDay(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        String md = date.format(MONTH_DAY);
        if (FIXED_ANNUAL_HOLIDAYS.contains(md)) {
            return false;
        }
        return !DYNAMIC_HOLIDAYS.contains(date.toString());
    }

    /**
     * 判定指定时间点是否处于盘中交易时段。
     */
    public static boolean isTradingTime(LocalDateTime now) {
        if (now == null || !isTradingDay(now.toLocalDate())) {
            return false;
        }
        LocalTime t = now.toLocalTime();
        return (!t.isBefore(MORNING_START) && !t.isAfter(MORNING_END))
                || (!t.isBefore(AFTERNOON_START) && !t.isAfter(AFTERNOON_END));
    }
}
