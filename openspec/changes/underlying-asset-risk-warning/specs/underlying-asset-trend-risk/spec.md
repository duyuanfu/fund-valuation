## ADDED Requirements

### Requirement: Underlying Asset and Theme Trend Risk Detection
The system SHALL evaluate the recent price momentum, continuous decline days, and drawdown of underlying assets (such as gold/precious metals, core industry sectors, and top underlying stocks) held in the user's active portfolio to identify negative trend risks.

#### Scenario: Continuous downtrend in gold or commodity thematic fund
- **WHEN** user holds a commodity-related or sector thematic fund (e.g., gold ETF / gold producer fund) whose recent cumulative return over 5 trading days is negative or recent maximum drawdown exceeds 5%
- **THEN** system SHALL identify a high-risk trend warning for this underlying asset, recording the asset name, drawdown rate, and risk severity level (`DANGER` or `WARNING`)

#### Scenario: Heavy underlying stock consecutive decline
- **WHEN** a top look-through stock accounting for more than 5% of the total portfolio experiences 3 or more consecutive down sessions or a 5-day drop exceeding 6%
- **THEN** system SHALL flag an individual stock negative momentum warning with the affected stock name, portfolio exposure ratio, and 5-day return

### Requirement: Trend Risk Scoring and Factor Penalization
The system SHALL integrate underlying asset trend risk as a quantitative factor into the portfolio health scoring model.

#### Scenario: Portfolio contains assets in acute downtrend
- **WHEN** one or more held funds or underlying assets trigger an acute downtrend warning
- **THEN** system SHALL deduct up to 15 points from the composite health score and append a distinct `DiagnosisRiskTag` indicating "标的资产持续走弱预警" or "大宗/重仓赛道破位下行"

#### Scenario: Underlying assets trend stable or positive
- **WHEN** all major underlying assets and held funds have 5-day returns above -1% and no severe drawdown
- **THEN** system SHALL maintain the baseline score and award a positive tag such as "标的动量平稳，无急跌风险"

### Requirement: Surface Trend Risk in Diagnosis Dashboard and AI Advisory
The system SHALL prominently display underlying asset trend alerts in the portfolio diagnosis modal and inject trend context into the LLM prompt.

#### Scenario: User views portfolio diagnosis modal with trend risks
- **WHEN** user opens the diagnosis dashboard for a portfolio containing weakening underlying assets
- **THEN** system SHALL render a dedicated trend risk banner highlighting the weakening asset (e.g. 黄金/贵金属、重仓个股), the recent drawdown percentage, and concrete tactical suggestions (such as stop-loss or grid rebalancing)

#### Scenario: AI report generation with trend context
- **WHEN** user triggers AI diagnosis report generation
- **THEN** system SHALL include the detected underlying asset trend drawdown metrics in the prompt to the LLM, prompting the AI advisor to explicitly analyze the downtrend cause and propose risk mitigation steps
