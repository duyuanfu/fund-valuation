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

        // 再次写入必须替换旧文案
        NoticeView replaced = service.updateNotice("系统公告：全新覆盖文案", "info", true, false);
        assertNotNull(replaced);
        assertEquals("系统公告：全新覆盖文案", replaced.message());
        assertFalse(replaced.closable());

        // 管理员关闭自定义公告后，用户端不再展示任何公告
        service.updateNotice("系统公告：全新覆盖文案", "info", false, false);
        assertNull(service.getActiveNotice(), "关闭自定义公告后应对用户隐藏公告");
    }
}
