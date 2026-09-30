// 基金持仓与今日收益预估页面 (VIP专享)
import { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Typography,
  Button,
  Table,
  Tag,
  Space,
  Popconfirm,
  Segmented,
  Tooltip,
  Empty,
  Spin,
  message,
} from 'antd'
import {
  CrownOutlined,
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  AppstoreOutlined,
  BarsOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  QuestionCircleOutlined,
  CustomerServiceOutlined,
  LockOutlined,
  CheckCircleFilled,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { PortfolioView, PositionItemView, VipConfigView } from '../api/types'
import { FUND_TYPE_LABEL, FUND_TYPE_COLOR } from '../api/types'
import { useAuth } from '../hooks/useAuth'
import { PositionEditModal } from '../components/PositionEditModal'
import { VipRechargeModal } from '../components/VipRechargeModal'
import { ContactModal } from '../components/ContactModal'
import { fmtPct, fmtNav } from '../utils/format'

const { Title, Text, Paragraph } = Typography

function fmtMoney(val: number | null | undefined, showPlus = false): string {
  if (val === null || val === undefined) return '--'
  const prefix = showPlus && val > 0 ? '+' : ''
  return `${prefix}¥${val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export default function PortfolioPage() {
  const { isVip, isAdmin } = useAuth()
  const hasAccess = isVip || isAdmin
  const navigate = useNavigate()

  const [data, setData] = useState<PortfolioView | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [lastUpdated, setLastUpdated] = useState<string>('')
  const [vipConfig, setVipConfig] = useState<VipConfigView | null>(null)

  // 弹窗状态
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<PositionItemView | null>(null)
  const [rechargeModalOpen, setRechargeModalOpen] = useState(false)
  const [contactModalOpen, setContactModalOpen] = useState(false)

  // 响应式与视图切换
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)
  const [viewMode, setViewMode] = useState<'cards' | 'table'>(() => (window.innerWidth < 768 ? 'cards' : 'table'))
  const [sortKey, setSortKey] = useState<'amount' | 'todayIncome' | 'pct'>('amount')

  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth < 768
      setIsMobile(mobile)
      if (mobile) setViewMode('cards')
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  // 加载持仓数据
  const loadPortfolio = useCallback(async (isManual = false) => {
    if (!hasAccess) {
      setLoading(false)
      return
    }
    if (isManual) setRefreshing(true)
    try {
      const res = await api.getPortfolio()
      setData(res)
      setLastUpdated(new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }))
      if (isManual) message.success('持仓估值已刷新')
    } catch (e: any) {
      if (e?.error) {
        message.error(e.error)
      }
    } finally {
      setLoading(false)
      if (isManual) setRefreshing(false)
    }
  }, [hasAccess])

  useEffect(() => {
    loadPortfolio()
  }, [loadPortfolio])

  // 非 VIP 时加载后台配置的 VIP 价格
  useEffect(() => {
    if (!hasAccess) {
      api.getVipConfig()
        .then((cfg) => {
          if (cfg) setVipConfig(cfg)
        })
        .catch(() => {})
    }
  }, [hasAccess])

  // 盘中定时轮询刷新 (60s)
  useEffect(() => {
    if (!hasAccess) return
    const timer = setInterval(() => {
      loadPortfolio()
    }, 60000)
    return () => clearInterval(timer)
  }, [hasAccess, loadPortfolio])

  // 删除单只持仓
  const handleDelete = async (fundCode: string) => {
    try {
      await api.removePosition(fundCode)
      message.success('已成功移出持仓')
      loadPortfolio()
    } catch (e: any) {
      message.error(e.message || '删除失败')
    }
  }

  // 排序处理
  const sortedItems = useMemo(() => {
    if (!data?.items) return []
    const list = [...data.items]
    if (sortKey === 'amount') {
      list.sort((a, b) => b.holdingAmount - a.holdingAmount)
    } else if (sortKey === 'todayIncome') {
      list.sort((a, b) => b.todayIncome - a.todayIncome)
    } else if (sortKey === 'pct') {
      list.sort((a, b) => (b.estimatePct ?? 0) - (a.estimatePct ?? 0))
    }
    return list
  }, [data?.items, sortKey])

  // -------------------------------------------------------------
  // 1. 非 VIP 会员展示：与主页面同风格的简洁轻奢预览看板 + 引导开通
  // -------------------------------------------------------------
  if (!hasAccess) {
    return (
      <div style={{ maxWidth: 1040, margin: '0 auto', padding: isMobile ? '14px 12px 64px' : '20px 20px 64px' }}>
        {/* 与自选页一致的纯净顶栏卡片 */}
        <div
          style={{
            background: 'linear-gradient(135deg, #ffffff 0%, #fffdf7 100%)',
            borderRadius: 16,
            border: '1px solid #fde68a',
            padding: isMobile ? '18px 16px' : '28px 24px',
            boxShadow: '0 4px 16px rgba(217, 119, 6, 0.06)',
            marginBottom: 16,
            position: 'relative',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
            <Tag
              color="gold"
              style={{
                borderRadius: 12,
                padding: '2px 8px',
                fontWeight: 700,
                fontSize: 12,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
                margin: 0,
              }}
            >
              <CrownOutlined /> VIP 会员尊享特权
            </Tag>
            <span style={{ fontSize: 12, color: '#92400e', fontWeight: 500 }}>
              实时洞察每一分钱的日内盈亏
            </span>
          </div>

          <Title level={3} style={{ margin: '0 0 8px 0', fontSize: isMobile ? 19 : 22, color: '#0f172a', fontWeight: 700 }}>
            基金持仓估值 · 今日收益实时测算
          </Title>

          <Paragraph style={{ fontSize: 13, color: '#64748b', lineHeight: 1.6, margin: '0 0 16px 0', maxWidth: 640 }}>
            全面对标<b>支付宝基金持有卡片</b> 4 大核心字段（金额、昨日收益、持有收益、收益率）。
            结合股票持仓透视与债券利率驱动模型，盘中毫秒级预估今日到手收益与最新资产现值。
          </Paragraph>

          <div style={{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(3, 1fr)', gap: 8, marginBottom: 18 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#334155' }}>
              <CheckCircleFilled style={{ color: '#16a34a' }} />
              <span>对标支付宝4字段智能联动录入</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#334155' }}>
              <CheckCircleFilled style={{ color: '#16a34a' }} />
              <span>日内盘中高频实时预估收益跳动</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#334155' }}>
              <CheckCircleFilled style={{ color: '#16a34a' }} />
              <span>预留支付宝截图OCR一键扫描导入</span>
            </div>
          </div>

          <Space size={10}>
            <Button
              type="primary"
              size={isMobile ? 'middle' : 'large'}
              icon={<CrownOutlined />}
              onClick={() => setRechargeModalOpen(true)}
              style={{
                borderRadius: 8,
                background: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)',
                borderColor: '#b45309',
                fontWeight: 650,
                boxShadow: '0 2px 8px rgba(217, 119, 6, 0.25)',
              }}
            >
              立即开通 VIP 会员 (¥{vipConfig?.monthlyPrice != null ? Number(vipConfig.monthlyPrice) : 2.9}起)
            </Button>
            <Button
              size={isMobile ? 'middle' : 'large'}
              icon={<CustomerServiceOutlined style={{ color: '#2563eb' }} />}
              onClick={() => setContactModalOpen(true)}
              style={{ borderRadius: 8, color: '#475569' }}
            >
              联系客服
            </Button>
          </Space>
        </div>

        {/* 虚化模拟持仓预览 (展示真实界面体验) */}
        <div style={{ position: 'relative' }}>
          <div
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              background: 'rgba(248, 250, 252, 0.55)',
              backdropFilter: 'blur(2.5px)',
              WebkitBackdropFilter: 'blur(2.5px)',
              zIndex: 10,
              borderRadius: 14,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 20,
            }}
          >
            <div
              style={{
                background: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: 14,
                padding: '20px 24px',
                textAlign: 'center',
                boxShadow: '0 8px 24px rgba(15, 23, 42, 0.08)',
                maxWidth: 380,
              }}
            >
              <LockOutlined style={{ fontSize: 28, color: '#d97706', marginBottom: 10 }} />
              <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a', marginBottom: 4 }}>
                解锁持仓列表与今日收益估算
              </div>
              <div style={{ fontSize: 12, color: '#64748b', lineHeight: 1.5, marginBottom: 14 }}>
                开通 VIP 后即可录入多只持仓基金，盘中随时掌握收益动态。
              </div>
              <Button
                type="primary"
                block
                icon={<CrownOutlined />}
                onClick={() => setRechargeModalOpen(true)}
                style={{
                  borderRadius: 8,
                  fontWeight: 650,
                  background: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)',
                  borderColor: '#b45309',
                }}
              >
                立即扫码解锁 VIP
              </Button>
            </div>
          </div>

          {/* 虚化模拟数据看板 */}
          <div style={{ filter: 'blur(3px)', pointerEvents: 'none', userSelect: 'none' }}>
            <div style={{ display: 'grid', gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)', gap: 10, marginBottom: 12 }}>
              <div style={{ background: '#ffffff', padding: '14px', borderRadius: 12, border: '1px solid #e2e8f0' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>今日预估总收益</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#dc2626', marginTop: 4 }}>+¥368.50</div>
                <div style={{ fontSize: 11, color: '#dc2626', marginTop: 2 }}>日内涨跌: +0.64%</div>
              </div>
              <div style={{ background: '#ffffff', padding: '14px', borderRadius: 12, border: '1px solid #e2e8f0' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>持仓总市值 (动态)</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', marginTop: 4 }}>¥58,260.00</div>
                <div style={{ fontSize: 11, color: '#64748b', marginTop: 2 }}>基准: ¥57,891.50</div>
              </div>
              <div style={{ background: '#ffffff', padding: '14px', borderRadius: 12, border: '1px solid #e2e8f0' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>最新累计总盈亏</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#dc2626', marginTop: 4 }}>+¥3,420.00</div>
                <div style={{ fontSize: 11, color: '#dc2626', marginTop: 2 }}>总回报率: +6.23%</div>
              </div>
              <div style={{ background: '#ffffff', padding: '14px', borderRadius: 12, border: '1px solid #e2e8f0' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>昨日官方收益</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', marginTop: 4 }}>+¥120.00</div>
                <div style={{ fontSize: 11, color: '#64748b', marginTop: 2 }}>官方确认到账</div>
              </div>
            </div>

            <div style={{ background: '#ffffff', borderRadius: 12, border: '1px solid #e2e8f0', padding: 14 }}>
              <div style={{ padding: '10px 0', borderBottom: '1px solid #f1f5f9', display: 'flex', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ fontWeight: 650 }}>易方达消费行业股票 (110022)</div>
                  <div style={{ fontSize: 12, color: '#64748b' }}>持有金额: ¥20,000.00 · 昨日收益: +¥50.00</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 17, fontWeight: 700, color: '#dc2626' }}>+¥260.00</div>
                  <span className="badge-up" style={{ fontSize: 12 }}>+1.30%</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 弹窗 */}
        <VipRechargeModal
          open={rechargeModalOpen}
          onClose={() => setRechargeModalOpen(false)}
          onContactCustomerService={() => setContactModalOpen(true)}
        />
        <ContactModal open={contactModalOpen} onClose={() => setContactModalOpen(false)} />
      </div>
    )
  }

  // -------------------------------------------------------------
  // 2. VIP 会员尊享持仓看板与收益预估列表 (简洁大气 · 原生质感)
  // -------------------------------------------------------------
  const summary = data?.summary
  const todayIncome = summary?.totalTodayIncome ?? 0
  const isUp = todayIncome > 0
  const isDown = todayIncome < 0
  const todayColor = isUp ? '#dc2626' : isDown ? '#16a34a' : '#475569'

  const desktopColumns: ColumnsType<PositionItemView> = [
    {
      title: '基金名称 / 代码',
      dataIndex: 'fundName',
      render: (_, record) => (
        <div style={{ cursor: 'pointer' }} onClick={() => navigate(`/fund/${record.fundCode}`)}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Text strong style={{ color: '#0f172a', fontWeight: 650, fontSize: 14 }}>
              {record.fundName}
            </Text>
            {record.fundType && (
              <Tag
                color={FUND_TYPE_COLOR[record.fundType] ?? 'default'}
                style={{ fontSize: 11, padding: '0 5px', borderRadius: 4, margin: 0, border: 'none' }}
              >
                {FUND_TYPE_LABEL[record.fundType] ?? record.fundType}
              </Tag>
            )}
          </div>
          <div style={{ marginTop: 2, display: 'flex', alignItems: 'center', gap: 6 }}>
            <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>
              {record.fundCode}
            </Text>
            {record.prevNav && (
              <Text type="secondary" style={{ fontSize: 11, color: '#94a3b8' }}>
                · 净值 {fmtNav(record.prevNav)}
              </Text>
            )}
          </div>
        </div>
      ),
    },
    {
      title: '持有金额',
      dataIndex: 'holdingAmount',
      align: 'right',
      render: (val, record) => (
        <div>
          <div
            style={{
              fontWeight: 700,
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
              fontSize: 14,
              color: '#0f172a',
            }}
          >
            {fmtMoney(val)}
          </div>
          <div style={{ fontSize: 11, color: '#94a3b8' }}>市值 {fmtMoney(record.dynamicMarketValue)}</div>
        </div>
      ),
    },
    {
      title: '昨日收益',
      dataIndex: 'yesterdayIncome',
      align: 'right',
      render: (val) => (
        <span
          style={{
            fontWeight: 600,
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
            color: val > 0 ? '#dc2626' : val < 0 ? '#16a34a' : '#64748b',
          }}
        >
          {fmtMoney(val, true)}
        </span>
      ),
    },
    {
      title: '持有收益',
      dataIndex: 'holdingProfit',
      align: 'right',
      render: (val, record) => (
        <div>
          <div
            style={{
              fontWeight: 600,
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
              color: val > 0 ? '#dc2626' : val < 0 ? '#16a34a' : '#64748b',
            }}
          >
            {fmtMoney(val, true)}
          </div>
          <div style={{ fontSize: 11, color: '#94a3b8' }}>{fmtPct(record.holdingProfitRate)}</div>
        </div>
      ),
    },
    {
      title: '估算涨跌',
      dataIndex: 'estimatePct',
      align: 'right',
      render: (val, record) => {
        const badgeClass = (val ?? 0) > 0 ? 'badge-up' : (val ?? 0) < 0 ? 'badge-down' : 'badge-flat'
        return (
          <div style={{ display: 'inline-flex', flexDirection: 'column', alignItems: 'flex-end', gap: 2 }}>
            <span className={badgeClass} style={{ fontSize: 13, minWidth: 64, textAlign: 'center' }}>
              {fmtPct(val)}
            </span>
            <span style={{ fontSize: 11, color: '#94a3b8' }}>{fmtNav(record.estimateNav)}</span>
          </div>
        )
      },
    },
    {
      title: '今日收益',
      dataIndex: 'todayIncome',
      align: 'right',
      render: (val) => {
        const color = val > 0 ? '#dc2626' : val < 0 ? '#16a34a' : '#64748b'
        return (
          <span
            style={{
              fontSize: 16,
              fontWeight: 750,
              color,
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
            }}
          >
            {fmtMoney(val, true)}
          </span>
        )
      },
    },
    {
      title: '累计盈亏',
      dataIndex: 'dynamicTotalProfit',
      align: 'right',
      render: (val, record) => (
        <div>
          <span
            style={{
              fontWeight: 650,
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
              color: val >= 0 ? '#dc2626' : '#16a34a',
            }}
          >
            {fmtMoney(val, true)}
          </span>
          <div style={{ fontSize: 11, color: val >= 0 ? '#dc2626' : '#16a34a' }}>
            {fmtPct(record.dynamicTotalProfitRate)}
          </div>
        </div>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      align: 'center',
      width: 80,
      render: (_, record) => (
        <Space size={2}>
          <Button
            size="small"
            type="text"
            icon={<EditOutlined style={{ color: '#2563eb' }} />}
            onClick={() => {
              setEditingItem(record)
              setEditModalOpen(true)
            }}
          />
          <Popconfirm
            title="确认移出持仓？"
            description="移出后不再测算收益"
            onConfirm={() => handleDelete(record.fundCode)}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button size="small" type="text" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ maxWidth: 1080, margin: '0 auto', padding: isMobile ? '12px 12px 64px 12px' : '20px 24px 64px 24px' }}>
      {/* 顶部标题与操作栏 (与自选页结构高度一致) */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: 10,
          marginBottom: 14,
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Title level={4} style={{ margin: 0, fontSize: isMobile ? 18 : 20, color: '#0f172a', fontWeight: 650 }}>
              持仓估值
            </Title>
            <Tag color="gold" style={{ margin: 0, borderRadius: 10, fontWeight: 700, fontSize: 11 }}>
              <CrownOutlined /> VIP
            </Tag>
            <span style={{ fontSize: 12, color: '#64748b' }}>
              共 {data?.items?.length ?? 0} 只持仓
            </span>
          </div>
          <Text type="secondary" style={{ fontSize: 12, marginTop: 2, display: 'block', color: '#94a3b8' }}>
            对标支付宝基金持仓4字段 · 结合实时高频估值自动测算
            {lastUpdated && ` · ${lastUpdated} 更新`}
          </Text>
        </div>

        <Space size={8}>
          <Button
            size={isMobile ? 'small' : 'middle'}
            icon={<ReloadOutlined spin={refreshing} />}
            loading={refreshing}
            onClick={() => loadPortfolio(true)}
            style={{ borderRadius: 8 }}
          >
            {isMobile ? '刷新' : '刷新估值'}
          </Button>

          <Button
            type="primary"
            size={isMobile ? 'small' : 'middle'}
            icon={<PlusOutlined />}
            onClick={() => {
              setEditingItem(null)
              setEditModalOpen(true)
            }}
            style={{
              borderRadius: 8,
              background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
              fontWeight: 550,
              boxShadow: '0 2px 6px rgba(37, 99, 235, 0.2)',
            }}
          >
            {isMobile ? '录入' : '录入持仓'}
          </Button>
        </Space>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '80px 0' }}>
          <Spin size="large" />
          <div style={{ marginTop: 12, color: '#64748b', fontSize: 13 }}>正在加载持仓与实时估算...</div>
        </div>
      ) : (
        <>
          {/* 顶部4大核心资产收益看板 (纯净白底 · 稳重大气) */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)',
              gap: 10,
              marginBottom: 14,
            }}
          >
            {/* 今日预估收益 */}
            <div
              style={{
                background: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: 12,
                padding: isMobile ? '12px 14px' : '16px 18px',
                boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>今日收益</Text>
                {isUp ? (
                  <ArrowUpOutlined style={{ color: '#dc2626', fontSize: 12 }} />
                ) : isDown ? (
                  <ArrowDownOutlined style={{ color: '#16a34a', fontSize: 12 }} />
                ) : null}
              </div>
              <div
                style={{
                  fontSize: isMobile ? 21 : 26,
                  fontWeight: 750,
                  color: todayColor,
                  fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                  lineHeight: 1.2,
                  marginTop: 4,
                  letterSpacing: -0.5,
                }}
              >
                {fmtMoney(summary?.totalTodayIncome, true)}
              </div>
              <div style={{ marginTop: 3, fontSize: 12, color: todayColor }}>
                涨跌 {fmtPct(summary?.totalTodayIncomePct)}
              </div>
            </div>

            {/* 持仓总资产 (最新市值) */}
            <div
              style={{
                background: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: 12,
                padding: isMobile ? '12px 14px' : '16px 18px',
                boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
              }}
            >
              <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>总市值</Text>
              <div
                style={{
                  fontSize: isMobile ? 21 : 26,
                  fontWeight: 700,
                  color: '#0f172a',
                  fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                  lineHeight: 1.2,
                  marginTop: 4,
                  letterSpacing: -0.5,
                }}
              >
                {fmtMoney(summary?.totalMarketValue)}
              </div>
              <div style={{ marginTop: 3, fontSize: 12, color: '#94a3b8' }}>
                持仓 {fmtMoney(summary?.totalHoldingAmount)}
              </div>
            </div>

            {/* 最新累计总盈亏 */}
            <div
              style={{
                background: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: 12,
                padding: isMobile ? '12px 14px' : '16px 18px',
                boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
              }}
            >
              <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>累计盈亏</Text>
              <div
                style={{
                  fontSize: isMobile ? 21 : 26,
                  fontWeight: 750,
                  color: (summary?.dynamicTotalProfit ?? 0) >= 0 ? '#dc2626' : '#16a34a',
                  fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                  lineHeight: 1.2,
                  marginTop: 4,
                  letterSpacing: -0.5,
                }}
              >
                {fmtMoney(summary?.dynamicTotalProfit, true)}
              </div>
              <div
                style={{
                  marginTop: 3,
                  fontSize: 12,
                  color: (summary?.dynamicTotalProfit ?? 0) >= 0 ? '#dc2626' : '#16a34a',
                }}
              >
                回报率 {fmtPct(summary?.dynamicTotalProfitRate)}
              </div>
            </div>

            {/* 支付宝昨日实际确认收益 */}
            <div
              style={{
                background: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: 12,
                padding: isMobile ? '12px 14px' : '16px 18px',
                boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>昨日收益</Text>
                <Tooltip title="支付宝昨日官方账面实际已结算收益总和">
                  <QuestionCircleOutlined style={{ fontSize: 12, color: '#cbd5e1' }} />
                </Tooltip>
              </div>
              <div
                style={{
                  fontSize: isMobile ? 21 : 26,
                  fontWeight: 700,
                  color: (summary?.totalYesterdayIncome ?? 0) >= 0 ? '#dc2626' : '#16a34a',
                  fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                  lineHeight: 1.2,
                  marginTop: 4,
                  letterSpacing: -0.5,
                }}
              >
                {fmtMoney(summary?.totalYesterdayIncome, true)}
              </div>
              <div style={{ marginTop: 3, fontSize: 12, color: '#94a3b8' }}>
                持有收益 {fmtMoney(summary?.totalHoldingProfit, true)}
              </div>
            </div>
          </div>

          {/* 列表控制与过滤栏 */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: 10,
              padding: '2px 0',
            }}
          >
            <Space size={8}>
              <span style={{ fontSize: 12, color: '#64748b' }}>排序:</span>
              <Segmented
                size="small"
                value={sortKey}
                onChange={(val) => setSortKey(val as 'amount' | 'todayIncome' | 'pct')}
                options={[
                  { label: '金额最多', value: 'amount' },
                  { label: '今日收益最高', value: 'todayIncome' },
                  { label: '涨幅最高', value: 'pct' },
                ]}
              />
            </Space>

            {!isMobile && (
              <Segmented
                size="small"
                value={viewMode}
                onChange={(val) => setViewMode(val as 'cards' | 'table')}
                options={[
                  { icon: <BarsOutlined />, value: 'table' },
                  { icon: <AppstoreOutlined />, value: 'cards' },
                ]}
              />
            )}
          </div>

          {/* 持仓列表主体 */}
          {sortedItems.length === 0 ? (
            <div
              style={{
                background: '#ffffff',
                borderRadius: 14,
                border: '1px solid #e2e8f0',
                padding: '48px 0',
                textAlign: 'center',
              }}
            >
              <Empty
                description={
                  <span style={{ color: '#64748b', fontSize: 13 }}>
                    暂无持仓基金，点击上方“录入持仓”添加您的第一只基金
                  </span>
                }
              >
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => {
                    setEditingItem(null)
                    setEditModalOpen(true)
                  }}
                  style={{
                    borderRadius: 8,
                    background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
                  }}
                >
                  立即录入持仓
                </Button>
              </Empty>
            </div>
          ) : viewMode === 'cards' || isMobile ? (
            /* 移动端与卡片流视图 (对标 WatchlistPage 卡片质感) */
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {sortedItems.map((item) => {
                const itemUp = item.todayIncome > 0
                const itemDown = item.todayIncome < 0
                const color = itemUp ? '#dc2626' : itemDown ? '#16a34a' : '#475569'
                const badgeClass = (item.estimatePct ?? 0) > 0 ? 'badge-up' : (item.estimatePct ?? 0) < 0 ? 'badge-down' : 'badge-flat'

                return (
                  <div
                    key={item.fundCode}
                    style={{
                      background: '#ffffff',
                      borderRadius: 12,
                      border: '1px solid #e2e8f0',
                      padding: '13px 15px',
                      boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
                    }}
                  >
                    {/* 卡片顶栏：基金名称 + Tag + 编辑删除 */}
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                      <div
                        style={{ flex: 1, minWidth: 0, marginRight: 8, cursor: 'pointer' }}
                        onClick={() => navigate(`/fund/${item.fundCode}`)}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span style={{ fontWeight: 650, fontSize: 15, color: '#0f172a' }}>
                            {item.fundName}
                          </span>
                          {item.fundType && (
                            <Tag
                              color={FUND_TYPE_COLOR[item.fundType] ?? 'default'}
                              style={{ fontSize: 10, margin: 0, padding: '0 4px', borderRadius: 4, border: 'none' }}
                            >
                              {FUND_TYPE_LABEL[item.fundType] ?? item.fundType}
                            </Tag>
                          )}
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
                          <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>
                            {item.fundCode}
                          </Text>
                          {item.prevNav && (
                            <Text type="secondary" style={{ fontSize: 11, color: '#94a3b8' }}>
                              · 昨净 {fmtNav(item.prevNav)}
                            </Text>
                          )}
                        </div>
                      </div>

                      <Space size={2}>
                        <Button
                          size="small"
                          type="text"
                          icon={<EditOutlined style={{ color: '#2563eb' }} />}
                          onClick={() => {
                            setEditingItem(item)
                            setEditModalOpen(true)
                          }}
                        />
                        <Popconfirm
                          title="确认移出持仓？"
                          onConfirm={() => handleDelete(item.fundCode)}
                          okText="删除"
                          cancelText="取消"
                          okButtonProps={{ danger: true }}
                        >
                          <Button size="small" type="text" danger icon={<DeleteOutlined />} />
                        </Popconfirm>
                      </Space>
                    </div>

                    {/* 卡片中部：主要核心收益指标 (与自选页大数字风格完全呼应) */}
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'flex-end',
                        justifyContent: 'space-between',
                        padding: '10px 12px',
                        background: '#f8fafc',
                        borderRadius: 10,
                        border: '1px solid #f1f5f9',
                        marginBottom: 10,
                      }}
                    >
                      <div>
                        <div style={{ fontSize: 11, color: '#64748b' }}>今日收益</div>
                        <div
                          style={{
                            fontSize: 20,
                            fontWeight: 750,
                            color,
                            lineHeight: 1.2,
                            marginTop: 2,
                            fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                            letterSpacing: -0.4,
                          }}
                        >
                          {fmtMoney(item.todayIncome, true)}
                        </div>
                        <div style={{ fontSize: 11, color: '#64748b', marginTop: 2 }}>
                          持有 {fmtMoney(item.holdingAmount)}
                        </div>
                      </div>

                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
                        <div
                          className={badgeClass}
                          style={{
                            fontSize: 14,
                            minWidth: 80,
                            textAlign: 'center',
                            padding: '3px 8px',
                            borderRadius: 6,
                          }}
                        >
                          {fmtPct(item.estimatePct)}
                        </div>
                        <div style={{ fontSize: 11, color: '#94a3b8' }}>
                          市值: {fmtMoney(item.dynamicMarketValue)}
                        </div>
                      </div>
                    </div>

                    {/* 卡片底栏：支付宝4字段详细对照 */}
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        fontSize: 12,
                        padding: '0 2px',
                      }}
                    >
                      <div>
                        <span style={{ color: '#64748b' }}>持有收益: </span>
                        <b style={{ color: item.holdingProfit >= 0 ? '#dc2626' : '#16a34a' }}>
                          {fmtMoney(item.holdingProfit, true)}
                        </b>
                        <span style={{ color: '#94a3b8', marginLeft: 4 }}>
                          ({fmtPct(item.holdingProfitRate)})
                        </span>
                      </div>
                      <div>
                        <span style={{ color: '#64748b' }}>昨日收益: </span>
                        <b style={{ color: (item.yesterdayIncome ?? 0) >= 0 ? '#dc2626' : '#16a34a' }}>
                          {fmtMoney(item.yesterdayIncome, true)}
                        </b>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          ) : (
            /* 桌面端全量数据表格 */
            <div
              style={{
                background: '#ffffff',
                borderRadius: 14,
                border: '1px solid #e2e8f0',
                boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
                overflow: 'hidden',
              }}
            >
              <Table
                rowKey="fundCode"
                dataSource={sortedItems}
                columns={desktopColumns}
                pagination={false}
              />
            </div>
          )}
        </>
      )}

      {/* 弹窗组件 */}
      <PositionEditModal
        open={editModalOpen}
        editingItem={editingItem}
        onClose={() => setEditModalOpen(false)}
        onSuccess={() => loadPortfolio(false)}
      />
      <VipRechargeModal
        open={rechargeModalOpen}
        onClose={() => setRechargeModalOpen(false)}
        onContactCustomerService={() => setContactModalOpen(true)}
      />
      <ContactModal open={contactModalOpen} onClose={() => setContactModalOpen(false)} />
    </div>
  )
}
