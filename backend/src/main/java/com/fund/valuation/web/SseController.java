package com.fund.valuation.web;

import com.fund.valuation.config.JwtAuthFilter;
import com.fund.valuation.service.EstimateResult;
import com.fund.valuation.service.SseService;
import com.fund.valuation.service.WatchlistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;
    private final WatchlistService watchlistService;

    /**
     * SSE 实时估值流:连接建立后立即返回 Emitter，后台异步推送当前快照，盘中推增量。身份来自 JWT(token 查询参数)。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request) {
        String userId = (String) request.getAttribute(JwtAuthFilter.ATTR_USERNAME);
        SseEmitter emitter = sseService.register(userId);
        CompletableFuture.runAsync(() -> {
            try {
                List<EstimateResult> snapshot = watchlistService.estimates(userId);
                sseService.push(userId, snapshot);
            } catch (Exception e) {
                log.debug("initial sse snapshot push skipped for {}: {}", userId, e.getMessage());
            }
        });
        return emitter;
    }
}
