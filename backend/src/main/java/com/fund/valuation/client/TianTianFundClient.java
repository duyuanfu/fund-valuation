package com.fund.valuation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.common.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 天天基金数据客户端:基础信息、前十大重仓、资产配置。
 * 该数据源存在反爬限流(61136403),所有调用经限流器与指数退避。
 */
@Slf4j
@Component
public class TianTianFundClient {

    private static final String BASIC_INFO_URL = "https://fundmobapi.eastmoney.com/FundMApi/FundBaseTypeInformation.ashx";
    private static final String MOBILE_HOLDINGS_URL = "https://fundmobapi.eastmoney.com/FundMNewApi/FundMNInverstPosition";
    private static final String MOBILE_ALLOC_URL = "https://fundmobapi.eastmoney.com/FundMNewApi/FundMNAssetAllocationNew";
    private static final String JJCC_URL = "https://fundf10.eastmoney.com/FundArchivesDatas.aspx";
    private static final String ZCPZ_PAGE_PREFIX = "https://fundf10.eastmoney.com/zcpz_";
    private static final String REFERER_F10 = "https://fundf10.eastmoney.com/";
    private static final int RATE_LIMIT_RETRIES = 4;

    private final HttpSupport http;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter = new RateLimiter(600);

    public TianTianFundClient(HttpSupport http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper;
    }

