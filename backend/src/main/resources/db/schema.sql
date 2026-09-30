CREATE DATABASE IF NOT EXISTS fund_valuation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fund_valuation;

CREATE TABLE IF NOT EXISTS fund (
    code         VARCHAR(10)  NOT NULL COMMENT '基金代码',
    name         VARCHAR(64)  NOT NULL COMMENT '基金名称',
    type         VARCHAR(20)  NOT NULL COMMENT '类型: index/enhanced/active/mixed/bond/other',
    type_raw     VARCHAR(32)  NULL     COMMENT '官方类型原文(如 债券型-长债)',
    track_index  VARCHAR(20)  NULL     COMMENT '跟踪指数代码',
    prev_nav     DECIMAL(10,4) NULL    COMMENT '昨日净值',
    nav_date     DATE         NULL     COMMENT '净值日期',
    fresh_flag   VARCHAR(64)  NULL     COMMENT '新鲜度/降级标记',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金主数据';

CREATE TABLE IF NOT EXISTS fund_holding (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    fund_code   VARCHAR(10)  NOT NULL COMMENT '基金代码',
    stock_code  VARCHAR(10)  NOT NULL COMMENT '股票代码',
    stock_name  VARCHAR(32)  NULL     COMMENT '股票名称',
    weight      DECIMAL(8,4) NOT NULL COMMENT '占净值比例(%)',
    report_qt   VARCHAR(32)  NOT NULL COMMENT '报告期(如 2026-06-30)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fund_stock_qt (fund_code, stock_code, report_qt),
    KEY idx_fund (fund_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前十大重仓股';

CREATE TABLE IF NOT EXISTS fund_asset_alloc (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    fund_code   VARCHAR(10)  NOT NULL COMMENT '基金代码',
    report_date DATE         NOT NULL COMMENT '报告期',
    stock_pct   DECIMAL(8,4) NOT NULL COMMENT '股票占净值(%)',
    bond_pct    DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '债券占净值(%)',
    cash_pct    DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '现金占净值(%)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fund_date (fund_code, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产配置';

CREATE TABLE IF NOT EXISTS estimate_history (
    id       BIGINT        NOT NULL AUTO_INCREMENT,
    fund_code VARCHAR(10)  NOT NULL COMMENT '基金代码',
    est_time DATETIME      NOT NULL COMMENT '估算时间',
    est_nav  DECIMAL(10,4) NOT NULL COMMENT '估算净值',
    est_pct  DECIMAL(8,4)  NOT NULL COMMENT '估算涨跌幅(%)',
    PRIMARY KEY (id),
    KEY idx_fund_time (fund_code, est_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='估值历史';

CREATE TABLE IF NOT EXISTS user_fund (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    user_id   VARCHAR(64) NOT NULL COMMENT '用户ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    sort_no   INT         NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_fund (user_id, fund_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自选基金';

CREATE TABLE IF NOT EXISTS user_position (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    user_id             VARCHAR(64)   NOT NULL COMMENT '用户ID',
    fund_code           VARCHAR(10)   NOT NULL COMMENT '基金代码',
    holding_amount      DECIMAL(14,2) NOT NULL COMMENT '持仓总金额/资产现值(元)',
    yesterday_income    DECIMAL(14,2) NULL     DEFAULT 0 COMMENT '昨日收益(元, 选填)',
    holding_profit      DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '持有收益/累计收益(元)',
    holding_profit_rate DECIMAL(8,4)  NULL     COMMENT '持有收益率(%)',
    cost_amount         DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '持仓成本(元)',
    holding_shares      DECIMAL(14,4) NULL     COMMENT '折算持有份额',
    cost_price          DECIMAL(10,4) NULL     COMMENT '折算成本净值',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_position (user_id, fund_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持仓基金';

CREATE TABLE IF NOT EXISTS quote_cache (
    id       BIGINT        NOT NULL AUTO_INCREMENT,
    secid    VARCHAR(20)   NOT NULL COMMENT '行情secid',
    name     VARCHAR(32)   NULL     COMMENT '名称',
    price    DECIMAL(12,3) NOT NULL COMMENT '最新价',
    pct_chg  DECIMAL(8,4)  NOT NULL COMMENT '涨跌幅(%)',
    quote_ts DATETIME      NOT NULL COMMENT '行情时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_secid (secid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行情缓存';

CREATE TABLE IF NOT EXISTS bond_duration (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    fund_code VARCHAR(10)  NOT NULL COMMENT '基金代码',
    report_qt VARCHAR(32)  NOT NULL COMMENT '报告期',
    duration  DECIMAL(6,2) NOT NULL COMMENT '组合久期(年)',
    source    VARCHAR(16)  NOT NULL DEFAULT 'default' COMMENT '来源: report/default',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fund_qt (fund_code, report_qt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='债券基金组合久期';

CREATE TABLE IF NOT EXISTS `user` (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    username      VARCHAR(32) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    role          VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    status        VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态: NORMAL/DISABLED',
    is_vip        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否为VIP: 0/1',
    vip_expire_at DATETIME    NULL COMMENT 'VIP过期时间',
    last_login_at DATETIME    NULL COMMENT '最后登录时间',
    login_count   INT         NOT NULL DEFAULT 0 COMMENT '累计登录次数',
    referral_source VARCHAR(32) NULL COMMENT '推荐来源: 闲鱼/B站/朋友推荐/自己搜索/其他',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号';

CREATE TABLE IF NOT EXISTS sys_calendar_holiday (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    holiday_date DATE        NOT NULL COMMENT '休市日期(yyyy-MM-dd)',
    description  VARCHAR(64) NULL COMMENT '节假日/休市原因描述',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_holiday_date (holiday_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统自定义交易休市日历';

CREATE TABLE IF NOT EXISTS sys_notice (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    notice_message VARCHAR(500) NULL COMMENT '公告正文',
    notice_type    VARCHAR(20)  NOT NULL DEFAULT 'info' COMMENT '公告类型: info/success/warning/error',
    is_enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    is_closable    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否允许关闭',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知公告配置';

CREATE TABLE IF NOT EXISTS sys_vip_config (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    monthly_price        DECIMAL(8,2) NOT NULL DEFAULT 2.90 COMMENT '月度会员价格',
    quarterly_price      DECIMAL(8,2) NOT NULL DEFAULT 6.90 COMMENT '季度会员价格',
    quarterly_orig_price DECIMAL(8,2) NULL     DEFAULT 8.70 COMMENT '季度会员原价',
    yearly_price         DECIMAL(8,2) NOT NULL DEFAULT 19.90 COMMENT '年度会员价格',
    yearly_orig_price    DECIMAL(8,2) NULL     DEFAULT 34.80 COMMENT '年度会员原价',
    wechat_qr_url        MEDIUMTEXT   NULL     COMMENT '微信收款二维码图片地址或base64',
    alipay_qr_url        MEDIUMTEXT   NULL     COMMENT '支付宝收款二维码图片地址或base64',
    payee_name           VARCHAR(64)  NULL     DEFAULT '管理员' COMMENT '收款人显示名称',
    payment_tip          VARCHAR(255) NULL     DEFAULT '付款请务必备注用户名' COMMENT '付款备注提示',
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP会员价格与收款配置';
