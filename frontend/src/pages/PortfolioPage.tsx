// 基金持仓与今日收益预估页面 (VIP专享)
import { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Typography,
  Button,
  Tag,
  Space,
  Popconfirm,
  Select,
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
  ArrowUpOutlined,
  ArrowDownOutlined,
  QuestionCircleOutlined,
  CustomerServiceOutlined,
  LockOutlined,
  CheckCircleFilled,
  BellOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { PortfolioView, PositionItemView, VipConfigView } from '../api/types'
import { FUND_TYPE_LABEL, FUND_TYPE_COLOR } from '../api/types'
import { useAuth } from '../hooks/useAuth'
import { PositionEditModal } from '../components/PositionEditModal'
import { VipRechargeModal } from '../components/VipRechargeModal'
import { ContactModal } from '../components/ContactModal'
import { NotificationChannelModal } from '../components/NotificationChannelModal'
import { ScreenshotImportModal } from '../components/ScreenshotImportModal'
import { ImportChoiceModal } from '../components/ImportChoiceModal'
import { fmtPct } from '../utils/format'

const { Title, Text, Paragraph } = Typography

function fmtMoney(val: number | null | undefined, showPlus = false): string {
  if (val === null || val === undefined) return '--'
  const prefix = showPlus && val > 0 ? '+' : ''
  return `${prefix}${val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export default function PortfolioPage() {
  const { isVip, isAdmin } = useAuth()
  const hasAccess = isVip || isAdmin
  const navigate = useNavigate()

  const [data, setData] = useState<PortfolioView | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [vipConfig, setVipConfig] = useState<VipConfigView | null>(null)

  // 弹窗状态
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<PositionItemView | null>(null)
  const [rechargeModalOpen, setRechargeModalOpen] = useState(false)
  const [contactModalOpen, setContactModalOpen] = useState(false)
  const [notificationModalOpen, setNotificationModalOpen] = useState(false)
  const [screenshotModalOpen, setScreenshotModalOpen] = useState(false)
  const [choiceModalOpen, setChoiceModalOpen] = useState(false)

  // 响应式与排序控制
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)
  const [sortKey, setSortKey] = useState<'amount' | 'holdingProfit' | 'todayIncome' | 'pct'>('amount')

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 768)
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
      list.sort((a, b) => (b.dynamicMarketValue ?? b.holdingAmount) - (a.dynamicMarketValue ?? a.holdingAmount))
    } else if (sortKey === 'holdingProfit') {
      list.sort((a, b) => (b.holdingProfit ?? 0) - (a.holdingProfit ?? 0))
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
              立即开通 VIP 会员 ({vipConfig?.monthlyPrice != null ? Number(vipConfig.monthlyPrice) : 2.9}元起)
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
                <div style={{ fontSize: 22, fontWeight: 700, color: '#dc2626', marginTop: 4 }}>+368.50</div>
                <div style={{ fontSize: 11, color: '#dc2626', marginTop: 2 }}>日内涨跌: +0.64%</div>
              </div>
              <div style={{ background: '#ffffff', padding: '14px', borderRadius: 12, border: '1px solid #e2e8f0' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>持仓总市值 (动态)</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', marginTop: 4 }}>58,260.00</div>
                <div style={{ fontSize: 11, color: '#64748b', marginTop: 2 }}>基准: 57,891.50</div>
              </div>
              <div style={{ background: '#ffffff', padding: '14px', borderRadius: 12, border: '1px solid #e2e8f0' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>最新累计总盈亏</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#dc2626', marginTop: 4 }}>+3,420.00</div>
                <div style={{ fontSize: 11, color: '#dc2626', marginTop: 2 }}>总回报率: +6.23%</div>
              </div>
              <div style={{ background: '#ffffff', padding: '14px', borderRadius: 12, border: '1px solid #e2e8f0' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>昨日官方收益</Text>
                <div style={{ fontSize: 22, fontWeight: 700, color: '#0f172a', marginTop: 4 }}>+120.00</div>
                <div style={{ fontSize: 11, color: '#64748b', marginTop: 2 }}>官方确认到账</div>
              </div>
            </div>

            <div style={{ background: '#ffffff', borderRadius: 12, border: '1px solid #e2e8f0', padding: 14 }}>
              <div style={{ padding: '10px 0', borderBottom: '1px solid #f1f5f9', display: 'flex', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ fontWeight: 650 }}>易方达消费行业股票 (110022)</div>
                  <div style={{ fontSize: 12, color: '#64748b' }}>持有金额: 20,000.00</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 17, fontWeight: 700, color: '#dc2626' }}>+260.00</div>
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
          <span style={{ fontSize: 13, color: '#64748b', fontWeight: 500 }}>
            共 {data?.items?.length ?? 0} 只持仓
          </span>
        </div>

        <Space size={8} style={{ marginLeft: 'auto' }}>
          <Button
            size={isMobile ? 'small' : 'middle'}
            icon={<BellOutlined style={{ color: '#d97706' }} />}
            onClick={() => setNotificationModalOpen(true)}
            style={{ borderRadius: 8 }}
          >
            {isMobile ? '提醒' : '收益提醒'}
          </Button>

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
            onClick={() => setChoiceModalOpen(true)}
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
        </div>
      ) : (
        <>
          {/* 顶部总览卡片 */}
          {isMobile ? (
            /* 移动端资产总览看板：与全站浅色调统一和谐，同时以蓝标标题栏与双层结构区分于下方持仓卡片 */
            <div
              style={{
                background: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: 14,
                padding: '12px 14px',
                marginBottom: 12,
                boxShadow: '0 1px 4px rgba(15, 23, 42, 0.04)',
              }}
            >
              {/* 总览标题栏：带微蓝装饰标识，清晰标明“持仓总览”，区别于下方个基卡片 */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  paddingBottom: 8,
                  marginBottom: 10,
                  borderBottom: '1px solid #f1f5f9',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <div
                    style={{
                      width: 3,
                      height: 12,
                      borderRadius: 2,
                      background: 'linear-gradient(180deg, #2563eb 0%, #3b82f6 100%)',
                    }}
                  />
                  <span style={{ fontSize: 12.5, fontWeight: 650, color: '#334155' }}>
                    持仓总览
                  </span>
                </div>
                <span style={{ fontSize: 11, color: '#94a3b8' }}>实时估算</span>
              </div>

              {/* 上层核心指标：今日预估收益 + 总资产市值 */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'space-between',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <span style={{ fontSize: 11.5, color: '#64748b', fontWeight: 500 }}>
                      今日预估收益
                    </span>
                    {isUp ? (
                      <ArrowUpOutlined style={{ color: '#dc2626', fontSize: 11 }} />
                    ) : isDown ? (
                      <ArrowDownOutlined style={{ color: '#16a34a', fontSize: 11 }} />
                    ) : null}
                  </div>
                  <div
                    style={{
                      fontSize: 23,
                      fontWeight: 750,
                      color: isUp ? '#dc2626' : isDown ? '#16a34a' : '#0f172a',
                      fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                      letterSpacing: -0.5,
                      lineHeight: 1.25,
                      marginTop: 2,
                    }}
                  >
                    {fmtMoney(summary?.totalTodayIncome, true)}
                  </div>
                  <div
                    style={{
                      marginTop: 2,
                      fontSize: 11.5,
                      fontWeight: 500,
                      color: isUp ? '#dc2626' : isDown ? '#16a34a' : '#64748b',
                    }}
                  >
                    涨跌 {fmtPct(summary?.totalTodayIncomePct)}
                  </div>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 11.5, color: '#64748b', fontWeight: 500 }}>
                    持仓总市值
                  </div>
                  <div
                    style={{
                      fontSize: 19,
                      fontWeight: 700,
                      color: '#0f172a',
                      fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                      letterSpacing: -0.3,
                      lineHeight: 1.25,
                      marginTop: 3,
                    }}
                  >
                    {fmtMoney(summary?.totalMarketValue)}
                  </div>
                  <div style={{ marginTop: 2, fontSize: 11.5, color: '#64748b' }}>
                    本金 {fmtMoney(summary?.totalHoldingAmount)}
                  </div>
                </div>
              </div>

              {/* 下层次级指标：嵌入浅灰柔和内嵌底栏，结构紧凑且与上方形成优雅分层 */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  background: '#f8fafc',
                  borderRadius: 8,
                  padding: '7px 10px',
                  marginTop: 10,
                  fontSize: 12,
                }}
              >
                <div>
                  <span style={{ color: '#64748b' }}>累计盈亏: </span>
                  <span
                    style={{
                      fontWeight: 650,
                      color: (summary?.dynamicTotalProfit ?? 0) >= 0 ? '#dc2626' : '#16a34a',
                      fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                    }}
                  >
                    {fmtMoney(summary?.dynamicTotalProfit, true)}
                  </span>
                  <span
                    style={{
                      fontSize: 11,
                      color: (summary?.dynamicTotalProfit ?? 0) >= 0 ? '#dc2626' : '#16a34a',
                      marginLeft: 3,
                    }}
                  >
                    ({fmtPct(summary?.dynamicTotalProfitRate)})
                  </span>
                </div>

                <div>
                  <span style={{ color: '#64748b' }}>持有收益: </span>
                  <span
                    style={{
                      fontWeight: 650,
                      color: (summary?.totalHoldingProfit ?? 0) >= 0 ? '#dc2626' : '#16a34a',
                      fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                    }}
                  >
                    {fmtMoney(summary?.totalHoldingProfit, true)}
                  </span>
                </div>
              </div>
            </div>
          ) : (
            /* 桌面端：4大核心资产收益看板 (纯净白底 · 稳重大气) */
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(4, 1fr)',
                gap: 12,
                marginBottom: 14,
              }}
            >
              {/* 今日预估收益 */}
              <div
                style={{
                  background: '#ffffff',
                  border: '1px solid #e2e8f0',
                  borderRadius: 12,
                  padding: '16px 18px',
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
                    fontSize: 26,
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
                  padding: '16px 18px',
                  boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
                }}
              >
                <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>总市值</Text>
                <div
                  style={{
                    fontSize: 26,
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
                  持仓本金 {fmtMoney(summary?.totalHoldingAmount)}
                </div>
              </div>

              {/* 最新累计总盈亏 */}
              <div
                style={{
                  background: '#ffffff',
                  border: '1px solid #e2e8f0',
                  borderRadius: 12,
                  padding: '16px 18px',
                  boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
                }}
              >
                <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>累计盈亏</Text>
                <div
                  style={{
                    fontSize: 26,
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

              {/* 累计持有收益 */}
              <div
                style={{
                  background: '#ffffff',
                  border: '1px solid #e2e8f0',
                  borderRadius: 12,
                  padding: '16px 18px',
                  boxShadow: '0 1px 3px rgba(15, 23, 42, 0.03)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>持有收益</Text>
                  <Tooltip title="支付宝官方账面累计确认持有收益">
                    <QuestionCircleOutlined style={{ fontSize: 12, color: '#cbd5e1' }} />
                  </Tooltip>
                </div>
                <div
                  style={{
                    fontSize: 26,
                    fontWeight: 700,
                    color: (summary?.totalHoldingProfit ?? 0) >= 0 ? '#dc2626' : '#16a34a',
                    fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                    lineHeight: 1.2,
                    marginTop: 4,
                    letterSpacing: -0.5,
                  }}
                >
                  {fmtMoney(summary?.totalHoldingProfit, true)}
                </div>
                <div style={{ marginTop: 3, fontSize: 12, color: '#94a3b8' }}>
                  账面累计已确认
                </div>
              </div>
            </div>
          )}

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
            <Space size={8} align="center">
              <span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>排序:</span>
              <Select
                size="small"
                value={sortKey}
                onChange={(val) => setSortKey(val as 'amount' | 'holdingProfit' | 'todayIncome' | 'pct')}
                style={{ width: 136 }}
                options={[
                  { label: '持仓金额最多', value: 'amount' },
                  { label: '持有收益最高', value: 'holdingProfit' },
                  { label: '今日收益最高', value: 'todayIncome' },
                  { label: '预估涨幅最高', value: 'pct' },
                ]}
              />
            </Space>
          </div>

          {/* 持仓列表主体：支付宝标准金融表格 */}
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
                  onClick={() => setChoiceModalOpen(true)}
                  style={{
                    borderRadius: 8,
                    background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
                    fontWeight: 550,
                  }}
                >
                  录入持仓基金
                </Button>
              </Empty>
            </div>
          ) : (
            /* 支付宝风格标准持仓表格 (单一连续表格容器 · 专业表头 · 无缝分割行 · 告别卡片堆叠) */
            <div
              style={{
                background: '#ffffff',
                borderRadius: 14,
                border: '1px solid #e2e8f0',
                boxShadow: '0 1px 4px rgba(15, 23, 42, 0.04)',
                overflow: 'hidden',
                marginBottom: 24,
              }}
            >
              {/* 优化后的专业金融表头 (单行防折行 · 支付宝原生简约风) */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'minmax(0, 1.8fr) minmax(0, 1.1fr) minmax(0, 1.1fr)',
                  alignItems: 'center',
                  padding: '11px 16px',
                  background: '#f8fafc',
                  borderBottom: '1px solid #e2e8f0',
                  fontSize: 12,
                  fontWeight: 650,
                  color: '#475569',
                  userSelect: 'none',
                  whiteSpace: 'nowrap',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <span>基金名称</span>
                </div>
                <div
                  style={{
                    textAlign: 'right',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'flex-end',
                    gap: 3,
                  }}
                  onClick={() => setSortKey(sortKey === 'holdingProfit' ? 'amount' : 'holdingProfit')}
                >
                  <span style={{ color: sortKey === 'holdingProfit' ? '#2563eb' : '#475569' }}>
                    持有收益
                  </span>
                  {!isMobile && (
                    <span style={{ fontSize: 11, fontWeight: 400, color: '#94a3b8' }}>
                      (收益率)
                    </span>
                  )}
                  {sortKey === 'holdingProfit' ? (
                    <ArrowDownOutlined style={{ fontSize: 10, color: '#2563eb' }} />
                  ) : null}
                </div>
                <div
                  style={{
                    textAlign: 'right',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'flex-end',
                    gap: 3,
                  }}
                  onClick={() =>
                    setSortKey(sortKey === 'todayIncome' ? 'pct' : sortKey === 'pct' ? 'amount' : 'todayIncome')
                  }
                >
                  <span style={{ color: sortKey === 'todayIncome' || sortKey === 'pct' ? '#2563eb' : '#475569' }}>
                    今日预估
                  </span>
                  {!isMobile && (
                    <span style={{ fontSize: 11, fontWeight: 400, color: '#94a3b8' }}>
                      (涨跌幅)
                    </span>
                  )}
                  {sortKey === 'todayIncome' || sortKey === 'pct' ? (
                    <ArrowDownOutlined style={{ fontSize: 10, color: '#2563eb' }} />
                  ) : null}
                </div>
              </div>

              {/* 表格数据行列表 */}
              <div>
                {sortedItems.map((item, idx) => {
                  const itemUp = item.todayIncome > 0
                  const itemDown = item.todayIncome < 0
                  const color = itemUp ? '#dc2626' : itemDown ? '#16a34a' : '#475569'
                  const isLast = idx === sortedItems.length - 1

                  return (
                    <div
                      key={item.fundCode}
                      className="alipay-fund-row"
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'minmax(0, 1.8fr) minmax(0, 1.1fr) minmax(0, 1.1fr)',
                        alignItems: 'center',
                        padding: '13px 16px',
                        borderBottom: isLast ? 'none' : '1px solid #f1f5f9',
                        cursor: 'pointer',
                        transition: 'background-color 0.15s ease',
                      }}
                      onClick={() => navigate(`/fund/${item.fundCode}`)}
                    >
                      {/* 第 1 列：基金名称 \n 代码 */}
                      <div style={{ minWidth: 0, paddingRight: 8 }}>
                        <div
                          style={{
                            fontWeight: 650,
                            fontSize: 14,
                            color: '#0f172a',
                            letterSpacing: -0.2,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            lineHeight: 1.35,
                          }}
                          title={item.fundName}
                        >
                          {item.fundName}
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4 }}>
                          <span
                            style={{
                              fontSize: 11.5,
                              fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                              color: '#64748b',
                            }}
                          >
                            {item.fundCode}
                          </span>
                          {item.fundType && (
                            <Tag
                              color={FUND_TYPE_COLOR[item.fundType] ?? 'default'}
                              style={{
                                fontSize: 10,
                                margin: 0,
                                padding: '0 4px',
                                borderRadius: 3,
                                border: 'none',
                                lineHeight: '15px',
                                fontWeight: 500,
                              }}
                            >
                              {FUND_TYPE_LABEL[item.fundType] ?? item.fundType}
                            </Tag>
                          )}
                          <span
                            style={{ display: 'inline-flex', alignItems: 'center', gap: 4, marginLeft: 'auto' }}
                            onClick={(e) => e.stopPropagation()}
                          >
                            <EditOutlined
                              style={{ fontSize: 12, color: '#94a3b8', padding: '2px 4px', cursor: 'pointer' }}
                              onClick={(e) => {
                                e.stopPropagation()
                                setEditingItem(item)
                                setEditModalOpen(true)
                              }}
                            />
                            <Popconfirm
                              title="确认移出持仓？"
                              onConfirm={(e) => {
                                e?.stopPropagation()
                                handleDelete(item.fundCode)
                              }}
                              okText="移出"
                              cancelText="取消"
                              okButtonProps={{ danger: true, size: 'small' }}
                            >
                              <DeleteOutlined style={{ fontSize: 12, color: '#94a3b8', padding: '2px 4px', cursor: 'pointer' }} />
                            </Popconfirm>
                          </span>
                        </div>
                      </div>

                      {/* 第 2 列：持有收益 \n 收益率 */}
                      <div style={{ textAlign: 'right' }}>
                        <div
                          style={{
                            fontSize: 15.5,
                            fontWeight: 700,
                            color: item.holdingProfit >= 0 ? '#dc2626' : '#16a34a',
                            fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                            letterSpacing: -0.3,
                            lineHeight: 1.25,
                          }}
                        >
                          {fmtMoney(item.holdingProfit, true)}
                        </div>
                        <div
                          style={{
                            fontSize: 11.5,
                            fontWeight: 550,
                            color: item.holdingProfit >= 0 ? '#dc2626' : '#16a34a',
                            fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                            marginTop: 2,
                          }}
                        >
                          {fmtPct(item.holdingProfitRate)}
                        </div>
                      </div>

                      {/* 第 3 列：今日预估收益 \n 预估涨跌幅 */}
                      <div style={{ textAlign: 'right' }}>
                        <div
                          style={{
                            fontSize: 15.5,
                            fontWeight: 750,
                            color,
                            fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                            letterSpacing: -0.3,
                            lineHeight: 1.25,
                          }}
                        >
                          {fmtMoney(item.todayIncome, true)}
                        </div>
                        <div
                          style={{
                            fontSize: 11.5,
                            fontWeight: 600,
                            color,
                            fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                            marginTop: 2,
                          }}
                        >
                          {fmtPct(item.estimatePct)}
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </>
      )}

      {/* 弹窗组件 */}
      <PositionEditModal
        open={editModalOpen}
        editingItem={editingItem}
        onClose={() => setEditModalOpen(false)}
        onSuccess={async () => {
          await loadPortfolio(true)
        }}
      />
      <VipRechargeModal
        open={rechargeModalOpen}
        onClose={() => setRechargeModalOpen(false)}
        onContactCustomerService={() => setContactModalOpen(true)}
      />
      <ContactModal open={contactModalOpen} onClose={() => setContactModalOpen(false)} />
      <NotificationChannelModal
        open={notificationModalOpen}
        onClose={() => setNotificationModalOpen(false)}
      />
      <ScreenshotImportModal
        open={screenshotModalOpen}
        onClose={() => setScreenshotModalOpen(false)}
        onSuccess={async () => {
          await loadPortfolio(true)
        }}
      />
      <ImportChoiceModal
        open={choiceModalOpen}
        onClose={() => setChoiceModalOpen(false)}
        onChooseScreenshot={() => setScreenshotModalOpen(true)}
        onChooseManual={() => {
          setEditingItem(null)
          setEditModalOpen(true)
        }}
      />
    </div>
  )
}
