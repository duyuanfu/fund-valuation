## 1. Backend Data Models and Trend Detection Service

- [ ] 1.1 Define `AssetTrendAlertItem` DTO in `backend/src/main/java/com/fund/valuation/dto/` and add to `PortfolioDiagnosisView`
- [ ] 1.2 Implement `AssetTrendRiskService` in `backend/src/main/java/com/fund/valuation/service/` to recognize commodity/sector themes (e.g. 黄金、原油、芯片) and compute 5-day return and drawdown
- [ ] 1.3 Add unit test `AssetTrendRiskServiceTest` covering thematic asset recognition and acute downtrend detection

## 2. Quantitative Scoring and AI Advisory Integration

- [ ] 2.1 Update `PortfolioDiagnosisService` to penalize health score upon severe trend down risks and append dynamic risk tags
- [ ] 2.2 Enrich `AiAdvisorClient` prompt template to feed asset trend drawdown data into the LLM diagnosis report
- [ ] 2.3 Update unit tests in `PortfolioDiagnosisServiceTest` verifying trend risk scoring and tag generation

## 3. Frontend Visualization and Alert UI

- [ ] 3.1 Update `frontend/src/api/types.ts` with `AssetTrendAlertItem` interface definition
- [ ] 3.2 Add dedicated "标的走势与趋势风险预警" alert card and tags in `PortfolioDiagnosisModal.tsx`
- [ ] 3.3 Add trend badges and warnings in look-through underlying stock table and asset allocation tab

## 4. Verification and End-to-End Testing

- [ ] 4.1 Run backend unit tests (`mvn test`) ensuring 100% test pass rate
- [ ] 4.2 Build frontend bundle (`npm run build`) and verify clean TypeScript type-check
- [ ] 4.3 Verify end-to-end diagnosis flow with sample portfolio containing commodity/thematic holdings
