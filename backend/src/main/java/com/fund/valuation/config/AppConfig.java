package com.fund.valuation.config;

import com.fund.valuation.common.TradingCalendar;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final AppProperties properties;

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * 专属后台爬虫异步线程池: 与默认计算线程池物理隔离，防止高延迟外部 HTTP 请求阻塞系统通用工作线程。
     */
    @Bean(name = "crawlerExecutor")
    public Executor crawlerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("fund-crawler-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    @PostConstruct
    public void initTradingCalendar() {
        if (properties.getCalendar() != null && properties.getCalendar().getHolidays() != null) {
            TradingCalendar.setHolidays(new HashSet<>(properties.getCalendar().getHolidays()));
        }
    }
}