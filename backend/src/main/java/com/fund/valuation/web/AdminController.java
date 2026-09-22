package com.fund.valuation.web;

import com.fund.valuation.service.BondDurationService;
import com.fund.valuation.service.IntradayValuationRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 运维接口(冒烟/调试用,生产可关闭)。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IntradayValuationRunner runner;
    private final BondDurationService bondDurationService;

    @PostMapping("/intraday")
    public Map<String, String> triggerIntraday() {
        runner.forceRun();
        return Map.of("status", "ok");
    }

    /**
     * 人工维护债券基金组合久期(覆盖自动推断)。
     */
    @PostMapping("/bond-duration")
    public Map<String, String> setBondDuration(@RequestBody DurationRequest req) {
        bondDurationService.upsert(req.fundCode(), req.reportQt(), req.duration(), "manual");
        return Map.of("status", "ok");
    }

    public record DurationRequest(String fundCode, String reportQt, double duration) {
    }
}