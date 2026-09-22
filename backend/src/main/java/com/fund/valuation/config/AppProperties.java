package com.fund.valuation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fund")
public class AppProperties {

    private final Schedule schedule = new Schedule();
    private final Quote quote = new Quote();
    private final History history = new History();
    private final Bond bond = new Bond();
    private final Jwt jwt = new Jwt();

    @Data
    public static class Jwt {
        private String secret = "fund-valuation-dev-secret-change-me-0123456789";
        private int expireDays = 7;
    }

    @Data
    public static class Schedule {
        private String navCron = "0 30 20 * * *";
        private String holdingCron = "0 0 21 * * *";
        private long intradayIntervalMs = 120000;
    }

    @Data
    public static class Quote {
        private long freshSeconds = 300;
    }

    @Data
    public static class History {
        private int retainDays = 30;
    }

    @Data
    public static class Bond {
        /** 10年期国债收益率指数 secid */
        private String indexSecid = "103.TY00Y";
        /** 参考久期(指数对应组合久期近似,年) */
        private double referenceDuration = 10.0;
        /** 久期兜底(年) */
        private double defaultDuration = 2.0;
    }
}
