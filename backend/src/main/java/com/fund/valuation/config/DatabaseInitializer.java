package com.fund.valuation.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 数据库启动检查与动态结构迁移器:
 * 确保已有生产 MySQL 及测试内存 H2 数据库无缝升级新增字段与表结构。
 */
@Slf4j
@Component("databaseInitializer")
@RequiredArgsConstructor
public class DatabaseInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    @PostConstruct
    public void init() {
        run(null);
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            DatabaseMetaData meta = conn.getMetaData();
            String dbType = meta.getDatabaseProductName().toLowerCase(Locale.ROOT);

            // 1. 确保 sys_calendar_holiday 和 sys_notice 表存在
            if (dbType.contains("h2")) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sys_calendar_holiday (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        holiday_date DATE NOT NULL UNIQUE,
                        description VARCHAR(64),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sys_notice (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        notice_message VARCHAR(500),
                        notice_type VARCHAR(20) DEFAULT 'info',
                        is_enabled BOOLEAN DEFAULT TRUE,
                        is_closable BOOLEAN DEFAULT TRUE,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            } else {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sys_calendar_holiday (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        holiday_date DATE NOT NULL,
                        description VARCHAR(64) NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_holiday_date (holiday_date)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sys_notice (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        notice_message VARCHAR(500) NULL,
                        notice_type VARCHAR(20) NOT NULL DEFAULT 'info',
                        is_enabled TINYINT(1) NOT NULL DEFAULT 1,
                        is_closable TINYINT(1) NOT NULL DEFAULT 1,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
            }

            // 2. 检查 user 表是否存在，若存在则动态补全 role, status, is_vip, vip_expire_at
            boolean userTableExists = false;
            try (ResultSet rs = meta.getTables(null, null, dbType.contains("h2") ? "USER" : "user", null)) {
                if (rs.next()) userTableExists = true;
            }
            if (!userTableExists) {
                try (ResultSet rs = meta.getTables(null, null, dbType.contains("h2") ? "user" : "USER", null)) {
                    if (rs.next()) userTableExists = true;
                }
            }
            if (!userTableExists) {
                log.debug("user table does not exist yet, skipping alter columns");
                return;
            }

            Set<String> existingColumns = new HashSet<>();
            try (ResultSet rs = meta.getColumns(null, null, dbType.contains("h2") ? "USER" : "user", null)) {
                while (rs.next()) {
                    existingColumns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            if (existingColumns.isEmpty()) {
                try (ResultSet rs = meta.getColumns(null, null, dbType.contains("h2") ? "user" : "USER", null)) {
                    while (rs.next()) {
                        existingColumns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                    }
                }
            }

            if (!existingColumns.contains("role")) {
                log.info("Adding column role to user table");
                stmt.execute(dbType.contains("h2")
                    ? "ALTER TABLE \"USER\" ADD COLUMN role VARCHAR(20) DEFAULT 'USER'"
                    : "ALTER TABLE `user` ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER'");
            }
            if (!existingColumns.contains("status")) {
                log.info("Adding column status to user table");
                stmt.execute(dbType.contains("h2")
                    ? "ALTER TABLE \"USER\" ADD COLUMN status VARCHAR(20) DEFAULT 'NORMAL'"
                    : "ALTER TABLE `user` ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态: NORMAL/DISABLED'");
            }
            if (!existingColumns.contains("is_vip")) {
                log.info("Adding column is_vip to user table");
                stmt.execute(dbType.contains("h2")
                    ? "ALTER TABLE \"USER\" ADD COLUMN is_vip BOOLEAN DEFAULT FALSE"
                    : "ALTER TABLE `user` ADD COLUMN is_vip TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为VIP: 0/1'");
            }
            if (!existingColumns.contains("vip_expire_at")) {
                log.info("Adding column vip_expire_at to user table");
                stmt.execute(dbType.contains("h2")
                    ? "ALTER TABLE \"USER\" ADD COLUMN vip_expire_at TIMESTAMP NULL"
                    : "ALTER TABLE `user` ADD COLUMN vip_expire_at DATETIME NULL COMMENT 'VIP过期时间'");
            }

            // 3. 初始管理员账户赋权 (如果存在 admin 用户，保证其 role=ADMIN)
            try {
                stmt.executeUpdate(dbType.contains("h2")
                    ? "UPDATE \"USER\" SET role = 'ADMIN' WHERE username = 'admin'"
                    : "UPDATE `user` SET role = 'ADMIN' WHERE username = 'admin'");
            } catch (Exception ignored) {
            }

            log.info("Database schema check and initialization completed successfully");
        } catch (Exception e) {
            log.warn("Database initialization check encountered an exception: {}", e.getMessage());
        }
    }
}
