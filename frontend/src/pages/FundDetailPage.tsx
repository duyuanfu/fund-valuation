import { useEffect, useRef, useState } from 'react'
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
  message,
} from 'antd'
import { ArrowLeftOutlined, FallOutlined, RiseOutlined, InfoCircleOutlined } from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import type { ColumnsType } from 'antd/es/table'
import { api } from '../api'
import type { EstimateResult, FundDetailView, HoldingView } from '../api/types'
import { FUND_TYPE_COLOR, FUND_TYPE_LABEL } from '../api/types'
import { useECharts } from '../hooks/useECharts'
import { colorOf, fmtNav, fmtPct } from '../utils/format'

const { Title, Text } = Typography

export default function FundDetailPage() {
  const { code } = useParams<{ code: string }>()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<FundDetailView | null>(null)
  const [loading, setLoading] = useState(true)
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768)
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  const chartRef = useRef<HTMLDivElement | null>(null)
  const { renderChart } = useECharts()

  useEffect(() => {
    if (!code) return
    let cancelled = false
    setLoading(true)
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

  // 当 loading 结束且 DOM 挂载成功后，确保可靠渲染
  useEffect(() => {
    if (loading || !detail || isBond || !chartRef.current) return
    const history = detail.history ?? []
    if (!history.length) return

    const times = history.map((h) =>
      new Date(h.estTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
    )
    const navs = history.map((h) => h.estNav)
    const isUp = (estimate?.estimatePct ?? 0) >= 0
    const lineColor = isUp ? '#cf1322' : '#389e0d'
    const areaColor = isUp ? 'rgba(207, 19, 34, 0.08)' : 'rgba(56, 158, 13, 0.08)'

    renderChart(chartRef.current, {
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
  }, [loading, detail, isBond, estimate, renderChart])

  const holdingColumns: ColumnsType<HoldingView> = [
    {
      title: '股票代码 / 名称',
      dataIndex: 'stockCode',
      render: (c: string, r) => (
        <div>
          <Text strong style={{ color: '#0f172a' }}>{r.stockName}</Text>
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
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
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
                <Text type="secondary" style={{ fontSize: 12 }}>昨日净值</Text>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>
                  {fmtNav(estimate?.prevNav)}
                </div>
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>净值日期</Text>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>
                  {estimate?.navDate ?? '--'}
                </div>
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>持仓报告期</Text>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>
                  {estimate?.reportQt ?? '--'}
                </div>
              </div>
              <div>
                <Text type="secondary" style={{ fontSize: 12 }}>行情时间</Text>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#334155' }}>
                  {estimate?.quoteTs
                    ? new Date(estimate.quoteTs).toLocaleTimeString('zh-CN')
                    : '--'}
                </div>
              </div>
            </div>
          </Card>

          {/* 纯债基金：利率驱动说明卡片 */}
          {isBond && detail.bondInfo ? (
            <Card
              title={
                <span style={{ fontSize: 16, fontWeight: 600, color: '#0f172a' }}>
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
          ) : (
            /* 偏股/指数基金：日内实时走势图 */
            !isBond && (
              <Card
                title={
                  <span style={{ fontSize: 16, fontWeight: 600, color: '#0f172a' }}>
                    日内实时估值走势
                  </span>
                }
                style={{
                  marginBottom: 14,
                  borderRadius: 14,
                  border: '1px solid #e2e8f0',
                }}
                styles={{ body: { padding: isMobile ? '10px 6px' : '16px' } }}
              >
                <div ref={chartRef} style={{ height: isMobile ? 260 : 320, width: '100%' }} />
                {(detail.history?.length ?? 0) === 0 && (
                  <div style={{ textAlign: 'center', padding: '24px 0' }}>
                    <Text type="secondary">盘中估值打点生成后将在此绘制实时脉搏</Text>
                  </div>
                )}
              </Card>
            )
          )}

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

          <Text type="secondary" style={{ display: 'block', marginTop: 16, textAlign: 'center', fontSize: 12 }}>
            {estimate?.disclaimer ?? '估值仅供参考，不构成投资建议'}
          </Text>
        </>
      )}
    </div>
  )
}
