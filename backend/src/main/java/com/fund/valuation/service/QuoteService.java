package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.client.EastMoneyQuoteClient;
import com.fund.valuation.client.Quote;
import com.fund.valuation.client.TencentQuoteClient;
import com.fund.valuation.common.TradingCalendar;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.QuoteCache;
import com.fund.valuation.dto.EstimatePointView;
import com.fund.valuation.mapper.QuoteCacheMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行情采集与缓存:批量拉取、去重、内存+DB缓存、新鲜度校验。
 * 新鲜度策略: 交易时段严格(配置秒数);非交易时段放宽至最近一个交易日的快照,
 * 保证盘后/重启后估值不归零。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteService {

    private final EastMoneyQuoteClient eastMoney;
    private final TencentQuoteClient tencent;
    private final QuoteCacheMapper quoteCacheMapper;
    private final AppProperties properties;

    private final ConcurrentHashMap<String, Quote> memoryCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadFromDb() {
        for (QuoteCache c : quoteCacheMapper.selectList(null)) {
            memoryCache.put(c.getSecid(), toQuote(c));
        }
        log.info("loaded {} quotes from db", memoryCache.size());
    }

    /**
     * 批量刷新行情并写入缓存。东财拉不到的标的用腾讯兜底。
     * 非事务:网络 IO 与落库分离,避免长事务占用连接池;单条写入自带原子性。
     */
    public Map<String, Quote> refreshQuotes(Set<String> requiredSecids) {
        Set<String> eastSecids = new HashSet<>();
        Set<String> cbCodes = new HashSet<>();
        for (String secid : requiredSecids) {
            String code = secid.substring(secid.indexOf('.') + 1);
            if (isConvertibleBond(code)) {
                cbCodes.add(code);
            } else {
                eastSecids.add(secid);
            }
        }
        Map<String, Quote> fetched = new HashMap<>();
        if (!eastSecids.isEmpty()) {
            Map<String, Quote> em = eastMoney.batchQuote(eastSecids);
            fetched.putAll(em);
            // 东财缺失的股票/指数走腾讯兜底(收益率指数等腾讯无覆盖,跳过)
            Set<String> missing = new HashSet<>(eastSecids);
            missing.removeAll(em.keySet());
            missing.removeIf(s -> s.startsWith("103."));
            if (!missing.isEmpty()) {
                fetched.putAll(tencent.batchQuote(missing));
            }
        }
        if (!cbCodes.isEmpty()) {
            fetched.putAll(tencent.batchConvertibleBondQuote(cbCodes));
        }
        if (fetched.isEmpty()) {
            return Map.of();
        }
        for (Quote q : fetched.values()) {
            memoryCache.put(q.secid(), q);
        }
        batchUpsertCache(fetched.values());
        return fetched;
    }

    private void batchUpsertCache(java.util.Collection<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }
        java.util.Set<String> secids = quotes.stream().map(Quote::secid).collect(java.util.stream.Collectors.toSet());
        java.util.List<QuoteCache> existing = quoteCacheMapper.selectList(new LambdaQueryWrapper<QuoteCache>()
                .in(QuoteCache::getSecid, secids));
        java.util.Map<String, QuoteCache> map = existing.stream()
                .collect(java.util.stream.Collectors.toMap(QuoteCache::getSecid, c -> c, (a, b) -> a));

        for (Quote q : quotes) {
            QuoteCache c = map.get(q.secid());
            if (c == null) {
                c = new QuoteCache();
                c.setSecid(q.secid());
                c.setName(q.name());
                c.setPrice(q.price());
                c.setPctChg(q.pctChg());
                c.setQuoteTs(q.quoteTs());
                try {
                    quoteCacheMapper.insert(c);
                } catch (DuplicateKeyException e) {
                    QuoteCache conflict = quoteCacheMapper.selectOne(new LambdaQueryWrapper<QuoteCache>()
                            .eq(QuoteCache::getSecid, q.secid()));
                    if (conflict != null) {
                        conflict.setName(q.name());
                        conflict.setPrice(q.price());
                        conflict.setPctChg(q.pctChg());
                        conflict.setQuoteTs(q.quoteTs());
                        quoteCacheMapper.updateById(conflict);
                    }
                }
            } else {
                c.setName(q.name());
                c.setPrice(q.price());
                c.setPctChg(q.pctChg());
                c.setQuoteTs(q.quoteTs());
                quoteCacheMapper.updateById(c);
            }
        }
    }

    /**
     * 判断内存缓存中是否存在指定 secid 且尚未过期的行情。
     * 纯内存判定，绝不触发外部网络 I/O。
     */
    public boolean hasFreshQuote(String secid) {
        if (secid == null) {
            return false;
        }
        Quote q = memoryCache.get(secid);
        return q != null && !isExpired(q);
    }

    /**
     * 批量确保一组 secids 的行情新鲜可用:
     * 仅对内存中缺失或已过期的标的收集后执行单次合并批量网络拉取，杜绝循环单只请求。
     */
    public void ensureFreshQuotes(Set<String> secids) {
        if (secids == null || secids.isEmpty()) {
            return;
        }
        Set<String> missing = new HashSet<>();
        for (String secid : secids) {
            if (!hasFreshQuote(secid)) {
                missing.add(secid);
            }
        }
        if (!missing.isEmpty()) {
            refreshQuotes(missing);
        }
    }

    /**
     * 取有效行情。交易时段严格新鲜度;非交易时段使用最近一个交易日的快照。
     * 若未命中或已过期，自动触发按需实时刷新补采。
     */
    public Quote getFreshQuote(String secid) {
        Quote q = memoryCache.get(secid);
        if (q == null || isExpired(q)) {
            Map<String, Quote> refreshed = refreshQuotes(Set.of(secid));
            if (refreshed != null && refreshed.containsKey(secid)) {
                q = refreshed.get(secid);
            }
        }
        if (q == null) {
            return null;
        }
        return isExpired(q) ? null : q;
    }

    public int getCachedQuoteCount() {
        return memoryCache.size();
    }

    private boolean isExpired(Quote q) {
        if (q == null || q.quoteTs() == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        if (TradingCalendar.isTradingTime(now)) {
            long age = java.time.Duration.between(q.quoteTs(), now).getSeconds();
            return age > properties.getQuote().getFreshSeconds();
        }
        LocalDate lastTradingDay = lastTradingDay(now.toLocalDate());
        return q.quoteTs().toLocalDate().isBefore(lastTradingDay);
    }

    private LocalDate lastTradingDay(LocalDate date) {
        LocalDate d = date;
        while (!TradingCalendar.isTradingDay(d)) {
            d = d.minusDays(1);
        }
        return d;
    }

    private boolean isConvertibleBond(String code) {
        return code.startsWith("110") || code.startsWith("111")
                || code.startsWith("113") || code.startsWith("118")
                || code.startsWith("123") || code.startsWith("127");
    }

    private Quote toQuote(QuoteCache c) {
        return new Quote(c.getSecid(), c.getName(), c.getPrice(), c.getPctChg(), c.getQuoteTs(), "db");
    }

    /**
     * 获取全天真实分时走势分钟线 (240+ 点集)。
     * 适用于场内交易型基金 (如 510300 ETF 等) 以及大盘行业基准指数。
     */
    public List<EstimatePointView> getIntradayTrends(Fund fund) {
        if (fund == null || fund.getPrevNav() == null) {
            return List.of();
        }
        // 1. 如果是场内基金 (如 510300, 159919 等): 直接拉取交易所全天真实分钟线
        if (com.fund.valuation.common.SecCodeConverter.isOnMarket(fund.getCode(), fund.getName())) {
            String secid = com.fund.valuation.common.SecCodeConverter.toSecid(fund.getCode());
            List<EstimatePointView> points = tencent.fetchMinuteTrends(secid, fund.getPrevNav());
            if (points != null && !points.isEmpty()) {
                return points;
            }
        }
        // 2. 如果是指数基金 (如 008888 跟踪 0.399959): 直接拉取基准指数的全天真实分钟线
        if (fund.getTrackIndex() != null) {
            List<EstimatePointView> points = tencent.fetchMinuteTrends(fund.getTrackIndex(), fund.getPrevNav());
            if (points != null && !points.isEmpty()) {
                return points;
            }
        }
        return List.of();
    }
}