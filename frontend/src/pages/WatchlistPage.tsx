import { createContext, useContext, useMemo, useRef, useState, useEffect } from 'react'
import {
  Alert,
  Button,
  Empty,
  Input,
  Modal,
  Popconfirm,
  Table,
  Tag,
  Typography,
  Card,
  message,
} from 'antd'
import {
  PlusOutlined,
  MenuOutlined,
  DeleteOutlined,
  CheckOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  HolderOutlined,
  SwapOutlined,
  SoundOutlined,
  CustomerServiceOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import type { ColumnsType } from 'antd/es/table'
import { ContactModal } from '../components/ContactModal'
import {
  DndContext,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core'
import {
  SortableContext,
  arrayMove,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { useWatchlist } from '../hooks/useWatchlist'
import { FUND_TYPE_COLOR, FUND_TYPE_LABEL } from '../api/types'
import type { EstimateResult, NoticeView } from '../api/types'
import { api } from '../api'
import { colorOf, fmtNav, fmtPct } from '../utils/format'

const { Title, Text } = Typography

// 严格锁定垂直单轴运动的 Modifier，彻底杜绝横向位移与甩出界面
const restrictToVerticalAxis = ({
  transform,
}: {
  transform: { x: number; y: number; scaleX: number; scaleY: number }
}) => {
  return {
    ...transform,
    x: 0,
  }
}

interface RowContextProps {
  attributes: any
  listeners: any
}

const RowContext = createContext<RowContextProps>({
  attributes: {},
  listeners: undefined,
})

function DragHandle() {
  const { attributes, listeners } = useContext(RowContext)
  return (
    <div
      {...attributes}
      {...listeners}
      style={{
        cursor: 'grab',
        touchAction: 'none',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '6px 4px',
        color: '#94a3b8',
        borderRadius: 4,
      }}
    >
      <HolderOutlined style={{ fontSize: 16 }} />
    </div>
  )
}

/* ------------------ 桌面端 Table 拖拽排序 Row ------------------ */
interface SortableRowProps {
  'data-row-key': string
  children: React.ReactNode
}

function SortableRow({ 'data-row-key': rowKey, children, ...props }: SortableRowProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: rowKey,
  })
  const style: React.CSSProperties = {
    transform: CSS.Translate.toString(transform ? { ...transform, x: 0 } : null),
    transition,
    ...(isDragging ? { position: 'relative', zIndex: 999, backgroundColor: '#f8fafc', boxShadow: '0 4px 12px rgba(15, 23, 42, 0.08)' } : {}),
  }
  return (
    <RowContext.Provider value={{ attributes, listeners }}>
      <tr
        {...props}
        ref={setNodeRef}
        style={style}
        data-row-key={rowKey}
      >
        {children}
      </tr>
    </RowContext.Provider>
  )
}

/* ------------------ 移动端 卡片 拖拽排序 Item ------------------ */
interface MobileFundCardProps {
  fund: EstimateResult
  isSorting: boolean
  onNavigate: (code: string) => void
  onRemove: (code: string) => void
  onMoveUp?: () => void
  onMoveDown?: () => void
  isFirst?: boolean
  isLast?: boolean
}

function MobileSortableCard({
  fund,
  isSorting,
  onNavigate,
  onRemove,
  onMoveUp,
  onMoveDown,
  isFirst,
  isLast,
}: MobileFundCardProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: fund.fundCode,
    disabled: !isSorting, // 非排序模式下完全禁用手势，保障 100% 原生流畅滚动
  })

  const style: React.CSSProperties = {
    transform: CSS.Translate.toString(transform ? { ...transform, x: 0 } : null),
    transition,
    marginBottom: 10,
    borderRadius: 14,
    border: '1px solid #f1f5f9',
    backgroundColor: '#ffffff',
    boxShadow: isDragging
      ? '0 12px 28px -8px rgba(15, 23, 42, 0.15)'
      : '0 1px 3px 0 rgba(15, 23, 42, 0.03), 0 1px 2px -1px rgba(15, 23, 42, 0.02)',
    zIndex: isDragging ? 99 : 1,
    position: 'relative',
    touchAction: isSorting ? 'none' : 'auto',
  }

  const isUp = (fund.estimatePct ?? 0) > 0
  const isDown = (fund.estimatePct ?? 0) < 0
  const badgeClass = isUp ? 'badge-up' : isDown ? 'badge-down' : 'badge-flat'

  return (
    <div ref={setNodeRef} style={style} className={!isSorting ? 'mobile-fund-card' : ''}>
      <div style={{ padding: '14px 16px' }}>
        {/* 卡片顶部：手柄(排序模式可见) + 基金名称 + 类型标签 */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              flex: 1,
              overflow: 'hidden',
              cursor: isSorting ? 'default' : 'pointer',
            }}
            onClick={() => !isSorting && onNavigate(fund.fundCode)}
          >
            {isSorting && (
              <div
                {...attributes}
                {...listeners}
                style={{
                  padding: '4px 6px',
                  cursor: 'grab',
                  color: '#94a3b8',
                  touchAction: 'none',
                }}
              >
                <MenuOutlined style={{ fontSize: 16 }} />
              </div>
            )}
            <Text
              strong
              ellipsis
              style={{
                fontSize: 15,
                color: '#0f172a',
                lineHeight: 1.3,
                fontWeight: 650,
              }}
            >
              {fund.fundName}
            </Text>
            <Tag
              color={FUND_TYPE_COLOR[fund.fundType] ?? 'default'}
              style={{
                margin: 0,
                fontSize: 11,
                padding: '0 6px',
                borderRadius: 4,
                lineHeight: '18px',
                border: 'none',
              }}
            >
              {FUND_TYPE_LABEL[fund.fundType] ?? fund.fundType}
            </Tag>
          </div>

          {!isSorting && (
            <Popconfirm
              title="删除基金"
              description={`确认从自选删除 ${fund.fundName}?`}
              okText="删除"
              cancelText="取消"
              okButtonProps={{ danger: true, size: 'small', style: { borderRadius: 6 } }}
              cancelButtonProps={{ size: 'small', style: { borderRadius: 6 } }}
              onConfirm={(e) => {
                e?.stopPropagation()
                onRemove(fund.fundCode)
              }}
            >
              <Button
                type="text"
                size="small"
                danger
                icon={<DeleteOutlined style={{ fontSize: 14 }} />}
                style={{ marginLeft: 4, color: '#94a3b8' }}
                onClick={(e) => e.stopPropagation()}
              />
            </Popconfirm>
          )}
        </div>

        {/* 卡片中部：主要核心财务指标 */}
        <div
          style={{
            display: 'flex',
            alignItems: 'flex-end',
            justifyContent: 'space-between',
            cursor: isSorting ? 'default' : 'pointer',
          }}
          onClick={() => !isSorting && onNavigate(fund.fundCode)}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
              <span
                style={{
                  fontSize: 22,
                  fontFamily: 'ui-monospace, SFMono-Regular, Roboto, "Helvetica Neue", sans-serif',
                  fontWeight: 700,
                  color: colorOf(fund.estimatePct),
                  letterSpacing: -0.5,
                }}
              >
                {fmtNav(fund.estimateNav)}
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
              <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>
                昨净 {fmtNav(fund.prevNav)}
              </Text>
              <Text type="secondary" style={{ fontSize: 12, color: '#94a3b8' }}>
                · {fund.fundCode}
              </Text>
              {fund.navDate && (
                <Text type="secondary" style={{ fontSize: 11, color: '#94a3b8' }}>
                  ({fund.navDate})
                </Text>
              )}
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
            <div
              className={badgeClass}
              style={{
                fontSize: 15,
                minWidth: 84,
                textAlign: 'center',
                padding: '4px 10px',
                borderRadius: 6,
                letterSpacing: -0.2,
              }}
            >
              {fmtPct(fund.estimatePct)}
            </div>

            {/* 排序微调按钮 (排序模式下辅助操作) */}
            {isSorting ? (
              <div style={{ display: 'flex', gap: 4, marginTop: 4 }}>
                <Button
                  size="small"
                  disabled={isFirst}
                  icon={<ArrowUpOutlined />}
                  style={{ borderRadius: 6 }}
                  onClick={(e) => {
                    e.stopPropagation()
                    onMoveUp?.()
                  }}
                />
                <Button
                  size="small"
                  disabled={isLast}
                  icon={<ArrowDownOutlined />}
                  style={{ borderRadius: 6 }}
                  onClick={(e) => {
                    e.stopPropagation()
                    onMoveDown?.()
                  }}
                />
              </div>
            ) : (
              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                {fund.stale && (
                  <Text type="warning" style={{ fontSize: 11 }}>
                    <WarningOutlined /> 延迟
                  </Text>
                )}
                <Text type="secondary" style={{ fontSize: 11, color: '#94a3b8' }}>
                  {fund.quoteTs ? new Date(fund.quoteTs).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '--'}
                </Text>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

/* ------------------ 主页面组件 ------------------ */
export default function WatchlistPage() {
  const { estimates, loading, error, addBatch, remove, reorder } = useWatchlist()
  const navigate = useNavigate()

  const [adding, setAdding] = useState(false)
  const [newCode, setNewCode] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [isSorting, setIsSorting] = useState(false) // 移动端专属的“排序模式”切换
  const [pctSort, setPctSort] = useState<'default' | 'desc' | 'asc'>('default') // 收益率正序/逆序/默认排序

  const [notice, setNotice] = useState<NoticeView | null>(null)
  const [dismissedNotice, setDismissedNotice] = useState<string | null>(() => {
    return localStorage.getItem('fund-valuation-dismissed-notice')
  })

  // 获取系统公告 (后台关闭自定义公告时返回空，此时不展示任何提示)
  // 定时轮询 + 窗口聚焦/可见时刷新，确保管理员推送的最新公告能及时同步到已打开页面
  useEffect(() => {
    let active = true
    const fetchNotice = () => {
      api
        .getNotice()
        .then((data) => {
          if (active) setNotice(data || null)
        })
        .catch(() => {
          if (active) setNotice(null)
        })
    }

    fetchNotice()
    const timer = window.setInterval(fetchNotice, 60000)
    const onFocus = () => fetchNotice()
    const onVisibility = () => {
      if (document.visibilityState === 'visible') fetchNotice()
    }
    window.addEventListener('focus', onFocus)
    document.addEventListener('visibilitychange', onVisibility)

    return () => {
      active = false
      window.clearInterval(timer)
      window.removeEventListener('focus', onFocus)
      document.removeEventListener('visibilitychange', onVisibility)
    }
  }, [])

  // 监听屏幕断点 (< 768px 为移动端视图)
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)
  const [contactOpen, setContactOpen] = useState(false)
  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768)
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  // 按照收益率大小进行正序 (从小到大) 或 逆序 (从大到小) 排序派生
  const displayEstimates = useMemo(() => {
    if (pctSort === 'default') {
      return estimates
    }
    return [...estimates].sort((a, b) => {
      const va = a.estimatePct ?? 0
      const vb = b.estimatePct ?? 0
      return pctSort === 'desc' ? vb - va : va - vb
    })
  }, [estimates, pctSort])

  const codesRef = useRef(displayEstimates.map((e) => e.fundCode))
  codesRef.current = displayEstimates.map((e) => e.fundCode)

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 4,
      },
    })
  )

  const onDragEnd = async ({ active, over }: DragEndEvent) => {
    if (!over || active.id === over.id) return
    const oldIndex = codesRef.current.indexOf(String(active.id))
    const newIndex = codesRef.current.indexOf(String(over.id))
    if (oldIndex < 0 || newIndex < 0) return
    const next = arrayMove(codesRef.current, oldIndex, newIndex)
    setPctSort('default') // 拖拽后自动保持此新的自定义排序
    try {
      await reorder(next)
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  // 移动端辅助移动函数
  const handleMove = async (index: number, direction: 'up' | 'down') => {
    const targetIndex = direction === 'up' ? index - 1 : index + 1
    if (targetIndex < 0 || targetIndex >= codesRef.current.length) return
    const next = arrayMove(codesRef.current, index, targetIndex)
    try {
      await reorder(next)
    } catch (e) {
      message.error((e as Error).message)
    }
  }

  const submitAdd = async () => {
    // 支持一次添加多个基金代码，以英文/中文逗号、顿号、分号或空白分隔
    const codes = Array.from(
      new Set(
        newCode
          .split(/[,，、;；\s]+/)
          .map((s) => s.trim())
          .filter(Boolean)
      )
    )
    if (codes.length === 0) {
      message.warning('请输入基金代码')
      return
    }

    setSubmitting(true)
    try {
      const results = await addBatch(codes)
      const ok = results.filter((r) => r.success)
      const fail = results.filter((r) => !r.success)

      if (ok.length > 0 && fail.length === 0) {
        message.success(`已成功添加 ${ok.length} 只基金：${ok.map((r) => r.fundCode).join('、')}`)
      } else if (ok.length > 0) {
        message.warning(
          `成功 ${ok.length} 只，失败 ${fail.length} 只（${fail.map((r) => `${r.fundCode}: ${r.message}`).join('；')}）`
        )
      } else {
        message.error(`添加失败：${fail.map((r) => `${r.fundCode}: ${r.message}`).join('；')}`)
      }

      if (ok.length > 0) {
        setNewCode('')
        setAdding(false)
      }
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setSubmitting(false)
    }
  }

  /* ------------------ PC端专用的全量数据表格列 ------------------ */
  const desktopColumns = useMemo<ColumnsType<EstimateResult>>(
    () => [
      {
        title: '',
        key: 'drag_handle',
        width: 44,
        align: 'center',
        render: () => <DragHandle />,
      },
      {
        title: '基金名称 / 代码',
        dataIndex: 'fundName',
        render: (name: string, r) => (
          <div style={{ cursor: 'pointer' }} onClick={() => navigate(`/fund/${r.fundCode}`)}>
            <Text strong style={{ color: '#0f172a', fontWeight: 650, fontSize: 14 }}>
              {name}
            </Text>
            <div style={{ marginTop: 2 }}>
              <Text type="secondary" style={{ fontSize: 12, color: '#64748b' }}>
                {r.fundCode}
              </Text>
              <Tag
                color={FUND_TYPE_COLOR[r.fundType] ?? 'default'}
                style={{ marginLeft: 8, fontSize: 11, padding: '0 5px', borderRadius: 4, border: 'none' }}
              >
                {FUND_TYPE_LABEL[r.fundType] ?? r.fundType}
              </Tag>
            </div>
          </div>
        ),
      },
      {
        title: '估算净值',
        dataIndex: 'estimateNav',
        width: 115,
        align: 'right',
        render: (v: number, r) => (
          <span
            style={{
              fontWeight: 700,
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
              fontSize: 15,
              color: colorOf(r.estimatePct),
            }}
          >
            {fmtNav(v)}
          </span>
        ),
      },
      {
        title: '估算涨跌幅',
        dataIndex: 'estimatePct',
        width: 135,
        align: 'right',
        sorter: (a, b) => (a.estimatePct ?? 0) - (b.estimatePct ?? 0),
        render: (v: number) => {
          const isUp = (v ?? 0) > 0
          const isDown = (v ?? 0) < 0
          const badgeClass = isUp ? 'badge-up' : isDown ? 'badge-down' : 'badge-flat'
          return <span className={badgeClass}>{fmtPct(v)}</span>
        },
      },
      {
        title: '昨日净值',
        dataIndex: 'prevNav',
        width: 100,
        align: 'right',
        render: (v: number) => (
          <span style={{ fontFamily: 'ui-monospace, SFMono-Regular, monospace', color: '#475569' }}>
            {fmtNav(v)}
          </span>
        ),
      },
      {
        title: '净值日期',
        dataIndex: 'navDate',
        width: 110,
        render: (v: string | null) => (
          <span style={{ color: '#475569', fontSize: 13 }}>{v ?? '--'}</span>
        ),
      },
      {
        title: '持仓报告期',
        dataIndex: 'reportQt',
        width: 110,
        render: (v: string | null, r) =>
          r.stale ? <Text type="danger">{v ?? '缺失'}</Text> : <span style={{ color: '#64748b' }}>{v ?? '--'}</span>,
      },
      {
        title: '估算时间',
        dataIndex: 'quoteTs',
        width: 100,
        render: (v: string | null) => (
          <span style={{ color: '#64748b', fontSize: 12 }}>
            {v ? new Date(v).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '--'}
          </span>
        ),
      },
      {
        title: '状态',
        dataIndex: 'message',
        width: 110,
        render: (_: string | null, r) => {
          if (r.message) return <Text type="danger">{r.message}</Text>
          if (r.stale) return <Text type="warning">行情延迟</Text>
          return <span style={{ color: '#10b981', fontSize: 12, fontWeight: 500 }}>实时计算</span>
        },
      },
      {
        title: '操作',
        key: 'ops',
        width: 70,
        align: 'center',
        render: (_, r) => (
          <Popconfirm
            title="从自选删除"
            description={`确认删除 ${r.fundName}?`}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true, size: 'small', style: { borderRadius: 6 } }}
            cancelButtonProps={{ size: 'small', style: { borderRadius: 6 } }}
            onConfirm={async () => {
              try {
                await remove(r.fundCode)
                message.success('已删除')
              } catch (e) {
                message.error((e as Error).message)
              }
            }}
          >
            <Button size="small" type="text" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        ),
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [navigate, estimates],
  )

  return (
    <div
      style={{
        padding: isMobile ? '16px 12px 48px 12px' : '28px 32px',
        maxWidth: 1200,
        margin: '0 auto',
      }}
    >
      {/* 顶部工具栏 (高端简雅金融看板标题) */}
      <div
        style={{
          display: 'flex',
          flexDirection: isMobile ? 'column' : 'row',
          alignItems: isMobile ? 'stretch' : 'center',
          justifyContent: 'space-between',
          gap: isMobile ? 12 : 16,
          marginBottom: 16,
        }}
      >
        <div>
          <Title
            level={4}
            style={{
              margin: 0,
              fontSize: isMobile ? 18 : 20,
              color: '#0f172a',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              fontWeight: 700,
              letterSpacing: -0.3,
            }}
          >
            自选估值
            <span
              style={{
                fontSize: 12,
                fontWeight: 500,
                color: '#475569',
                background: '#f1f5f9',
                borderRadius: 12,
                padding: '2px 8px',
              }}
            >
              {estimates.length} 只
            </span>
          </Title>
        </div>

        {/* 操作区：移动端单行等宽排布，杜绝换行错位 */}
        <div
          style={{
            display: 'flex',
            gap: 8,
            flexWrap: 'nowrap',
            width: isMobile ? '100%' : undefined,
            alignItems: 'center',
          }}
        >
          {estimates.length > 1 && (
            <Button
              size={isMobile ? 'small' : 'middle'}
              icon={
                pctSort === 'desc' ? (
                  <ArrowDownOutlined style={{ color: '#dc2626' }} />
                ) : pctSort === 'asc' ? (
                  <ArrowUpOutlined style={{ color: '#16a34a' }} />
                ) : (
                  <SwapOutlined />
                )
              }
              type={pctSort !== 'default' ? 'primary' : 'default'}
              ghost={pctSort !== 'default'}
              style={{ borderRadius: 8, flex: isMobile ? 1 : undefined }}
              onClick={() => {
                if (isSorting) setIsSorting(false)
                setPctSort((prev) => (prev === 'default' ? 'desc' : prev === 'desc' ? 'asc' : 'default'))
              }}
            >
              {pctSort === 'desc'
                ? isMobile
                  ? '降序'
                  : '涨幅降序'
                : pctSort === 'asc'
                ? isMobile
                  ? '升序'
                  : '涨幅升序'
                : isMobile
                ? '排序'
                : '收益率排序'}
            </Button>
          )}

          {isMobile && estimates.length > 1 && (
            <Button
              size="small"
              icon={isSorting ? <CheckOutlined /> : <MenuOutlined />}
              type={isSorting ? 'primary' : 'default'}
              style={{ borderRadius: 8, flex: 1 }}
              onClick={() => {
                if (!isSorting) {
                  setPctSort('default') // 开启拖拽模式时恢复为默认自选顺序
                }
                setIsSorting(!isSorting)
              }}
            >
              {isSorting ? '完成' : '拖拽'}
            </Button>
          )}

          <Button
            type="primary"
            icon={<PlusOutlined />}
            size={isMobile ? 'small' : 'middle'}
            style={{
              borderRadius: 8,
              flex: isMobile ? 1 : undefined,
              background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
              fontWeight: 550,
              boxShadow: '0 2px 6px rgba(37, 99, 235, 0.2)',
            }}
            onClick={() => setAdding(true)}
          >
            {isMobile ? '添加' : '添加基金'}
          </Button>

          <Button
            size={isMobile ? 'small' : 'middle'}
            icon={<CustomerServiceOutlined style={{ color: '#2563eb' }} />}
            style={{ borderRadius: 8, flex: isMobile ? 1 : undefined, color: '#334155' }}
            onClick={() => setContactOpen(true)}
          >
            联系
          </Button>
        </div>
      </div>

      {/* 通用系统公告 / 市场时段状态提示 */}
      {notice && (!notice.closable || dismissedNotice !== notice.message) && (
        <Alert
          type={notice.type || 'info'}
          showIcon
          icon={
            notice.custom ? (
              <SoundOutlined
                style={{
                  color: notice.type === 'success' ? '#16a34a' : '#2563eb',
                  fontSize: 15,
                }}
              />
            ) : (
              <ClockCircleOutlined style={{ color: '#64748b' }} />
            )
          }
          message={notice.message}
          closable={notice.closable}
          onClose={() => {
            if (notice.closable) {
              setDismissedNotice(notice.message)
              localStorage.setItem('fund-valuation-dismissed-notice', notice.message)
            }
          }}
          style={{
            marginBottom: 14,
            borderRadius: 10,
            fontSize: 13,
            backgroundColor:
              notice.type === 'success'
                ? '#f0fdf4'
                : notice.custom
                ? '#eff6ff'
                : '#f8fafc',
            border:
              notice.type === 'success'
                ? '1px solid #bbf7d0'
                : notice.custom
                ? '1px solid #bfdbfe'
                : '1px solid #e2e8f0',
          }}
        />
      )}

      {error && (
        <Alert
          type="error"
          showIcon
          message={error}
          style={{ marginBottom: 14, borderRadius: 10 }}
          closable
        />
      )}

      {/* 核心内容区 */}
      {displayEstimates.length === 0 && !loading ? (
        <Card
          style={{
            borderRadius: 16,
            textAlign: 'center',
            padding: '48px 0',
            border: '1px solid #f1f5f9',
            boxShadow: '0 1px 3px 0 rgba(15, 23, 42, 0.04)',
          }}
        >
          <Empty description="暂无自选基金，快添加你的第一只基金吧" />
          <Button
            type="primary"
            style={{ marginTop: 16, borderRadius: 8, height: 38 }}
            onClick={() => setAdding(true)}
          >
            立即添加
          </Button>
        </Card>
      ) : isMobile ? (
        /* 移动端专用的金融卡片流 —— 顺滑原生，绝无左右横移，绝无手势冲突 */
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          modifiers={[restrictToVerticalAxis as any]}
          onDragEnd={onDragEnd}
        >
          <SortableContext
            items={displayEstimates.map((e) => e.fundCode)}
            strategy={verticalListSortingStrategy}
            disabled={!isSorting}
          >
            {displayEstimates.map((fund, idx) => (
              <MobileSortableCard
                key={fund.fundCode}
                fund={fund}
                isSorting={isSorting}
                onNavigate={(code) => navigate(`/fund/${code}`)}
                onRemove={(code) => remove(code)}
                isFirst={idx === 0}
                isLast={idx === displayEstimates.length - 1}
                onMoveUp={() => handleMove(idx, 'up')}
                onMoveDown={() => handleMove(idx, 'down')}
              />
            ))}
          </SortableContext>
        </DndContext>
      ) : (
        /* PC桌面端：多指标全能表格 */
        <Card
          styles={{ body: { padding: 0 } }}
          style={{
            borderRadius: 14,
            overflow: 'hidden',
            border: '1px solid #f1f5f9',
            boxShadow: '0 1px 3px 0 rgba(15, 23, 42, 0.04), 0 1px 2px -1px rgba(15, 23, 42, 0.02)',
          }}
        >
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            modifiers={[restrictToVerticalAxis as any]}
            onDragEnd={onDragEnd}
          >
            <SortableContext
              items={displayEstimates.map((e) => e.fundCode)}
              strategy={verticalListSortingStrategy}
            >
              <Table<EstimateResult>
                rowKey="fundCode"
                columns={desktopColumns}
                dataSource={displayEstimates}
                loading={loading}
                pagination={false}
                components={{ body: { row: SortableRow } }}
              />
            </SortableContext>
          </DndContext>
        </Card>
      )}

      {/* 免责声明提示 */}
      <div style={{ marginTop: 24, textAlign: 'center' }}>
        <Text type="secondary" style={{ fontSize: 12, color: '#94a3b8' }}>
          {displayEstimates[0]?.disclaimer ?? '实时估值基于持仓与算法模型计算，仅供参考，不构成投资建议'}
        </Text>
      </div>

      {/* 添加基金弹出层 */}
      <Modal
        title="添加自选基金"
        open={adding}
        onOk={submitAdd}
        confirmLoading={submitting}
        onCancel={() => setAdding(false)}
        okText="确认添加"
        cancelText="取消"
        okButtonProps={{ style: { borderRadius: 8 } }}
        cancelButtonProps={{ style: { borderRadius: 8 } }}
        centered
      >
        <div style={{ padding: '16px 0' }}>
          <Input.TextArea
            placeholder="可一次添加多个，用逗号分隔，例如：110022,510300,008559"
            value={newCode}
            autoSize={{ minRows: 2, maxRows: 5 }}
            style={{ borderRadius: 8, fontSize: 14 }}
            onChange={(e) => setNewCode(e.target.value)}
            autoFocus
            allowClear
          />
          <Text type="secondary" style={{ display: 'block', marginTop: 8, fontSize: 12, color: '#94a3b8' }}>
            多个代码可用英文逗号「,」、中文逗号「，」、顿号或换行分隔。
          </Text>
        </div>
      </Modal>

      {/* 联系管理员弹窗 */}
      <ContactModal open={contactOpen} onClose={() => setContactOpen(false)} />
    </div>
  )
}
