package com.fund.valuation.service;

import com.fund.valuation.dto.NoticeView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统公告持久化回归测试:
 * 覆盖 Admin 每次写入替换旧文案、事务落库、关闭后对用户不展示等核心诉求。
 */
@SpringBootTest
class SystemNoticePersistenceTest {

    @Autowired
    private SystemNoticeService service;

    @Autowired
    private DataSource dataSource;

    @AfterEach
    void cleanup() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM sys_notice");
        }
    }

    @Test
    void testPersistReplaceAndDisable() throws Exception {
        NoticeView first = service.updateNotice("第一条公告", "info", true, true);
        assertNotNull(first);
        assertEquals("第一条公告", first.message());

        // 第二次写入必须替换旧文案
        NoticeView second = service.updateNotice("第二条替换公告", "success", true, false);
        assertEquals("第二条替换公告", second.message());

        // 数据库中仅保留最新一条，证明是覆盖替换而非追加
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM sys_notice")) {
            rs.next();
            assertEquals(1, rs.getInt(1), "每次写入应替换旧文案，仅保留一条记录");
        }

        // 关闭自定义公告后，用户端不再展示
        service.updateNotice("第二条替换公告", "success", false, false);
        assertNull(service.getActiveNotice(), "关闭自定义公告后应对用户隐藏");

        // 但后台仍可回显配置文案与关闭状态
        NoticeView configured = service.getConfiguredNotice();
        assertNotNull(configured);
        assertEquals("第二条替换公告", configured.message());
        assertFalse(configured.custom(), "关闭后 custom=false 表示未启用");
    }
}
