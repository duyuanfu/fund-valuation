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
import java.sql.Statement;
import java.util.Locale;

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

            // 2. 补全 user 表字段: role, status, is_vip, vip_expire_at, last_login_at, login_count
            ensureColumn(stmt, dbType, "role",
                    dbType.contains("h2") ? "ALTER TABLE \"USER\" ADD COLUMN role VARCHAR(20) DEFAULT 'USER'"
                            : "ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER'");
            ensureColumn(stmt, dbType, "status",
                    dbType.contains("h2") ? "ALTER TABLE \"USER\" ADD COLUMN status VARCHAR(20) DEFAULT 'NORMAL'"
                            : "ALTER TABLE `user` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态: NORMAL/DISABLED'");
            ensureColumn(stmt, dbType, "is_vip",
                    dbType.contains("h2") ? "ALTER TABLE \"USER\" ADD COLUMN is_vip BOOLEAN DEFAULT FALSE"
                            : "ALTER TABLE `user` ADD COLUMN `is_vip` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为VIP: 0/1'");
            ensureColumn(stmt, dbType, "vip_expire_at",
                    dbType.contains("h2") ? "ALTER TABLE \"USER\" ADD COLUMN vip_expire_at TIMESTAMP NULL"
                            : "ALTER TABLE `user` ADD COLUMN `vip_expire_at` DATETIME NULL COMMENT 'VIP过期时间'");
            ensureColumn(stmt, dbType, "last_login_at",
                    dbType.contains("h2") ? "ALTER TABLE \"USER\" ADD COLUMN last_login_at TIMESTAMP NULL"
                            : "ALTER TABLE `user` ADD COLUMN `last_login_at` DATETIME NULL COMMENT '最后登录时间'");
            ensureColumn(stmt, dbType, "login_count",
                    dbType.contains("h2") ? "ALTER TABLE \"USER\" ADD COLUMN login_count INT DEFAULT 0"
                            : "ALTER TABLE `user` ADD COLUMN `login_count` INT NOT NULL DEFAULT 0 COMMENT '累计登录次数'");

            // 3. 初始管理员账户赋权 (如果存在 admin 用户，保证其 role=ADMIN)
            try {
                stmt.executeUpdate(dbType.contains("h2")
                    ? "UPDATE \"USER\" SET role = 'ADMIN' WHERE username = 'admin'"
                    : "UPDATE `user` SET `role` = 'ADMIN' WHERE username = 'admin'");
            } catch (Exception ignored) {
            }

            log.info("Database schema check and initialization completed successfully");
        } catch (Exception e) {
            log.warn("Database initialization check encountered an exception: {}", e.getMessage());
        }
    }

    private void ensureColumn(Statement stmt, String dbType, String column, String alterSql) {
        String testSql = dbType.contains("h2")
                ? "SELECT " + column + " FROM \"USER\" WHERE 1=0"
                : "SELECT `" + column + "` FROM `user` WHERE 1=0";
        try {
            stmt.execute(testSql);
            // 列已存在
        } catch (Exception e) {
            log.info("Column {} is missing in user table, executing alter: {}", column, alterSql);
            try {
                stmt.execute(alterSql);
                log.info("Column {} added successfully", column);
            } catch (Exception ex) {
                log.warn("Notice when adding column {}: {}", column, ex.getMessage());
            }
        }
    }
}
