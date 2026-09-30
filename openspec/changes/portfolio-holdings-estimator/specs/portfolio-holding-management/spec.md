## ADDED Requirements

### Requirement: User can add and edit fund holdings via Alipay 4-field model
The system SHALL allow authenticated VIP users to add and edit fund holdings by specifying fund code, holding amount (持有金额), yesterday income (昨日收益, optional), holding profit (持有收益), and holding profit rate (持有收益率).

#### Scenario: Add holding with amount and profit
- **WHEN** user submits fund code "110022", holding amount 10000.00, yesterday income 30.50, and holding profit 500.00
- **THEN** system automatically calculates cost amount as 9500.00, calculates holding profit rate as 5.26%, saves the record for the current user, and returns success status

#### Scenario: Bi-directional linkage between holding profit and rate
- **WHEN** user inputs holding amount 10000.00 and inputs holding profit rate 10.00%
- **THEN** system automatically calculates holding profit as approximately 909.09 and cost amount as 9090.91

#### Scenario: Edit existing holding
- **WHEN** user updates holding amount to 12000.00 and holding profit to 650.00 for an existing fund
- **THEN** system recalibrates cost, profit rate, and shares, updates the record, and returns the modified holding

#### Scenario: Validation of invalid inputs
- **WHEN** user submits non-positive holding amount or an invalid fund code
- **THEN** system rejects the request with an explanatory validation error

### Requirement: User can delete a fund holding
The system SHALL allow authenticated VIP users to remove a fund from their portfolio holdings.

#### Scenario: Remove holding
- **WHEN** user requests deletion of holding for fund "110022"
- **THEN** system deletes the holding record and removes it from the user's holding list

### Requirement: VIP permission check for holding mutations
The system SHALL enforce VIP status on portfolio holding management endpoints.

#### Scenario: Non-VIP user attempts to add or edit holding
- **WHEN** a non-VIP user submits a holding mutation request to `/api/portfolio`
- **THEN** system rejects the request with HTTP 403 status and a message indicating VIP membership is required
