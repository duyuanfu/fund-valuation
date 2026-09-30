## Why

目前系统已具备自选基金列表与实时估值计算功能，但用户只能观察单只基金的日内涨跌幅，无法直接量化与自身投资关联的实际金额收益。用户迫切需要一个持仓列表页，方便录入在支付宝等平台持有的基金资产与收益数据，结合盘中实时估值脉搏，自动预估今日基金收益总额与明细，辅助盘中交易决策。
同时，持仓收益实时预估作为核心高粘性、高商业价值功能，定位为 **VIP 会员专享特权**。非会员访问时提供功能预览与高转化率的开通引导，通过弹出微信/支付宝二维码完成充值开通，实现良性商业闭环。

## What Changes

- **会员权限与高转化引导体系**：
  - 持仓估值功能限制为 VIP 会员专享，后端接口校验 VIP 权限保护。
  - 前端对非 VIP 用户进入持仓页时提供**方案 1：精美功能预览 + 动态模拟看板 + VIP 解锁引导卡片**，最大化激发用户充值意愿。
  - 充值履约提供**方案 1：微信与支付宝收款二维码弹窗**，展示会员价格体系、扫码支付说明与账号备注指引，支付后由管理员/客服快速审核或后台激活。
- **100% 对齐支付宝持仓卡片的录入与数据模型（支持 4 大核心字段）**：
  - 录入方式完全对齐支付宝基金持仓的 4 个标准字段：**“持有金额”**、**“昨日收益”**、**“持有收益”**和**“持有收益率”**。
  - 表单支持**“持有收益（元）”与“持有收益率（%）”双向智能联动反算**，用户填任一项均可自动换算出持仓成本与另一项。
  - 数据模型与接口规格全面为后续“支付宝持仓截图 OCR 自动扫描录入”预留完整兼容的数据协议。
- **昨日收益与今日预估收益双屏对照估算**：
  - 保留录入的“昨日收益”（官方已确认账面），盘中结合实时估值动态展示“今日预估收益”，形成鲜明日内对比。
  - 实时动态推算：最新估算市值（`持有金额 + 今日预估收益`）、最新动态累计盈亏（`持有收益 + 今日预估收益`）以及最新累计收益率。
- **持仓列表前端页面与汇总看板**：
  - 顶部汇总看板：今日预估总收益（红涨绿跌）、持仓总资产市值、累计总盈亏、今日预估总收益率。
  - 响应式持仓列表（桌面端清晰表格 + 移动端卡片流），卡片清晰标注持有金额、昨日收益、持有收益/率与今日预估收益。
  - 全局导航栏集成“自选基金 / 我的持仓 👑VIP”快速切换。
  - 联动 SSE 与实时刷新机制，行情与估值变化时即时更新收益预估。

## Capabilities

### New Capabilities
- `portfolio-holding-management`: 用户持仓基金的增删改查、支持支付宝持仓卡片 4 大字段（持有金额、昨日收益、持有收益、持有收益率）的双向智能联动录入、预留未来截图 OCR 识别扩展。
- `portfolio-valuation-ui`: 持仓列表与今日收益预估界面，包括顶部汇总看板、持仓卡片/表格、非 VIP 预览与微信/支付宝扫码充值引导弹窗。

### Modified Capabilities
<!-- Existing capabilities whose REQUIREMENTS are changing (not just implementation).
     Only list here if spec-level behavior changes. Each needs a delta spec file.
     Use existing spec names from openspec/specs/. Leave empty if no requirement changes. -->

## Impact

- **数据层**：新增 `user_position` 持仓数据表（包含 `user_id`, `fund_code`, `holding_amount`, `yesterday_income`, `holding_profit`, `holding_profit_rate`, `cost_amount`, `holding_shares`, `cost_price` 等字段），支持 MySQL 与 H2 内存库。
- **后端 API**：
  - 新增 `PortfolioController` 及 `PortfolioService`，提供 `/api/portfolio` 列表查询（整合实时估值与收益测算）、新增/修改、删除接口，增加 VIP 权限拦截。
- **前端系统**：
  - 新增 `PortfolioPage.tsx` 页面组件与 `VipRechargeModal.tsx`（微信/支付宝收款弹窗）。
  - `App.tsx` 导航栏增加“自选基金 / 我的持仓 👑”Tab 切换与路由 `/portfolio`。
  - `frontend/src/api` 新增持仓相关的 API 客户端方法与 TypeScript 类型定义。
