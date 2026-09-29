## Context

系统当前面向单角色用户，`app_user` 仅存储账号与密码散列，缺乏权限等级与账号可用状态标记。`/api/admin/**` 相关运维接口此前未纳入 JWT 鉴权保护，且日历休市日与系统公告缺乏前端可视化控制台，导致日常运营维护依赖直接修改配置文件或执行脚本。

本设计旨在引入轻量实用的 RBAC（基于角色的权限控制）体系，构建一体化管理后台（Admin Console），赋予管理员全方位的系统治理、日历编排、公告推送与运维监控能力。

## Goals / Non-Goals

**Goals:**
- **角色与权限治理 (RBAC)**: 用户表扩展 `role` (`ADMIN` / `USER`) 与 `status` (`NORMAL` / `DISABLED`)；`JwtAuthFilter` 强制拦截并校验 `/api/admin/**` 请求，被禁用账号立即阻断访问。
- **用户全生命周期管理**: 管理员可在后台查询用户列表、一键切换授权访问状态（启用/停用），并支持调阅任意普通用户的自选基金与实时估值快照。
- **VIP 与普通会员分级治理**: 用户表扩展 `is_vip` 与 `vip_expire_at`；后台提供 VIP 会员资格授予/取消；提供可插拔复用的 VIP 权益保护工具，支持未来高级功能无缝挂载 VIP 权限门槛。
- **动态交易日历 (Calendar Governance)**: 提供日历与休市日管理面板，支持查看、新增、删除自定义休市日，热重载内存 `TradingCalendar` 并持久化到数据库。
- **系统通知公告配置 (Notice Configuration)**: 在页面可视化维护通用公告正文、类型、启用开关与关闭策略，即时推送生效。
- **运维大盘与监控 (Operations & Metrics)**: 可视化一键触发实时估值与净值拉齐、人工维护债券组合久期、监控行情缓存容量与核心业务指标。
- **前端控制台界面**: 新增 `/admin` 管理后台路由与现代卡片化标签页，顶部导航栏对管理员角色显式展示入口。

**Non-Goals:**
- 不引入复杂的企业级多租户或多层级细粒度权限树（如 Casbin、Spring Security 大型框架），保持轻量高效与零多余开销。
- 不引入短信/邮件等外部重置密码通道，管理员具有最高治理权。

## Decisions

### 1. 数据模型与向后平滑兼容
- **决策**: 对 `app_user` 动态补充 `role`（默认 `USER`）、`status`（默认 `NORMAL`）、`is_vip`（默认 `0`）与 `vip_expire_at`（默认 `NULL`）列。新建 `sys_calendar_holiday` 与 `sys_notice_config` 表。
- **VIP 权益判定标准**: 用户为 ADMIN 或 `is_vip = 1 且 (vip_expire_at IS NULL OR vip_expire_at > NOW())` 时判定为有效 VIP，提供 `VipAuthChecker` 供后续任何新业务模块通过一行代码进行权益门槛拦截。
- **平滑兼容**: 启动阶段通过 `DatabaseInitializer` 执行幂等初始化（`ADD COLUMN IF NOT EXISTS` 或元数据比对补全），确保腾讯云已有 MySQL 实例无缝升级无需手动刷库脚本。
- **初始化管理员**: 支持在配置文件中指定超级管理员账号（如 `fund.admin.default-username: admin`），当该账号注册或服务启动时自动赋予 `ADMIN` 角色，杜绝管理员权限丢失风险。

### 2. 鉴权架构与安全拦截 (JwtAuthFilter)
- **决策**:
  - `/api/admin/**` 路由由 `JwtAuthFilter` 强制校验 Token，并在数据库或缓存中核验对应用户角色为 `ADMIN` 且状态为 `NORMAL`。
  - `/api/watchlist/**` 鉴权时若检测到用户 `status == DISABLED`，直接返回 HTTP 403 并提示账号已停用。
  - 未登录或非管理员访问管理接口一律拒绝，保障接口安全。

### 3. 休市日历持久化与热加载设计
- **决策**:
  - `TradingCalendar` 维持内存 `Set<LocalDate>` 高性能高速查找（O(1)）。
  - 在服务启动及管理员增删休市日后，将 `application.yml` 配置的基础节假日与 `sys_calendar_holiday` 数据库记录合并写入内存集合，实现零延迟生效。

### 4. 前端管理控制台布局
- **决策**:
  - 路由设计为 `/admin`，受前端 `AdminRoute` 守卫保护，非 `ADMIN` 角色自动重定向回首页。
  - 界面采用 Ant Design Tabs 设计：
    1. **用户与自选管理**: 用户概览表格、授权/取消授权切换按钮、调阅自选 Modal 弹窗。
    2. **交易休市日历**: 日历展示看板、新增休市表单、自定义休市日列表与删除。
    3. **公告与消息通知**: 表单化编辑公告内容、通知类型、启用开关与前端展示预览。
    4. **运维监控控制台**: 触发实时估值、拉齐最新净值、债券久期维护表单、系统缓存健康指标卡片。

## Risks / Trade-offs

- **[Risk] 云端旧库未执行新字段建表导致启动或查询报错**
  → **Mitigation**: 后端在应用启动阶段执行自动建表建列检查，SQL 均包含 `IF NOT EXISTS` 与兼容异常捕获。
- **[Risk] 封禁用户后，其现有 Token 在过期前仍可能产生访问**
  → **Mitigation**: `JwtAuthFilter` 解析用户后即刻校验用户实时状态，禁用状态下立即返回 403 阻断访问，无需等待 Token 7 天过期。
- **[Risk] 管理员调阅其他用户自选时可能产生越权误修改**
  → **Mitigation**: 严格设计只读接口 `GET /api/admin/users/{username}/watchlist`，仅返回该用户的自选基金实时估值计算结果，不暴露修改入口。
