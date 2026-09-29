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
    private final Calendar calendar = new Calendar();
    private final Notice notice = new Notice();

    @Data
    public static class Notice {
        /** 是否启用自定义公告 (false 时自动退化为非交易时段提示) */
        private boolean enabled = true;
        /** 公告正文内容 */
        private String message = "系统公告：自选列表现已上线收益率升序/降序快速排序功能，并支持交易所全天分时线！";
        /** 公告类型: info / success / warning / error */
        private String type = "info";
        /** 用户是否可手动关闭 */
        private boolean closable = true;
    }

    @Data
    public static class Calendar {
        /** 自定义/动态节假日列表(格式 yyyy-MM-dd) */
        private java.util.List<String> holidays = new java.util.ArrayList<>();
    }

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
