package com.fund.valuation.client;

import com.fund.valuation.common.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 统一 HTTP 客户端封装:超时、指数退避重试、UA 轮换、限流。
 */
@Slf4j
@Component
public class HttpSupport {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 1000;
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
    );

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile RateLimiter rateLimiter;

    /**
     * 设置全局请求限流(对易触发反爬的接口)。
     */
    public void setRateLimiter(long minIntervalMs) {
        this.rateLimiter = new RateLimiter(minIntervalMs);
    }

    /**
     * 发起 GET 请求并返回原始字节内容。
     */
    public byte[] getBytes(String url, String referer) {
        return getBytes(url, referer, MAX_ATTEMPTS);
    }

    public byte[] getBytes(String url, String referer, int attempts) {
        if (rateLimiter != null) {
            rateLimiter.acquire();
        }
        Exception lastErr = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (attempt > 0) {
                long delay = BASE_BACKOFF_MS * (1L << attempt)
                        + ThreadLocalRandom.current().nextLong(0, 500);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted", e);
                }
            }
            try {
                String ua = USER_AGENTS.get(attempt % USER_AGENTS.size());
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(4))
                        .header("User-Agent", ua)
                        .header("Accept", "*/*");
                if (referer != null && !referer.isBlank()) {
                    builder.header("Referer", referer);
                }
                HttpResponse<byte[]> resp = client.send(builder.build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200) {
                    return resp.body();
                }
                lastErr = new IllegalStateException("HTTP " + resp.statusCode() + " for " + url);
                if (isRateLimited(resp.statusCode())) {
                    log.warn("rate limited (status {}) on {}, backing off", resp.statusCode(), url);
                    TimeUnit.MILLISECONDS.sleep(BASE_BACKOFF_MS * 3);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            } catch (Exception e) {
                lastErr = e;
                log.debug("request failed attempt {}: {}", attempt + 1, e.getMessage());
            }
        }
        throw new IllegalStateException("request failed after " + attempts + " attempts: " + url, lastErr);
    }

    public String getString(String url, String referer) {
        return new String(getBytes(url, referer), StandardCharsets.UTF_8);
    }

    public String getString(String url, String referer, int attempts) {
        return new String(getBytes(url, referer, attempts), StandardCharsets.UTF_8);
    }

    public String getString(String url, String referer, Charset charset) {
        return new String(getBytes(url, referer), charset);
    }

    /**
     * 识别天天基金接口返回的业务限流码(如 61136403)。
     */
    public static boolean isRateLimitedCode(String json) {
        return json != null && (json.contains("61136403") || json.contains("ErrCode\":1"));
    }

    private static boolean isRateLimited(int status) {
        return status == 403 || status == 429;
    }

    public static String buildUrl(String base, java.util.Map<String, String> params) {
        StringBuilder sb = new StringBuilder(base);
        boolean first = true;
        for (var e : params.entrySet()) {
            sb.append(first ? '?' : '&');
            first = false;
            sb.append(e.getKey()).append('=');
            // secids 等逗号分隔参数保持原始逗号(部分行情网关不接受 %2C)
            String encoded = URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                    .replace("%2C", ",");
            sb.append(encoded);
        }
        return sb.toString();
    }
}
