package com.fund.valuation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 推送管理:按用户维护在线连接,盘中广播估值更新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /**
     * 为用户注册连接,返回 emitter(替换并关闭旧连接)。
     */
    public SseEmitter register(String userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        SseEmitter old = emitters.put(userId, emitter);
        if (old != null) {
            try {
                old.complete();
            } catch (Exception ignored) {
            }
        }

        Runnable cleanup = () -> emitters.remove(userId, emitter);

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        emitter.onError(e -> {
            cleanup.run();
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
        });
        return emitter;
    }

    /**
     * 向用户推送估值列表。发送失败则移除并彻底关闭该连接。
     */
    public void push(String userId, List<EstimateResult> estimates) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("estimate")
                    .data(objectMapper.writeValueAsString(estimates)));
        } catch (Exception e) {
            emitters.remove(userId, emitter);
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
            log.debug("sse push to {} failed, closed and removed: {}", userId, e.getMessage());
        }
    }
}
