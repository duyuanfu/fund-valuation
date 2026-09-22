## 1. 后端认证基础设施

- [ ] 1.1 新增 user 表建表 SQL(MySQL + H2)与 User 实体/Mapper
- [ ] 1.2 pom.xml 新增 jjwt(api/impl/jackson)与 spring-security-crypto 依赖
- [ ] 1.3 配置项 jwt.secret、jwt.expire-days;实现 JwtService(签发/解析)
- [ ] 1.4 实现 AuthService(注册/登录,BCrypt 密码哈希)与 AuthController(/api/auth/register、/login)
- [ ] 1.5 实现 JwtAuthFilter(拦截 /api/** ,放行 /api/auth、/api/fund、/api/admin;watchlist 校验 Bearer 或 SSE token query)并注册

## 2. 后端接口改造

- [ ] 2.1 WatchlistController 去掉 {userId} 路径参数,从 request attribute 取 username
- [ ] 2.2 SseController 改为 /api/watchlist/stream,从 token query 校验并注册连接
- [ ] 2.3 FundController.detail 对债券基金返回 bondInfo(收益率指数涨跌、久期、参考久期、公式)
- [ ] 2.4 FundDetailView/DTO 增加可选 bondInfo 字段

## 3. 前端认证

- [ ] 3.1 实现 useAuth(token+username 持久化、login/register/logout、401 拦截)
- [ ] 3.2 api 层自动附加 Authorization header;SSE URL 携带 token query
- [ ] 3.3 实现 AuthPage(登录/注册表单)替换 EntryPage;App 路由守卫与顶部用户信息/退出
- [ ] 3.4 WatchlistPage 改用 auth 用户,移除 userId 输入

## 4. 拖拽排序

- [ ] 4.1 安装 dnd-kit 依赖,实现可排序 Table 行组件
- [ ] 4.2 接入 onDragEnd → reorder API → 本地更新;移除上移/下移按钮

## 5. 债券基金详情增强

- [ ] 5.1 FundDetailPage: 债券基金展示利率驱动说明卡片(收益率指数、久期、参考久期、公式)
- [ ] 5.2 FundDetailPage: 债券基金隐藏当日估值走势曲线

## 6. 联调与收尾

- [ ] 6.1 后端单元/集成测试: AuthService、JwtService、鉴权 Filter(含 401 场景)
- [ ] 6.2 全量 mvn test 通过
- [ ] 6.3 前后端联调: 注册 → 登录 → 列表/SSE/详情/拖拽;401 跳登录
- [ ] 6.4 更新 README(认证说明、接口变更、老 userId 迁移)