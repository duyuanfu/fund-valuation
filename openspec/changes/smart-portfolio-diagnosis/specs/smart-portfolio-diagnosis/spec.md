## ADDED Requirements

### Requirement: Quantitative Health Scoring and Risk Detection
The system SHALL evaluate the user's active fund portfolio against a multi-factor risk model, generating an overall health score between 0 and 100, along with specific diagnostic risk tags.

#### Scenario: High single-stock concentration risk
- **WHEN** any single look-through underlying stock accounts for more than 20% of the total equity portfolio value
- **THEN** system SHALL flag a warning risk tag "重仓个股集中度过高" and deduct points from the diversification score

#### Scenario: Well-diversified balanced portfolio
- **WHEN** the portfolio has balanced asset allocation across stock/bond classes and no single stock exceeds 10%
- **THEN** system SHALL assign a health score of 80 or higher and highlight positive tags such as "资产配置均衡"

### Requirement: AI-Powered Diagnosis Report Generation
The system SHALL support generating an intelligent advisory diagnosis report using configured LLM models, summarizing portfolio structure, specific weaknesses, and concrete rebalancing suggestions.

#### Scenario: User requests AI diagnosis report
- **WHEN** user clicks "生成 AI 诊断报告" in the diagnosis panel
- **THEN** system sends structured quantitative portfolio metrics to the configured LLM API and returns structured diagnosis sections including overview, risk warnings, and action advice

#### Scenario: AI service unavailable or unconfigured
- **WHEN** the system LLM API is unconfigured, disabled, or encounters network timeouts
- **THEN** system SHALL gracefully display complete quantitative scoring and rule-based risk tags without throwing system errors

### Requirement: Interactive Portfolio Diagnosis Dashboard
The system SHALL provide an interactive visual dashboard in the portfolio view with asset allocation charts and diagnosis reports.

#### Scenario: User opens diagnosis dashboard
- **WHEN** user clicks "持仓透视与诊断" from the Portfolio page
- **THEN** system renders a modal or drawer containing health score gauge, look-through Top 10 bar chart, asset allocation donut chart, and AI advisory report
