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

  const estimate: EstimateResult | null = detail?.estimate ?? null
  const isBond = estimate?.fundType === 'bond'
  const hasIntraday = !isBond
  const activeChartView = hasIntraday ? chartView : 'performance'

  // 若为纯债基金等无日内分时估值的产品，自动且强制聚焦到“业绩走势与预估对照”
  useEffect(() => {
    if (isBond && chartView !== 'performance') {
      setChartView('performance')
    }
  }, [isBond, chartView])

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
    if (activeChartView === 'performance') {
      fetchPerformance(selectedRange)
    }
  }, [activeChartView, selectedRange, fetchPerformance])

  // 3. 渲染日内分时走势图
  useEffect(() => {
    if (loading || !detail || !hasIntraday || activeChartView !== 'intraday' || !intradayChartRef.current) return
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
  }, [loading, detail, hasIntraday, activeChartView, estimate, renderIntradayChart])

  // 4. 渲染历史业绩走势与预估准确度图表
  useEffect(() => {
    if (activeChartView !== 'performance' || perfLoading || !performanceData || !perfChartRef.current) return
    const navPoints = performanceData.navPoints ?? []
    if (!navPoints.length) return

    const dates = navPoints.map((p) => p.date)
    const returns = navPoints.map((p) => Number(p.accumReturnPct ?? 0))
    const baseNav = Number(navPoints[0]?.nav ?? 1)

    const accuracyMap = new Map(
      (performanceData.accuracy?.points ?? []).map((pt) => [pt.date, pt])
    )

    // 构建预估累计收益率对照曲线数据点 (有预估点映射为收益率数值，无预估点置为 null)
    const estReturns: Array<number | null> = navPoints.map((p, i) => {
      const acc = accuracyMap.get(p.date)
      if (!acc) return null
      if (acc.estAccumReturnPct != null) {
        return Number(acc.estAccumReturnPct)
      }
      // 兼容兜底: 动态根据前一日净值和预估日涨跌计算区间累计收益
      const prevNav = i > 0 ? Number(navPoints[i - 1]?.nav ?? baseNav) : baseNav
      if (prevNav > 0 && baseNav > 0) {
        const estNav = prevNav * (1 + Number(acc.estPct) / 100)
        return Number((((estNav - baseNav) / baseNav) * 100).toFixed(2))
      }
      return null
    })

    const hasEstimates = (performanceData.accuracy?.sampleCount ?? 0) > 0

    renderPerfChart(perfChartRef.current, {
      legend: {
        show: true,
        top: 2,
        right: 12,
        itemWidth: 16,
        itemHeight: 8,
        itemGap: 16,
        textStyle: { color: '#64748b', fontSize: 12 },
        data: [
          { name: '官方实际走势', icon: 'roundRect', itemStyle: { color: '#1677ff' } },
          { name: '模型预估对照', icon: 'roundRect', itemStyle: { color: '#fa8c16' } },
        ],
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        padding: [10, 12],
        extraCssText: 'box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08); border-radius: 10px;',
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

          const estRet = estReturns[idx]
          const estRetColor = (estRet ?? 0) >= 0 ? '#cf1322' : '#389e0d'

          let html = `<div style="font-size:12px;min-width:185px;line-height:1.7;color:#1e293b;">`
          html += `<div style="font-weight:700;color:#0f172a;margin-bottom:6px;border-bottom:1px solid #f1f5f9;padding-bottom:4px;display:flex;justify-content:space-between;align-items:center;">`
          html += `<span>${point.date}</span>`
          html += `<span style="font-size:11px;font-weight:normal;color:#94a3b8">收盘确认</span>`
          html += `</div>`

          // 官方实际走势
          html += `<div style="margin-bottom:4px;">`
          html += `<div style="display:flex;align-items:center;justify-content:space-between;">`
          html += `<span style="color:#475569"><span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#1677ff;margin-right:6px;"></span>实际累计收益:</span>`
          html += `<b style="color:${retColor};font-size:13px">${fmtPct(ret)}</b>`
          html += `</div>`
          html += `<div style="display:flex;align-items:center;justify-content:space-between;font-size:11px;color:#64748b;padding-left:14px;">`
          html += `<span>当日实际 / 净值:</span>`
          html += `<span><b style="color:${dailyColor}">${fmtPct(daily)}</b> / ${fmtNav(point.nav)}</span>`
          html += `</div>`
          html += `</div>`

          // 模型预估对照
          if (acc) {
            const dirText = acc.directionMatched ? '✓ 同向吻合' : '✗ 偏离'
            const dirColor = acc.directionMatched ? '#16a34a' : '#dc2626'
            html += `<div style="margin-top:6px;padding-top:6px;border-top:1px dashed #fed7aa;background:#fffaf0;margin-left:-6px;margin-right:-6px;padding:6px 8px;border-radius:6px;">`
            html += `<div style="display:flex;align-items:center;justify-content:space-between;">`
            html += `<span style="color:#b45309;font-weight:600;"><span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#fa8c16;margin-right:6px;"></span>模型预估累计:</span>`
            html += `<b style="color:${estRetColor};font-size:13px">${fmtPct(estRet)}</b>`
            html += `</div>`
            html += `<div style="display:flex;align-items:center;justify-content:space-between;font-size:11px;color:#92400e;padding-left:14px;margin-top:2px;">`
            html += `<span>盘中最终预估:</span>`
            html += `<b>${fmtPct(acc.estPct)}</b>`
            html += `</div>`
            html += `<div style="display:flex;align-items:center;justify-content:space-between;font-size:11px;color:#92400e;padding-left:14px;">`
            html += `<span>预估绝对偏差:</span>`
            html += `<b style="color:#d97706">${fmtPct(acc.diffPct)}</b>`
            html += `</div>`
            html += `<div style="display:flex;align-items:center;justify-content:space-between;font-size:11px;padding-left:14px;margin-top:2px;">`
            html += `<span>预测方向判定:</span>`
            html += `<b style="color:${dirColor}">${dirText}</b>`
            html += `</div>`
            html += `</div>`
          } else {
            html += `<div style="margin-top:6px;padding-top:6px;border-top:1px dashed #f1f5f9;font-size:11px;color:#94a3b8;display:flex;align-items:center;">`
            html += `<span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:#cbd5e1;margin-right:6px;"></span>当日无盘中估算快照打点`
            html += `</div>`
          }

          html += `</div>`
          return html
        },
      },
      grid: { left: 48, right: 18, top: 36, bottom: 25 },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisTick: { show: false },
        axisLabel: {
          color: '#94a3b8',
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
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          color: '#94a3b8',
          fontSize: 11,
          formatter: (v: number) => `${Number(v).toFixed(1)}%`,
        },
      },
      series: [
        {
          name: '官方实际走势',
          type: 'line',
          data: returns,
          smooth: 0.2,
          symbol: 'circle',
          showSymbol: false,
          symbolSize: 6,
          itemStyle: { color: '#1677ff' },
          lineStyle: { color: '#1677ff', width: 2.2 },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(22, 119, 255, 0.14)' },
                { offset: 1, color: 'rgba(22, 119, 255, 0.00)' },
              ],
              global: false,
            },
          },
          markLine: {
            silent: true,
            symbol: 'none',
            lineStyle: { color: '#cbd5e1', type: 'dashed' },
            data: [{ yAxis: 0 }],
            label: {
              formatter: '0.00%',
              position: 'end',
              color: '#94a3b8',
              fontSize: 10,
            },
          },
        },
        {
          name: '模型预估对照',
          type: 'line',
          data: estReturns,
          connectNulls: true,
          smooth: 0.2,
          symbol: 'circle',
          showSymbol: hasEstimates,
          symbolSize: (value: unknown) => (value != null ? 5 : 0),
          itemStyle: {
            color: '#fa8c16',
            borderColor: '#ffffff',
            borderWidth: 1.5,
          },
          lineStyle: {
            color: '#fa8c16',
            width: 2,
            type: [5, 4],
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
              {hasIntraday ? (
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
              ) : (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 6,
                      padding: '5px 12px',
                      backgroundColor: '#eff6ff',
                      borderRadius: 8,
                      color: '#1d4ed8',
                      fontWeight: 600,
                      fontSize: 13,
                    }}
                  >
                    <HistoryOutlined style={{ color: '#2563eb' }} />
                    <span>业绩走势与预估对照</span>
                  </div>
                  <Tag color="cyan" style={{ borderRadius: 6, margin: 0, fontSize: 11, padding: '1px 8px' }}>
                    纯债型 · 净值对照
                  </Tag>
                </div>
              )}

              {activeChartView === 'performance' && performanceData?.totalReturnPct !== undefined && performanceData?.totalReturnPct !== null && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Tag
                    color={Number(performanceData.totalReturnPct) >= 0 ? 'red' : 'green'}
                    style={{ fontSize: 13, padding: '2px 8px', fontWeight: 600, borderRadius: 6 }}
                  >
                    区间收益: {fmtPct(performanceData.totalReturnPct)}
                  </Tag>
                  {performanceData.accuracy?.hasData && (
                    <Tag color="orange" style={{ fontSize: 12, borderRadius: 6, margin: 0 }}>
                      双曲线已对齐 {performanceData.accuracy.sampleCount} 天
                    </Tag>
                  )}
                </div>
              )}
            </div>

            {/* 支付宝风格 7 档时间周期切换 Pill 条 */}
            {activeChartView === 'performance' && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'flex-start',
                  overflowX: 'auto',
                  paddingBottom: 6,
                  marginBottom: 12,
                  scrollbarWidth: 'none',
                }}
              >
                <Segmented
                  size={isMobile ? 'small' : 'middle'}
                  value={selectedRange}
                  onChange={(val) => setSelectedRange(val as PerformanceRange)}
                  options={RANGE_OPTIONS}
                  style={{
                    backgroundColor: '#f1f5f9',
                    padding: 3,
                    borderRadius: 8,
                  }}
                />
              </div>
            )}

            {/* 图表主区域 */}
            {activeChartView === 'intraday' ? (
              <>
                <div ref={intradayChartRef} style={{ height: isMobile ? 260 : 320, width: '100%' }} />
                {(detail.history?.length ?? 0) === 0 && (
                  <div style={{ textAlign: 'center', padding: '20px 0' }}>
                    <Text type="secondary">盘中估值打点生成后将在此绘制实时脉搏</Text>
                  </div>
                )}
              </>
            ) : (
              /* 历史业绩与预估对照视图 (支付宝体验) */
              <Spin spinning={perfLoading}>
                {performanceData && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'baseline',
                      justifyContent: 'space-between',
                      flexWrap: 'wrap',
                      gap: 8,
                      padding: '2px 0 8px 0',
                      borderBottom: '1px solid #f1f5f9',
                      marginBottom: 8,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
                      <span style={{ fontSize: 13, color: '#64748b' }}>区间收益率:</span>
                      <span
                        style={{
                          fontSize: 22,
                          fontWeight: 700,
                          color: Number(performanceData.totalReturnPct ?? 0) >= 0 ? '#cf1322' : '#389e0d',
                          fontVariantNumeric: 'tabular-nums',
                        }}
                      >
                        {fmtPct(performanceData.totalReturnPct)}
                      </span>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 14, fontSize: 12, color: '#64748b' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                        <span style={{ display: 'inline-block', width: 14, height: 3, backgroundColor: '#1677ff', borderRadius: 2 }} />
                        官方实际
                      </span>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                        <span style={{ display: 'inline-block', width: 14, height: 0, borderTop: '2px dashed #fa8c16' }} />
                        模型预估
                      </span>
                    </div>
                  </div>
                )}

                <div ref={perfChartRef} style={{ height: isMobile ? 280 : 340, width: '100%' }} />
                {!perfLoading && (performanceData?.navPoints?.length ?? 0) === 0 && (
                  <div style={{ textAlign: 'center', padding: '32px 0' }}>
                    <Text type="secondary">暂无该时间区间的历史净值数据</Text>
                  </div>
                )}

                {/* 支付宝风格：模型估值准确性分析看板 */}
                {performanceData && (
                  <div
                    style={{
                      marginTop: 18,
                      padding: '14px 16px',
                      background: 'linear-gradient(180deg, #f8fafc 0%, #ffffff 100%)',
                      borderRadius: 12,
                      border: '1px solid #e2e8f0',
                      boxShadow: '0 2px 6px rgba(15, 23, 42, 0.02)',
                    }}
                  >
                    {/* 看板头部 */}
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        marginBottom: 12,
                      }}
                    >
                      <Space size={8}>
                        <div
                          style={{
                            width: 26,
                            height: 26,
                            borderRadius: 6,
                            backgroundColor: 'rgba(22, 119, 255, 0.08)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#1677ff',
                          }}
                        >
                          <CompassOutlined style={{ fontSize: 15 }} />
                        </div>
                        <div>
                          <span style={{ fontWeight: 600, color: '#0f172a', fontSize: 14 }}>
                            模型估值准确性分析
                          </span>
                          <span style={{ fontSize: 11, color: '#94a3b8', marginLeft: 8 }}>
                            盘中估算 vs 官方净值
                          </span>
                        </div>
                      </Space>

                      {performanceData.accuracy?.hasData && (
                        <Tag
                          color="processing"
                          icon={<CheckCircleOutlined />}
                          style={{ borderRadius: 6, margin: 0, fontWeight: 500 }}
                        >
                          比对样本: {performanceData.accuracy.sampleCount} 个交易日
                        </Tag>
                      )}
                    </div>

                    {performanceData.accuracy?.hasData ? (
                      <>
                        {/* 核心指标三列网格 */}
                        <div
                          style={{
                            display: 'grid',
                            gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(3, 1fr)',
                            gap: 12,
                            padding: '10px 0',
                            borderTop: '1px solid #f1f5f9',
                            borderBottom: '1px solid #f1f5f9',
                          }}
                        >
                          <div>
                            <Text type="secondary" style={{ fontSize: 12 }}>
                              涨跌方向预测吻合率
                            </Text>
                            <div
                              style={{
                                fontSize: 20,
                                fontWeight: 700,
                                color:
                                  Number(performanceData.accuracy.directionMatchRate ?? 0) >= 80
                                    ? '#16a34a'
                                    : '#d97706',
                                marginTop: 2,
                                fontVariantNumeric: 'tabular-nums',
                              }}
                            >
                              {performanceData.accuracy.directionMatchRate != null
                                ? `${Number(performanceData.accuracy.directionMatchRate).toFixed(1)}%`
                                : '--'}
                            </div>
                            <Progress
                              percent={Math.min(Number(performanceData.accuracy.directionMatchRate ?? 0), 100)}
                              showInfo={false}
                              size="small"
                              strokeColor={
                                Number(performanceData.accuracy.directionMatchRate ?? 0) >= 80
                                  ? '#16a34a'
                                  : '#d97706'
                              }
                              style={{ marginTop: 2 }}
                            />
                            <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>
                              同涨同跌方向预测度
                            </div>
                          </div>

                          <div>
                            <Text type="secondary" style={{ fontSize: 12 }}>
                              平均绝对偏差 (MAE)
                            </Text>
                            <div
                              style={{
                                fontSize: 20,
                                fontWeight: 700,
                                color: '#0f172a',
                                marginTop: 2,
                                fontVariantNumeric: 'tabular-nums',
                              }}
                            >
                              {performanceData.accuracy.mae != null
                                ? `±${Number(performanceData.accuracy.mae).toFixed(2)}%`
                                : '--'}
                            </div>
                            <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 8 }}>
                              收盘预估偏离官方净值均值
                            </div>
                          </div>

                          {!isMobile && (
                            <div>
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                评估结论与参考级
                              </Text>
                              <div style={{ marginTop: 4 }}>
                                {Number(performanceData.accuracy.directionMatchRate ?? 0) >= 85 ? (
                                  <Tag color="success" style={{ borderRadius: 6, fontWeight: 600 }}>高参考价值</Tag>
                                ) : Number(performanceData.accuracy.directionMatchRate ?? 0) >= 75 ? (
                                  <Tag color="processing" style={{ borderRadius: 6, fontWeight: 600 }}>良好参考级</Tag>
                                ) : (
                                  <Tag color="warning" style={{ borderRadius: 6, fontWeight: 600 }}>观察磨合级</Tag>
                                )}
                              </div>
                              <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 8 }}>
                                具备较强盘中调仓参考性
                              </div>
                            </div>
                          )}
                        </div>

                        {/* 支付宝风格底部提示 */}
                        <div
                          style={{
                            marginTop: 10,
                            padding: '8px 10px',
                            backgroundColor: '#eff6ff',
                            borderRadius: 6,
                            fontSize: 12,
                            color: '#1e40af',
                            display: 'flex',
                            alignItems: 'center',
                            gap: 6,
                          }}
                        >
                          <InfoCircleOutlined style={{ color: '#3b82f6' }} />
                          <span>
                            图中<b>橙色虚线</b>为各交易日收盘预估对照走势，偏离越小表明模型对持仓还原度越高，可用于盘中波段与定投决策。
                          </span>
                        </div>
                      </>
                    ) : (
                      <div
                        style={{
                          padding: '12px 10px',
                          backgroundColor: '#f8fafc',
                          borderRadius: 8,
                          fontSize: 12,
                          color: '#64748b',
                          display: 'flex',
                          alignItems: 'center',
                          gap: 8,
                        }}
                      >
                        <InfoCircleOutlined style={{ color: '#94a3b8', fontSize: 14 }} />
                        <span>
                          该区间内暂无本地历史盘中预估打点记录（新添加自选或系统冷启动基金）。系统将在每个交易日收盘后自动积累对齐数据，并在此绘制橙色预估虚线。
                        </span>
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
