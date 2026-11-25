## Why

当前持仓估值仅支持按单只基金逐条手动输入 6 位代码与 4 个持仓字段（[PositionEditModal.tsx](file:///F:/Agent/Workspace/fund-valuation/frontend/src/components/PositionEditModal.tsx)），当用户在支付宝持有 5~20 只基金时，逐一查代码并手工抄录金额与收益门槛较高、体验繁琐。同时在原 `portfolio-holdings-estimator` 提案中已按支付宝 4 字段预留了完整数据结构，因此现在具备了引入“支付宝持仓截图一键识图录入”的充分条件，可显著降低 VIP 用户冷启动门槛并提升付费转化率。

## What Changes

- **技术可行性与方案定型**：
  - **视觉识别引擎**：采用“后端调用兼容 OpenAI Vision 协议的多模态视觉模型（如 Qwen-VL / 豆包 / 智谱 GLM-4V 等）为主 + 人工二次核对清单”方案，规避传统 OCR（如 Tesseract）对支付宝多列数字、千分位及红绿配色识别率低、以及 Java 引入本地重型 C++ 模型包运维复杂的问题。
  - **基金名称到代码自动解析**：针对支付宝持仓列表截图通常只显示“基金简称（可能截断）”而不显示 6 位基金代码的核心难点，后端新增基金名称检索服务（本地 `fund` 表优先 + 天天基金检索接口兜底），自动返回匹配度最高的 6 位基金代码与 Top-3 候选列表。
  - **前端图片压缩与交互闭环**：在持仓录入入口新增“截图导入”方式，前端自动使用 Canvas 对大图进行轻度缩放压缩（不落盘、仅内存识别），识别结果进入可编辑核对表格（支持核对/更换候选基金代码、修改金额与收益、勾选条目），确认后一键批量保存入库。
- **管理后台视觉服务配置与防刷保护**：
  - 后台新增 AI 视觉识别配置（开关、`baseUrl`、`apiKey`、`model`、`每日单用户限流次数`），未配置或关闭时前端优雅降级提示使用手动录入。
  - 后端接口严格复用 VIP 权限校验与每日调用频次限制，防止外部 API 额度滥用；截图文件仅内存透传、绝不落盘存储以保障用户资产隐私。
- **批量持仓保存接口**：
  - 在 `PortfolioController` / `PortfolioService` 新增批量保存接口（`/api/portfolio/batch`），复用现有双向联动公式与事务保存逻辑，支持一次确认导入多只基金持仓。

## Capabilities

### New Capabilities
- `portfolio-screenshot-import`: 支持 VIP 用户上传（1~3 张）支付宝持仓列表或单只详情截图，经前端压缩与后端多模态视觉识别提取 4 核心字段，结合基金名称智能匹配 6 位基金代码，提供可编辑核对清单并批量导入持仓。
- `fund-search-lookup`: 提供基于基金名称关键字或简称前缀的基金代码检索引擎（本地数据库 + 天天基金官方搜索接口）及管理后台视觉 OCR 配置能力。

### Modified Capabilities
<!-- No existing spec files in openspec/specs/ require delta modification; extends portfolio holding entry flow cleanly. -->

## Impact

- **后端代码**：
  - 新增 `VisionOcrConfig` 持久化实体与 `SysVisionConfigService`（或复用配置管理服务与 `DatabaseAutoMigrator` 自动建表）。
  - 新增 `PortfolioScreenshotService`（负责图片大小/格式校验、调用 Vision API 提取结构化 JSON、名称匹配基金代码、单用户每日限流计数）。
  - 新增 `TianTianFundClient.searchFundsByName(keyword)` 方法，支持通过基金简称检索候选 `FundSearchItem(code, name, type)`。
  - 扩展 `PortfolioController` 新增 `POST /api/portfolio/import-screenshot`、`POST /api/portfolio/batch`、`GET /api/funds/search`（供前端核对清单手动下拉搜索基金代码）。
  - 扩展 `AdminConfigController` 管理视觉模型 API 配置与连通性测试。
- **前端系统**：
  - `PortfolioPage.tsx`“录入持仓”旁增加“截图导入”按钮（或集成入口），新建 `ScreenshotImportModal.tsx` 组件（上传/拖拽/粘贴截图、Canvas 压缩、识别骨架屏、可编辑核对表格与候选代码选择器）。
  - `AdminPage.tsx` 增设视觉识别模型配置表单（API Endpoint、Key、Model、每日限额）。
- **外部依赖与合规**：
  - 后端零新增重型 OCR Maven 依赖，直接复用 JDK 17 `java.net.http.HttpClient` 调用 Vision API 与天天基金搜索接口；依赖管理后台配置有效的视觉模型 Key。
