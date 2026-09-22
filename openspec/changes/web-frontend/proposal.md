## Why

后端基金实时估值服务已实现(REST + SSE),需要配套前端页面,让用户能直观地查看自选基金盘中估值、走势与持仓明细,并通过 SSE 实时感知估值变化。

## What Changes

在仓库 `frontend/` 下新建 React + TypeScript 前端:

- **用户入口**: 顶部输入 userId 进入,本地持久化(localStorage),与后端无认证的 userId 传参保持一致
- **自选估值列表**: 展示自选基金的估算净值、估算涨跌幅、净值日期、持仓报告期、新鲜度标记与免责声明;通过 SSE 盘中实时更新
- **自选管理**: 添加/删除基金、拖拽或按钮排序
- **单只基金详情**: 基本信息 + 当前估值 + 当日估值走势曲线(ECharts)+ 持仓明细(含实时涨跌幅)
- **基建**: Vite 开发代理 `/api` → 后端 8080,Ant Design 组件库,ECharts 图表

## Capabilities

### New Capabilities
- `watchlist-ui`: 自选基金估值列表页——列表渲染、SSE 实时更新、类型徽标、新鲜度与免责展示
- `fund-detail-ui`: 单只基金详情页——当日估值走势曲线、持仓明细表、基本信息
- `user-entry`: 用户入口——userId 输入、localStorage 持久化、路由保护
- `watchlist-management-ui`: 自选管理交互——添加/删除/排序

### Modified Capabilities
- 无(前端为新增,不改动后端规格)

## Impact

- **新增**: `frontend/` 目录(Vite + React + TS),依赖 antd、echarts、react-router-dom
- **配置**: Vite dev server proxy `/api` → `http://localhost:8080`
- **对接后端接口**: `/api/watchlist/{userId}`(列表/增删改)、`/api/fund/{code}`(详情)、`/api/watchlist/{userId}/stream`(SSE)
- **不影响**: 后端代码与数据模型保持不变