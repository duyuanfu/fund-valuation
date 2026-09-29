package com.fund.valuation.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    /**
     * 捕获并静默处理 SSE 长连接或客户端主动关闭浏览器/刷新页面引发的连接中断异常。
     * 这类异常属于客户端网络正常挂断(Broken pipe / Connection abort / 中止了一个已建立的连接)，
     * 此时客户端连接已不存在，无需且无法向已断开的 socket 写回响应，记录 DEBUG 日志即可。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncDisconnect(AsyncRequestNotUsableException e) {
        log.debug("client disconnected async sse stream: {}", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        // 客户端连接中断类异常 (Broken pipe, Socket closed, 主机中止连接) 静默忽略
        if (isClientAbortException(e)) {
            log.debug("client aborted connection: {}", e.getMessage());
            return null;
        }
        log.error("unhandled error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "内部错误: " + e.getMessage()));
    }

    private boolean isClientAbortException(Throwable e) {
        if (e == null) {
            return false;
        }
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("broken pipe") || msg.contains("connection reset")
                || msg.contains("中止了一个已建立的连接") || msg.contains("connection abort")) {
            return true;
        }
        return isClientAbortException(e.getCause());
    }
}