package com.fund.valuation.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库平滑升级自动迁移器:
 * 在应用启动时自动检测并补齐旧版本数据库中缺失的字段与新表，
 * 解决旧数据库升级新版本时因缺失列报 MyBatisSystemException 500 的问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseAutoMigrator {

    private final DataSource dataSource;

    @PostConstruct
    public void migrate() {
        try (Connection conn = dataSource.getConnection()) {
            boolean isH2 = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");

            // 1. 补齐 user 表字段 (RBAC、VIP、登录统计、获客渠道)
            addColumnIfNotExists(conn, "user", "role", "VARCHAR(20) DEFAULT 'USER'");
            addColumnIfNotExists(conn, "user", "status", "VARCHAR(20) DEFAULT 'NORMAL'");
            addColumnIfNotExists(conn, "user", "is_vip", isH2 ? "BOOLEAN DEFAULT FALSE" : "TINYINT(1) DEFAULT 0");
            addColumnIfNotExists(conn, "user", "vip_expire_at", isH2 ? "TIMESTAMP NULL" : "DATETIME NULL");
            addColumnIfNotExists(conn, "user", "last_login_at", isH2 ? "TIMESTAMP NULL" : "DATETIME NULL");
            addColumnIfNotExists(conn, "user", "login_count", "INT DEFAULT 0");
            addColumnIfNotExists(conn, "user", "referral_source", "VARCHAR(32) NULL");

            // 2. 补齐 fund 表字段
            addColumnIfNotExists(conn, "fund", "type_raw", "VARCHAR(32) NULL");
            addColumnIfNotExists(conn, "fund", "nav_date", "DATE NULL");
            addColumnIfNotExists(conn, "fund", "fresh_flag", "VARCHAR(64) NULL");

            // 3. 补齐各新表
            executeSqlQuietly(conn, """
                CREATE TABLE IF NOT EXISTS user_position (
                    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id             VARCHAR(64)   NOT NULL,
                    fund_code           VARCHAR(10)   NOT NULL,
                    holding_amount      DECIMAL(14,2) NOT NULL,
                    yesterday_income    DECIMAL(14,2) DEFAULT 0,
                    holding_profit      DECIMAL(14,2) DEFAULT 0,
                    holding_profit_rate DECIMAL(8,4)  NULL,
                    cost_amount         DECIMAL(14,2) DEFAULT 0,
                    holding_shares      DECIMAL(14,4) NULL,
                    cost_price          DECIMAL(10,4) NULL,
                    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
                    updated_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
                )
            """);

            executeSqlQuietly(conn, """
                CREATE TABLE IF NOT EXISTS sys_calendar_holiday (
                    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                    holiday_date DATE NOT NULL UNIQUE,
                    description  VARCHAR(64),
                    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            executeSqlQuietly(conn, """
                CREATE TABLE IF NOT EXISTS sys_notice (
                    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                    notice_message VARCHAR(500),
                    notice_type    VARCHAR(20) DEFAULT 'info',
                    is_enabled     TINYINT(1) DEFAULT 1,
                    is_closable    TINYINT(1) DEFAULT 1,
                    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            executeSqlQuietly(conn, """
                CREATE TABLE IF NOT EXISTS sys_vip_config (
                    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                    monthly_price        DECIMAL(8,2) DEFAULT 2.90,
                    quarterly_price      DECIMAL(8,2) DEFAULT 6.90,
                    quarterly_orig_price DECIMAL(8,2) DEFAULT 8.70,
                    yearly_price         DECIMAL(8,2) DEFAULT 19.90,
                    yearly_orig_price    DECIMAL(8,2) DEFAULT 34.80,
                    wechat_qr_url        MEDIUMTEXT   NULL,
                    alipay_qr_url        MEDIUMTEXT   NULL,
                    payee_name           VARCHAR(64)  DEFAULT '管理员',
                    payment_tip          VARCHAR(255) DEFAULT '付款请务必备注用户名',
                    require_approval     TINYINT(1)   DEFAULT 1,
                    updated_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
                )
            """);

            log.info("Database schema check and auto-migration finished successfully.");
        } catch (Exception e) {
            log.warn("Database auto-migration skipped or encountered error: {}", e.getMessage());
        }
    }

    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnDef) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            boolean exists = false;
            // 尝试全小写查找
            try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
                if (rs.next()) {
                    exists = true;
                }
            }
            // 尝试全大写查找
            if (!exists) {
                try (ResultSet rs = metaData.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                    if (rs.next()) {
                        exists = true;
                    }
                }
            }
            if (!exists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + columnDef);
                    log.info("Auto-migrated: added missing column {} to table {}", columnName, tableName);
                }
            }
        } catch (Exception e) {
            log.debug("Column {} in table {} check/add skipped: {}", columnName, tableName, e.getMessage());
        }
    }

    private void executeSqlQuietly(Connection conn, String sql) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            log.debug("SQL execution skipped: {}", e.getMessage());
        }
    }
}
