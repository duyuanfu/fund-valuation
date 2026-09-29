package com.fund.valuation.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.domain.User;
import com.fund.valuation.dto.NoticeView;
import com.fund.valuation.mapper.FundMapper;
import com.fund.valuation.mapper.UserFundMapper;
import com.fund.valuation.mapper.UserMapper;
import com.fund.valuation.service.BondDurationService;
import com.fund.valuation.service.CalendarHolidayService;
import com.fund.valuation.service.EstimateResult;
import com.fund.valuation.service.FundCatalogService;
import com.fund.valuation.service.IntradayValuationRunner;
import com.fund.valuation.service.QuoteService;
import com.fund.valuation.service.SystemNoticeService;
import com.fund.valuation.service.UserManagementService;
import com.fund.valuation.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运维与管理后台接口 (强制经由 JwtAuthFilter 进行 ADMIN 角色校验)。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IntradayValuationRunner runner;
    private final BondDurationService bondDurationService;
    private final FundCatalogService fundCatalogService;
    private final SystemNoticeService noticeService;
    private final UserManagementService userManagementService;
    private final WatchlistService watchlistService;
    private final CalendarHolidayService calendarHolidayService;
    private final QuoteService quoteService;
    private final UserMapper userMapper;
    private final UserFundMapper userFundMapper;
    private final FundMapper fundMapper;

    /* ---------------- 用户管理与自选调阅 ---------------- */

    @GetMapping("/users")
    public List<UserManagementService.UserView> listUsers(@RequestParam(required = false) String keyword) {
        return userManagementService.listUsers(keyword);
    }

    @PostMapping("/users/{username}/status")
    public Map<String, String> updateUserStatus(@PathVariable String username, @RequestBody UserStatusRequest req) {
        userManagementService.updateStatus(username, req.status());
        return Map.of("status", "ok");
    }

    @PostMapping("/users/{username}/vip")
    public Map<String, String> updateUserVip(@PathVariable String username, @RequestBody UserVipRequest req) {
        userManagementService.updateVip(username, req.isVip(), req.expireAt());
        return Map.of("status", "ok");
    }

    @PostMapping("/users/{username}/role")
    public Map<String, String> updateUserRole(@PathVariable String username, @RequestBody UserRoleRequest req) {
        userManagementService.updateRole(username, req.role());
        return Map.of("status", "ok");
    }

    @DeleteMapping("/users/{username}")
    public Map<String, String> deleteUser(@PathVariable String username) {
        userManagementService.deleteUser(username);
        return Map.of("status", "ok");
    }

    @GetMapping("/funds")
    public List<FundView> listFunds() {
        List<com.fund.valuation.domain.Fund> funds = fundMapper.selectList(new LambdaQueryWrapper<com.fund.valuation.domain.Fund>()
                .orderByDesc(com.fund.valuation.domain.Fund::getUpdatedAt));
        List<com.fund.valuation.domain.UserFund> allUserFunds = userFundMapper.selectList(null);
        Map<String, Long> userCountByFund = allUserFunds.stream()
                .collect(Collectors.groupingBy(com.fund.valuation.domain.UserFund::getFundCode, Collectors.counting()));

        return funds.stream().map(f -> new FundView(
                f.getCode(),
                f.getName(),
                f.getType(),
                f.getTypeRaw(),
                f.getTrackIndex(),
                f.getPrevNav(),
                f.getNavDate() != null ? f.getNavDate().toString() : "-",
                userCountByFund.getOrDefault(f.getCode(), 0L).intValue(),
                f.getUpdatedAt()
        )).toList();
    }

    @GetMapping("/funds/{fundCode}/watchers")
    public List<UserManagementService.UserView> getFundWatchers(@PathVariable String fundCode) {
        return userManagementService.listFundWatchers(fundCode);
    }

    @GetMapping("/users/{username}/watchlist")
    public List<EstimateResult> getUserWatchlist(@PathVariable String username) {
        return watchlistService.estimates(username);
    }

    /* ---------------- 交易日历休市管理 ---------------- */

    @GetMapping("/calendar/holidays")
    public Map<String, Object> listHolidays() {
        return Map.of(
                "customHolidays", calendarHolidayService.listHolidays(),
                "allDynamicHolidays", TradingCalendar.getDynamicHolidays()
        );
    }

    @PostMapping("/calendar/holidays")
    public Map<String, String> addHoliday(@RequestBody HolidayRequest req) {
        calendarHolidayService.addHoliday(req.date(), req.description());
        return Map.of("status", "ok");
    }

    @DeleteMapping("/calendar/holidays/{date}")
    public Map<String, String> deleteHoliday(@PathVariable String date) {
        calendarHolidayService.removeHoliday(date);
        return Map.of("status", "ok");
    }

    /* ---------------- 运维与调度任务 ---------------- */

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
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(noticeService.getConfiguredNotice());
    }

    /**
     * Admin 配置/发布系统公告。
     */
    @PostMapping("/notice")
    public ResponseEntity<NoticeView> setNotice(@RequestBody NoticeRequest req) {
        NoticeView v = noticeService.updateNotice(req.message(), req.type(), req.enabled(), req.closable());
        return ResponseEntity.ok(v);
    }

    /**
     * 系统运维监控与资源统计大盘。
     */
    @GetMapping("/stats")
    public Map<String, Object> getSystemStats() {
        Long totalUsers = userMapper.selectCount(null);
        Long vipUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getIsVip, true).or().eq(User::getRole, User.ROLE_ADMIN)));
        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        Long activeUsersToday = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getLastLoginAt, startOfToday));
        Long totalWatchlists = userFundMapper.selectCount(null);
        Long totalFunds = fundMapper.selectCount(null);
        int cachedQuotes = quoteService.getCachedQuoteCount();
        LocalDateTime lastRun = runner.getLastRunTime();
        int dynamicHolidays = TradingCalendar.getDynamicHolidays().size();

        Map<String, Long> referralStats = new java.util.HashMap<>();
        try {
            List<Map<String, Object>> rows = userMapper.countByReferralSource();
            if (rows != null) {
                for (Map<String, Object> r : rows) {
                    Object s = r.get("source");
                    if (s == null) {
                        s = r.get("SOURCE");
                    }
                    String src = s != null && !s.toString().isBlank() ? s.toString() : "自己搜索";
                    Object t = r.get("total");
                    if (t == null) {
                        t = r.get("TOTAL");
                    }
                    long count = t instanceof Number num ? num.longValue() : 0L;
                    referralStats.merge(src, count, Long::sum);
                }
            }
        } catch (Exception e) {
            // 兜底保障
        }

        return Map.of(
                "totalUsers", totalUsers != null ? totalUsers : 0,
                "vipUsers", vipUsers != null ? vipUsers : 0,
                "activeUsersToday", activeUsersToday != null ? activeUsersToday : 0,
                "totalWatchlists", totalWatchlists != null ? totalWatchlists : 0,
                "totalFunds", totalFunds != null ? totalFunds : 0,
                "cachedQuotes", cachedQuotes,
                "lastValuationRunTime", lastRun != null ? lastRun.toString() : "尚未执行",
                "activeDynamicHolidays", dynamicHolidays,
                "referralStats", referralStats
        );
    }

    public record DurationRequest(String fundCode, String reportQt, double duration) {
    }

    public record NoticeRequest(String message, String type, Boolean enabled, Boolean closable) {
    }

    public record UserStatusRequest(String status) {
    }

    public record UserVipRequest(boolean isVip, LocalDateTime expireAt) {
    }

    public record UserRoleRequest(String role) {
    }

    public record HolidayRequest(String date, String description) {
    }

    public record FundView(
            String code,
            String name,
            String type,
            String typeRaw,
            String trackIndex,
            BigDecimal prevNav,
            String navDate,
            int userCount,
            LocalDateTime updatedAt
    ) {
    }
}