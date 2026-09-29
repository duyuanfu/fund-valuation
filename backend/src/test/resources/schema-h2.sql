CREATE TABLE IF NOT EXISTS fund (
    code         VARCHAR(10)  NOT NULL,
    name         VARCHAR(64)  NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    type_raw     VARCHAR(32)  NULL,
    track_index  VARCHAR(20)  NULL,
    prev_nav     DECIMAL(10,4) NULL,
    nav_date     DATE         NULL,
    fresh_flag   VARCHAR(64)  NULL,
    updated_at   TIMESTAMP    NOT NULL,
    PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS fund_holding (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    fund_code   VARCHAR(10)  NOT NULL,
    stock_code  VARCHAR(10)  NOT NULL,
    stock_name  VARCHAR(32)  NULL,
    weight      DECIMAL(8,4) NOT NULL,
    report_qt   VARCHAR(32)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS fund_asset_alloc (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    fund_code   VARCHAR(10)  NOT NULL,
    report_date DATE         NOT NULL,
    stock_pct   DECIMAL(8,4) NOT NULL,
    bond_pct    DECIMAL(8,4) NOT NULL DEFAULT 0,
    cash_pct    DECIMAL(8,4) NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS estimate_history (
    id        BIGINT        NOT NULL AUTO_INCREMENT,
    fund_code VARCHAR(10)   NOT NULL,
    est_time  TIMESTAMP     NOT NULL,
    est_nav   DECIMAL(10,4) NOT NULL,
    est_pct   DECIMAL(8,4)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_fund (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    user_id   VARCHAR(64) NOT NULL,
    fund_code VARCHAR(10) NOT NULL,
    sort_no   INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS quote_cache (
    id       BIGINT        NOT NULL AUTO_INCREMENT,
    secid    VARCHAR(20)   NOT NULL,
    name     VARCHAR(32)   NULL,
    price    DECIMAL(12,3) NOT NULL,
    pct_chg  DECIMAL(8,4)  NOT NULL,
    quote_ts TIMESTAMP     NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS bond_duration (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    fund_code VARCHAR(10)  NOT NULL,
    report_qt VARCHAR(32)  NOT NULL,
    duration  DECIMAL(6,2) NOT NULL,
    source    VARCHAR(16)  NOT NULL DEFAULT 'default',
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS `user` (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(32)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  DEFAULT 'USER',
    status        VARCHAR(20)  DEFAULT 'NORMAL',
    is_vip        BOOLEAN      DEFAULT FALSE,
    vip_expire_at TIMESTAMP    NULL,
    last_login_at TIMESTAMP    NULL,
    login_count   INT          DEFAULT 0,
    referral_source VARCHAR(32) NULL,
    created_at    TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_calendar_holiday (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    description  VARCHAR(64),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_notice (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_message VARCHAR(500),
    notice_type    VARCHAR(20) DEFAULT 'info',
    is_enabled     BOOLEAN DEFAULT TRUE,
    is_closable    BOOLEAN DEFAULT TRUE,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);