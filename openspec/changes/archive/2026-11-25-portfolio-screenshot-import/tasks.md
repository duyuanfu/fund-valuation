## 1. 数据库与配置层（视觉识别配置与自动迁移）

- [x] 1.1 在 `DatabaseAutoMigrator` 与 `schema.sql` 中新增 `sys_vision_config` 表，支持视觉模型配置持久化（`enabled`, `api_base_url`, `api_key`, `model_name`, `daily_user_limit`）
- [x] 1.2 新建 `SysVisionConfig` 实体类、`SysVisionConfigMapper` 与 `SysVisionConfigService`（含启动加载、更新保存与 API Key 脱敏返回）

## 2. 基金名称模糊检索引擎（解决支付宝截图无代码痛点）

- [x] 2.1 在 `TianTianFundClient` 中新增 `searchFundsByName(String keyword)` 方法，接入天天基金搜索接口并复用现有 `RateLimiter` 防限流
- [x] 2.2 新增 `FundSearchService`，实现“本地 `fund` 表模糊匹配 + 远端天天基金兜底 + 简称截断清理与 A/C 类权重排序”逻辑
- [x] 2.3 新增 `GET /api/funds/search` 接口，供前端人工核对时实时搜索并替换基金代码

## 3. 持仓截图识别与批量保存后端接口

- [x] 3.1 新建 `VisionOcrClient`，使用 JDK `HttpClient` 对接兼容 OpenAI Vision 协议的视觉大模型，通过 Structured JSON Prompt 提取支付宝列表/详情截图的基金名称、金额、昨日收益、持有收益、收益率
- [x] 3.2 新建 `PortfolioScreenshotService`，实现图片格式与大小校验（<=5MB、仅内存处理不落盘）、单用户当日调用频次限制、结构化提取及自动关联 Top-3 候选基金代码
- [x] 3.3 在 `PortfolioService` 中新增 `batchSavePositions(String userId, List<PositionSaveRequest> items)` 事务方法，复用 4 字段双向联动计算与更新/覆盖逻辑
- [x] 3.4 在 `PortfolioController` 新增 `POST /api/portfolio/import-screenshot` 与 `POST /api/portfolio/batch` 接口并复用 VIP 权限拦截
- [x] 3.5 在管理后台控制器扩展视觉 OCR 配置读写及连通性测试接口（仅 ADMIN 可访问）

## 4. 前端截图导入流程与管理后台配置 UI

- [x] 4.1 在 `frontend/src/api` 新增 `searchFunds`、`importPortfolioScreenshots`、`batchSavePortfolio` 及管理员视觉配置接口与 TypeScript 类型定义
- [x] 4.2 新建 `ScreenshotImportModal.tsx` 组件：支持拖拽、点击上传及 `Ctrl+V` 剪贴板粘贴（最多 3 张）、Canvas 客户端等比压缩、隐私承诺提示与识别骨架屏动效
- [x] 4.3 在 `ScreenshotImportModal.tsx` 中实现可编辑识别核对清单表格：展示识别结果与置信/异常提示、支持候选基金代码下拉选择与远程模糊搜索、数值微调、勾选批量导入
- [x] 4.4 在 `PortfolioPage.tsx`“录入持仓”旁或录入弹窗内接入“截图导入”入口，导入成功后自动刷新持仓看板
- [x] 4.5 在 `AdminPage.tsx` 新增“AI 视觉识图配置”管理表单及一键连通性测试按钮

## 5. 测试验证与文档同步

- [x] 5.1 编写 `FundSearchServiceTest` 单元测试，覆盖本地模糊命中、截断名称清理与远端候选排序
- [x] 5.2 编写 `PortfolioScreenshotServiceTest` 单元测试（Mock Vision API 响应），覆盖多只基金 JSON 解析、非持仓图片空返回、非 VIP 拒绝、每日配额拦截与批量入库事务
- [x] 5.3 执行 `npm run build` 与 `mvn test` 全量构建测试，确认所有用例通过
