import { useState, useEffect } from 'react'
import { Modal, Typography, Segmented, Button, message } from 'antd'
import {
  CrownOutlined,
  WechatOutlined,
  AlipayCircleOutlined,
  CopyOutlined,
  CheckCircleFilled,
  CustomerServiceOutlined,
} from '@ant-design/icons'
import { useAuth } from '../hooks/useAuth'
import { api } from '../api'
import type { VipConfigView } from '../api/types'

const { Text } = Typography

interface VipRechargeModalProps {
  open: boolean
  onClose: () => void
  onContactCustomerService?: () => void
}

interface PlanItem {
  key: string
  name: string
  price: string
  originalPrice?: string
  unit: string
  badge?: string
  features: string[]
}

const DEFAULT_PLANS: PlanItem[] = [
  {
    key: 'month',
    name: '月度会员',
    price: '2.9',
    unit: '/月',
    features: ['持仓今日收益实时预估', '对标支付宝4字段录入', '盘中每分钟实时刷新'],
  },
  {
    key: 'quarter',
    name: '季度会员',
    price: '6.9',
    originalPrice: '8.7',
    unit: '/季',
    badge: '最受欢迎 · 省1.8元',
    features: ['持仓今日收益实时预估', '持仓多端多设备同步', '高频行情极速通道', '客服专属优先支持'],
  },
  {
    key: 'year',
    name: '年度会员',
    price: '19.9',
    originalPrice: '34.8',
    unit: '/年',
    badge: '超值特惠 · 5.7折',
    features: ['涵盖所有VIP特权', '预留未来截图识别OCR', '整年无间断盘中估算', '专属1对1客服支持'],
  },
]

