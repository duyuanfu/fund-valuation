## ADDED Requirements

### Requirement: Fuzzy fund search by name or code prefix
The system SHALL provide a fund search capability that resolves abbreviated or truncated fund names (such as those appearing in Alipay holding lists) to 6-digit fund codes and top candidate matches.

#### Scenario: Exact or local database match
- **WHEN** a search query matches a fund code or name already cached in the local `fund` table
- **THEN** the system returns matching candidates prioritizing exact prefix and share class (`A`/`C`) alignment

#### Scenario: Remote TianTianFund catalog fallback
- **WHEN** local database matching yields insufficient candidates for an abbreviated fund name (e.g., stripped trailing `...`)
- **THEN** the system queries TianTianFund's public search API with rate-limit protection and returns ranked candidates containing `code`, `name`, and `type`

#### Scenario: Authenticated frontend search endpoint
- **WHEN** a user types a fund name or partial code in the import review dropdown and queries `/api/funds/search?q=<keyword>`
- **THEN** the system returns up to 10 ranked matching fund items for quick selection
