## 1. Backend Core Look-Through Engine

- [x] 1.1 Define look-through DTO models (`PortfolioLookThroughView`, `StockExposureItem`, `AssetAllocationItem`, `CrossFundOverlapItem`)
- [x] 1.2 Implement `PortfolioLookThroughService` to calculate weighted stock exposures, asset class distribution, and cross-fund overlap from `fund_holding` and `user_position`
- [x] 1.3 Add controller endpoint `GET /api/portfolio/look-through` with unit test coverage

## 2. Backend Smart Diagnosis & AI Report Service

- [x] 2.1 Implement quantitative health scoring model (concentration HHI, stock weight bounds, asset allocation balance) and risk tag rule engine
- [x] 2.2 Implement `PortfolioDiagnosisService` to assemble structured diagnostic prompts and invoke LLM via `SysVisionConfigService`
- [x] 2.3 Implement fingerprint-based caching for diagnosis reports and expose endpoint `GET /api/portfolio/diagnosis`
- [x] 2.4 Add unit tests for quantitative scoring, risk rules, and graceful fallback when LLM is unavailable

## 3. Frontend UI Components & Visualization

- [x] 3.1 Add API methods in `frontend/src/api/index.ts` and interface types in `frontend/src/api/types.ts`
- [x] 3.2 Implement `PortfolioDiagnosisModal` component displaying health score gauge, risk tags, and AI advisory report
- [x] 3.3 Implement ECharts visualization for look-through Top 10 underlying stocks and asset type allocation
- [x] 3.4 Integrate "持仓透视与诊断" action trigger button into `PortfolioPage.tsx`

## 4. Verification & Testing

- [x] 4.1 Run backend unit tests (`mvn test`) to verify look-through aggregation and scoring logic
- [x] 4.2 Build frontend production bundle (`npm run build`) and verify clean TypeScript type-check
- [x] 4.3 Verify end-to-end diagnosis flow with sample portfolio holdings
