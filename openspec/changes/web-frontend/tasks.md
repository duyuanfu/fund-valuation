## 1. 工程初始化

- [x] 1.1 在 frontend/ 初始化 Vite + React + TS 工程,配置依赖(antd、echarts、react-router-dom)
- [x] 1.2 配置 vite.config.ts: /api 代理到 http://localhost:8080
- [x] 1.3 配置 antd 主题(中文语言包)与应用骨架(main.tsx / App.tsx)

## 2. API 层与数据 hooks

- [x] 2.1 实现 api/fetch 封装(REST,统一错误处理)与接口定义(列表/增删改/详情/SSE)
- [x] 2.2 实现 TypeScript 类型: EstimateResult、FundDetailView、HoldingView、EstimatePoint
- [x] 2.3 实现 hooks: useUserId(localStorage)、useWatchlist(估值列表)、useSse(EventSource 自动重连)、useECharts

## 3. 用户入口

- [x] 3.1 实现入口页(userId 输入 + 进入)
- [x] 3.2 实现路由拦截:无 userId 跳回入口;顶部 userId 切换

## 4. 自选估值列表页

- [x] 4.1 实现列表页:表格/卡片展示估算净值、估算涨跌幅(红涨绿跌)、净值日期、报告期、类型徽标
- [x] 4.2 实现 SSE 实时更新(替换列表数据)与断线重连
- [x] 4.3 实现非交易时段提示与免责声明展示
- [x] 4.4 实现异常状态展示(无净值基准/数据缺失)

## 5. 自选管理交互

- [x] 5.1 实现添加基金对话框(输入代码,错误提示)
- [x] 5.2 实现删除确认(Popconfirm)与排序(上下移动按钮)

## 6. 单只基金详情页

- [x] 6.1 实现详情页:基本信息 + 当前估值 + 免责声明
- [x] 6.2 实现 ECharts 当日走势曲线(时间-净值折线/面积图)
- [x] 6.3 实现持仓明细表格(代码/名称/权重/实时涨跌幅)

## 7. 联调与收尾

- [x] 7.1 本地启动前后端联调:入口 → 列表 → SSE 更新 → 详情
- [x] 7.2 添加/删除/排序交互验证
- [x] 7.3 README 补充前端启动说明