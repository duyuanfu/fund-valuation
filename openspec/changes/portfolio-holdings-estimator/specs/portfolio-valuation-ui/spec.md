## ADDED Requirements

### Requirement: VIP gating with preview mockup and recharge modal
The system SHALL provide a high-conversion preview and VIP unlock guidance for non-VIP users visiting the portfolio holdings page.

#### Scenario: Non-VIP user views portfolio page
- **WHEN** a non-VIP user navigates to `/portfolio`
- **THEN** system renders a preview state with blurred/mock holding figures and an unlock CTA card explaining VIP real-time earnings features

#### Scenario: Non-VIP user triggers VIP unlock
- **WHEN** user clicks "立即开通 VIP" or "充值解锁"
- **THEN** system displays the recharge modal with WeChat and Alipay payment QR codes, pricing options, and payment note guidance

### Requirement: Portfolio valuation and income aggregation for VIPs
The system SHALL aggregate the VIP user's holdings with real-time intraday valuation data and provide today's estimated income, total market value, and total cumulative profit or loss alongside yesterday's officially confirmed income.

#### Scenario: Aggregate portfolio valuation on page load
- **WHEN** an authenticated VIP user opens the portfolio page
- **THEN** system loads all holdings with real-time estimate Nav, estimate percentage, today's estimated income, current market value, recorded yesterday income, and total cumulative profit

#### Scenario: Dynamic calculation of today estimated income
- **WHEN** fund intraday valuation updates with estimate percentage $P_{est}$ and holding amount $A$
- **THEN** system calculates today's estimated income as $A \times (P_{est} / 100)$ and updates the summary metrics

### Requirement: Portfolio UI layout and responsiveness
The system SHALL display the portfolio holding page with a summary dashboard card and a responsive fund list (card list on mobile and table on desktop) featuring Alipay 4-fields plus intraday estimates.

#### Scenario: Display summary dashboard
- **WHEN** VIP user views the portfolio page
- **THEN** system renders summary metrics including total holding market value, total estimated today income, total cumulative return, and today estimate percentage

#### Scenario: Desktop and mobile responsive rendering
- **WHEN** viewing on a screen width below 768px
- **THEN** system displays holdings as card items showing fund name, code, holding amount, yesterday income, holding profit/rate, estimated income, and return badge

#### Scenario: Empty state guidance
- **WHEN** user has no holdings yet
- **THEN** system displays an empty placeholder with a button to add holding

### Requirement: Top navigation integration
The system SHALL provide top navigation between Watchlist and Portfolio pages for authenticated non-admin users, with a VIP badge on the portfolio entry.

#### Scenario: Navigate to portfolio
- **WHEN** user clicks on the "我的持仓" navigation tab
- **THEN** system navigates to the `/portfolio` route