export function VipRechargeModal({ open, onClose, onContactCustomerService }: VipRechargeModalProps) {
  const { username } = useAuth()
  const [payMethod, setPayMethod] = useState<'wechat' | 'alipay'>('wechat')
  const [selectedPlan, setSelectedPlan] = useState<string>('quarter')
  const [vipConfig, setVipConfig] = useState<VipConfigView | null>(null)

  // 打开时动态加载后台配置的 VIP 价格与收款码
  useEffect(() => {
    if (open) {
      api.getVipConfig()
        .then((cfg) => {
          if (cfg) setVipConfig(cfg)
        })
        .catch(() => {
          // 降级使用默认值
        })
    }
  }, [open])

  // 根据后台配置动态生成套餐价格
  const plans = DEFAULT_PLANS.map((p) => {
    if (!vipConfig) return p
    if (p.key === 'month') {
      return { ...p, price: String(vipConfig.monthlyPrice) }
    }
    if (p.key === 'quarter') {
      return {
        ...p,
        price: String(vipConfig.quarterlyPrice),
        originalPrice: vipConfig.quarterlyOrigPrice ? String(vipConfig.quarterlyOrigPrice) : p.originalPrice,
      }
    }
    if (p.key === 'year') {
      return {
        ...p,
        price: String(vipConfig.yearlyPrice),
        originalPrice: vipConfig.yearlyOrigPrice ? String(vipConfig.yearlyOrigPrice) : p.originalPrice,
      }
    }
    return p
  })

  const currentPlan = plans.find((p) => p.key === selectedPlan) ?? plans[1]
  const payeeName = vipConfig?.payeeName || '管理员'
  const paymentTip = vipConfig?.paymentTip || '付款请务必备注用户名'

  const copyText = (text: string, label: string) => {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(
        () => message.success(`${label}已复制到剪贴板`),
        () => fallbackCopy(text, label)
      )
    } else {
      fallbackCopy(text, label)
    }
  }

  const fallbackCopy = (text: string, label: string) => {
    try {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      message.success(`${label}已复制到剪贴板`)
    } catch {
      message.info(`请手动复制: ${text}`)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 16 }}>
          <div
            style={{
              width: 30,
              height: 30,
              borderRadius: 8,
              background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 6px rgba(217, 119, 6, 0.3)',
            }}
          >
            <CrownOutlined style={{ color: '#ffffff', fontSize: 16 }} />
          </div>
          <span style={{ fontWeight: 700, color: '#0f172a' }}>开通 VIP 会员 · 解锁持仓估值</span>
        </div>
      }
      open={open}
      onCancel={onClose}
      footer={[
        <Button
          key="contact"
          icon={<CustomerServiceOutlined />}
          onClick={() => {
            onClose()
            onContactCustomerService?.()
          }}
          style={{ borderRadius: 8 }}
        >
          联系客服咨询
        </Button>,
        <Button
          key="done"
          type="primary"
          onClick={() => {
            message.success('支付后请联系管理员或稍候，系统将为您快速开通')
            onClose()
          }}
          style={{
            borderRadius: 8,
            background: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)',
            borderColor: '#b45309',
            fontWeight: 600,
          }}
        >
          我已完成支付
        </Button>,
      ]}
      width={520}
      centered
      destroyOnClose
    >
      <div style={{ padding: '6px 0' }}>
        {/* 套餐选择卡片 (动态渲染后台价格) */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 10, marginBottom: 16 }}>
          {plans.map((plan) => {
            const isSelected = plan.key === selectedPlan
            return (
              <div
                key={plan.key}
                onClick={() => setSelectedPlan(plan.key)}
                style={{
                  border: isSelected ? '2px solid #d97706' : '1px solid #e2e8f0',
                  borderRadius: 12,
                  padding: '12px 10px',
                  background: isSelected ? '#fffbeb' : '#ffffff',
                  cursor: 'pointer',
                  position: 'relative',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  transition: 'all 0.2s',
                  boxShadow: isSelected ? '0 4px 12px rgba(217, 119, 6, 0.12)' : 'none',
                }}
              >
                {plan.badge && (
                  <div
                    style={{
                      position: 'absolute',
                      top: -10,
                      background: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
                      color: '#ffffff',
                      fontSize: 10,
                      fontWeight: 700,
                      padding: '1px 6px',
                      borderRadius: 10,
                      whiteSpace: 'nowrap',
                      boxShadow: '0 2px 4px rgba(220, 38, 38, 0.25)',
                    }}
                  >
                    {plan.badge}
                  </div>
                )}
                <div style={{ fontSize: 13, fontWeight: 650, color: '#334155', marginTop: plan.badge ? 2 : 0 }}>
                  {plan.name}
                </div>
                <div style={{ marginTop: 6, display: 'flex', alignItems: 'baseline', color: '#b45309' }}>
                  <span style={{ fontSize: 12, fontWeight: 600 }}>¥</span>
                  <span style={{ fontSize: 22, fontWeight: 800, lineHeight: 1 }}>{plan.price}</span>
                  <span style={{ fontSize: 11, color: '#94a3b8' }}>{plan.unit}</span>
                </div>
                {plan.originalPrice && (
                  <div style={{ fontSize: 11, color: '#94a3b8', textDecoration: 'line-through', marginTop: 2 }}>
                    ¥{plan.originalPrice}
                  </div>
                )}
              </div>
            )
          })}
        </div>

        {/* 支付方式切换 */}
        <div style={{ marginBottom: 14 }}>
          <Segmented
            block
            value={payMethod}
            onChange={(val) => setPayMethod(val as 'wechat' | 'alipay')}
            options={[
              {
                label: (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, padding: '4px 0' }}>
                    <WechatOutlined style={{ color: '#16a34a', fontSize: 16 }} />
                    <span style={{ fontWeight: 600 }}>微信支付</span>
                  </div>
                ),
                value: 'wechat',
              },
              {
                label: (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, padding: '4px 0' }}>
                    <AlipayCircleOutlined style={{ color: '#0284c7', fontSize: 16 }} />
                    <span style={{ fontWeight: 600 }}>支付宝</span>
                  </div>
                ),
                value: 'alipay',
              },
            ]}
          />
        </div>

        {/* 二维码展示区域 (动态加载后台配置的收款码) */}
        <div
          style={{
            border: '1px solid #e2e8f0',
            borderRadius: 14,
            padding: '16px',
            textAlign: 'center',
            background: payMethod === 'wechat' ? '#f0fdf4' : '#f0f9ff',
            marginBottom: 14,
          }}
        >
          <div style={{ marginBottom: 10, fontSize: 13, color: '#475569' }}>
            请使用 <b style={{ color: payMethod === 'wechat' ? '#15803d' : '#0369a1' }}>{payMethod === 'wechat' ? '微信扫一扫' : '支付宝扫一扫'}</b> 支付{' '}
            <b style={{ color: '#b45309', fontSize: 16 }}>¥{currentPlan.price}</b>
          </div>

          {/* 收款二维码卡片: 若后台配置了二维码图片则渲染图片，否则优雅降级 */}
          <div
            style={{
              width: 175,
              height: 175,
              margin: '0 auto',
              background: '#ffffff',
              borderRadius: 12,
              border: `2px solid ${payMethod === 'wechat' ? '#22c55e' : '#38bdf8'}`,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 6,
              boxShadow: '0 4px 12px rgba(0,0,0,0.06)',
              overflow: 'hidden',
            }}
          >
            {payMethod === 'wechat' ? (
              <img
                src={vipConfig?.wechatQrUrl || '/wechat-pay.png'}
                alt="微信收款码"
                style={{ width: '100%', height: '100%', objectFit: 'contain', borderRadius: 8 }}
                onError={(e) => {
                  // 图片加载失败时优雅降级
                  (e.target as HTMLElement).style.display = 'none'
                }}
              />
            ) : (
              <img
                src={vipConfig?.alipayQrUrl || '/alipay.jpg'}
                alt="支付宝收款码"
                style={{ width: '100%', height: '100%', objectFit: 'contain', borderRadius: 8 }}
                onError={(e) => {
                  (e.target as HTMLElement).style.display = 'none'
                }}
              />
            )}
          </div>

          {/* 备注提示卡片 */}
          <div
            style={{
              marginTop: 12,
              backgroundColor: '#fffbeb',
              border: '1px solid #fef3c7',
              borderRadius: 8,
              padding: '8px 12px',
              textAlign: 'left',
              fontSize: 12,
              color: '#92400e',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span>
                <b>⚠️ {paymentTip}：</b>
                <Text code copyable={false} style={{ color: '#b45309', fontWeight: 700, fontSize: 13 }}>
                  {username}
                </Text>
              </span>
              <Button
                size="small"
                type="link"
                icon={<CopyOutlined />}
                style={{ padding: 0, height: 'auto', color: '#b45309' }}
                onClick={() => copyText(username, '用户名')}
              >
                复制用户名
              </Button>
            </div>
            <div style={{ fontSize: 11, color: '#b45309', marginTop: 3 }}>
              收款人【{payeeName}】核对付款备注后将立即为您开通对应时长的 VIP 会员。
            </div>
          </div>
        </div>

        {/* 会员权益明细 */}
        <div style={{ background: '#f8fafc', padding: '10px 14px', borderRadius: 10, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 6 }}>
            尊享 VIP 会员特权：
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 6 }}>
            {currentPlan.features.map((feat, idx) => (
              <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#334155' }}>
                <CheckCircleFilled style={{ color: '#16a34a', fontSize: 12 }} />
                <span>{feat}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Modal>
  )
}
