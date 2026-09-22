## ADDED Requirements

### Requirement: 登录/注册页
系统 SHALL 提供登录与注册页面,替换原 userId 输入入口。

#### Scenario: 未登录跳转
- **WHEN** 用户未登录(本地无有效 token)访问页面
- **THEN** 跳转至登录页

#### Scenario: 登录与注册切换
- **WHEN** 用户在登录页点击"注册"
- **THEN** 切换到注册表单(username + 密码)

#### Scenario: 登录成功进入看板
- **WHEN** 登录或注册成功
- **THEN** 保存 token 与 username 到 localStorage,进入自选估值列表

### Requirement: token 管理与请求鉴权
系统 SHALL 持久化 JWT,并在所有自选相关请求中携带鉴权头。

#### Scenario: 请求携带 token
- **WHEN** 前端调用自选接口
- **THEN** 请求头携带 Authorization: Bearer <token>

#### Scenario: 401 处理
- **WHEN** 接口返回 401(token 过期/无效)
- **THEN** 清空本地 token,跳转登录页

#### Scenario: 退出登录
- **WHEN** 用户点击退出
- **THEN** 清空 token 与 username,回到登录页

### Requirement: SSE token 传递
系统 SHALL 在建立 SSE 连接时通过查询参数传递 token。

#### Scenario: SSE 建立连接
- **WHEN** 前端创建 EventSource
- **THEN** URL 携带 token 查询参数,鉴权通过后建立连接并接收推送
