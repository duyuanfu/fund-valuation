## ADDED Requirements

### Requirement: User notification channel binding and preferences
The system SHALL allow authenticated users to view and update their notification channel configuration and reminder preferences. Supported channels SHALL include Email and WeChat/Webhook (such as WxPusher, WeCom, DingTalk, Feishu, ServerChan, Bark).

#### Scenario: Query current user notification settings
- **WHEN** an authenticated user requests their notification settings via `GET /api/user/notification-channel`
- **THEN** the system SHALL return their bound email, WeChat push URL/token, enabled status, push scope preference (`ALL_WATCHLIST` or `PORTFOLIO_ONLY`), and amount privacy masking flag

#### Scenario: Update notification channel and toggle reminder
- **WHEN** an authenticated user submits channel updates via `POST /api/user/notification-channel` with valid email and/or WeChat webhook URL
- **THEN** the system SHALL validate the format, persist the settings in `user_notification_channel`, and return success

#### Scenario: Reject invalid email format
- **WHEN** a user submits an email address with an invalid format
- **THEN** the system SHALL reject the request with HTTP 400 and an informative error message

---

### Requirement: Channel connectivity verification test
The system SHALL allow users to trigger a test message to their configured email or WeChat notification channel to verify receipt and deliverability.

#### Scenario: Test email delivery
- **WHEN** a user with a configured email address triggers `POST /api/user/notification-channel/test-email`
- **THEN** the system SHALL immediately send a sample 14:30 valuation report email to the user's email address and return delivery status

#### Scenario: Test WeChat / Webhook delivery
- **WHEN** a user with a configured WeChat/Webhook channel triggers `POST /api/user/notification-channel/test-webhook`
- **THEN** the system SHALL deliver a test Markdown card to the specified webhook and return delivery status

---

### Requirement: Trading day 14:30 scheduled valuation reminder
The system SHALL automatically execute a scheduled valuation summary task every Monday through Friday at 14:30 (Asia/Shanghai timezone).

#### Scenario: Skip reminder on non-trading days and public holidays
- **WHEN** the 14:30 scheduler runs on a weekend or on a date recorded as a holiday in `TradingCalendar`
- **THEN** the system SHALL log that the market is closed and terminate the run without sending any notifications

#### Scenario: Aggregate and push personalized reports on trading days
- **WHEN** the 14:30 scheduler runs on a valid trading day
- **THEN** the system SHALL identify all users with active notification settings, fetch their real-time intraday valuation snapshot (portfolio holdings and/or watchlist), render the channel-specific templates, and dispatch notifications asynchronously

#### Scenario: HTML email report format
- **WHEN** dispatching to a user's bound email
- **THEN** the system SHALL send a multi-part MIME email containing a responsive HTML table with red/green valuation changes, today's estimated profit, and a direct link to the portfolio page

#### Scenario: WeChat and Webhook report format
- **WHEN** dispatching to a user's bound WeChat/Webhook channel
- **THEN** the system SHALL format a concise, high-information-density Markdown message card summarizing today's market performance, top portfolio moves, and estimated profit

---

### Requirement: System email service configuration
The system SHALL allow administrators to configure and test the system SMTP outgoing mail server settings from the management console.

#### Scenario: Admin updates SMTP server configuration
- **WHEN** an admin updates SMTP host, port, username, password/auth code, and sender address via `POST /api/admin/email-config`
- **THEN** the system SHALL persist the configuration into `sys_email_config` and reinitialize the runtime mail sender

#### Scenario: Admin tests system mail sending
- **WHEN** an admin triggers `POST /api/admin/email-config/test` with a recipient address
- **THEN** the system SHALL attempt to connect to the SMTP server, deliver a test email, and return the connection or error diagnostics
