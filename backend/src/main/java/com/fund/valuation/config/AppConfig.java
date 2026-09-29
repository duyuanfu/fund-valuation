package com.fund.valuation.config;

import com.fund.valuation.common.TradingCalendar;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final AppProperties properties;

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @PostConstruct
    public void initTradingCalendar() {
        if (properties.getCalendar() != null && properties.getCalendar().getHolidays() != null) {
            TradingCalendar.setHolidays(new HashSet<>(properties.getCalendar().getHolidays()));
        }
    }
}