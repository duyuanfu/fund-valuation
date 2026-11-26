import { useState, useEffect, useRef, useCallback } from 'react'
import {
  Modal,
  Tabs,
  Typography,
  Tag,
  Space,
  Button,
  Spin,
  Empty,
  Progress,
  Card,
  Table,
  Tooltip,
  message,
} from 'antd'
import {
  SafetyCertificateOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  BulbOutlined,
  PieChartOutlined,
  BarChartOutlined,
  ApartmentOutlined,
} from '@ant-design/icons'
import { api } from '../api'
import type {
  PortfolioDiagnosisView,
  StockExposureItem,
  DiagnosisRiskTag,
} from '../api/types'
import { useECharts } from '../hooks/useECharts'

const { Title, Text, Paragraph } = Typography

interface PortfolioDiagnosisModalProps {
  open: boolean
  onClose: () => void
}

function formatAmount(val: number | null | undefined): string {
  if (val === null || val === undefined) return '--'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatPct(val: number | null | undefined): string {
  if (val === null || val === undefined) return '--'
  return `${val.toFixed(2)}%`
}

export function PortfolioDiagnosisModal({ open, onClose }: PortfolioDiagnosisModalProps) {
  const [loading, setLoading] = useState(false)
  const [refreshingAi, setRefreshingAi] = useState(false)
  const [data, setData] = useState<PortfolioDiagnosisView | null>(null)
  const [activeTab, setActiveTab] = useState<'diagnosis' | 'stocks' | 'allocation'>('diagnosis')
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768)
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  const stockChartRef = useRef<HTMLDivElement | null>(null)
  const allocChartRef = useRef<HTMLDivElement | null>(null)
  const { renderChart: renderStockChart } = useECharts()
  const { renderChart: renderAllocChart } = useECharts()

  const loadData = useCallback(async (forceRefresh = false) => {
    if (forceRefresh) {
      setRefreshingAi(true)
    } else {
      setLoading(true)
    }
    try {
      const res = await api.getPortfolioDiagnosis(forceRefresh)
      setData(res)
      if (forceRefresh) {
        message.success('已根据最新持仓刷新AI诊断')
      }
    } catch (e: any) {
      message.error(e?.message || '获取持仓诊断失败')
    } finally {
      setLoading(false)
      setRefreshingAi(false)
    }
  }, [])

  useEffect(() => {
    if (open) {
      loadData(false)
    } else {
      setData(null)
      setActiveTab('diagnosis')
    }
  }, [open, loadData])

  // 渲染 Top 10 重仓股柱状图
  useEffect(() => {
    if (!open || !data || activeTab !== 'stocks' || !stockChartRef.current) return
    const topStocks = data.lookThrough?.topStockExposures || []
    if (topStocks.length === 0) return

    const reversed = [...topStocks].reverse()
    const names = reversed.map((s) => s.stockName || s.stockCode)
    const ratios = reversed.map((s) => Number(s.portfolioRatio.toFixed(2)))

    const timer = setTimeout(() => {
      renderStockChart(stockChartRef.current, {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: (params: any) => {
            if (!params || !params[0]) return ''
            const item = reversed[params[0].dataIndex]
            const lines = [
              `<strong>${item.stockName} (${item.stockCode})</strong>`,
              `组合穿透占比: <strong>${formatPct(item.portfolioRatio)}</strong>`,
              `穿透总额: <strong>${formatAmount(item.holdingAmount)}</strong>`,
            ]
            if (item.contributors && item.contributors.length > 0) {
              lines.push('<br/>贡献基金:')
              item.contributors.forEach((c) => {
                lines.push(`· ${c.fundName} (${c.weightInFund.toFixed(1)}%)`)
              })
            }
            return lines.join('<br/>')
          },
        },
        grid: {
          left: '2%',
          right: isMobile ? '12%' : '8%',
          bottom: '2%',
          top: '2%',
          containLabel: true,
        },
        xAxis: {
          type: 'value',
          axisLabel: { formatter: '{value}%', fontSize: 11 },
          splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } },
        },
        yAxis: {
          type: 'category',
          data: names,
          axisLabel: {
            fontSize: 11,
            formatter: (val: string) => (val.length > 5 ? `${val.substring(0, 4)}..` : val),
          },
        },
        series: [
          {
            name: '组合穿透占比',
            type: 'bar',
            data: ratios,
            itemStyle: {
              color: '#2563eb',
              borderRadius: [0, 4, 4, 0],
            },
            label: {
              show: true,
              position: 'right',
              formatter: '{c}%',
              fontSize: 11,
              color: '#64748b',
            },
          },
        ],
      })
    }, 50)

    return () => clearTimeout(timer)
  }, [open, data, activeTab, isMobile, renderStockChart])

  // 渲染资产配置环形图
  useEffect(() => {
    if (!open || !data || activeTab !== 'allocation' || !allocChartRef.current) return
    const allocations = data.lookThrough?.assetAllocations || []
    if (allocations.length === 0) return

    const chartData = allocations.map((a) => ({
      name: a.assetTypeLabel,
      value: a.holdingAmount,
      ratio: a.ratio,
    }))

    const timer = setTimeout(() => {
      renderAllocChart(allocChartRef.current, {
        tooltip: {
          trigger: 'item',
          formatter: (p: any) => {
            const d = p.data
            return `<strong>${d.name}</strong><br/>持有: ${formatAmount(d.value)}<br/>占比: ${formatPct(d.ratio)}`
          },
        },
        legend: {
          orient: 'horizontal',
          bottom: '0%',
          textStyle: { fontSize: 11 },
        },
        series: [
          {
            name: '资产配置',
            type: 'pie',
            radius: ['45%', '72%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderRadius: 5,
              borderColor: '#fff',
              borderWidth: 2,
            },
            label: {
              show: !isMobile,
              formatter: '{b}: {d}%',
              fontSize: 11,
            },
            data: chartData,
          },
        ],
      })
    }, 50)

    return () => clearTimeout(timer)
  }, [open, data, activeTab, isMobile, renderAllocChart])

  // 得分颜色与文案判定
  const score = data?.healthScore ?? 0
  const scoreColor =
    score >= 85 ? '#16a34a' : score >= 75 ? '#2563eb' : score >= 60 ? '#d97706' : '#dc2626'

  const scoreLevelLabel = data?.scoreLevel || (score >= 85 ? '极佳' : score >= 75 ? '良好' : score >= 60 ? '一般' : '较脆弱')

  const getRiskTagColor = (type: DiagnosisRiskTag['type']) => {
    switch (type) {
      case 'DANGER':
        return 'error'
      case 'WARNING':
        return 'warning'
      case 'SUCCESS':
        return 'success'
      default:
        return 'processing'
    }
  }

  // 重仓股表格列配置
  const stockColumns = [
    {
      title: '排名',
      key: 'rank',
      width: 48,
      render: (_: any, __: any, index: number) => (
        <span
          style={{
            display: 'inline-block',
            width: 18,
            height: 18,
            lineHeight: '18px',
            textAlign: 'center',
            borderRadius: 9,
            fontSize: 11,
            fontWeight: 600,
            backgroundColor: index < 3 ? '#ea580c' : '#f1f5f9',
            color: index < 3 ? '#fff' : '#64748b',
          }}
        >
          {index + 1}
        </span>
      ),
    },
    {
      title: '股票',
      key: 'stock',
      render: (_: any, r: StockExposureItem) => (
        <div>
          <Text strong style={{ fontSize: 13, color: '#1e293b' }}>{r.stockName}</Text>
          <Text type="secondary" style={{ fontSize: 11, display: 'block', lineHeight: 1.2 }}>{r.stockCode}</Text>
        </div>
      ),
    },
    {
      title: '穿透金额',
      dataIndex: 'holdingAmount',
      key: 'holdingAmount',
      align: 'right' as const,
      render: (v: number) => <Text style={{ fontFamily: 'monospace', fontSize: 12 }}>{formatAmount(v)}</Text>,
    },
    {
      title: '穿透占比',
      dataIndex: 'portfolioRatio',
      key: 'portfolioRatio',
      align: 'right' as const,
      render: (v: number) => (
        <Text strong style={{ fontSize: 12, color: v > 15 ? '#dc2626' : v > 10 ? '#d97706' : '#2563eb' }}>
          {formatPct(v)}
        </Text>
      ),
    },
    {
      title: '贡献基金',
      key: 'contributors',
      responsive: ['md'] as any,
      render: (_: any, r: StockExposureItem) => (
        <Space size={[4, 4]} wrap>
          {r.contributors?.map((c) => (
            <Tooltip
              key={c.fundCode}
              title={`持仓: ${formatAmount(c.fundHoldingAmount)} | 仓位占比: ${formatPct(c.weightInFund)}`}
            >
              <Tag style={{ margin: 0, fontSize: 11, borderRadius: 4, background: '#f8fafc', border: '1px solid #e2e8f0' }}>
                {c.fundName} {c.weightInFund.toFixed(1)}%
              </Tag>
            </Tooltip>
          ))}
        </Space>
      ),
    },
  ]

  const items = [
    {
      key: 'diagnosis',
      label: (
        <span>
          <SafetyCertificateOutlined /> {isMobile ? '健康诊断' : '智能健康诊断'}
        </span>
      ),
      children: (
        <div>
          {/* 健康评分与核心维度卡片 */}
          <div
            style={{
              background: '#f8fafc',
              borderRadius: 12,
              padding: isMobile ? '12px 14px' : '16px 18px',
              marginBottom: 14,
              border: '1px solid #e2e8f0',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
              {/* 大分数与状态 */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div
                  style={{
                    width: isMobile ? 58 : 68,
                    height: isMobile ? 58 : 68,
                    borderRadius: '50%',
                    border: `3px solid ${scoreColor}`,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    backgroundColor: '#fff',
                    boxShadow: '0 2px 6px rgba(0,0,0,0.04)',
                    flexShrink: 0,
                  }}
                >
                  <span style={{ fontSize: isMobile ? 22 : 26, fontWeight: 700, color: scoreColor, lineHeight: 1 }}>
                    {score}
                  </span>
                  <span style={{ fontSize: 10, color: '#64748b', marginTop: 2 }}>分</span>
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Title level={5} style={{ margin: 0, fontSize: isMobile ? 15 : 17, color: '#0f172a' }}>
                      持仓健康度
                    </Title>
                    <Tag
                      color={scoreColor}
                      style={{
                        margin: 0,
                        fontWeight: 600,
                        fontSize: 11,
                        borderRadius: 10,
                        padding: '1px 8px',
                        lineHeight: '16px',
                      }}
                    >
                      {scoreLevelLabel}
                    </Tag>
                  </div>
                  <Text type="secondary" style={{ fontSize: 11, color: '#64748b' }}>
                    多因子风险量化评级
                  </Text>
                </div>
              </div>

              {/* 重新生成按钮 */}
              <Button
                size={isMobile ? 'small' : 'middle'}
                type="primary"
                ghost
                icon={<ReloadOutlined spin={refreshingAi} />}
                loading={refreshingAi}
                onClick={() => loadData(true)}
                style={{ borderRadius: 6, fontWeight: 500 }}
              >
                {isMobile ? '重新诊断' : '重新生成诊断'}
              </Button>
            </div>

            {/* 得分细项拆解 */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(3, 1fr)',
                gap: isMobile ? 8 : 16,
                marginTop: 14,
                paddingTop: 12,
                borderTop: '1px solid #e2e8f0',
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                  <Text type="secondary" style={{ fontSize: 11 }}>集中度</Text>
                  <Text strong style={{ fontSize: 11 }}>{data?.concentrationScore ?? 0}/40</Text>
                </div>
                <Progress
                  percent={((data?.concentrationScore ?? 0) / 40) * 100}
                  showInfo={false}
                  strokeColor="#2563eb"
                  size="small"
                />
              </div>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                  <Text type="secondary" style={{ fontSize: 11 }}>资产平衡</Text>
                  <Text strong style={{ fontSize: 11 }}>{data?.assetBalanceScore ?? 0}/30</Text>
                </div>
                <Progress
                  percent={((data?.assetBalanceScore ?? 0) / 30) * 100}
                  showInfo={false}
                  strokeColor="#16a34a"
                  size="small"
                />
              </div>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                  <Text type="secondary" style={{ fontSize: 11 }}>分散去重</Text>
                  <Text strong style={{ fontSize: 11 }}>{data?.stockOverlapScore ?? 0}/30</Text>
                </div>
                <Progress
                  percent={((data?.stockOverlapScore ?? 0) / 30) * 100}
                  showInfo={false}
                  strokeColor="#7c3aed"
                  size="small"
                />
              </div>
            </div>
          </div>

          {/* 风险特征标签 */}
          {data?.riskTags && data.riskTags.length > 0 && (
            <div
              style={{
                background: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: 10,
                padding: '10px 12px',
                marginBottom: 14,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
                <ThunderboltOutlined style={{ color: '#d97706', fontSize: 13 }} />
                <Text strong style={{ fontSize: 12, color: '#334155' }}>风险特征提示</Text>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {data.riskTags.map((tag, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      padding: '4px 8px',
                      background: '#f8fafc',
                      borderRadius: 6,
                    }}
                  >
                    <Tag
                      color={getRiskTagColor(tag.type)}
                      style={{ margin: 0, fontWeight: 550, fontSize: 11, borderRadius: 4, flexShrink: 0 }}
                    >
                      {tag.title}
                    </Tag>
                    <Text style={{ fontSize: 12, color: '#475569', flex: 1 }}>{tag.description}</Text>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* AI 诊断报告 */}
          <div
            style={{
              background: '#ffffff',
              border: '1px solid #e2e8f0',
              borderRadius: 10,
              padding: isMobile ? '12px 12px' : '16px 18px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <span style={{ fontSize: 13, fontWeight: 650, color: '#0f172a', display: 'flex', alignItems: 'center', gap: 6 }}>
                <BulbOutlined style={{ color: '#2563eb' }} />
                AI 智能诊断建议
              </span>
              {data?.aiReport?.generatedAt && (
                <Text type="secondary" style={{ fontSize: 11 }}>
                  {data.aiReport.generatedAt.slice(0, 16)}
                </Text>
              )}
            </div>

            {data?.aiReport ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {/* 概述 */}
                <div style={{ background: '#f8fafc', padding: '10px 12px', borderRadius: 8, borderLeft: '3px solid #2563eb' }}>
                  <Paragraph style={{ margin: 0, fontSize: 13, lineHeight: 1.6, color: '#334155' }}>
                    {data.aiReport.summary}
                  </Paragraph>
                </div>

                {/* 组合优势 */}
                {data.aiReport.strengths && data.aiReport.strengths.length > 0 && (
                  <div>
                    <Text strong style={{ color: '#16a34a', display: 'flex', alignItems: 'center', gap: 4, fontSize: 12, marginBottom: 4 }}>
                      <CheckCircleOutlined /> 组合优势
                    </Text>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 3, paddingLeft: 16 }}>
                      {data.aiReport.strengths.map((s, idx) => (
                        <Text key={idx} style={{ fontSize: 12, color: '#475569' }}>• {s}</Text>
                      ))}
                    </div>
                  </div>
                )}

                {/* 风险暴露 */}
                {data.aiReport.weaknesses && data.aiReport.weaknesses.length > 0 && (
                  <div>
                    <Text strong style={{ color: '#dc2626', display: 'flex', alignItems: 'center', gap: 4, fontSize: 12, marginBottom: 4 }}>
                      <WarningOutlined /> 风险暴露
                    </Text>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 3, paddingLeft: 16 }}>
                      {data.aiReport.weaknesses.map((w, idx) => (
                        <Text key={idx} style={{ fontSize: 12, color: '#475569' }}>• {w}</Text>
                      ))}
                    </div>
                  </div>
                )}

                {/* 调仓建议 */}
                {data.aiReport.suggestions && data.aiReport.suggestions.length > 0 && (
                  <div>
                    <Text strong style={{ color: '#2563eb', display: 'flex', alignItems: 'center', gap: 4, fontSize: 12, marginBottom: 4 }}>
                      <BulbOutlined /> 调仓建议
                    </Text>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 3, paddingLeft: 16 }}>
                      {data.aiReport.suggestions.map((sg, idx) => (
                        <Text key={idx} style={{ fontSize: 12, color: '#475569' }}>• {sg}</Text>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无AI诊断报告，可点击上方按钮重新生成"
              />
            )}
          </div>
        </div>
      ),
    },
    {
      key: 'stocks',
      label: (
        <span>
          <BarChartOutlined /> {isMobile ? '持仓穿透' : '穿透重仓股票 (Top 10)'}
        </span>
      ),
      children: (
        <div>
          {/* 精简信息条 */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: 10,
              padding: '6px 10px',
              background: '#f8fafc',
              borderRadius: 6,
              fontSize: 12,
            }}
          >
            <span style={{ color: '#64748b' }}>
              前十大重仓股累计占比: <strong style={{ color: '#2563eb' }}>{formatPct(data?.lookThrough?.top10StockRatioSum)}</strong>
            </span>
            <Text type="secondary" style={{ fontSize: 11 }}>
              {data?.lookThrough?.latestReportQuarter || '最新季报'}
            </Text>
          </div>

          {/* 柱状图展示 */}
          <div style={{ marginBottom: 14 }}>
            <div ref={stockChartRef} style={{ width: '100%', height: isMobile ? 220 : 260 }} />
          </div>

          {/* 明细表格 */}
          <Table
            rowKey="stockCode"
            dataSource={data?.lookThrough?.topStockExposures || []}
            columns={stockColumns}
            pagination={false}
            size="small"
            scroll={{ x: isMobile ? 320 : 500 }}
          />
        </div>
      ),
    },
    {
      key: 'allocation',
      label: (
        <span>
          <PieChartOutlined /> {isMobile ? '资产配置' : '资产配置与交叉重叠'}
        </span>
      ),
      children: (
        <div>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: isMobile ? '1fr' : '1fr 1fr',
              gap: 12,
              marginBottom: 14,
            }}
          >
            {/* 资产类别分布图 */}
            <Card size="small" title={<span style={{ fontSize: 13 }}>资产大类分布</span>} style={{ borderRadius: 8 }}>
              <div ref={allocChartRef} style={{ width: '100%', height: isMobile ? 200 : 230 }} />
            </Card>

            {/* 资产配置明细 */}
            <Card size="small" title={<span style={{ fontSize: 13 }}>资产占比清单</span>} style={{ borderRadius: 8 }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {data?.lookThrough?.assetAllocations?.map((item) => (
                  <div key={item.assetType} style={{ padding: '6px 10px', background: '#f8fafc', borderRadius: 6 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                      <Text strong style={{ fontSize: 12 }}>{item.assetTypeLabel}</Text>
                      <Text strong style={{ color: '#2563eb', fontSize: 12 }}>{formatPct(item.ratio)}</Text>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Text type="secondary" style={{ fontSize: 11 }}>持仓</Text>
                      <Text type="secondary" style={{ fontSize: 11, fontFamily: 'monospace' }}>
                        {formatAmount(item.holdingAmount)}
                      </Text>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          </div>

          {/* 基金重叠度分析 */}
          <Card
            size="small"
            title={
              <span style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}>
                <ApartmentOutlined style={{ color: '#7c3aed' }} />
                基金重仓重合度分析
              </span>
            }
            style={{ borderRadius: 8 }}
          >
            {data?.lookThrough?.majorOverlaps && data.lookThrough.majorOverlaps.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {data.lookThrough.majorOverlaps.map((ov, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: '8px 10px',
                      background: '#fffbeb',
                      borderRadius: 6,
                      border: '1px solid #fef3c7',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                      <Text strong style={{ fontSize: 12, color: '#1e293b' }}>
                        {ov.fundNameA} 与 {ov.fundNameB}
                      </Text>
                      <Tag color="orange" style={{ margin: 0, fontSize: 11, fontWeight: 600 }}>重合 {formatPct(ov.overlapRatio)}</Tag>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 4, flexWrap: 'wrap' }}>
                      <Text type="secondary" style={{ fontSize: 11 }}>共同持股:</Text>
                      {ov.commonStockNames?.map((stk) => (
                        <Tag key={stk} style={{ margin: 0, fontSize: 10, padding: '0 4px', background: '#eff6ff', border: '1px solid #bfdbfe' }}>
                          {stk}
                        </Tag>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '16px 0' }}>
                <CheckCircleOutlined style={{ fontSize: 26, color: '#16a34a', marginBottom: 6 }} />
                <div>
                  <Text strong style={{ fontSize: 13 }}>基金重叠度良好</Text>
                </div>
                <Text type="secondary" style={{ fontSize: 11 }}>
                  基金间重合度均低于 15%，持仓分散健康。
                </Text>
              </div>
            )}
          </Card>
        </div>
      ),
    },
  ]

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      width={isMobile ? '95%' : 780}
      style={isMobile ? { top: 12, margin: '0 auto', maxWidth: '96vw', paddingBottom: 12 } : { top: 32 }}
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <SafetyCertificateOutlined style={{ color: '#2563eb', fontSize: 18 }} />
          <span style={{ fontWeight: 650, fontSize: isMobile ? 15 : 17 }}>持仓透视与智能诊断</span>
          <Tag color="gold" style={{ marginLeft: 2, fontSize: 10, padding: '0 6px', borderRadius: 8 }}>VIP</Tag>
        </div>
      }
      styles={{
        body: { maxHeight: isMobile ? '82vh' : '75vh', overflowY: 'auto', padding: isMobile ? '4px 0' : '8px 12px' },
      }}
    >
      <Spin spinning={loading} tip="正在计算持仓穿透与健康评分...">
        {data ? (
          <Tabs
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as any)}
            items={items}
            size={isMobile ? 'small' : 'middle'}
          />
        ) : (
          !loading && <Empty description="暂无持仓数据" />
        )}
      </Spin>
    </Modal>
  )
}