    /**
     * 拉取基金基础信息(名称、官方类型、昨日净值、净值日期)。
     */
    public FundBasicInfo fetchBasicInfo(String code) {
        String url = HttpSupport.buildUrl(BASIC_INFO_URL, Map.of(
                "FCODE", code,
                "deviceid", "Wap",
                "plat", "Wap",
                "product", "EFund",
                "version", "2.0.0"
        ));
        String body = fetchWithRateLimitRetry(url, null);
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("Datas");
            if (data.isMissingNode() || !data.hasNonNull("FCODE")) {
                log.warn("no basic info for fund {}", code);
                return null;
            }
            LocalDate navDate = null;
            if (data.hasNonNull("FSRQ") && !data.path("FSRQ").asText().isBlank()) {
                try {
                    navDate = LocalDate.parse(data.path("FSRQ").asText());
                } catch (Exception ignored) {
                }
            }
            BigDecimal prevNav = null;
            if (data.hasNonNull("DWJZ") && !data.path("DWJZ").asText().isBlank()) {
                try {
                    prevNav = new BigDecimal(data.path("DWJZ").asText());
                } catch (Exception ignored) {
                }
            }
            return new FundBasicInfo(
                    data.path("FCODE").asText(),
                    data.path("SHORTNAME").asText(),
                    data.path("FTYPE").asText(),
                    prevNav,
                    navDate
            );
        } catch (Exception e) {
            throw new IllegalStateException("parse basic info failed for " + code, e);
        }
    }

    /**
     * 拉取前十大重仓股(含报告期)。
     * 首选: 天天基金移动端官方纯 JSON 接口 (FundMNInverstPosition)，字段干净精准，绝无排版移位；
     * 兜底: 网页端 F10 (FundArchivesDatas.aspx) 表格自适应解析。
     */
    public HoldingData fetchTopHoldings(String code) {
        // 1. 首选天天基金移动端纯 JSON 接口 (接口 1)
        HoldingData mobile = fetchHoldingsFromMobileApi(code);
        if (mobile != null && mobile.rows() != null && !mobile.rows().isEmpty()) {
            return mobile;
        }
        log.info("fund {} mobile holdings empty or failed, fallback to F10", code);
        return fetchHoldingsFromF10(code);
    }

    private HoldingData fetchHoldingsFromMobileApi(String code) {
        String url = HttpSupport.buildUrl(MOBILE_HOLDINGS_URL, Map.of(
                "FCODE", code,
                "deviceid", "Wap",
                "plat", "Wap",
                "product", "EFund",
                "version", "2.0.0"
        ));
        try {
            String body = fetchWithRateLimitRetry(url, null);
            if (body == null || body.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("Datas");
            if (data.isMissingNode() || data.isNull()) {
                return null;
            }

            // 提取报告期
            String reportDate = null;
            if (data.hasNonNull("FSRQ") && !data.path("FSRQ").asText().isBlank()) {
                reportDate = data.path("FSRQ").asText().trim();
            } else if (data.hasNonNull("REPORTDATE") && !data.path("REPORTDATE").asText().isBlank()) {
                reportDate = data.path("REPORTDATE").asText().trim();
            }

            JsonNode stockList = data.path("fundStocks");
            if (stockList.isMissingNode() || !stockList.isArray() || stockList.isEmpty()) {
                return null;
            }

            List<HoldingRow> rows = new ArrayList<>();
            for (JsonNode item : stockList) {
                String stockCode = item.path("GPDM").asText().trim();
                String stockName = item.path("GPJC").asText().trim();
                String weightStr = item.path("JZBL").asText().trim();
                if (!stockCode.matches("\\d{6}")) {
                    continue;
                }
                BigDecimal weight = null;
                try {
                    weight = new BigDecimal(weightStr);
                } catch (Exception ignored) {
                }
                // 合规性校验: 占净值比例必须在合理范围 (0 ~ 100%)
                if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0 || weight.compareTo(BigDecimal.valueOf(100)) > 0) {
                    continue;
                }
                rows.add(new HoldingRow(stockCode, stockName, weight));
                if (rows.size() >= 10) {
                    break;
                }
            }
            return rows.isEmpty() ? null : new HoldingData(reportDate, rows);
        } catch (Exception e) {
            log.warn("fetch mobile holdings failed for {}: {}", code, e.getMessage());
            return null;
        }
    }

    private HoldingData fetchHoldingsFromF10(String code) {
        String url = HttpSupport.buildUrl(JJCC_URL, Map.of(
                "type", "jjcc",
                "code", code,
                "topline", "10",
                "year", "",
                "month", ""
        ));
        String body = fetchWithRateLimitRetry(url, REFERER_F10);
        String html = extractContentJs(body);
        if (html == null || html.isBlank()) {
            log.warn("empty holdings content for fund {}", code);
            return null;
        }
        Document doc = Jsoup.parse(html);
        String reportDate = extractReportDate(doc);
        List<HoldingRow> rows = new ArrayList<>();
        // 只选择第一个最新季报的表格，避免跨季度多表格累加
        Element firstTable = doc.selectFirst("table.tzxq");
        if (firstTable == null) {
            log.warn("empty holdings content for fund {}", code);
            return null;
        }

        // 动态匹配表头列索引
        int codeCol = 1;
        int nameCol = 2;
        int weightCol = 6;
        Elements ths = firstTable.select("thead tr th, tr th");
        for (int i = 0; i < ths.size(); i++) {
            String th = ths.get(i).text();
            if (th.contains("代码")) {
                codeCol = i;
            } else if (th.contains("名称")) {
                nameCol = i;
            } else if (th.contains("占净值") || th.contains("比例")) {
                weightCol = i;
            }
        }

        Elements trs = firstTable.select("tbody tr");
        if (trs.isEmpty()) {
            trs = firstTable.select("tr:gt(0)");
        }

        for (Element tr : trs) {
            Elements tds = tr.select("td");
            if (tds.size() <= Math.max(codeCol, Math.max(nameCol, weightCol))) {
                continue;
            }
            String codeRaw = tds.get(codeCol).text().trim();
            String name = tds.get(nameCol).text().trim();
            // 纯数字 6 位股票代码有效性校验
            if (!codeRaw.matches("\\d{6}")) {
                continue;
            }

            // 提取占净值比例
            String weightStr = tds.get(weightCol).text().replace("%", "").trim();
            BigDecimal weight = null;
            try {
                weight = new BigDecimal(weightStr);
            } catch (Exception ignored) {
            }

            // 若匹配列未得到有效数值或数值超标(0 ~ 100%)，扫描该行中带有 % 且数值在合规范围内的单元格兜底
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0 || weight.compareTo(BigDecimal.valueOf(100)) > 0) {
                for (Element td : tds) {
                    String text = td.text();
                    if (text.contains("%")) {
                        try {
                            BigDecimal candidate = new BigDecimal(text.replace("%", "").trim());
                            if (candidate.compareTo(BigDecimal.ZERO) > 0 && candidate.compareTo(BigDecimal.valueOf(100)) <= 0) {
                                weight = candidate;
                                break;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            // 最终合规性校验：任何基金单只持仓权重不可能超过 100%，绝不可能达到 900%+
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0 || weight.compareTo(BigDecimal.valueOf(100)) > 0) {
                log.warn("fund {} stock {} invalid weight ignored: {}", code, codeRaw, weightStr);
                continue;
            }

            rows.add(new HoldingRow(codeRaw, name, weight));
            if (rows.size() >= 10) {
                break; // 只取前十大重仓
            }
        }
        return new HoldingData(reportDate, rows);
    }

    /**
     * 拉取资产配置(股票/债券/现金占净值)。
     * 首选: 移动端官方纯 JSON 接口 (FundMNAssetAllocationNew), 直接提取 GP(股票)、ZQ(债券)、HB(货币现金)
     * 兜底: 网页端 F10 (zcpz_{code}.html) 表格自适应解析
     */
    public AssetAllocData fetchAssetAlloc(String code) {
        AssetAllocData mobile = fetchAssetAllocFromMobileApi(code);
        if (mobile != null) {
            return mobile;
        }
        log.info("fund {} mobile asset alloc empty, fallback to F10 HTML", code);
        return fetchAssetAllocFromF10(code);
    }

    private AssetAllocData fetchAssetAllocFromMobileApi(String code) {
        String url = HttpSupport.buildUrl(MOBILE_ALLOC_URL, Map.of(
                "FCODE", code,
                "deviceid", "Wap",
                "plat", "Wap",
                "product", "EFund",
                "version", "2.0.0"
        ));
        try {
            String body = fetchWithRateLimitRetry(url, null);
            if (body == null || body.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("Datas");
            if (data.isMissingNode() || data.isNull()) {
                return null;
            }
            JsonNode target = data;
            if (data.isArray()) {
                if (data.isEmpty()) {
                    return null;
                }
                target = data.get(0);
            }
            // 提取报告期
            LocalDate reportDate = null;
            String dateStr = target.path("FSRQ").asText();
            if (dateStr == null || dateStr.isBlank()) {
                dateStr = target.path("REPORTDATE").asText();
            }
            if (dateStr != null && !dateStr.isBlank()) {
                try {
                    reportDate = LocalDate.parse(dateStr.trim());
                } catch (Exception ignored) {
                }
            }
            if (reportDate == null) {
                reportDate = LocalDate.now();
            }
            BigDecimal stock = parsePct(target.path("GP").asText());
            BigDecimal bond = parsePct(target.path("ZQ").asText());
            BigDecimal cash = parsePct(target.path("HB").asText());
            return new AssetAllocData(reportDate, stock, bond, cash);
        } catch (Exception e) {
            log.debug("fetch mobile asset alloc failed for {}: {}", code, e.getMessage());
            return null;
        }
    }

    private AssetAllocData fetchAssetAllocFromF10(String code) {
        String url = ZCPZ_PAGE_PREFIX + code + ".html";
        // zcpz 页面为 GBK 编码,按 GBK 解码
        byte[] bytes = fetchWithRateLimitRetryBytes(url, REFERER_F10);
        String body = new String(bytes, java.nio.charset.Charset.forName("GBK"));
        Document doc = Jsoup.parse(body);
        Element table = doc.select("table.tzxq").first();
        if (table == null) {
            log.warn("no asset alloc table for fund {}", code);
            return null;
        }
        Element firstRow = table.select("tbody tr").first();
        if (firstRow == null) {
            return null;
        }
        Elements tds = firstRow.select("td");
        if (tds.size() < 4) {
            return null;
        }
        try {
            LocalDate reportDate = LocalDate.parse(tds.get(0).text().trim());
            BigDecimal stock = parsePct(tds.get(1).text());
            BigDecimal bond = parsePct(tds.get(2).text());
            BigDecimal cash = parsePct(tds.get(3).text());
            return new AssetAllocData(reportDate, stock, bond, cash);
        } catch (Exception e) {
            log.warn("parse asset alloc failed for {}: {}", code, e.getMessage());
            return null;
        }
    }

    /**
     * 解析 FundArchivesDatas 返回的 var apidata={ content:"<html>"} 中的 HTML。
     */
    private String extractContentJs(String body) {
        int start = body.indexOf("content:\"");
        if (start < 0) {
            return null;
        }
        start += "content:\"".length();
        int end = body.lastIndexOf('"');
        if (end <= start) {
            return null;
        }
        return body.substring(start, end);
    }

    private String extractReportDate(Document doc) {
        Element label = doc.selectFirst("h4.t");
        if (label == null) {
            return null;
        }
        String text = label.text();
        int idx = text.lastIndexOf("截止至");
        if (idx < 0) {
            return null;
        }
        var matcher = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(text.substring(idx));
        return matcher.find() ? matcher.group(1) : null;
    }

    private BigDecimal parsePct(String text) {
        if (text == null) {
            return BigDecimal.ZERO;
        }
        String t = text.replace("%", "").replace(" ", "").replace("\u00A0", "").replace("—", "").trim();
        if (t.isEmpty() || t.matches("^[-–—]+$")) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(t);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 带限流器与业务限流码重试的请求(UTF-8)。
     */
    private String fetchWithRateLimitRetry(String url, String referer) {
        return new String(fetchWithRateLimitRetryBytes(url, referer), StandardCharsets.UTF_8);
    }

    /**
     * 带限流器与业务限流码重试的请求(原始字节)。
     */
    private byte[] fetchWithRateLimitRetryBytes(String url, String referer) {
        rateLimiter.acquire();
        for (int attempt = 0; attempt < RATE_LIMIT_RETRIES; attempt++) {
            try {
                byte[] body = http.getBytes(url, referer);
                String text = new String(body, StandardCharsets.UTF_8);
                if (!HttpSupport.isRateLimitedCode(text)) {
                    return body;
                }
                log.warn("fund api rate limited on {}, retry {}", url, attempt + 1);
            } catch (Exception e) {
                log.debug("fund api error attempt {}: {}", attempt + 1, e.getMessage());
            }
            sleepBackoff(attempt);
            rateLimiter.acquire();
        }
        throw new IllegalStateException("fund api unavailable after retries: " + url);
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(1000L * (1L << Math.min(attempt, 4)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
