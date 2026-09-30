## 1. Database & Persistence Layer

- [x] 1.1 在 `schema.sql` 与 `schema-h2.sql` 中新增 `user_position` 持仓表（支持 `holding_amount`, `yesterday_income`, `holding_profit`, `holding_profit_rate`, `cost_amount`, `holding_shares`, `cost_price` 等 4+ 核心字段）
- [x] 1.2 创建 `UserPosition` 实体类与 MyBatis-Plus `UserPositionMapper` 接口

## 2. Backend Services & VIP Protection

- [x] 2.1 定义持仓数据传输对象 DTO（`PositionItemView`、`PortfolioSummaryView`、`PortfolioView`、`PositionSaveRequest`，包含金额、昨日收益、持有收益、收益率、今日预估收益等字段）
- [x] 2.2 实现 `PortfolioService`：支持基于持有金额+持有收益+昨日收益的增删改查，以及结合实时估值引擎聚合计算今日预估收益和最新总盈亏
- [x] 2.3 编写 `PortfolioController`：增加 VIP 会员权限拦截（非 VIP 返回 403），并编写单元测试验证权限与金额计算逻辑

## 3. Frontend VIP Access & API Integration

- [x] 3.1 在 `frontend/src/api/types.ts` 和 `frontend/src/api/index.ts` 中增加持仓相关类型定义与 API 请求方法
- [x] 3.2 在 `frontend/src/App.tsx` 中集成顶部“自选基金 / 我的持仓 👑”导航切换与 `/portfolio` 路由配置
- [x] 3.3 开发 `VipRechargeModal.tsx`（微信/支付宝收款码切换、会员价格阶梯展示、备注指引与快速开通说明）

## 4. Frontend Portfolio UI & Visual Estimation

- [x] 4.1 开发 `frontend/src/pages/PortfolioPage.tsx`：为非 VIP 用户展示精美功能预览看板与 VIP 解锁引导卡片，一键唤起充值弹窗
- [x] 4.2 为 VIP 用户实现持仓收益汇总看板（今日预估总收益、持仓总市值、累计总盈亏、今日总收益率）
- [x] 4.3 实现响应式持仓列表组件（桌面端表格与移动端卡片），直观展示持有金额、昨日收益、持有收益/率、估值涨跌幅与今日预估收益
- [x] 4.4 实现 100% 对齐支付宝持仓卡片的“添加/编辑持仓”弹窗（支持输入基金代码、金额、昨日收益、持有收益与收益率的双向联动推算，预留未来截图识别扩展）
- [x] 4.5 接入盘中定时刷新与估值联动更新机制，保持今日预估收益动态更新

## 5. Verification & Validation

- [x] 5.1 运行后端测试验证 VIP 鉴权拦截、持仓增删改以及收益公式计算准确性
- [x] 5.2 运行前端 TypeScript 编译与 Vite 构建打包验证
