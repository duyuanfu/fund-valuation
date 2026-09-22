## Why

前端看板已可展示自选估值,但存在三点不足:①自选排序仅支持上下移按钮,拖拽体验更好;②债券基金详情页缺少"利率驱动"的说明(用户不知估值从何而来),且当日走势对纯债基金意义有限;③后端无认证,任何人凭 userId 即可访问他人自选数据,需要注册登录。

## What Changes

- **用户注册登录(JWT)**: 后端新增用户表、注册/登录接口、JWT 签发与鉴权过滤器;自选接口改为从 token 解析用户身份(保留现有 userId 数据:注册 username 与老 userId 同值即可看到既有自选)
- **前端登录/注册页**: 替换原 userId 入口;token 持久化、请求带 Authorization header、路由守卫、SSE 携带 token
- **拖拽排序**: 前端用 dnd-kit 实现自选列表拖拽排序,替换上移/下移按钮
- **债券基金详情增强**: 后端详情接口返回利率驱动信息(收益率指数涨跌、组合久期、参考久期);前端债券基金详情页展示"利率驱动"说明卡片、隐藏当日估值走势

## Capabilities

### New Capabilities
- `user-auth`: 后端用户注册/登录、JWT 签发与校验、自选接口鉴权
- `auth-ui`: 前端登录/注册页、token 管理、请求鉴权头、路由守卫、SSE token 传递

### Modified Capabilities
- `watchlist-management-ui`: 排序交互由按钮改为拖拽(dnd-kit)
- `fund-detail-ui`: 债券基金详情展示利率驱动说明、隐藏当日估值走势
- `watchlist-api`: 自选接口由路径 userId 传参改为 token 解析用户身份

## Impact

- **后端新增**: `user` 表、User 实体/Mapper、JwtService、AuthService/AuthController、JWT 鉴权 Filter
- **后端修改**: WatchlistController/SseController 去 userId 路径参数(改从 token 解析);FundController 详情返回债券利率信息;pom.xml 新增 jjwt、spring-security-crypto
- **前端修改**: 新增 AuthPage、useAuth;api 层带 token;WatchlistPage 拖拽排序;FundDetailPage 债券说明卡片;路由守卫
- **配置**: 新增 `jwt.secret`、`jwt.expire-days`
- **不兼容**: `/api/watchlist/{userId}` 及 `/stream/{userId}` 路径变更 —— **BREAKING**(老客户端需更新)