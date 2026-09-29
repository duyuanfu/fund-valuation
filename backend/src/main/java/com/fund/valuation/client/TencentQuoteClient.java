package com.fund.valuation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.dto.EstimatePointView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 腾讯行情客户端(qt.gtimg.cn)。
 * 用于可转债及海外/云服务器（东财 push2 限流时）跨域稳定兜底，
 * 以及提供全市场交易所全天 240+ 真实分时走势分钟线。
 */
@Slf4j
@Component
public class TencentQuoteClient {

    private static final String URL = "https://qt.gtimg.cn/q=";
    private static final int BATCH_LIMIT = 60;

    private final HttpSupport http;
    private final ObjectMapper objectMapper;

    public TencentQuoteClient(HttpSupport http) {
        this(http, new ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TencentQuoteClient(HttpSupport http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * 批量拉取可转债行情,入参为 6 位代码(如 113050)。
     */
    public Map<String, Quote> batchConvertibleBondQuote(Iterable<String> codes) {
        Map<String, Quote> result = new HashMap<>();
        java.util.List<String> list = new java.util.ArrayList<>();
        codes.forEach(list::add);

        for (int i = 0; i < list.size(); i += BATCH_LIMIT) {
            int end = Math.min(i + BATCH_LIMIT, list.size());
            StringJoiner joiner = new StringJoiner(",");
            for (int j = i; j < end; j++) {
                String code = list.get(j);
                joiner.add(code.startsWith("12") ? "sz" + code : "sh" + code);
            }
            fetchAndParse(joiner.toString(), result);
        }
        return result;
    }

    /**
     * 批量拉取股票/指数行情(兜底通道),入参为东财 secid(如 1.600519 / 0.000300 / 1.000300)。
     */
    public Map<String, Quote> batchQuote(Iterable<String> secids) {
        Map<String, Quote> result = new HashMap<>();
        java.util.List<String> list = new java.util.ArrayList<>();
        secids.forEach(list::add);

        for (int i = 0; i < list.size(); i += BATCH_LIMIT) {
            int end = Math.min(i + BATCH_LIMIT, list.size());
            StringJoiner joiner = new StringJoiner(",");
            for (int j = i; j < end; j++) {
                String secid = list.get(j);
                if (secid.contains(".")) {
                    String prefix = secid.startsWith("1.") ? "sh" : "sz";
                    joiner.add(prefix + secid.substring(secid.indexOf('.') + 1));
                }
            }
            fetchAndParse(joiner.toString(), result);
        }
        return result;
    }

    private void fetchAndParse(String symbols, Map<String, Quote> result) {
        if (symbols.isBlank()) {
            return;
        }
        try {
            byte[] bytes = http.getBytes(URL + symbols, "https://gu.qq.com/");
            Charset gbkCharset;
            try {
                gbkCharset = Charset.forName("GBK");
            } catch (Exception e) {
                gbkCharset = StandardCharsets.UTF_8;
            }
            parseResponse(new String(bytes, gbkCharset), result);
        } catch (Exception e) {
            log.warn("tencent quote request failed for [{}]: {}", symbols, e.getMessage());
        }
    }

    private void parseResponse(String body, Map<String, Quote> result) {
        for (String line : body.split(";")) {
            line = line.trim();
            if (!line.contains("~")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String symbol = line.substring(0, eq).replace("v_", "").trim().toLowerCase();
            String[] f = line.substring(eq + 1).replace("\"", "").split("~");
            if (f.length < 5) {
                continue;
            }
            String name = f[1];
            String code = f[2];
            String market = symbol.startsWith("sh") ? "1" : "0";
            String secid = market + "." + code;

            try {
                BigDecimal price = new BigDecimal(f[3]);
                BigDecimal prevClose = new BigDecimal(f[4]);
                BigDecimal pct = null;

                // 优先使用腾讯直接提供的官方涨跌幅字段 (索引 32)
                if (f.length > 32 && !f[32].isBlank()) {
                    try {
                        pct = new BigDecimal(f[32]).setScale(4, RoundingMode.HALF_UP);
                    } catch (Exception ignored) {
                    }
                }

                // 兜底通过 (现价 - 昨收) / 昨收 计算
                if (pct == null) {
                    if (prevClose.signum() != 0) {
                        pct = price.subtract(prevClose).multiply(BigDecimal.valueOf(100))
                                .divide(prevClose, 4, RoundingMode.HALF_UP);
                    } else {
                        pct = BigDecimal.ZERO;
                    }
                }

                result.put(secid, new Quote(secid, name, price, pct, LocalDateTime.now(), "tencent"));
            } catch (Exception e) {
                log.debug("skip unparsable line: {}", line);
            }
        }
    }

    /**
     * 拉取交易所分时走势分钟线 (全天 240+ 真实连续竞价分钟点集)。
     * 适用于场内交易型基金 (如 510300 ETF 等) 以及大盘行业基准指数 (如 000300, 399959 等)。
     */
    public List<EstimatePointView> fetchMinuteTrends(String secid, BigDecimal prevNav) {
        if (secid == null || prevNav == null || prevNav.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        String symbol;
        if (secid.contains(".")) {
            String prefix = secid.startsWith("1.") ? "sh" : "sz";
            symbol = prefix + secid.substring(secid.indexOf('.') + 1);
        } else {
            symbol = secid;
        }
        String url = "https://web.ifzq.gtimg.cn/appstock/app/minute/query?code=" + symbol;
        try {
            byte[] bytes = http.getBytes(url, "https://gu.qq.com/", 2);
            String body = new String(bytes, StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(body);
            JsonNode arr = root.path("data").path(symbol).path("data").path("data");
            if (arr.isMissingNode() || !arr.isArray() || arr.isEmpty()) {
                return List.of();
            }
            LocalDate today = LocalDate.now();
            List<EstimatePointView> points = new ArrayList<>();
            for (JsonNode item : arr) {
                String str = item.asText();
                String[] parts = str.split(" ");
                if (parts.length < 2) {
                    continue;
                }
                String timeStr = parts[0];
                if (timeStr.length() != 4) {
                    continue;
                }
                int hour = Integer.parseInt(timeStr.substring(0, 2));
                int min = Integer.parseInt(timeStr.substring(2, 4));
                if (hour < 9 || (hour == 9 && min < 30) || hour > 15 || (hour == 15 && min > 0)) {
                    continue;
                }
                LocalDateTime estTime = today.atTime(hour, min);
                BigDecimal price = new BigDecimal(parts[1]);
                BigDecimal pct = price.subtract(prevNav).multiply(BigDecimal.valueOf(100))
                        .divide(prevNav, 4, RoundingMode.HALF_UP);
                points.add(new EstimatePointView(estTime, price, pct));
            }
            return points;
        } catch (Exception e) {
            log.debug("fetch minute trends failed for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }
}
