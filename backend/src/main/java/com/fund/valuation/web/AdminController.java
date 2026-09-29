package com.fund.valuation.web;

import com.fund.valuation.dto.NoticeView;
import com.fund.valuation.service.BondDurationService;
import com.fund.valuation.service.FundCatalogService;
import com.fund.valuation.service.IntradayValuationRunner;
import com.fund.valuation.service.SystemNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final FundCatalogService fundCatalogService;
    private final SystemNoticeService noticeService;

    @PostMapping("/intraday")
    public Map<String, String> triggerIntraday(@RequestParam(defaultValue = "false") boolean force) {
        if (force) {
            runner.forceRun();
        } else {
            runner.run();
        }
        return Map.of("status", "ok");
    }

    @PostMapping("/refresh-nav")
    public Map<String, Object> refreshNav() {
        int count = fundCatalogService.refreshAll();
        return Map.of("status", "ok", "updatedFunds", count);
    }

    /**
     * 人工维护债券基金组合久期(覆盖自动推断)。
     */
    @PostMapping("/bond-duration")
    public Map<String, String> setBondDuration(@RequestBody DurationRequest req) {
        bondDurationService.upsert(req.fundCode(), req.reportQt(), req.duration(), "manual");
        return Map.of("status", "ok");
    }

    /**
     * 查询当前系统公告配置。
     */
    @GetMapping("/notice")
    public ResponseEntity<NoticeView> getNotice() {
        return ResponseEntity.ok(noticeService.getActiveNotice());
    }

    /**
     * Admin 配置/发布系统公告。
     */
    @PostMapping("/notice")
    public ResponseEntity<NoticeView> setNotice(@RequestBody NoticeRequest req) {
        NoticeView v = noticeService.updateNotice(req.message(), req.type(), req.enabled(), req.closable());
        return ResponseEntity.ok(v);
    }

    public record DurationRequest(String fundCode, String reportQt, double duration) {
    }

    public record NoticeRequest(String message, String type, Boolean enabled, Boolean closable) {
    }
}