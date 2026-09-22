## Context

前端看板已实现。本期引入 JWT 认证、拖拽排序与债券基金详情增强。认证采用轻量方案:jjwt 签发 HS256 自签 token + 自定义 Filter 校验,不引入 Spring Security 全家桶(参考级可控)。现有 user_fund 表以 userId 存储,新注册的 username 与老 userId 同值即自然兼容既有数据。

## Goals / Non-Goals

**Goals:**
- 注册/登录 + JWT 鉴权(自选接口受保护)
- 前端登录/注册页、token 管理、路由守卫、SSE 鉴权
- 自选列表拖拽排序(dnd-kit)
- 债券基金详情页展示利率驱动说明、隐藏当日走势

**Non-Goals:**
- 权限分级/角色(仅普通用户)
- token 刷新机制(7 天有效期,过期重新登录)
- 密码找回/修改

## Decisions

### D1: JWT 鉴权实现
- 依赖: `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.x`(HS256)、`spring-security-crypto`(仅用 BCryptPasswordEncoder,不引入完整 security)
- `JwtService`: 签发(HS256, subject=username, 有效期默认 7 天)/解析
- `JwtAuthFilter extends OncePerRequestFilter`: 拦截 `/api/**`
  - 放行 `/api/auth/**`、`/api/fund/**`(详情公开)、`/api/admin/**`(参考级暂不鉴权,生产关闭)
  - `/api/watchlist/**`: 从 `Authorization: Bearer <token>` 或 SSE 的 `token` 查询参数取 token → 校验 → request attribute 写入 username
- 401 响应统一 JSON `{"error": "未登录或登录已过期"}`
- 配置: `jwt.secret`(≥32字节)、`jwt.expire-days: 7`

### D2: 接口改造(自选)
- WatchlistController: `/{userId}` 路径参数删除,改为从 request attribute 取 username
- SseController: `/api/watchlist/stream?token=xxx`,Filter 从 query 取 token 校验
- 向后兼容: username 即老 userId,注册同名账号即看到既有自选数据

### D3: 债券基金利率驱动信息(后端)
- FundController.detail: 若基金 type=bond,额外返回 `bondInfo`:
  ```json
  { "indexName": "10年期国债收益率", "indexPct": 0.14,
    "duration": 3.8, "referenceDuration": 10,
    "formula": "估算涨跌幅 = 收益率指数涨跌 × (久期 / 参考久期)" }
  ```
  - indexPct 来自 quoteService.getFreshQuote(103.TY00Y);duration 来自 bondDurationService
- FundDetailView 增加可选 `bondInfo` 字段

### D4: 前端拖拽排序
- 依赖 `@dnd-kit/core` + `@dnd-kit/sortable` + `@dnd-kit/utilities`
- antd Table 用 `components={{ body: { row: SortableRow } }}` 包裹行,`onDragEnd` 计算新顺序 → 调 reorder API → 本地更新
- 移除"上移/下移"按钮

### D5: 前端认证
- `useAuth`: token + username(localStorage 持久化),login/register/logout 方法,401 全局拦截清 token 跳登录
- api 层自动附加 `Authorization: Bearer <token>`(fetch 包装)
- 路由守卫: 无 token → `<Navigate to="/login">`;SSE URL 拼接 `?token=`
- 顶部 Header: 显示 username + 退出按钮(替换"切换用户")

### D6: 页面结构调整
- `EntryPage` → 替换为 `AuthPage`(登录/注册 tab)
- `App.tsx` 守卫逻辑基于 token

## Risks / Trade-offs

- **[自签 JWT secret 泄露]** 配置文件泄露即可伪造 token → secret 用环境变量注入,README 提示;参考级可接受
- **[SSE query token]** token 出现在 URL 会进日志 → 参考级可接受,已在 specs 说明;后续可换 cookie/子协议
- **[老 userId 兼容]** 用户必须注册同名账号才看到老数据 → 已在需求明确,README 说明迁移
- **[jjwt 版本]** 需与 Java 17 兼容(0.12.x 支持)→ 已选定

## Migration Plan

1. 后端: 加 user 表 schema、JwtService、AuthController、Filter、改造 watchlist 接口、detail 加 bondInfo
2. 前端: 加 AuthPage/useAuth、api 鉴权、路由守卫、dnd-kit 拖拽、债券详情卡片
3. 联调: 注册 → 登录 → 列表/SSE/详情/拖拽

## Open Questions

- 注册是否需要邮箱等更多字段?默认仅 username+password
- admin 接口是否纳入鉴权(建议后续加独立 admin key)?本期保持公开