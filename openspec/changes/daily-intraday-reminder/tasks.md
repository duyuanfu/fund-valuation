## 1. 数据库与基础依赖

- [x] 1.1 在 `backend/pom.xml` 中引入 `spring-boot-starter-mail` 依赖
- [x] 1.2 在 `schema.sql`、`schema-h2.sql` 与 `DatabaseAutoMigrator.java` 中定义并自动迁移 `user_notification_channel` 和 `sys_email_config` 表结构
- [x] 1.3 创建 `UserNotificationChannel` 与 `SysEmailConfig` 实体类及对应的 MyBatis-Plus Mapper

## 2. 发信服务与模板渲染核心

- [x] 2.1 实现 `SysEmailConfigService`，支持从数据库动态读取与配置 `JavaMailSenderImpl`
- [x] 2.2 实现 `EmailSenderService`，支持异步发送标准多部分 HTML 邮件及网络重试
- [x] 2.3 实现 `WxNotificationService`，支持微信 Webhook、WxPusher、Bark 与 Server酱推送及连通性测试
- [x] 2.4 实现 `DailyReminderTemplateService`，渲染移动端响应式 HTML 邮件与紧凑 Markdown 走势卡片（包含红绿涨跌、持仓盈亏、自选榜单与网页直达链接）

## 3. 用户设置与 14:30 定时调度

- [x] 3.1 实现 `UserNotificationChannelService`，支持用户渠道绑定、推送偏好维护与一键测试触发
- [x] 3.2 实现 `DailyReminderScheduler`，配置交易日 14:30 定时器，集成 `TradingCalendar` 严格过滤休市日，批量拉取用户持仓/自选估值并异步派发
- [x] 3.3 新增 `NotificationChannelController` 用户接口与 `AdminEmailController` 运维接口

## 4. 前端界面与控制台集成

- [x] 4.1 在 `frontend/src/api/` 中增加通知渠道与系统邮件配置相关 API 接口
- [x] 4.2 开发「每日 14:30 盘中提醒」设置组件 `NotificationChannelModal.tsx`，支持邮箱绑定、微信渠道配置、推送偏好设置及测试按钮
- [x] 4.3 在自选基金列表页及持仓页面导航中加入「14:30 盘中提醒」醒目操作入口与状态指示
- [x] 4.4 在管理员后台 `AdminPage.tsx` 中新增系统发件 SMTP 配置面板及 14:30 推送测试工具

## 5. 自动化测试与整体验证

- [x] 5.1 编写 `UserNotificationChannelServiceTest` 与 `DailyReminderSchedulerTest` 单元测试，验证交易日过滤、多通道渲染与异常隔离
- [x] 5.2 执行前端全量构建与后端测试套件验证，确保零回归
