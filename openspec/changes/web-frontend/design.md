## Context

后端基金实时估值服务已实现(REST + SSE,Spring Boot,端口 8080)。需要配套前端展示自选基金盘中估值、走势与持仓。技术选型已确认:Vite + React + TypeScript、Ant Design 5、ECharts、react-router-dom v6,代码位于仓库 `frontend/` 子目录。

后端无认证,以 userId 传参;SSE 推送接口为 `/api/watchlist/{userId}/stream`。

## Goals / Non-Goals

**Goals:**
- 自选估值列表页(SSE 实时更新、红涨绿跌、类型徽标、免责声明)
- 单只基金详情页(当日走势 ECharts 曲线、持仓明细表)
- userId 入口(localStorage 持久化、顶部切换)
- 自选添加/删除/排序交互
- Vite dev proxy `/api` → `localhost:8080`

**Non-Goals:**
- 登录鉴权体系(沿用后端无认证 userId 模型)
- 移动端优先设计(以桌面为主,布局自适应即可)
- 与后端强耦合的 SSR

## Decisions

### D1: Vite + React 18 + TS 工程
`npm create vite@latest` 的 `react-ts` 模板。开发代理 `/api` → `http://localhost:8080`,避免跨域。

### D2: Ant Design 5 + ECharts
- antd: Layout(顶部 Header + 内容)、Table、Card、Modal、Input、Popconfirm、Tag、message
- ECharts: 详情页走势曲线用 `echarts` 直接实例化(体积可控),封装一个 `useECharts` hook
- 红涨绿跌:涨跌幅正数红色、负数绿色(中国市场惯例),通过工具函数 `colorOf(pct)` 处理

### D3: 数据获取与状态
- `fetch` + 自定义 hook(`useApi`)封装 REST;SSE 用原生 `EventSource`
- 列表状态: `useWatchlist` hook 管理估值数组与 userId;SSE 推送直接替换列表数据
- 不需要引入 React Query(请求面窄,自定义 hook 足够);状态用 React 内置 + Context(一个 `UserIdContext`)

### D4: 路由与页面
- 路由: `/`(列表)、`/fund/:code`(详情)
- 详情页参数 `code`,进入时请求 `/api/fund/{code}`
- 无 userId 时跳转回入口(在 App 层拦截)

### D5: 目录结构
```
frontend/
  src/
    api/           // fetch 封装 + 接口定义
    components/    // 列表项、图表、类型徽标等
    hooks/         // useWatchlist, useECharts, useUserId, useSse
    pages/         // WatchlistPage, FundDetailPage, EntryPage
    router.tsx
    App.tsx, main.tsx
  vite.config.ts   // proxy
```

## Risks / Trade-offs

- **[SSE 与本地环境]** EventSource 不支持自定义 header;userId 通过 URL 路径传,无需 header,无影响。
- **[ECharts 体积]** 按需引入 echarts/core 组件,避免全量打包。
- **[后端接口字段]** 以当前后端 EstimateResult 字段为准(fundCode/fundName/fundType/estimateNav/estimatePct/prevNav/navDate/reportQt/quoteTs/stale/message/disclaimer),前端类型定义与之对齐。

## Migration Plan

1. `frontend/` 初始化 Vite 工程
2. 实现 api 层与 hooks
3. 实现入口页、列表页、详情页
4. 对接后端联调(dev proxy)

## Open Questions

- 列表页排序交互:用 antd Table 拖拽排序(dnd)还是简单上移/下移按钮?(参考级建议上下移动按钮,依赖少)
- 详情页是否需要显示"利率驱动"说明(债券基金估算基于收益率指数)(建议展示,增强可信度)