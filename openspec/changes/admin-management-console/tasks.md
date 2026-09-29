## 1. 数据模型与管理员角色鉴权 (RBAC & Security)

- [x] 1.1 扩展 `app_user` 表结构增加 `role`、`status`、`is_vip` 与 `vip_expire_at` 字段，并在服务启动时幂等初始化管理员角色
- [x] 1.2 改造 `AuthService` 登录接口返回用户角色与 VIP 状态，对 `DISABLED` 用户拒绝登录并返回禁用提示
- [x] 1.3 强化 `JwtAuthFilter`：严格校验 `/api/admin/**` 路由的 ADMIN 权限，并对禁用账号即刻拦截访问
- [x] 1.4 编写用户角色鉴权与禁用阻断单元测试
- [x] 1.5 实现可复用的 VIP 权益拦截与校验工具 `VipSecurityService`，支持未来新功能权限限制

## 2. 用户治理与自选查看后端实现 (User Management APIs)

- [x] 2.1 新增管理员用户管理接口：支持列表查询用户基础信息、状态、角色与 VIP 会员状态
- [x] 2.2 新增用户授权与禁用接口：`POST /api/admin/users/{username}/status` 切换 NORMAL/DISABLED
- [x] 2.3 新增 VIP 会员资格授权接口：`POST /api/admin/users/{username}/vip` 支持开通、续期与取消 VIP
- [x] 2.4 新增查看目标用户自选列表与实时估值接口：`GET /api/admin/users/{username}/watchlist`
- [x] 2.5 编写用户管理、VIP 调整与自选查询单元测试

## 3. 交易日历休市管理后端实现 (Calendar Management APIs)

- [x] 3.1 创建休市日持久化实体与数据表 `sys_calendar_holiday`
- [x] 3.2 改造 `TradingCalendar` 支持动态增删休市日与持久化合并
- [x] 3.3 新增管理员日历接口：`GET /api/admin/calendar/holidays`、`POST /api/admin/calendar/holidays` 与 `DELETE /api/admin/calendar/holidays/{date}`
- [x] 3.4 编写动态日历休市日增删与热重载测试

## 4. 运维大盘与监控指标接口 (Operations & Metrics APIs)

- [x] 4.1 新增运维监控接口 `GET /api/admin/stats`，汇总缓存行情数量、用户总数、自选基金总量及最后调度时间
- [x] 4.2 完善公告管理持久化支持与重置通知状态机制

## 5. 前端管理员控制台与页面联动 (Frontend Admin Console)

- [x] 5.1 扩展前端类型定义与 API 封装（用户管理、VIP 配置、休市日历、公告配置、运维监控）
- [x] 5.2 实现管理员权限守卫路由 `AdminRoute` 及顶部导航栏管理员入口徽章
- [x] 5.3 实现“用户管理”Tab：支持列表展示、授权/禁用切换、VIP 开通/取消操作、弹窗调阅用户自选与实时估值
- [x] 5.4 实现“交易日历”Tab：支持日历面板可视化呈现休市日、新增与删除休市日
- [x] 5.5 实现“系统公告”Tab：支持可视化编辑公告正文、类型、启用开关与实时预览
- [x] 5.6 实现“运维监控”Tab：一键触发估值任务、一键拉齐净值、债基久期维护表单与运行指标卡片
- [x] 5.7 前端全局状态增加 👑 VIP 会员徽章展示与通用 VIP 权限守卫组件

## 6. 全链路验证与构建归档 (Verification & Packaging)

- [x] 6.1 执行 Maven 后端单元测试并验证编译正常
- [x] 6.2 执行前端 TypeScript 校验与 Vite 生产打包
- [x] 6.3 同步静态资源至 Spring Boot static 目录并完成最终验证
