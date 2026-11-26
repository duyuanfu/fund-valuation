import { useState, useEffect } from 'react'
import {
  Modal,
  Form,
  Input,
  Switch,
  Radio,
  Button,
  Space,
  message,
  Card,
  Tag,
  TimePicker,
} from 'antd'
import {
  BellOutlined,
  MailOutlined,
  WechatOutlined,
  SendOutlined,
  SafetyOutlined,
  LineChartOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { api } from '../api'
import type { NotificationChannelView } from '../api/types'

interface NotificationChannelModalProps {
  open: boolean
  onClose: () => void
  onSuccess?: (email: string) => void
}

const INTRADAY_PRESET_TIMES = [
  { label: '14:30 (推荐·尾盘决策)', value: '14:30' },
  { label: '14:15', value: '14:15' },
  { label: '14:45', value: '14:45' },
]

const EVENING_PRESET_TIMES = [
  { label: '21:30 (推荐·官方收益)', value: '21:30' },
  { label: '21:00', value: '21:00' },
  { label: '22:00', value: '22:00' },
]

export function NotificationChannelModal({ open, onClose, onSuccess }: NotificationChannelModalProps) {
  const [form] = Form.useForm<NotificationChannelView>()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [testingEmail, setTestingEmail] = useState(false)
  const [testingServerchan, setTestingServerchan] = useState(false)
  const [selectedIntradayTime, setSelectedIntradayTime] = useState<string>('14:30')
  const [selectedEveningTime, setSelectedEveningTime] = useState<string>('21:30')

  const loadChannel = async () => {
    setLoading(true)
    try {
      const data = await api.getNotificationChannel()
      if (data) {
        const intra = data.remindTime && !data.remindTime.startsWith('2') ? data.remindTime : '14:30'
        const eve = data.eveningRemindTime || '21:30'
        setSelectedIntradayTime(intra)
        setSelectedEveningTime(eve)
        form.setFieldsValue({
          email: data.email || '',
          emailEnabled: data.emailEnabled ?? true,
          wechatChannelType: 'serverchan',
          serverchanKey: data.serverchanKey || '',
          wechatEnabled: data.wechatEnabled ?? true,
          remindEnabled: data.remindEnabled ?? true,
          remindTime: intra,
          eveningRemindEnabled: data.eveningRemindEnabled ?? true,
          eveningRemindTime: eve,
          pushScope: data.pushScope || 'ALL_WATCHLIST',
          hideAmount: data.hideAmount ?? false,
        })
      }
    } catch {
      // 容错静默
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (open) {
      loadChannel()
    }
  }, [open])

  // 盘中时段限制 (09:30-11:30, 13:00-15:00)
  const disabledIntradayTime = () => ({
    disabledHours: () => {
      const allowed = [9, 10, 11, 13, 14, 15]
      const disabled: number[] = []
      for (let i = 0; i < 24; i++) {
        if (!allowed.includes(i)) disabled.push(i)
      }
      return disabled
    },
    disabledMinutes: (selectedHour: number) => {
      const disabled: number[] = []
      if (selectedHour === 9) {
        for (let m = 0; m < 30; m++) disabled.push(m)
      } else if (selectedHour === 11) {
        for (let m = 31; m < 60; m++) disabled.push(m)
      } else if (selectedHour === 15) {
        for (let m = 1; m < 60; m++) disabled.push(m)
      }
      return disabled
    },
  })

  // 盘后时段限制 (20:00-23:00)
  const disabledEveningTime = () => ({
    disabledHours: () => {
      const allowed = [20, 21, 22, 23]
      const disabled: number[] = []
      for (let i = 0; i < 24; i++) {
        if (!allowed.includes(i)) disabled.push(i)
      }
      return disabled
    },
  })

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      await api.updateNotificationChannel({
        ...values,
        remindTime: selectedIntradayTime,
        eveningRemindTime: selectedEveningTime,
      })
      message.success('提醒设置已保存！已为您独立配置盘中与盘后收益推送。')
      if (onSuccess && values.email) {
        onSuccess(values.email.trim())
      }
      onClose()
    } catch (e: any) {
      if (e?.errorFields) {
        message.error(e.errorFields[0]?.errors[0] || '请完善表单输入')
      } else {
        message.error(e?.message || '保存失败，请检查输入项')
      }
    } finally {
      setSaving(false)
    }
  }

  const handleTestEmail = async () => {
    const emailVal = form.getFieldValue('email')
    if (!emailVal || !emailVal.trim()) {
      message.warning('当前尚未绑定邮箱，请先前往右上角个人中心绑定后再进行测试')
      return
    }
    const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/
    if (!emailRegex.test(emailVal.trim())) {
      message.error('请输入有效的电子邮箱格式')
      return
    }

    setTestingEmail(true)
    try {
      const currentValues = form.getFieldsValue()
      await api.updateNotificationChannel({
        ...currentValues,
        remindTime: selectedIntradayTime,
        eveningRemindTime: selectedEveningTime,
      }).catch(() => {})
      const res = await api.testNotificationEmail(emailVal.trim())
      message.success(res.message || '测试邮件已发出，请检查收件箱或垃圾箱！')
    } catch (e: any) {
      message.error(e?.message || '测试邮件发送失败')
    } finally {
      setTestingEmail(false)
    }
  }

  const handleTestServerchan = async () => {
    const keyVal = form.getFieldValue('serverchanKey')
    if (!keyVal || !keyVal.trim()) {
      message.warning('请先输入 Server酱 SendKey（以 SCT 开头）')
      return
    }
    setTestingServerchan(true)
    try {
      const currentValues = form.getFieldsValue()
      await api.updateNotificationChannel({
        ...currentValues,
        remindTime: selectedIntradayTime,
        eveningRemindTime: selectedEveningTime,
      }).catch(() => {})
      const res = await api.testNotificationServerchan(keyVal.trim())
      message.success(res.message || 'Server酱测试消息已发出，请在微信中查收！')
    } catch (e: any) {
      message.error(e?.message || 'Server酱推送失败，请检查 SendKey 是否正确')
    } finally {
      setTestingServerchan(false)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 16 }}>
          <BellOutlined style={{ color: '#2563eb', fontSize: 18 }} />
          <span style={{ fontWeight: 650, color: '#0f172a' }}>消息与收益提醒设置</span>
          <Tag color="blue" style={{ marginLeft: 4, fontSize: 11, borderRadius: 4, fontWeight: 500 }}>
            盘中 / 盘后独立推送
          </Tag>
        </div>
      }
      open={open}
      onCancel={onClose}
      width={560}
      centered
      destroyOnClose
      footer={[
        <Button key="cancel" onClick={onClose} style={{ borderRadius: 8 }}>
          取消
        </Button>,
        <Button
          key="save"
          type="primary"
          loading={saving}
          onClick={handleSave}
          style={{
            borderRadius: 8,
            background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
            fontWeight: 600,
          }}
        >
          保存设置
        </Button>,
      ]}
    >
      <div style={{ padding: '4px 0', maxHeight: '72vh', overflowY: 'auto' }}>
        <Form form={form} layout="vertical" disabled={loading}>
          {/* 1. 交易日盘中提醒 (14:30 尾盘决策快报) */}
          <Card
            size="small"
            style={{
              borderRadius: 12,
              background: '#f8fafc',
              marginBottom: 12,
              border: '1px solid #e2e8f0',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
              <div>
                <div style={{ fontWeight: 650, color: '#0f172a', fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}>
                  <LineChartOutlined style={{ color: '#2563eb' }} />
                  <span>交易日盘中提醒 (实时估值)</span>
                </div>
                <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                  15:00 闭市前半小时发送盘中预估快报，辅助申赎调仓决策
                </div>
              </div>
              <Form.Item name="remindEnabled" valuePropName="checked" noStyle>
                <Switch checkedChildren="已开启" unCheckedChildren="已关闭" />
              </Form.Item>
            </div>

            <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: 8 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                <span style={{ fontSize: 12, fontWeight: 600, color: '#334155' }}>盘中提醒送达时间</span>
                <span style={{ fontSize: 11, color: '#2563eb', background: '#eff6ff', padding: '1px 6px', borderRadius: 4 }}>
                  限交易时段 (推荐 14:30)
                </span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                <TimePicker
                  value={selectedIntradayTime ? dayjs(selectedIntradayTime, 'HH:mm') : dayjs('14:30', 'HH:mm')}
                  format="HH:mm"
                  minuteStep={5}
                  disabledTime={disabledIntradayTime}
                  allowClear={false}
                  showNow={false}
                  onChange={(_, str) => {
                    if (str) {
                      setSelectedIntradayTime(str as string)
                      form.setFieldValue('remindTime', str as string)
                    }
                  }}
                  style={{
                    width: 105,
                    borderRadius: 8,
                    fontWeight: 650,
                    fontSize: 13,
                    borderColor: '#2563eb',
                  }}
                />
                <div style={{ display: 'flex', gap: 6 }}>
                  {INTRADAY_PRESET_TIMES.map((opt) => {
                    const isSelected = selectedIntradayTime === opt.value
                    return (
                      <Button
                        key={opt.value}
                        size="small"
                        type={isSelected ? 'primary' : 'default'}
                        onClick={() => {
                          setSelectedIntradayTime(opt.value)
                          form.setFieldValue('remindTime', opt.value)
                        }}
                        style={{
                          borderRadius: 6,
                          fontSize: 12,
                          height: 28,
                          fontWeight: isSelected ? 600 : 400,
                          ...(isSelected
                            ? { background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)' }
                            : { borderColor: '#cbd5e1' }),
                        }}
                      >
                        {opt.label}
                      </Button>
                    )
                  })}
                </div>
              </div>
            </div>
          </Card>

          {/* 2. 交易日盘后收益提醒 (21:30 官方净值确认) */}
          <Card
            size="small"
            style={{
              borderRadius: 12,
              background: '#f8fafc',
              marginBottom: 12,
              border: '1px solid #e2e8f0',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
              <div>
                <div style={{ fontWeight: 650, color: '#0f172a', fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}>
                  <CheckCircleOutlined style={{ color: '#059669' }} />
                  <span>交易日盘后收益提醒 (官方确认)</span>
                </div>
                <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                  官方收盘净值确认出炉后发送，汇总当日实际收益与持仓累计盈亏
                </div>
              </div>
              <Form.Item name="eveningRemindEnabled" valuePropName="checked" noStyle>
                <Switch checkedChildren="已开启" unCheckedChildren="已关闭" />
              </Form.Item>
            </div>

            <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: 8 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                <span style={{ fontSize: 12, fontWeight: 600, color: '#334155' }}>盘后收益提醒送达时间</span>
                <span style={{ fontSize: 11, color: '#059669', background: '#ecfdf5', padding: '1px 6px', borderRadius: 4 }}>
                  限晚间时段 (推荐 21:30)
                </span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                <TimePicker
                  value={selectedEveningTime ? dayjs(selectedEveningTime, 'HH:mm') : dayjs('21:30', 'HH:mm')}
                  format="HH:mm"
                  minuteStep={5}
                  disabledTime={disabledEveningTime}
                  allowClear={false}
                  showNow={false}
                  onChange={(_, str) => {
                    if (str) {
                      setSelectedEveningTime(str as string)
                      form.setFieldValue('eveningRemindTime', str as string)
                    }
                  }}
                  style={{
                    width: 105,
                    borderRadius: 8,
                    fontWeight: 650,
                    fontSize: 13,
                    borderColor: '#059669',
                  }}
                />
                <div style={{ display: 'flex', gap: 6 }}>
                  {EVENING_PRESET_TIMES.map((opt) => {
                    const isSelected = selectedEveningTime === opt.value
                    return (
                      <Button
                        key={opt.value}
                        size="small"
                        type={isSelected ? 'primary' : 'default'}
                        onClick={() => {
                          setSelectedEveningTime(opt.value)
                          form.setFieldValue('eveningRemindTime', opt.value)
                        }}
                        style={{
                          borderRadius: 6,
                          fontSize: 12,
                          height: 28,
                          fontWeight: isSelected ? 600 : 400,
                          ...(isSelected
                            ? { background: 'linear-gradient(135deg, #059669 0%, #047857 100%)' }
                            : { borderColor: '#cbd5e1' }),
                        }}
                      >
                        {opt.label}
                      </Button>
                    )
                  })}
                </div>
              </div>
            </div>
          </Card>

          {/* 3. 渠道 1: 电子邮箱 (我的绑定邮箱) */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Space size={6}>
                  <MailOutlined style={{ color: '#2563eb', fontSize: 14 }} />
                  <span style={{ fontWeight: 650, fontSize: 13 }}>电子邮箱 (我的绑定邮箱)</span>
                </Space>
                <Form.Item name="emailEnabled" valuePropName="checked" noStyle>
                  <Switch size="small" checkedChildren="开启" unCheckedChildren="关闭" />
                </Form.Item>
              </div>
            }
            size="small"
            style={{ borderRadius: 12, marginBottom: 12, border: '1px solid #f1f5f9' }}
          >
            <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
              <Form.Item
                name="email"
                style={{ flex: 1, marginBottom: 0 }}
              >
                <Input
                  disabled
                  placeholder="未绑定邮箱，请前往个人中心绑定"
                  prefix={<MailOutlined style={{ color: '#94a3b8' }} />}
                  style={{
                    borderRadius: 8,
                    backgroundColor: '#f8fafc',
                    color: '#334155',
                    cursor: 'not-allowed',
                  }}
                />
              </Form.Item>
              <Button
                size="middle"
                icon={<SendOutlined />}
                loading={testingEmail}
                onClick={handleTestEmail}
                style={{ borderRadius: 8, flexShrink: 0 }}
              >
                发送测试
              </Button>
            </div>
            <div style={{ marginTop: 8, fontSize: 12, color: '#64748b', lineHeight: 1.4 }}>
              * 电子邮箱不能在此修改，只能前往右上角<b>个人中心</b>邮箱处修改。
            </div>
          </Card>

          {/* 4. 渠道 2: 微信推送 (Server酱) */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Space size={6}>
                  <WechatOutlined style={{ color: '#16a34a', fontSize: 14 }} />
                  <span style={{ fontWeight: 650, fontSize: 13 }}>微信推送 (Server酱)</span>
                </Space>
                <Form.Item name="wechatEnabled" valuePropName="checked" noStyle>
                  <Switch size="small" checkedChildren="开启" unCheckedChildren="关闭" />
                </Form.Item>
              </div>
            }
            size="small"
            style={{ borderRadius: 12, marginBottom: 12, border: '1px solid #f1f5f9' }}
          >
            <div style={{ background: '#f0fdf4', padding: '10px 12px', borderRadius: 10, border: '1px solid #bbf7d0' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                <div style={{ fontSize: 12, fontWeight: 650, color: '#166534' }}>
                  Server酱微信推送配置指引
                </div>
                <a
                  href="https://sct.ftqq.com/login/"
                  target="_blank"
                  rel="noreferrer"
                  style={{ fontSize: 12, fontWeight: 600, color: '#2563eb' }}
                >
                  前往登录获取 SendKey ↗
                </a>
              </div>
              <div style={{ fontSize: 12, color: '#15803d', lineHeight: 1.6, marginBottom: 10 }}>
                1. 手机微信打开并扫码登录{' '}
                <a href="https://sct.ftqq.com/login/" target="_blank" rel="noreferrer" style={{ fontWeight: 600, color: '#166534', textDecoration: 'underline' }}>
                  https://sct.ftqq.com/login/
                </a>
                ；<br />
                2. 关注官方服务号，在「Key&API」页面复制您的 <b>SendKey</b>（以 <code>SCT</code> 开头）；<br />
                3. 粘贴至下方输入框，点击测试即可在微信接收提醒。
              </div>

              <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
                <Form.Item
                  name="serverchanKey"
                  style={{ flex: 1, marginBottom: 0 }}
                >
                  <Input placeholder="输入 Server酱 SendKey (选填，留空不开启)" allowClear style={{ borderRadius: 8 }} />
                </Form.Item>
                <Button
                  size="middle"
                  icon={<SendOutlined />}
                  loading={testingServerchan}
                  onClick={handleTestServerchan}
                  style={{ borderRadius: 8, flexShrink: 0 }}
                >
                  发送测试
                </Button>
              </div>
            </div>
          </Card>

          {/* 5. 个性化偏好 */}
          <Card title="个性化推送偏好" size="small" style={{ borderRadius: 12, border: '1px solid #f1f5f9' }}>
            <Form.Item name="pushScope" label={<span style={{ fontSize: 13, color: '#334155' }}>推送内容范围</span>} style={{ marginBottom: 12 }}>
              <Radio.Group>
                <Radio value="ALL_WATCHLIST">自选与持仓全部推送</Radio>
                <Radio value="PORTFOLIO_ONLY">仅推送实际持仓基金 (VIP)</Radio>
              </Radio.Group>
            </Form.Item>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: 6, borderTop: '1px solid #f1f5f9' }}>
              <div>
                <div style={{ fontWeight: 600, fontSize: 13, color: '#334155' }}>
                  <SafetyOutlined style={{ marginRight: 6, color: '#64748b' }} />
                  隐藏具体资产金额 (隐私保护模式)
                </div>
                <div style={{ fontSize: 12, color: '#64748b' }}>
                  开启后在邮件和通知中仅显示百分比涨跌幅，不显示持仓总额与盈亏具体金额
                </div>
              </div>
              <Form.Item name="hideAmount" valuePropName="checked" noStyle>
                <Switch />
              </Form.Item>
            </div>
          </Card>
        </Form>
      </div>
    </Modal>
  )
}
