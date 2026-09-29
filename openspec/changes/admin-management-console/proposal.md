## Why

当前系统缺乏角色权限隔离与统一的可视化管理后台：管理员接口暴露且未鉴权、交易休市日历与公告修改依赖硬编码或配置文件重启、无法对普通用户的注册和访问进行授权封禁，亦无法在排查问题时查看用户自选。

引入管理员角色 (Admin RBAC)、用户授权与自选监管体系、以及动态日历与公告配置面板，可赋予管理员对用户生命周期、系统运行节律、公告通知与后台运维的闭环管控能力。

## What Changes

- **管理员角色与鉴权体系 (Admin RBAC)**:
  - 用户表新增 `role` (`ADMIN` / `USER`) 和 `status` (`NORMAL` / `DISABLED`) 字段，系统预置或配置初始超级管理员。
  - 增强 `JwtAuthFilter`，严密拦截所有 `/api/admin/**` 路由，强制校验 JWT 并验证 `ADMIN` 角色。
  - 用户被停用后立即拒绝后续接口访问与重新登录。
- **用户治理与自选查看 (User Management & Watchlist Audit)**:
  - 提供用户管理列表：查看注册用户名、注册时间、角色、账号状态与 VIP 会员状态。
  - 支持一键授权访问或取消授权访问（禁用/启用账号）。
  - 支持管理员调阅任意普通用户的自选基金列表及实时估值，便于技术支持与客服排查。
- **VIP 与普通会员权益体系 (VIP Membership Tier)**:
  - 用户表新增 `is_vip` 与 `vip_expire_at` (到期时间，空表示未开通或已到期) 字段。
  - 管理员在后台可一键开通/续期/取消用户的 VIP 会员资格。
  - 登录与状态接口返回 `isVip` 标识，前端展现会员徽章，并提供通用的 VIP 鉴权拦截器供后续新高级功能快速复用接入。
- **交易日历与休市配置 (Trading Calendar Management)**:
  - 后台日历看板：支持查看当前年度所有生效休市日（公历节日、法定长假、临时闭市）。
  - 支持在页面直接新增、批量标记或删除休市日，修改后即时热生效至 `TradingCalendar` 并持久化。
- **系统通用公告管理 (Notice & Announcement Management)**:
  - 提供公告可视化表单：配置公告正文、展示类型 (`info` / `success` / `warning` / `error`)、启用开关与是否允许用户关闭。
- **扩展运维大盘与调优 (Operations & Diagnostics)**:
  - 运维看板：可视化一键触发盘中实时估值任务、一键触发官方净值全面拉齐刷新。
  - 人工债券组合久期覆盖表单：直观查看并维护指定债券基金的推断久期。
  - 行情缓存与系统健康大盘：展示缓存证券行情数、最后更新时间、总用户数与监控基金总数。

## Capabilities

### New Capabilities
- `admin-rbac-and-governance`: 管理员权限判定、用户管理（列表查询、授权/停用封禁、查看用户自选基金与估值）。
- `system-calendar-management`: 交易休市日可视化管理（休市日维护、内存日历热重载与持久化）。
- `system-notice-management`: 系统通知公告管理（后台公告配置、类型切换、开关控制与客户端同步）。
- `system-operations-console`: 后台运维控制台（一键估值触发、净值全量同步、债券久期维护与系统状态监控）。

### Modified Capabilities
- `user-entry`: 登录鉴权返回角色信息，受限/停用账号拒绝登录并返回明确原因；JWT 鉴权拦截非正常状态用户。

## Impact

- **数据模型**: `app_user` 表结构扩展 `role`、`status`、`is_vip`、`vip_expire_at`、`created_at`、`updated_at`；新增 `sys_calendar_holiday` 与 `sys_notice` 持久化表。
- **后端服务**: `JwtAuthFilter`、`AuthService`、`TradingCalendar`、`AdminController` 增强鉴权与业务接口。
- **前端页面**: 新增 `/admin` 管理后台模块（用户管理、日历配置、公告配置、运维监控 Tab 页），顶部导航栏对管理员角色显式展示“管理后台”入口。
- **破坏性变更**: `/api/admin/**` 不再匿名公开访问，需携带有效 ADMIN Token。
