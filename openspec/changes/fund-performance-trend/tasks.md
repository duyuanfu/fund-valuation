## 1. 后端历史数据客户端与 DTO

- [x] 1.1 在 `TianTianFundClient` 中增加拉取基金历史业绩走势接口方法（对接 `FundNetDiagram.ashx`，支持 7 档 range 参数与反爬限流重试）
- [x] 1.2 创建历史走势与准确度视图 DTO：`FundPerformanceView`、`NavPointView`、`ValuationAccuracyView` 与 `AccuracyPointView`
- [x] 1.3 编写 `TianTianFundClient` 历史业绩解析单元测试

## 2. 后端核心服务与算法准确度分析

- [x] 2.1 实现 `FundPerformanceService`：整合历史走势拉取与 Caffeine 内存缓存管理（12小时 TTL）
- [x] 2.2 实现估值准确度对比引擎：从 `estimate_history` 提取各日收盘前最后估算点，与对应日期官方实际涨跌幅关联计算 MAE 与方向预测吻合率
- [x] 2.3 在 `FundController` 提供 `/api/fund/{code}/performance` REST 接口，支持 `range` 参数
- [x] 2.4 编写 `FundPerformanceServiceTest` 单元与准确度计算测试

## 3. 前端 API 与类型定义

- [x] 3.1 在 `frontend/src/api/types.ts` 中定义历史走势、时间范围参数与预估准确度类型
- [x] 3.2 在 `frontend/src/api/index.ts` 中封装 `getFundPerformance(code, range)` 请求方法

## 4. 前端走势图表升级与预估准确性 UI

- [x] 4.1 在 `FundDetailPage.tsx` 中增加“日内分时估值 / 历史业绩走势”分段控制器与 7 档时间周期（近1月、近3月、近6月、近1年、近3年、近5年、成立以来）切换组件
- [x] 4.2 基于 ECharts 实现历史业绩走势折线/面积图，横轴为日期，纵轴为区间收益率与净值，支持平滑缩放与双 Y 轴切换
- [x] 4.3 在走势图 Tooltip 与对比面板中叠加展示各交易日官方实际涨跌幅 vs 盘中最终预估涨跌幅，并展示 MAE 与方向预测吻合率分析卡片
- [x] 4.4 适配移动端卡片流与桌面端响应式交互

## 5. 联调、测试与验证

- [x] 5.1 前后端联调：验证 7 档时间周期切换、ECharts 走势图渲染及预估偏离指标计算
- [x] 5.2 边界场景测试：验证次新基金（历史数据不足 3/5 年）与无本地历史预估的新自选基金展示
- [x] 5.3 运行全量 `mvn test` 与前端 `npm run build` 确保无编译与回归错误
