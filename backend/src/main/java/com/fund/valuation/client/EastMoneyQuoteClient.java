package com.fund.valuation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 东方财富 push2 行情客户端(股票/指数)。
 * 批量接口 ulist.np + fltt=2 直接返回小数价格。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EastMoneyQuoteClient {

    private static final String BATCH_PATH = "/api/qt/ulist.np/get";
    private static final String SINGLE_PATH = "/api/qt/stock/get";
    private static final List<String> HOSTS = List.of(
            "https://push2.eastmoney.com",
            "https://push2delay.eastmoney.com"
    );
    private static final int BATCH_LIMIT = 80;

    private final HttpSupport http;
    private final ObjectMapper objectMapper;

    /**
     * 批量拉取行情,返回 secid -> Quote 映射(仅返回有数据的标的)。
     */
    public Map<String, Quote> batchQuote(Iterable<String> secids) {
        Map<String, Quote> result = new HashMap<>();
        java.util.List<String> list = new java.util.ArrayList<>();
        secids.forEach(list::add);

        for (int i = 0; i < list.size(); i += BATCH_LIMIT) {
            int end = Math.min(i + BATCH_LIMIT, list.size());
            StringJoiner joiner = new StringJoiner(",");
            for (int j = i; j < end; j++) {
                joiner.add(list.get(j));
            }
            result.putAll(fetchBatch(joiner.toString()));
        }
        return result;
    }

    private Map<String, Quote> fetchBatch(String secidList) {
        // 多节点轮换:主节点被限流时自动切换备用节点
        for (String host : HOSTS) {
            Map<String, Quote> result = tryFetchBatch(host + BATCH_PATH, secidList);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return Map.of();
    }

    private Map<String, Quote> tryFetchBatch(String url, String secidList) {
        Map<String, Quote> result = new HashMap<>();
        String fullUrl = HttpSupport.buildUrl(url, Map.of(
                "secids", secidList,
                "fields", "f2,f3,f4,f12,f13,f14",
                "fltt", "2"
        ));
        try {
            String body = http.getString(fullUrl, "https://quote.eastmoney.com/", 1);
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (data.isMissingNode() || !data.hasNonNull("diff")) {
                return result;
            }
            for (JsonNode item : data.path("diff")) {
                if (!item.hasNonNull("f12")) {
                    continue;
                }
                String code = item.path("f12").asText();
                int market = item.path("f13").asInt();
                String secid = market + "." + code;
                BigDecimal price = item.path("f2").decimalValue();
                BigDecimal pct = item.path("f3").decimalValue().setScale(4, RoundingMode.HALF_UP);
                String name = item.path("f14").asText();
                result.put(secid, new Quote(secid, name, price, pct, LocalDateTime.now(), "eastmoney"));
            }
        } catch (Exception e) {
            log.debug("batch quote failed on {} for {}, will fallback: {}", url, secidList, e.getMessage());
        }
        return result;
    }

    /**
     * 单只行情兜底(多节点轮换)。
     */
    public Quote singleQuote(String secid) {
        for (String host : HOSTS) {
            Quote q = trySingleQuote(host + SINGLE_PATH, secid);
            if (q != null) {
                return q;
            }
        }
        return null;
    }

    private Quote trySingleQuote(String url, String secid) {
        String fullUrl = HttpSupport.buildUrl(url, Map.of(
                "secid", secid,
                "fields", "f43,f44,f45,f46,f57,f58,f169,f170",
                "fltt", "2"
        ));
        try {
            String body = http.getString(fullUrl, "https://quote.eastmoney.com/");
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (data.isMissingNode() || !data.hasNonNull("f43")) {
                return null;
            }
            BigDecimal price = data.path("f43").decimalValue();
            BigDecimal pct = data.path("f170").decimalValue().setScale(4, RoundingMode.HALF_UP);
            String name = data.path("f58").asText();
            return new Quote(secid, name, price, pct, LocalDateTime.now(), "eastmoney");
        } catch (Exception e) {
            log.warn("single quote failed on {} for {}: {}", url, secid, e.getMessage());
            return null;
        }
    }
}
