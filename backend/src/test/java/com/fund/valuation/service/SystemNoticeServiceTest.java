package com.fund.valuation.service;

import com.fund.valuation.config.AppProperties;
import com.fund.valuation.dto.NoticeView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemNoticeServiceTest {

    @Test
    void testInitialNoticeFromProperties() {
        AppProperties props = new AppProperties();
        props.getNotice().setEnabled(true);
        props.getNotice().setMessage("测试公告内容");
        props.getNotice().setType("info");

        SystemNoticeService service = new SystemNoticeService(props);
        service.init();

        NoticeView view = service.getActiveNotice();
        assertNotNull(view);
        assertEquals("测试公告内容", view.message());
        assertEquals("info", view.type());
        assertTrue(view.custom());
        assertTrue(view.closable());
    }

    @Test
    void testUpdateNoticeByAdmin() {
        AppProperties props = new AppProperties();
        props.getNotice().setEnabled(false);

        SystemNoticeService service = new SystemNoticeService(props);
        service.init();

        // 动态发布新公告
        NoticeView updated = service.updateNotice("系统公告：支持排序", "success", true, true);
        assertNotNull(updated);
        assertEquals("系统公告：支持排序", updated.message());
        assertEquals("success", updated.type());
        assertTrue(updated.custom());

        // 动态关闭公告
        service.updateNotice(null, "info", false, false);
        // 关闭后不再是 custom 公告 (可能是 null 或 兜底非交易时段提示)
        NoticeView fallback = service.getActiveNotice();
        if (fallback != null) {
            assertFalse(fallback.custom());
            assertEquals("当前为非交易时段，展示最新行情快照", fallback.message());
        }
    }
}
