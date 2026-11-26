## ADDED Requirements

### Requirement: Portfolio Look-Through Calculation
The system SHALL aggregate the disclosed top 10 holdings of each fund in the user's active portfolio, weighted by each fund's current holding market value, to compute the combined look-through stock exposures and asset type allocations.

#### Scenario: User views look-through stock exposures
- **WHEN** user requests portfolio look-through data for their active portfolio
- **THEN** system returns the aggregate Top 10 underlying stocks, each with total look-through holding value, overall portfolio percentage, and underlying fund contributors

#### Scenario: Portfolio contains funds without disclosed stock holdings
- **WHEN** user holds bond funds or money market funds that have no equity holdings
- **THEN** system SHALL categorize these holding amounts into fixed income/cash asset classes and calculate overall asset allocation ratios without breaking equity look-through

### Requirement: Cross-Fund Holding Overlap Analysis
The system SHALL compute the pairwise and aggregate stock overlap between funds in the portfolio to detect redundant concentration.

#### Scenario: Multiple funds hold the same underlying stock
- **WHEN** two or more funds in the portfolio hold the same underlying stock (e.g., both hold 600519 贵州茅台)
- **THEN** system SHALL report the combined exposure across all funds and highlight the overlapping stock list
