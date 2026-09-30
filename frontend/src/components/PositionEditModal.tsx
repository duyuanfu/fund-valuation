import { useState, useEffect } from 'react'
import { Modal, Form, Input, InputNumber, Typography, message, Alert, Spin } from 'antd'
import {
  WalletOutlined,
  DollarOutlined,
  PercentageOutlined,
  SearchOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import { api } from '../api'
import type { PositionItemView, PositionSaveRequest } from '../api/types'

const { Text } = Typography

interface PositionEditModalProps {
  open: boolean
  editingItem?: PositionItemView | null
  onClose: () => void
  onSuccess: () => void
}

export function PositionEditModal({ open, editingItem, onClose, onSuccess }: PositionEditModalProps) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [fundLoading, setFundLoading] = useState(false)
  const [fundInfo, setFundInfo] = useState<{ name: string; prevNav?: number | null } | null>(null)
  const [computedCost, setComputedCost] = useState<number | null>(null)

  // 当打开弹窗或切换编辑项时初始化表单
  useEffect(() => {
    if (open) {
      if (editingItem) {
        form.setFieldsValue({
          fundCode: editingItem.fundCode,
          holdingAmount: editingItem.holdingAmount,
          yesterdayIncome: editingItem.yesterdayIncome ?? 0,
          holdingProfit: editingItem.holdingProfit,
          holdingProfitRate: editingItem.holdingProfitRate,
        })
        setFundInfo({ name: editingItem.fundName, prevNav: editingItem.prevNav })
        setComputedCost(editingItem.costAmount)
      } else {
        form.resetFields()
        setFundInfo(null)
        setComputedCost(null)
      }
    }
  }, [open, editingItem, form])

  // 基金代码输入失焦或回车时，自动查询基金名称与昨日净值
  const handleFundCodeBlur = async () => {
    const code = form.getFieldValue('fundCode')?.trim()
    if (!code || code.length < 6) return
    setFundLoading(true)
    try {
      const res = await api.fundDetail(code)
      if (res && res.estimate) {
        setFundInfo({
          name: res.estimate.fundName,
          prevNav: res.estimate.prevNav,
        })
      }
    } catch {
      // 容错处理
    } finally {
      setFundLoading(false)
    }
  }

  // 智能双向联动计算
  const handleProfitChange = (val: number | null) => {
    const amount = form.getFieldValue('holdingAmount')
    if (amount != null && val != null) {
      const cost = amount - val
      setComputedCost(cost)
      if (cost > 0) {
        const rate = Number(((val / cost) * 100).toFixed(2))
        form.setFieldValue('holdingProfitRate', rate)
      }
    }
  }

  const handleProfitRateChange = (rateVal: number | null) => {
    const amount = form.getFieldValue('holdingAmount')
    if (amount != null && rateVal != null) {
      const cost = amount / (1 + rateVal / 100)
      const profit = Number((amount - cost).toFixed(2))
      setComputedCost(Number(cost.toFixed(2)))
      form.setFieldValue('holdingProfit', profit)
    }
  }

  const handleAmountChange = (amountVal: number | null) => {
    const profit = form.getFieldValue('holdingProfit')
    if (amountVal != null && profit != null) {
      const cost = amountVal - profit
      setComputedCost(cost)
      if (cost > 0) {
        const rate = Number(((profit / cost) * 100).toFixed(2))
        form.setFieldValue('holdingProfitRate', rate)
      }
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      const payload: PositionSaveRequest = {
        fundCode: values.fundCode.trim(),
        holdingAmount: values.holdingAmount,
        yesterdayIncome: values.yesterdayIncome ?? 0,
        holdingProfit: values.holdingProfit ?? 0,
        holdingProfitRate: values.holdingProfitRate ?? null,
      }
      await api.savePosition(payload)
      message.success(editingItem ? '持仓已成功更新' : '持仓基金添加成功')
      onSuccess()
      onClose()
    } catch (e: any) {
      if (e?.error) {
        message.error(e.error)
      } else if (e?.message) {
        message.error(e.message)
      }
    } finally {
      setLoading(false)
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
              background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 6px rgba(37, 99, 235, 0.25)',
            }}
          >
            <WalletOutlined style={{ color: '#ffffff', fontSize: 15 }} />
          </div>
          <span style={{ fontWeight: 650, color: '#0f172a' }}>
            {editingItem ? '编辑基金持仓' : '录入支付宝基金持仓'}
          </span>
        </div>
      }
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={loading}
      okText={editingItem ? '保存修改' : '确认添加'}
      cancelText="取消"
      width={480}
      centered
      destroyOnClose
    >
      <div style={{ padding: '8px 0' }}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16, fontSize: 12, borderRadius: 8 }}
          message={
            <span>
              对标<b>支付宝基金持有卡片</b>4核心字段。输入金额与收益即可自动联动推算，盘中实时测算今日收益。
            </span>
          }
        />

        <Form form={form} layout="vertical" requiredMark="optional">
          {/* 基金代码 */}
          <Form.Item
            name="fundCode"
            label={<span style={{ fontWeight: 600 }}>基金代码 (6位)</span>}
            rules={[
              { required: true, message: '请输入6位基金代码' },
              { pattern: /^[0-9]{6}$/, message: '请输入标准6位纯数字基金代码' },
            ]}
          >
            <Input
              placeholder="例如: 110022"
              maxLength={6}
              disabled={!!editingItem}
              onBlur={handleFundCodeBlur}
              prefix={<SearchOutlined style={{ color: '#94a3b8' }} />}
              suffix={fundLoading ? <Spin size="small" /> : null}
              style={{ borderRadius: 8, fontSize: 14 }}
            />
          </Form.Item>

          {/* 自动解析出的基金名称展示 */}
          {fundInfo && (
            <div
              style={{
                marginTop: -10,
                marginBottom: 14,
                padding: '6px 10px',
                background: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: 8,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                fontSize: 12,
              }}
            >
              <span style={{ color: '#0f172a', fontWeight: 600 }}>{fundInfo.name}</span>
              {fundInfo.prevNav && (
                <span style={{ color: '#64748b' }}>最新净值: {Number(fundInfo.prevNav).toFixed(4)}</span>
              )}
            </div>
          )}

          {/* 持有金额 (对标支付宝“金额”) */}
          <Form.Item
            name="holdingAmount"
            label={<span style={{ fontWeight: 600 }}>持有金额 (资产总额/元)</span>}
            rules={[
              { required: true, message: '请输入持有总金额' },
              { type: 'number', min: 0.01, message: '金额必须大于0' },
            ]}
          >
            <InputNumber
              style={{ width: '100%', borderRadius: 8 }}
              placeholder="例如: 10000.00"
              precision={2}
              min={0.01}
              prefix={<DollarOutlined style={{ color: '#94a3b8' }} />}
              onChange={handleAmountChange}
            />
          </Form.Item>

          {/* 昨日收益 (对标支付宝“昨日收益”) */}
          <Form.Item
            name="yesterdayIncome"
            label={
              <span style={{ fontWeight: 600 }}>
                昨日收益 (元，选填)
                <Text type="secondary" style={{ fontSize: 12, fontWeight: 400, marginLeft: 6 }}>
                  (支付宝官方昨日已确认收益)
                </Text>
              </span>
            }
          >
            <InputNumber
              style={{ width: '100%', borderRadius: 8 }}
              placeholder="例如: +35.20 或 -12.50 (可填0)"
              precision={2}
              prefix="¥"
            />
          </Form.Item>

          {/* 持有收益 与 持有收益率 双向联动 */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
            <Form.Item
              name="holdingProfit"
              label={<span style={{ fontWeight: 600 }}>持有收益 (累计/元)</span>}
            >
              <InputNumber
                style={{ width: '100%', borderRadius: 8 }}
                placeholder="例如: 500.00"
                precision={2}
                prefix="¥"
                onChange={handleProfitChange}
              />
            </Form.Item>

            <Form.Item
              name="holdingProfitRate"
              label={<span style={{ fontWeight: 600 }}>持有收益率 (%)</span>}
            >
              <InputNumber
                style={{ width: '100%', borderRadius: 8 }}
                placeholder="例如: 5.26"
                precision={2}
                prefix={<PercentageOutlined style={{ color: '#94a3b8' }} />}
                onChange={handleProfitRateChange}
              />
            </Form.Item>
          </div>

          {/* 自动推算的持仓成本提示 */}
          {computedCost !== null && (
            <div
              style={{
                marginTop: -6,
                marginBottom: 6,
                fontSize: 12,
                color: '#64748b',
                display: 'flex',
                alignItems: 'center',
                gap: 4,
              }}
            >
              <InfoCircleOutlined style={{ color: '#2563eb' }} />
              <span>
                自动推算持仓成本约为: <b style={{ color: '#0f172a' }}>¥{computedCost.toFixed(2)}</b>
              </span>
            </div>
          )}
        </Form>
      </div>
    </Modal>
  )
}
