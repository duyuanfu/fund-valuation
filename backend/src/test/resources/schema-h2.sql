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

CREATE TABLE IF NOT EXISTS user_position (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    user_id             VARCHAR(64)   NOT NULL,
    fund_code           VARCHAR(10)   NOT NULL,
    holding_amount      DECIMAL(14,2) NOT NULL,
    yesterday_income    DECIMAL(14,2) DEFAULT 0,
    holding_profit      DECIMAL(14,2) NOT NULL DEFAULT 0,
    holding_profit_rate DECIMAL(8,4)  NULL,
    cost_amount         DECIMAL(14,2) NOT NULL DEFAULT 0,
    holding_shares      DECIMAL(14,4) NULL,
    cost_price          DECIMAL(10,4) NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    username      VARCHAR(64)  NOT NULL,
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

CREATE TABLE IF NOT EXISTS sys_vip_config (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    monthly_price        DECIMAL(8,2) DEFAULT 2.90,
    quarterly_price      DECIMAL(8,2) DEFAULT 6.90,
    quarterly_orig_price DECIMAL(8,2) DEFAULT 8.70,
    yearly_price         DECIMAL(8,2) DEFAULT 19.90,
    yearly_orig_price    DECIMAL(8,2) DEFAULT 34.80,
    wechat_qr_url        CLOB         NULL,
    alipay_qr_url        CLOB         NULL,
    payee_name           VARCHAR(64)  DEFAULT '管理员',
    payment_tip          VARCHAR(255) DEFAULT '付款请务必备注用户名',
    require_approval     BOOLEAN      DEFAULT TRUE,
    webhook_url          VARCHAR(500) NULL,
    updated_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_vip_order (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(32)   NOT NULL,
    plan_key     VARCHAR(32)   NOT NULL,
    plan_name    VARCHAR(64)   NOT NULL,
    amount       DECIMAL(8,2)  NOT NULL,
    pay_method   VARCHAR(32)   NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP     NULL
);

CREATE TABLE IF NOT EXISTS user_notification_channel (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             VARCHAR(64)   NOT NULL UNIQUE,
    email               VARCHAR(128)  NULL,
    email_enabled       BOOLEAN       DEFAULT TRUE,
    wechat_channel_type VARCHAR(32)   DEFAULT 'serverchan',
    serverchan_key      VARCHAR(128)  NULL,
    bark_url            VARCHAR(255)  NULL,
    wechat_uid          VARCHAR(128)  NULL,
    wechat_app_token    VARCHAR(255)  NULL,
    wechat_webhook_url  VARCHAR(500)  NULL,
    wechat_enabled      BOOLEAN       DEFAULT TRUE,
    remind_enabled      BOOLEAN       DEFAULT TRUE,
    remind_time         VARCHAR(8)    DEFAULT '14:30',
    push_scope          VARCHAR(32)   DEFAULT 'ALL_WATCHLIST',
    hide_amount         BOOLEAN       DEFAULT FALSE,
    updated_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_email_config (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    smtp_host      VARCHAR(128)  DEFAULT 'smtp.163.com',
    smtp_port      INT           DEFAULT 465,
    smtp_username  VARCHAR(128)  NULL,
    smtp_password  VARCHAR(128)  NULL,
    sender_name    VARCHAR(64)   DEFAULT '基金实时估值',
    sender_address VARCHAR(128)  NULL,
    ssl_enabled        BOOLEAN       DEFAULT TRUE,
    wxpusher_app_token VARCHAR(255)  NULL,
    wxpusher_qr_url    VARCHAR(500)  NULL,
    updated_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);