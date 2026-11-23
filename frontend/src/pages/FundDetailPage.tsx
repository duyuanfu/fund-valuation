import { useEffect, useRef, useState, useCallback } from 'react'
import {
  Alert,
  Button,
  Card,
  Skeleton,
  Statistic,
  Table,
  Tag,
  Typography,
  Progress,
  Segmented,
  Spin,
  Space,
  message,
} from 'antd'
import {
  ArrowLeftOutlined,
  FallOutlined,
  RiseOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined,
  LineChartOutlined,
  HistoryOutlined,
  CompassOutlined,
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import type { ColumnsType } from 'antd/es/table'
import { api } from '../api'
import type {
  EstimateResult,
  FundDetailView,
  HoldingView,
  FundPerformanceView,
  PerformanceRange,
} from '../api/types'
import { FUND_TYPE_COLOR, FUND_TYPE_LABEL } from '../api/types'
import { useECharts } from '../hooks/useECharts'
import { colorOf, fmtNav, fmtPct } from '../utils/format'

const { Title, Text } = Typography

const RANGE_OPTIONS: Array<{ label: string; value: PerformanceRange }> = [
  { label: '近1月', value: '1m' },
  { label: '近3月', value: '3m' },
  { label: '近6月', value: '6m' },
  { label: '近1年', value: '1y' },
  { label: '近3年', value: '3y' },
  { label: '近5年', value: '5y' },
  { label: '成立以来', value: 'all' },
]

export default function FundDetailPage() {
  const { code } = useParams<{ code: string }>()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<FundDetailView | null>(null)
  const [loading, setLoading] = useState(true)
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)

  // 走势图视图切换: 'intraday' (日内分时) | 'performance' (历史业绩与预估对照)
  const [chartView, setChartView] = useState<'intraday' | 'performance'>('intraday')
  const [selectedRange, setSelectedRange] = useState<PerformanceRange>('1m')
  const [performanceData, setPerformanceData] = useState<FundPerformanceView | null>(null)
  const [perfLoading, setPerfLoading] = useState(false)

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768)
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  const intradayChartRef = useRef<HTMLDivElement | null>(null)
  const perfChartRef = useRef<HTMLDivElement | null>(null)
  const { renderChart: renderIntradayChart } = useECharts()
  const { renderChart: renderPerfChart } = useECharts()

  // 1. 获取基本详情
  useEffect(() => {
    if (!code) return
    let cancelled = false
    api
      .fundDetail(code)
      .then((d) => {
        if (!cancelled) setDetail(d)
      })
      .catch((e: Error) => message.error(e.message))
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [code])

  // 2. 获取历史业绩与预估对比数据
  const fetchPerformance = useCallback(
    (targetRange: PerformanceRange) => {
      if (!code) return
      setPerfLoading(true)
      api
        .fundPerformance(code, targetRange)
        .then((res) => {
          setPerformanceData(res)
        })
        .catch((e: Error) => {
          message.error(`拉取历史业绩失败: ${e.message}`)
        })
        .finally(() => {
          setPerfLoading(false)
        })
    },
    [code]
  )

  useEffect(() => {
    if (chartView === 'performance') {
      fetchPerformance(selectedRange)
    }
  }, [chartView, selectedRange, fetchPerformance])

  const estimate: EstimateResult | null = detail?.estimate ?? null
  const isBond = estimate?.fundType === 'bond'

  // 3. 渲染日内分时走势图
  useEffect(() => {
    if (loading || !detail || isBond || chartView !== 'intraday' || !intradayChartRef.current) return
    const history = detail.history ?? []
    if (!history.length) return

    const times = history.map((h) =>
      new Date(h.estTime).toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      })
    )
    const navs = history.map((h) => h.estNav)
    const isUp = (estimate?.estimatePct ?? 0) >= 0
    const lineColor = isUp ? '#cf1322' : '#389e0d'
    const areaColor = isUp ? 'rgba(207, 19, 34, 0.08)' : 'rgba(56, 158, 13, 0.08)'

    renderIntradayChart(intradayChartRef.current, {
      tooltip: {
        trigger: 'axis',
        formatter: (params: unknown) => {
          const arr = params as Array<{ name: string; value: number }>
          if (!arr || !arr.length) return ''
          const item = arr[0]
          return `${item.name}<br/>估算净值: <b>${item.value.toFixed(4)}</b>`
        },
      },
      grid: { left: 45, right: 15, top: 20, bottom: 25 },
      xAxis: {
        type: 'category',
        data: times,
        axisLine: { lineStyle: { color: '#cbd5e1' } },
        axisLabel: { color: '#64748b', fontSize: 11 },
      },
      yAxis: {
        type: 'value',
        scale: true,
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: {
          color: '#64748b',
          fontSize: 11,
          formatter: (v: number) => v.toFixed(3),
        },
      },
      series: [
        {
          type: 'line',
          data: navs,
          smooth: true,
          symbol: 'none',
          lineStyle: { color: lineColor, width: 2 },
          areaStyle: { color: areaColor },
        },
      ],
    })
  }, [loading, detail, isBond, estimate, chartView, renderIntradayChart])

  // 4. 渲染历史业绩走势与预估准确度图表
  useEffect(() => {
    if (chartView !== 'performance' || perfLoading || !performanceData || !perfChartRef.current) return
    const navPoints = performanceData.navPoints ?? []
    if (!navPoints.length) return

    const dates = navPoints.map((p) => p.date)
    const returns = navPoints.map((p) => Number(p.accumReturnPct ?? 0))
    const totalReturn = Number(performanceData.totalReturnPct ?? 0)
    const isUp = totalReturn >= 0
    const lineColor = isUp ? '#cf1322' : '#389e0d'
    const areaColor = isUp ? 'rgba(207, 19, 34, 0.08)' : 'rgba(56, 158, 13, 0.08)'

    const accuracyMap = new Map(
      (performanceData.accuracy?.points ?? []).map((pt) => [pt.date, pt])
    )

    renderPerfChart(perfChartRef.current, {
      tooltip: {
        trigger: 'axis',
        formatter: (params: unknown) => {
          const arr = params as Array<{ dataIndex: number }>
          if (!arr || !arr.length) return ''
          const idx = arr[0].dataIndex
          const point = navPoints[idx]
          if (!point) return ''

          const acc = accuracyMap.get(point.date)
          const ret = Number(point.accumReturnPct ?? 0)
          const daily = Number(point.dailyGrowthPct ?? 0)
          const retColor = ret >= 0 ? '#cf1322' : '#389e0d'
          const dailyColor = daily >= 0 ? '#cf1322' : '#389e0d'

          let html = `<div style="font-size:12px;min-width:170px;line-height:1.7;">`
          html += `<div style="font-weight:700;color:#0f172a;margin-bottom:4px;border-bottom:1px solid #e2e8f0;padding-bottom:3px;">${point.date}</div>`
          html += `<div>区间收益: <b style="color:${retColor}">${fmtPct(ret)}</b></div>`
          html += `<div>当日涨跌: <b style="color:${dailyColor}">${fmtPct(daily)}</b></div>`
          html += `<div>单位净值: <b>${fmtNav(point.nav)}</b></div>`

          if (acc) {
            const dirText = acc.directionMatched ? '✓ 吻合' : '✗ 偏离'
            const dirColor = acc.directionMatched ? '#16a34a' : '#dc2626'
            html += `<div style="margin-top:6px;padding-top:5px;border-top:1px dashed #cbd5e1;background:#f8fafc;padding:4px 6px;border-radius:4px;">`
            html += `<div style="font-weight:600;color:#475569;margin-bottom:2px;">盘中模型预估对照:</div>`
            html += `<div>盘中最终预估: <b style="color:#0f172a">${fmtPct(acc.estPct)}</b></div>`
            html += `<div>预估偏差绝对值: <b style="color:#d97706">${fmtPct(acc.diffPct)}</b></div>`
            html += `<div>预测方向: <b style="color:${dirColor}">${dirText}</b></div>`
            html += `</div>`
          }

          html += `</div>`
          return html
        },
      },
      grid: { left: 52, right: 20, top: 25, bottom: 25 },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#cbd5e1' } },
        axisLabel: {
          color: '#64748b',
          fontSize: 11,
          formatter: (val: string) => {
            if (val && val.length >= 10) {
              return val.substring(5) // MM-dd
            }
            return val
          },
        },
      },
      yAxis: {
        type: 'value',
        scale: true,
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: {
          color: '#64748b',
          fontSize: 11,
          formatter: (v: number) => `${v.toFixed(1)}%`,
        },
      },
      series: [
        {
          type: 'line',
          data: returns,
          smooth: true,
          symbol: 'none',
          lineStyle: { color: lineColor, width: 2 },
          areaStyle: { color: areaColor },
          markLine: {
            silent: true,
            symbol: 'none',
            lineStyle: { color: '#cbd5e1', type: 'dashed' },
            data: [{ yAxis: 0 }],
          },
        },
      ],
    })
  }, [chartView, perfLoading, performanceData, renderPerfChart])

  const holdingColumns: ColumnsType<HoldingView> = [
    {
      title: '股票代码 / 名称',
      dataIndex: 'stockCode',
      render: (c: string, r) => (
        <div>
          <Text strong style={{ color: '#0f172a' }}>
            {r.stockName}
          </Text>
          <div style={{ fontSize: 12, color: '#64748b' }}>{c}</div>
        </div>
      ),
    },
    {
      title: '持仓占比',
      dataIndex: 'weight',
      width: 140,
      align: 'right',
      render: (v: number) => (
        <div>
          <span style={{ fontWeight: 600 }}>{v.toFixed(2)}%</span>
          <Progress percent={Math.min(v * 5, 100)} showInfo={false} size="small" strokeColor="#1677ff" />
        </div>
      ),
    },
    {
      title: '今日涨跌幅',
      dataIndex: 'pctChg',
      width: 120,
      align: 'right',
      render: (v: number | null) => {
        const isUp = (v ?? 0) > 0
        const isDown = (v ?? 0) < 0
        const badgeClass = isUp ? 'badge-up' : isDown ? 'badge-down' : 'badge-flat'
        return <span className={badgeClass}>{fmtPct(v)}</span>
      },
    },
  ]

  return (
    <div
      style={{
        padding: isMobile ? '12px 10px 48px 10px' : '24px',
        maxWidth: 1000,
        margin: '0 auto',
      }}
    >
      {/* 顶部简易导航栏 */}
      <div style={{ marginBottom: 12 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          type="text"
          style={{ paddingLeft: 0, color: '#475569' }}
          onClick={() => navigate('/')}
        >
          返回自选
        </Button>
      </div>

      {loading ? (
        <Card style={{ borderRadius: 12 }}>
          <Skeleton active paragraph={{ rows: 6 }} />
        </Card>
      ) : !detail ? (
        <Alert type="error" showIcon message="未找到该基金详情" />
      ) : (
        <>
          {/* 基金核心 Hero 卡片 */}
          <Card
            style={{
              marginBottom: 14,
              borderRadius: 14,
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
              border: '1px solid #e2e8f0',
            }}
            styles={{ body: { padding: isMobile ? '16px' : '24px' } }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 12,
              }}
            >
              <div>
                <Title level={4} style={{ margin: 0, fontSize: isMobile ? 18 : 20, color: '#0f172a' }}>
                  {estimate?.fundName ?? code}
                </Title>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4 }}>
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    代码: {estimate?.fundCode ?? code}
                  </Text>
                  <Tag
                    color={FUND_TYPE_COLOR[estimate?.fundType ?? '']}
                    style={{ margin: 0, fontSize: 11, padding: '0 6px', borderRadius: 4 }}
                  >
                    {FUND_TYPE_LABEL[estimate?.fundType ?? ''] ?? estimate?.fundType}
                  </Tag>
                </div>
              </div>

              {estimate?.stale ? (
                <Tag color="warning">行情延迟</Tag>
              ) : (
                <Tag color="success">实时计算</Tag>
              )}
            </div>

            {/* 大数字核心指标行 */}
            <div
              style={{
                display: 'flex',
                alignItems: 'baseline',
                flexWrap: 'wrap',
                gap: 16,
                padding: '12px 0 16px 0',
                borderBottom: '1px solid #f1f5f9',
              }}
            >
              <div>
                <div style={{ fontSize: 12, color: '#64748b' }}>实时估算净值</div>
                <div
                  style={{
                    fontSize: isMobile ? 30 : 36,
                    fontWeight: 700,
                    color: colorOf(estimate?.estimatePct),
                    fontFamily: 'Roboto, "Helvetica Neue", sans-serif',
                    lineHeight: 1.1,
                    marginTop: 4,
                  }}
                >
                  {fmtNav(estimate?.estimateNav)}
                </div>
              </div>

              <div>
                <div style={{ fontSize: 12, color: '#64748b' }}>日内估算涨跌幅</div>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 4,
                    marginTop: 4,
                  }}
                >
                  <span
                    className={
                      (estimate?.estimatePct ?? 0) > 0
                        ? 'badge-up'
                        : (estimate?.estimatePct ?? 0) < 0
                        ? 'badge-down'
                        : 'badge-flat'
                    }
                    style={{ fontSize: isMobile ? 18 : 20, padding: '2px 10px', borderRadius: 6 }}
                  >
                    {(estimate?.estimatePct ?? 0) > 0 ? (
                      <RiseOutlined style={{ marginRight: 4 }} />
                    ) : (estimate?.estimatePct ?? 0) < 0 ? (
                      <FallOutlined style={{ marginRight: 4 }} />
                    ) : null}
                    {fmtPct(estimate?.estimatePct)}
                  </span>
                </div>
              </div>
            </div>

            {/* 关键辅助属性自适应网格 */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)',
                gap: 12,
                marginTop: 14,
              }}
            >
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  昨日净值
                </Text>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>
                  {fmtNav(estimate?.prevNav)}
                </div>
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  净值日期
                </Text>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>
                  {estimate?.navDate ?? '--'}
                </div>
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  持仓报告期
                </Text>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>
                  {estimate?.reportQt ?? '--'}
                </div>
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  行情时间
                </Text>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>
                  {estimate?.quoteTs ? new Date(estimate.quoteTs).toLocaleTimeString('zh-CN') : '--'}
                </div>
              </div>
            </div>
          </Card>

          {/* 纯债基金：利率驱动说明卡片（仅纯债基金且有 bondInfo 时额外呈现） */}
          {isBond && detail.bondInfo && (
            <Card
              title={
                <span style={{ fontSize: 15, fontWeight: 600, color: '#0f172a' }}>
                  债券利率驱动模型
                </span>
              }
              style={{
                marginBottom: 14,
                borderRadius: 14,
                border: '1px solid #e2e8f0',
              }}
            >
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(3, 1fr)',
                  gap: 16,
                  marginBottom: 14,
                }}
              >
                <Statistic
                  title={detail.bondInfo.indexName}
                  value={detail.bondInfo.indexPct ?? 0}
                  precision={2}
                  suffix="%"
                  valueStyle={{ color: colorOf(detail.bondInfo.indexPct), fontWeight: 700 }}
                />
                <Statistic
                  title="组合久期"
                  value={detail.bondInfo.duration}
                  precision={1}
                  suffix="年"
                  valueStyle={{ fontWeight: 700 }}
                />
                <Statistic
                  title="基准参考久期"
                  value={detail.bondInfo.referenceDuration}
                  precision={1}
                  suffix="年"
                  valueStyle={{ fontWeight: 700 }}
                />
              </div>

              <div
                style={{
                  backgroundColor: '#f8fafc',
                  padding: '10px 12px',
                  borderRadius: 8,
                  border: '1px solid #e2e8f0',
                }}
              >
                <Text strong style={{ fontSize: 13, color: '#334155', display: 'block', marginBottom: 4 }}>
                  <InfoCircleOutlined style={{ marginRight: 6, color: '#1677ff' }} />
                  估值算法: {detail.bondInfo.formula}
                </Text>
                <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                  提示: 纯债基金持仓不公开逐券秒级行情，估值主要追踪 10 年期国债收益率变化率、结合基金定期报告披露的组合平均久期进行理论测算。
                </Text>
              </div>
            </Card>
          )}

          {/* 综合走势看板：日内分时 / 历史业绩与预估对照 */}
          <Card
            style={{
              marginBottom: 14,
              borderRadius: 14,
              border: '1px solid #e2e8f0',
            }}
            styles={{ body: { padding: isMobile ? '12px 8px' : '18px' } }}
          >
            {/* 顶部分段控制器 */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: 10,
                marginBottom: 14,
                paddingBottom: 12,
                borderBottom: '1px solid #f1f5f9',
              }}
            >
              <Segmented
                value={chartView}
                onChange={(val) => setChartView(val as 'intraday' | 'performance')}
                options={[
                  {
                    label: (
                      <span style={{ padding: '0 4px' }}>
                        <LineChartOutlined style={{ marginRight: 6 }} />
                        日内分时估值
                      </span>
                    ),
                    value: 'intraday',
                  },
                  {
                    label: (
                      <span style={{ padding: '0 4px' }}>
                        <HistoryOutlined style={{ marginRight: 6 }} />
                        业绩走势与预估对照
                      </span>
                    ),
                    value: 'performance',
                  },
                ]}
              />

              {chartView === 'performance' && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  {performanceData?.totalReturnPct !== undefined && performanceData?.totalReturnPct !== null && (
                    <Tag
                      color={Number(performanceData.totalReturnPct) >= 0 ? 'red' : 'green'}
                      style={{ fontSize: 13, padding: '2px 8px', fontWeight: 600 }}
                    >
                      区间收益: {fmtPct(performanceData.totalReturnPct)}
                    </Tag>
                  )}
                </div>
              )}
            </div>

            {/* 历史业绩视图下的 7 档时间周期切换 */}
            {chartView === 'performance' && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'flex-start',
                  overflowX: 'auto',
                  paddingBottom: 8,
                  marginBottom: 10,
                }}
              >
                <Segmented
                  size={isMobile ? 'small' : 'middle'}
                  value={selectedRange}
                  onChange={(val) => setSelectedRange(val as PerformanceRange)}
                  options={RANGE_OPTIONS}
                />
              </div>
            )}

            {/* 图表主区域 */}
            {chartView === 'intraday' ? (
              isBond ? (
                <div style={{ textAlign: 'center', padding: '36px 0', color: '#64748b' }}>
                  <Text type="secondary">
                    纯债基金日内无交易所撮合分时脉冲，请切换至【业绩走势与预估对照】查看中长期净值增长与估值误差分析。
                  </Text>
                </div>
              ) : (
                <>
                  <div ref={intradayChartRef} style={{ height: isMobile ? 260 : 320, width: '100%' }} />
                  {(detail.history?.length ?? 0) === 0 && (
                    <div style={{ textAlign: 'center', padding: '20px 0' }}>
                      <Text type="secondary">盘中估值打点生成后将在此绘制实时脉搏</Text>
                    </div>
                  )}
                </>
              )
            ) : (
              /* 历史业绩与预估对照视图 */
              <Spin spinning={perfLoading}>
                <div ref={perfChartRef} style={{ height: isMobile ? 280 : 340, width: '100%' }} />
                {!perfLoading && (performanceData?.navPoints?.length ?? 0) === 0 && (
                  <div style={{ textAlign: 'center', padding: '32px 0' }}>
                    <Text type="secondary">暂无该时间区间的历史净值数据</Text>
                  </div>
                )}

                {/* 预估准确性分析看板 */}
                {performanceData && (
                  <div
                    style={{
                      marginTop: 16,
                      padding: '12px 14px',
                      backgroundColor: '#f8fafc',
                      borderRadius: 10,
                      border: '1px solid #e2e8f0',
                    }}
                  >
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        marginBottom: 8,
                      }}
                    >
                      <Space>
                        <CompassOutlined style={{ color: '#1677ff' }} />
                        <span style={{ fontWeight: 600, color: '#1e293b', fontSize: 13 }}>
                          模型估值准确性分析
                        </span>
                      </Space>

                      {performanceData.accuracy?.hasData && (
                        <Tag color="processing" icon={<CheckCircleOutlined />}>
                          样本: {performanceData.accuracy.sampleCount} 个交易日
                        </Tag>
                      )}
                    </div>

                    {performanceData.accuracy?.hasData ? (
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(3, 1fr)',
                          gap: 12,
                          marginTop: 6,
                        }}
                      >
                        <div>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            涨跌方向预测吻合率
                          </Text>
                          <div
                            style={{
                              fontSize: 18,
                              fontWeight: 700,
                              color:
                                Number(performanceData.accuracy.directionMatchRate ?? 0) >= 80
                                  ? '#16a34a'
                                  : '#d97706',
                              marginTop: 2,
                            }}
                          >
                            {performanceData.accuracy.directionMatchRate != null
                              ? `${Number(performanceData.accuracy.directionMatchRate).toFixed(1)}%`
                              : '--'}
                          </div>
                        </div>

                        <div>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            平均绝对偏差 (MAE)
                          </Text>
                          <div
                            style={{
                              fontSize: 18,
                              fontWeight: 700,
                              color: '#0f172a',
                              marginTop: 2,
                            }}
                          >
                            {performanceData.accuracy.mae != null
                              ? `±${Number(performanceData.accuracy.mae).toFixed(2)}%`
                              : '--'}
                          </div>
                        </div>

                        {!isMobile && (
                          <div>
                            <Text type="secondary" style={{ fontSize: 12 }}>
                              评估结论
                            </Text>
                            <div style={{ marginTop: 4 }}>
                              {(performanceData.accuracy.directionMatchRate ?? 0) >= 85 ? (
                                <Tag color="success">高参考价值</Tag>
                              ) : (
                                <Tag color="warning">正常参考级</Tag>
                              )}
                            </div>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div style={{ fontSize: 12, color: '#64748b', padding: '4px 0' }}>
                        提示: 该区间内暂无本地历史盘中预估打点记录（通常为新添加自选或冷启动基金），系统将在每个交易日收盘后自动积累对齐数据并展示拟合度。
                      </div>
                    )}
                  </div>
                )}
              </Spin>
            )}
          </Card>

          {/* 重仓持仓明细（移动端卡片式/桌面端完整表格） */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span style={{ fontSize: 16, fontWeight: 600, color: '#0f172a' }}>
                  前十大重仓股票
                </span>
                <span style={{ fontSize: 12, color: '#64748b', fontWeight: 400 }}>
                  报告期: {estimate?.reportQt ?? '最新定期报告'}
                </span>
              </div>
            }
            style={{ borderRadius: 14, border: '1px solid #e2e8f0' }}
            styles={{ body: { padding: isMobile ? '8px 12px' : '0' } }}
          >
            {isMobile ? (
              /* 移动端持仓简明卡片/列表项 */
              detail.holdings && detail.holdings.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {detail.holdings.map((h) => {
                    const isUp = (h.pctChg ?? 0) > 0
                    const isDown = (h.pctChg ?? 0) < 0
                    const badgeClass = isUp ? 'badge-up' : isDown ? 'badge-down' : 'badge-flat'
                    return (
                      <div
                        key={h.stockCode}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '10px 0',
                          borderBottom: '1px solid #f1f5f9',
                        }}
                      >
                        <div style={{ flex: 1 }}>
                          <Text strong style={{ fontSize: 14, color: '#0f172a' }}>
                            {h.stockName}
                          </Text>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
                            <span style={{ fontSize: 12, color: '#94a3b8' }}>{h.stockCode}</span>
                            <span style={{ fontSize: 12, color: '#64748b' }}>
                              占比: {h.weight.toFixed(2)}%
                            </span>
                          </div>
                        </div>

                        <div className={badgeClass} style={{ minWidth: 70, fontSize: 14 }}>
                          {fmtPct(h.pctChg)}
                        </div>
                      </div>
                    )
                  })}
                </div>
              ) : (
                <div style={{ padding: '24px 0', textAlign: 'center' }}>
                  <Text type="secondary">纯债基金或暂无成分券公开实时披露</Text>
                </div>
              )
            ) : (
              /* 桌面端全量持仓表格 */
              <Table<HoldingView>
                rowKey="stockCode"
                columns={holdingColumns}
                dataSource={detail.holdings ?? []}
                pagination={false}
                locale={{ emptyText: <Text type="secondary">无重仓数据(可能为纯债基金)</Text> }}
              />
            )}
          </Card>

          <Text
            type="secondary"
            style={{ display: 'block', marginTop: 16, textAlign: 'center', fontSize: 12 }}
          >
            {estimate?.disclaimer ?? '估值仅供参考，不构成投资建议'}
          </Text>
        </>
      )}
    </div>
  )
}
