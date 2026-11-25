## Context

目前持仓估值模块已构建完整的“支付宝持仓 4 核心字段”数据结构与计算体系（`user_position` 表包含 `fund_code`, `holding_amount`, `yesterday_income`, `holding_profit`, `holding_profit_rate`, `cost_amount`），但前端仅支持在 [PositionEditModal.tsx](file:///F:/Agent/Workspace/fund-valuation/frontend/src/components/PositionEditModal.tsx) 中逐只手动输入 6 位代码和金额。

在真实使用场景中，用户往往在支付宝同时持有 5~20 只基金。支付宝 App 不提供持仓导出功能，且持仓列表页**通常仅展示基金名称（长名称带 `...` 截断）、持有金额、昨日收益、持有收益/收益率，而不展示 6 位基金代码**。因此，需要设计一套高准确度、低延迟、可控成本且隐私安全的“支付宝持仓截图一键录入”链路。

## Goals / Non-Goals

**Goals:**
- **高可行性与高准确率的识图提取**：支持识别支付宝基金持仓列表页截图（单张多只基金）及单只基金详情页截图，准确抽取基金名称、6位代码（若有）、持有金额、昨日收益、持有收益、持有收益率。
- **解决“有简称无代码”的业务难点**：根据识别出的基金简称（即使存在 `...` 截断）自动匹配最可能的 6 位基金代码，并给出 Top-3 候选列表供用户在预览确认表中秒级校正。
- **可控成本与零重型本地依赖**：不向 Spring Boot 打包庞大的本地 C++ OCR JNI 依赖，而是借助 JDK 17 内置 `HttpClient` 对接可配置的 OpenAI Vision 兼容多模态大模型接口（如 Qwen-VL、GLM-4V、豆包 Vision 等）。
- **人工核对闭环与批量入库**：识别后绝不静默直接覆盖数据库，必须经过可视化确认弹窗，允许勾选、修改基金代码、调整数值，再调用批量保存接口事务落库。
- **隐私安全与防刷**：图片仅在前端 Canvas 压缩后于内存透传识别，绝不在服务端磁盘落盘保存；接口强制要求 VIP 鉴权与单用户每日识别次数上限控制。

**Non-Goals:**
- 本阶段不接入支付宝开放平台账户授权或自动化爬虫抓取用户个人支付宝账单（合规风险极高）。
- 本阶段不在前端打包引入 20MB+ 的 `tesseract.js` 中文模型包，亦不承诺脱离网络配置后离线本地精准识图。
- 不支持自动识别交易记录流水（买入/卖出单笔明细），聚焦当前持仓快照录入。

## Decisions

### 1. 识图引擎选型：多模态视觉大模型（OpenAI Vision 兼容协议）vs 本地传统 OCR
- **备选方案 A：前端 `tesseract.js` / 后端 `Tess4J` / `RapidOCR`**
  - 评估：支付宝持仓截图存在多列错落排版、千分位逗号、红绿色号区分正负（部分界面或特定配色仅靠位置区分收益与收益率）、长基金名省略号截断，纯文本 OCR 识别后行/列坐标容易错位，导致金额与基金张冠李戴；同时 `chi_sim` 语言包或 ONNX 模型几十 MB，会严重拖慢首屏加载或增加 Docker 镜像及内存开销。
- **选定方案：后端可配置的 OpenAI Vision 兼容协议（Structured JSON Prompting）**
  - 理由：多模态视觉模型具备原生空间排版理解能力，能直接区分“持有金额 / 昨日收益 / 持有收益 / 收益率”的字段语义与正负符号，输出严格 JSON 数组，准确率显著高于纯规则 OCR；单张 1080p 截图经前端缩放至长边 <= 1280px 后约 150~300KB，单次识别成本仅约 0.001~0.005 元。
  - 配置灵活性：后台新增 `sys_vision_config` 配置表，管理员可自由填写国内主流免费或高性价比兼容服务商（如阿里云百炼 `qwen-vl-plus`、智谱 `glm-4v-flash`、火山引擎或 OpenAI），支持一键测试连通性及全局开关。

### 2. 支付宝截图“无 6 位代码”的匹配引擎（`fund-search-lookup`）
- **问题分析**：支付宝持仓列表仅显示基金简称（例如 `易方达蓝筹精选混合` 或 `广发纳斯达克100ETF联接...`），而估值系统强依赖 6 位 `fund_code`。
- **两级混合匹配链路**：
  1. **直出优先**：若截图内（如单只详情页）识别出 6 位数字代码，直接校验入库信息并设为默认匹配。
  2. **本地库 + 天天基金模糊检索**：对无代码的名称，清理 `...` 截断符及特殊符号后：
     - 先查本地 `fund` 表模糊匹配（`LIKE`）；
     - 本地未命中或不足 3 条时，调用天天基金公开搜索 API（`FundSearchAPI.ashx`）获取候选集，按名称重合度与 A/C 类后缀精确打分排序，返回 `matchedCode` 与最多 3 个 `candidates`（`code`, `name`, `type`）。
  3. **前端核对清单容错**：若同名 A/C 类难以唯一确定或截断严重，前端高亮标识“请核对代码”，用户可直接从下拉候选中点选，或输入关键字/6位代码实时调用 `/api/funds/search` 替换。

### 3. 前后端数据流与接口协议
- **前端上传与压缩 (`ScreenshotImportModal`)**：
  - 支持拖拽、点击选择及 `Ctrl+V` 剪贴板粘贴（PC 端微信截图直接粘贴体验极佳），单次最多 3 张。
  - 利用浏览器 `<canvas>` 将图片最大边限制在 `1280px`（JPEG quality `0.85`），转为 Base64/Multipart 提交，大幅降低网络耗时并防止 Spring Multipart 体积超限。
- **识别接口：`POST /api/portfolio/import-screenshot`**
  - 鉴权：VIP / ADMIN 专享；检查系统视觉识别开关是否启用及当前用户当日调用配额。
  - 限流：基于数据库或本地缓存记录 `user_id + date` 的当日成功调用次数（默认每日上限 10 次，后台可调）。
  - 返回结构 `ScreenshotParseResponse`：
    - `items`: `List<ParsedPositionCandidate>`，每条含 `rawFundName`, `matchedCode`, `matchedName`, `candidates`, `holdingAmount`, `yesterdayIncome`, `holdingProfit`, `holdingProfitRate`, `confidence`, `warningMessage`。
    - `remainingQuota`: 当日剩余识图次数。
- **批量保存接口：`POST /api/portfolio/batch`**
  - 接收用户核对勾选后的 `List<PositionSaveRequest>`，在单事务中复用 `PortfolioService.savePosition` 逐条校验、换算成本与份额并保存（同 `fund_code` 自动覆盖更新）。

### 4. 配置存储设计
- **新建 `sys_vision_config` 表**（由 `DatabaseAutoMigrator` 自动迁移）：
  - `id` (BIGINT PK)
  - `enabled` (TINYINT(1) DEFAULT 0)
  - `api_base_url` (VARCHAR(255), e.g. `https://dashscope.aliyuncs.com/compatible-mode/v1`)
  - `api_key` (VARCHAR(255))
  - `model_name` (VARCHAR(128), e.g. `qwen-vl-plus`)
  - `daily_user_limit` (INT DEFAULT 10)
  - `updated_at` (DATETIME)

## Risks / Trade-offs

- **[Risk] 视觉模型 API 响应较慢（2~6 秒）或偶发超时/限流**
  - → **Mitigation**: 前端上传前 Canvas 压缩控图在 300KB 内，请求超时设为 25s；界面展示扫描进度提示与重试按钮；后端支持多张图单次并发或打包识别，失败时返回明确友好报错，引导使用手动录入兜底。
- **[Risk] 基金 A/C 份额简称在列表中被截断（如均显示 `易方达沪深300ETF联接...`），导致选错收费类型**
  - → **Mitigation**: A 类与 C 类虽净值略有差异但当日跟踪涨跌幅（`estimatePct`）高度接近；同时系统在候选列表中完整展示 A/C 全称并用橙色徽标提示用户“检测到 A/C 多份额，请确认类别”。
- **[Risk] 用户误传非持仓截图、模糊图或恶意高频刷接口消耗 Token**
  - → **Mitigation**: Prompt 严格要求仅提取基金持仓卡片字段，非持仓截图返回空列表并友好提示“未识别到有效的支付宝持仓卡片”；接口严格执行 VIP 鉴权 + 每日单用户配额限制 + 最大 5MB 图片校验。
- **[Risk] 用户担心资产截图隐私泄露**
  - → **Mitigation**: 服务端全程内存流转调用 Vision API，严禁将用户图片写入服务器本地磁盘或数据库，并在上传区域显式标注隐私承诺。
