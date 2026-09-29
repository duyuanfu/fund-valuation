## MODIFIED Requirements

### Requirement: 用户入口
系统 SHALL 提供用户登录与注册入口，校验账号密码与授权状态；登录成功后持久化 token、username、role 与 isVip 到本地存储，并根据用户角色与会员身份提供对应的页面导航与专属标识展示。若账号处于被停用/取消授权状态，系统 SHALL 拒绝登录。

#### Scenario: 正常用户成功登录
- **WHEN** 状态为 NORMAL 的用户提交正确账号密码
- **THEN** 系统签发 JWT Token 并返回用户名、角色 (`role`) 与会员身份 (`isVip`)，写入 localStorage，跳转自选页面；若为 VIP 用户则在顶部个人信息区展示 👑 VIP 徽章；若角色为 ADMIN 则额外展示管理后台入口

#### Scenario: 被禁用用户尝试登录
- **WHEN** 状态为 DISABLED 的用户尝试登录
- **THEN** 系统拒绝登录并返回“账号已被管理员停用，请联系管理员”错误提示

#### Scenario: 管理员角色登录识别
- **WHEN** 角色为 ADMIN 的用户登录成功后访问系统
- **THEN** 导航栏显式展示“管理控制台”入口按钮，点击可进入 `/admin` 管理界面
