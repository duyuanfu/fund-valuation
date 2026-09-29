package com.fund.valuation.web;

import com.fund.valuation.dto.NoticeView;
import com.fund.valuation.service.SystemNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final SystemNoticeService noticeService;

    /**
     * 获取当前有效公告或非交易时段状态提示。
     */
    @GetMapping
    public ResponseEntity<NoticeView> getNotice() {
        return ResponseEntity.ok(noticeService.getActiveNotice());
    }
}
