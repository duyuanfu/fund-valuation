## ADDED Requirements

### Requirement: 用户注册
系统 SHALL 允许用户注册账号(username + password),密码以 BCrypt 哈希存储,username 全局唯一。

#### Scenario: 注册成功
- **WHEN** 用户提交未占用的 username 与合法密码
- **THEN** 创建用户并返回成功,密码以 BCrypt 哈希入库

#### Scenario: 用户名已存在
- **WHEN** username 已被注册
- **THEN** 返回 400 与"用户名已存在"提示

### Requirement: 用户登录与 JWT 签发
系统 SHALL 校验用户名密码,成功则签发 JWT(HS256,含 username 与过期时间),用于后续接口鉴权。

#### Scenario: 登录成功
- **WHEN** 用户名密码校验通过
- **THEN** 返回 JWT、username 与过期时间

#### Scenario: 登录失败
- **WHEN** 用户名不存在或密码错误
- **THEN** 返回 401 与"用户名或密码错误"提示

### Requirement: 自选接口鉴权
系统 SHALL 对自选相关接口(/api/watchlist/**)实施 JWT 鉴权,用户身份从 token 解析,不再依赖路径参数。

#### Scenario: 携带有效 token 访问
- **WHEN** 请求带有效 Authorization Bearer token
- **THEN** 从 token 解析 username 作为用户标识,正常处理请求

#### Scenario: 缺少/无效 token
- **WHEN** 请求无 token 或 token 无效/过期
- **THEN** 返回 401,不处理业务

#### Scenario: SSE 鉴权
- **WHEN** 客户端建立 SSE 连接
- **THEN** 通过 URL 查询参数传递 token 并校验,校验通过才建立连接

### Requirement: 用户表
系统 SHALL 持久化用户账号(id、username 唯一、password_hash、created_at)。

#### Scenario: 数据持久化
- **WHEN** 用户注册成功
- **THEN** 用户记录写入 user 表,username 唯一约束生效
