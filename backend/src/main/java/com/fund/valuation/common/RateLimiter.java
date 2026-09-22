package com.fund.valuation.common;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单令牌节流器:保证相邻两次放行之间的最小间隔。
 * CAS 循环实现,并发线程轮流获得放行,间隔严格不小于配置值。
 */
public class RateLimiter {

    private final long minIntervalNs;
    private final AtomicLong lastAllowedNanos = new AtomicLong(0);

    public RateLimiter(long minIntervalMs) {
        this.minIntervalNs = minIntervalMs * 1_000_000L;
    }

    /**
     * 阻塞直到满足最小请求间隔。
     */
    public void acquire() {
        while (true) {
            long now = System.nanoTime();
            long last = lastAllowedNanos.get();
            if (now - last >= minIntervalNs) {
                if (lastAllowedNanos.compareAndSet(last, now)) {
                    return;
                }
                continue;
            }
            long sleepNs = last + minIntervalNs - now;
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}