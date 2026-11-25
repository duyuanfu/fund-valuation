## ADDED Requirements

### Requirement: VIP users can upload Alipay holding screenshots for structured extraction
The system SHALL allow authenticated VIP (or ADMIN) users to upload up to 3 Alipay fund holding screenshots (list view or detail view) in a single request and return structured holding candidates extracted via the configured Vision OCR engine.

#### Scenario: Successful recognition of Alipay holding list screenshot
- **WHEN** a VIP user uploads a valid PNG/JPG/WEBP screenshot containing Alipay fund holding rows and the Vision OCR service is enabled
- **THEN** the system extracts each visible fund's name, holding amount, yesterday income (if present), holding profit, and holding profit rate, resolves matching 6-digit fund codes, and returns the candidate list along with remaining daily quota

#### Scenario: Empty or non-holding screenshot uploaded
- **WHEN** a user uploads a screenshot that does not contain recognizable fund holding records
- **THEN** the system returns an empty candidate list with an explanatory hint message without throwing an unhandled server error

#### Scenario: Vision OCR feature disabled or unconfigured
- **WHEN** a user invokes screenshot import while the admin has disabled or not configured the Vision OCR API key
- **THEN** the system rejects the request with a clear message instructing the user to use manual input or contact the administrator

#### Scenario: Non-VIP user attempts screenshot import
- **WHEN** a non-VIP user sends a request to `/api/portfolio/import-screenshot`
- **THEN** the system rejects the request with HTTP 403 indicating VIP membership is required

#### Scenario: Daily per-user quota exceeded
- **WHEN** a user exceeds the configured `daily_user_limit` for screenshot imports within the same calendar day
- **THEN** the system rejects further recognition requests for that day with HTTP 429 or explanatory quota message and suggests manual entry

### Requirement: Interactive review and batch confirmation before persistence
The frontend SHALL display extracted screenshot results in an editable review checklist before persisting any positions, allowing users to adjust fund codes, holding amounts, profits, and select which items to import.

#### Scenario: Review, correct candidate fund code, and batch import
- **WHEN** the user reviews the recognized rows, switches an ambiguous fund from a Top-3 candidate dropdown or searches by code/name, edits an amount if needed, and clicks confirm batch import
- **THEN** the frontend submits selected valid rows to `/api/portfolio/batch`, the backend saves all positions in a single transaction using the 4-field bi-directional cost model, and the portfolio dashboard refreshes immediately

#### Scenario: Duplicate fund code handling during batch import
- **WHEN** a batch import payload contains a fund code already held by the current user
- **THEN** the system updates the existing position for that `(user_id, fund_code)` instead of creating duplicate rows

### Requirement: Screenshot privacy and payload protection
The system SHALL process uploaded screenshots strictly in memory without writing image files to disk or database storage, and reject oversized (>5MB) or unsupported image formats.

#### Scenario: Reject oversized image upload
- **WHEN** a user attempts to send an image payload exceeding the maximum allowed size or an invalid MIME format
- **THEN** the system rejects the payload before invoking any external Vision API

### Requirement: Admin Vision OCR configuration management
The system SHALL allow administrators to configure Vision API settings (`enabled`, `apiBaseUrl`, `apiKey`, `modelName`, `dailyUserLimit`) and verify connectivity from the admin console.

#### Scenario: Admin saves and tests Vision OCR settings
- **WHEN** an administrator updates Vision OCR configuration in the admin console and triggers a connectivity test
- **THEN** the system persists the configuration securely (masking `apiKey` on subsequent reads) and verifies endpoint reachability
