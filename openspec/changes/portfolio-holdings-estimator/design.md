## Context

系统目前具备高频抓取、债券利率模型与股票持仓透视的日内实时估值体系，并为用户提供了自选关注列表（`WatchlistPage`）。在实际理财场景中，用户最核心的需求是掌握自己在支付宝等平台上实际买入的基金“今天到底赚了或亏了多少钱”。
为了满足该高价值需求，同时构建良性商业变现闭环，系统将“持仓今日收益预估”设计为 **VIP 会员专享核心权益**。
录入方式全面 100% 对标用户最熟悉的支付宝基金持有界面 4 大核心数据（**“持有金额”**、**“昨日收益”**、**“持有收益”**和**“持有收益率”**），并预留后续截图 OCR 自动录入的扩展性。非会员用户进入持仓页时，采用高转化率的“功能预览 + 引导开通”模式，通过微信/支付宝扫码充值激活 VIP。

## Goals / Non-Goals

**Goals:**
- **会员权限体系与充值体验**：
  - 后端 `/api/portfolio` 接口对修改及真实持仓数据做 VIP 鉴权（非 VIP 拦截或返回会员限制）。
  - 前端为非 VIP 用户提供高转化率的持仓预览效果（毛玻璃虚化模拟看板 + 权益介绍卡片），点击弹出微信/支付宝充值开通弹窗。
- **100% 对标支付宝持仓 4 字段的数据录入与智能联动**：
  - 支持用户录入：基金代码/名称、**持有金额（元）**、**昨日收益（元，选填）**、**持有收益（元）**与**持有收益率（%，选填）**。
  - 前端表单提供“持有收益（元）”与“持有收益率（%）”**双向智能实时联动**：用户录入收益金额自动换算收益率，录入收益率自动反算收益金额与成本。
  - 数据模型与接口规格全面为后续“支付宝持仓截图 OCR 自动识别录入”预留完整对齐的结构。
- **昨日收益与今日预估收益双屏联动**：
  - 动态计算单只持仓今日预估收益：$今日预估收益 = 持有金额 \times (estimatePct / 100)$。
  - 最新动态估算市值：$持有金额 + 今日预估收益$。
  - 最新动态累计总盈亏：$持有收益 + 今日预估收益$。
  - 展示用户记录的“昨日收益”对照“今日预估收益”，直观展现两日波动脉搏。
- **汇总看板与多端适配**：
  - 顶部看板实时统计持仓总市值、今日预估总收益、累计总盈亏、今日总收益率。
  - 移动端卡片流 + 桌面端表格，支持实时轮询与 SSE 动态跳动。

**Non-Goals:**
- 本阶段不直接调用微信商户/支付宝商户 API 进行自动化回调（采用微信/支付宝扫码支付 + 填写备注，人工或后台审核开通，合规且落地极快）。
- 本阶段不直接内置复杂 OCR 算法包，但数据模型和接口格式与支付宝持仓截图完全对齐，为未来一键识图录入做好完整准备。

## Decisions

### 1. 数据模型与存储设计
- **新建 `user_position` 表**：
  - `id`: BIGINT AUTO_INCREMENT 主键
  - `user_id`: VARCHAR(64) 用户名
  - `fund_code`: VARCHAR(10) 基金代码
  - `holding_amount`: DECIMAL(14, 2) NOT NULL COMMENT '持仓总金额/资产现值(元)'
  - `yesterday_income`: DECIMAL(14, 2) NULL DEFAULT 0 COMMENT '支付宝昨日收益(元, 选填)'
  - `holding_profit`: DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '持有收益/累计收益(元)'
  - `holding_profit_rate`: DECIMAL(8, 4) NULL COMMENT '持有收益率(%, 选填/联动推算)'
  - `cost_amount`: DECIMAL(14, 2) NOT NULL DEFAULT 0 COMMENT '持仓成本金额(元, 自动推算: holding_amount - holding_profit)'
  - `holding_shares`: DECIMAL(14, 4) NULL COMMENT '折算持有份额(用于净值差额精确估算)'
  - `cost_price`: DECIMAL(10, 4) NULL COMMENT '折算成本净值'
  - `created_at` / `updated_at`: DATETIME
  - 唯一约束：`uk_user_fund (user_id, fund_code)`
- **决策理由**：字段命名与支付宝“金额、昨日收益、持有收益、持有收益率”一一对应，用户记账零心智负担，未来 OCR 识别直接 1:1 落库。

### 2. 会员权限拦截与充值履约设计
- **后端鉴权**：
  - 从 `request.getAttribute(JwtAuthFilter.ATTR_USERNAME)` 获取当前用户。
  - 校验该用户是否拥有 VIP（或 ADMIN 角色）。若未开通，对持仓接口返回 HTTP 403 及明确文案。
- **前端预览与充值弹窗 (`VipRechargeModal`)**：
  - 当非 VIP 用户访问 `/portfolio` 时，展示高质感模拟看板（如今日收益 +¥386.20，展示 2~3 只典型基金持仓模拟行），上层叠加毛玻璃效果与解锁 VIP 专属卡片。
  - 点击“开通 VIP 会员”呼出充值弹窗，展示微信、支付宝收款码切换，提供常用开通选项（如：月度会员 / 季度会员 / 年度会员），提示支付时备注“用户名”，并附带客服联系与快速开通通道。

### 3. 收益估算引擎计算公式与表单联动
- **表单智能联动公式**：
  - 用户输入 $Amount$（金额）和 $Profit$（持有收益）：
    $Cost = Amount - Profit$；$Rate = Profit / Cost \times 100\%$。
  - 用户输入 $Amount$（金额）和 $Rate$（收益率 %）：
    $Cost = Amount / (1 + Rate / 100)$；$Profit = Amount - Cost$。
- **日内盘中实时估值推算**：
  - 估值涨跌幅：$Pct = estimatePct / 100$
  - 最新预估市值：$MarketValue = Amount \times (1 + Pct)$
  - 今日预估收益：$TodayIncome = Amount \times Pct$
  - 最新累计总盈亏：$TotalProfit = Profit + TodayIncome$
  - 最新累计收益率：$TotalProfit / Cost \times 100\%$
- **看板聚合总计**：
  - 持仓总市值：$\sum MarketValue$
  - 今日预估总收益：$\sum TodayIncome$
  - 累计总盈亏：$\sum TotalProfit$
  - 今日总收益率：$\sum TodayIncome / \sum Amount \times 100\%$

## Risks / Trade-offs

- **[Risk] 用户在支付宝查看的持有收益是截至昨天的，如果今天盘中收益加进去是否会重复？**
  - → **Mitigation**: 支付宝界面的“持有收益”在交易时间内是不包含今日日内波动的（今日波动以“昨日收益”或盘中估值单列）。因此“最新累计总盈亏 = 持有收益 + 今日预估收益”公式完全符合金融常识。
- **[Risk] 收益率与收益金额由于四舍五入产生的微小尾差**
  - → **Mitigation**: 后端与前端统一以 `holding_amount` 和 `holding_profit` 为金标准基准，`holding_profit_rate` 作为辅助显示与输入参数，保留 2 位小数，杜绝精度漂移。
