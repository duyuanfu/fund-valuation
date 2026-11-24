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
  Segmented,
} from 'antd'
import {
  BellOutlined,
  MailOutlined,
  WechatOutlined,
  SendOutlined,
  SafetyOutlined,
  AppleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons'
import { api } from '../api'
import type { NotificationChannelView } from '../api/types'

interface NotificationChannelModalProps {
  open: boolean
  onClose: () => void
}

const REMIND_TIME_OPTIONS = [
  { label: '14:00 (提前看盘)', value: '14:00' },
  { label: '14:15 (中盘异动)', value: '14:15' },
  { label: '14:30 (🔥推荐·黄金半小时)', value: '14:30' },
  { label: '14:45 (尾盘冲刺)', value: '14:45' },
  { label: '14:50 (最后10分钟)', value: '14:50' },
]

export function NotificationChannelModal({ open, onClose }: NotificationChannelModalProps) {
  const [form] = Form.useForm<NotificationChannelView>()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [testingEmail, setTestingEmail] = useState(false)
  const [testingServerchan, setTestingServerchan] = useState(false)
  const [testingBark, setTestingBark] = useState(false)
  const [channelType, setChannelType] = useState<'serverchan' | 'bark'>('serverchan')

  const loadChannel = async () => {
    setLoading(true)
    try {
      const data = await api.getNotificationChannel()
      if (data) {
        form.setFieldsValue({
          email: data.email || '',
          emailEnabled: data.emailEnabled ?? true,
          wechatChannelType: (data.wechatChannelType === 'bark' ? 'bark' : 'serverchan'),
          serverchanKey: data.serverchanKey || '',
          barkUrl: data.barkUrl || '',
          wechatEnabled: data.wechatEnabled ?? true,
          remindEnabled: data.remindEnabled ?? true,
          remindTime: data.remindTime || '14:30',
          pushScope: data.pushScope || 'ALL_WATCHLIST',
          hideAmount: data.hideAmount ?? false,
        })
        setChannelType(data.wechatChannelType === 'bark' ? 'bark' : 'serverchan')
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

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      await api.updateNotificationChannel(values)
      const timeStr = values.remindTime || '14:30'
      message.success(`提醒设置已成功保存！交易日 ${timeStr} 将准时为您推送。`)
      onClose()
    } catch (e: any) {
      if (e?.errorFields) {
        message.error(e.errorFields[0]?.errors[0] || '请完善表单必填项')
      } else {
        message.error(e?.message || '保存失败，请检查输入项')
      }
    } finally {
      setSaving(false)
    }
  }

  const handleTestEmail = async () => {
    try {
      await form.validateFields(['email'])
      const emailVal = form.getFieldValue('email')
      if (!emailVal || !emailVal.trim()) {
        message.warning('请先输入接收测试的电子邮箱地址')
        return
      }
      setTestingEmail(true)
      const currentValues = form.getFieldsValue()
      await api.updateNotificationChannel(currentValues).catch(() => {})
      const res = await api.testNotificationEmail(emailVal.trim())
      message.success(res.message || '测试邮件已发出，请检查收件箱或垃圾箱！')
    } catch (e: any) {
      if (e?.errorFields) {
        message.error(e.errorFields[0]?.errors[0] || '请输入有效的电子邮箱格式')
      } else {
        message.error(e?.message || '测试邮件发送失败')
      }
    } finally {
      setTestingEmail(false)
    }
  }

  const handleTestServerchan = async () => {
    try {
      await form.validateFields(['serverchanKey'])
      const keyVal = form.getFieldValue('serverchanKey')
      if (!keyVal || !keyVal.trim()) {
        message.warning('请先输入 Server酱 SendKey')
        return
      }
      setTestingServerchan(true)
      const currentValues = form.getFieldsValue()
      await api.updateNotificationChannel(currentValues).catch(() => {})
      const res = await api.testNotificationServerchan(keyVal.trim())
      message.success(res.message || 'Server酱测试消息已发出，请在微信中查收！')
    } catch (e: any) {
      if (e?.errorFields) {
        message.error(e.errorFields[0]?.errors[0] || '请输入 Server酱 SendKey')
      } else {
        message.error(e?.message || 'Server酱推送失败，请检查 SendKey 是否正确')
      }
    } finally {
      setTestingServerchan(false)
    }
  }

  const handleTestBark = async () => {
    try {
      await form.validateFields(['barkUrl'])
      const barkVal = form.getFieldValue('barkUrl')
      if (!barkVal || !barkVal.trim()) {
        message.warning('请先输入 Bark 推送 Key 或完整 URL')
        return
      }
      setTestingBark(true)
      const currentValues = form.getFieldsValue()
      await api.updateNotificationChannel(currentValues).catch(() => {})
      const res = await api.testNotificationBark(barkVal.trim())
      message.success(res.message || 'Bark 测试消息已发出，请在 iOS 设备中查收！')
    } catch (e: any) {
      if (e?.errorFields) {
        message.error(e.errorFields[0]?.errors[0] || '请输入 Bark 设备 Key 或链接')
      } else {
        message.error(e?.message || 'Bark 推送失败，请检查设备 Key 是否有效')
      }
    } finally {
      setTestingBark(false)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 16 }}>
          <div
            style={{
              width: 34,
              height: 34,
              borderRadius: 10,
              background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 8px rgba(37, 99, 235, 0.25)',
              flexShrink: 0,
            }}
          >
            <BellOutlined style={{ color: '#ffffff', fontSize: 16 }} />
          </div>
          <div>
            <div style={{ fontWeight: 700, color: '#0f172a', lineHeight: 1.2 }}>
              盘中提醒设置
              <Tag color="blue" style={{ marginLeft: 8, fontSize: 11, borderRadius: 6, fontWeight: 500 }}>
                交易日自动推送
              </Tag>
            </div>
            <div style={{ fontSize: 12, color: '#64748b', fontWeight: 400, marginTop: 2 }}>
              支持邮箱、微信 (Server酱) 与 iOS Bark，精准把握闭市前决策黄金期
            </div>
          </div>
        </div>
      }
      open={open}
      onCancel={onClose}
      width={580}
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
          保存提醒设置
        </Button>,
      ]}
    >
      <div style={{ padding: '4px 0', maxHeight: '72vh', overflowY: 'auto' }}>
        <Form form={form} layout="vertical" disabled={loading}>
          {/* 总开关与提醒时间 */}
          <Card
            size="small"
            style={{
              borderRadius: 12,
              background: '#f8fafc',
              marginBottom: 14,
              border: '1px solid #e2e8f0',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <div>
                <div style={{ fontWeight: 650, color: '#0f172a', fontSize: 14 }}>
                  开启交易日盘中提醒
                </div>
                <div style={{ fontSize: 12, color: '#64748b' }}>
                  法定休市日与节假日自动静默，不打扰休息
                </div>
              </div>
              <Form.Item name="remindEnabled" valuePropName="checked" noStyle>
                <Switch checkedChildren="已开启" unCheckedChildren="已关闭" />
              </Form.Item>
            </div>

            <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: 10 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8, fontSize: 13, fontWeight: 600, color: '#334155' }}>
                <ClockCircleOutlined style={{ color: '#2563eb' }} />
                <span>选择提醒送达时间</span>
                <span style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
                  (A股每日 15:00 申购/赎回确认截止)
                </span>
              </div>
              <Form.Item name="remindTime" noStyle initialValue="14:30">
                <Radio.Group style={{ width: '100%' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 8 }}>
                    {REMIND_TIME_OPTIONS.map((opt) => (
                      <Radio.Button
                        key={opt.value}
                        value={opt.value}
                        style={{
                          textAlign: 'center',
                          borderRadius: 8,
                          fontSize: 12,
                          height: 36,
                          lineHeight: '34px',
                        }}
                      >
                        {opt.label}
                      </Radio.Button>
                    ))}
                  </div>
                </Radio.Group>
              </Form.Item>
            </div>
          </Card>

          {/* 渠道 1: 电子邮箱 */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Space size={6}>
                  <MailOutlined style={{ color: '#2563eb', fontSize: 15 }} />
                  <span style={{ fontWeight: 650, fontSize: 14 }}>电子邮箱 (推荐 · 排版清晰精美)</span>
                </Space>
                <Form.Item name="emailEnabled" valuePropName="checked" noStyle>
                  <Switch size="small" checkedChildren="开启" unCheckedChildren="关闭" />
                </Form.Item>
              </div>
            }
            size="small"
            style={{ borderRadius: 12, marginBottom: 14, border: '1px solid #f1f5f9' }}
          >
            <Form.Item
              name="email"
              label={<span style={{ fontSize: 13, color: '#334155' }}>电子邮箱地址</span>}
              rules={[
                { type: 'email', message: '请输入有效的电子邮箱格式' },
                {
                  validator: (_, val) => {
                    const emailEnabled = form.getFieldValue('emailEnabled')
                    if (emailEnabled && (!val || !val.trim())) {
                      return Promise.reject(new Error('开启邮箱通知时请输入电子邮箱地址'))
                    }
                    return Promise.resolve()
                  },
                },
              ]}
              extra="支持 QQ邮箱、网易163、Gmail、Outlook 等任何常用邮箱，邮件客户端将准时送达卡片。"
              style={{ marginBottom: 10 }}
            >
              <Input
                placeholder="例如: yourname@163.com"
                allowClear
                prefix={<MailOutlined style={{ color: '#94a3b8' }} />}
                style={{ borderRadius: 8 }}
              />
            </Form.Item>

            <div style={{ textAlign: 'right' }}>
              <Button
                size="small"
                icon={<SendOutlined />}
                loading={testingEmail}
                onClick={handleTestEmail}
                style={{ borderRadius: 6 }}
              >
                发送测试邮件
              </Button>
            </div>
          </Card>

          {/* 渠道 2 & 3: 移动端 (Server酱微信 或 iOS Bark) */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Space size={6}>
                  <WechatOutlined style={{ color: '#16a34a', fontSize: 15 }} />
                  <span style={{ fontWeight: 650, fontSize: 14 }}>移动端推送 (微信 / iOS 原生通知)</span>
                </Space>
                <Form.Item name="wechatEnabled" valuePropName="checked" noStyle>
                  <Switch size="small" checkedChildren="开启" unCheckedChildren="关闭" />
                </Form.Item>
              </div>
            }
            size="small"
            style={{ borderRadius: 12, marginBottom: 14, border: '1px solid #f1f5f9' }}
          >
            <Form.Item name="wechatChannelType" label={<span style={{ fontSize: 13, color: '#334155' }}>选择移动端推送方式</span>} style={{ marginBottom: 12 }}>
              <Segmented
                block
                value={channelType}
                onChange={(val) => {
                  setChannelType(val as 'serverchan' | 'bark')
                  form.setFieldValue('wechatChannelType', val)
                }}
                options={[
                  {
                    label: (
                      <div style={{ padding: '3px 0' }}>
                        <WechatOutlined style={{ color: '#16a34a', marginRight: 5 }} />
                        <b>Server酱 (微信公众号接收)</b>
                      </div>
                    ),
                    value: 'serverchan',
                  },
                  {
                    label: (
                      <div style={{ padding: '3px 0' }}>
                        <AppleOutlined style={{ color: '#0f172a', marginRight: 5 }} />
                        <b>Bark (iOS 设备原生弹窗)</b>
                      </div>
                    ),
                    value: 'bark',
                  },
                ]}
              />
            </Form.Item>

            {channelType === 'serverchan' ? (
              <div style={{ background: '#f0fdf4', padding: '12px 14px', borderRadius: 10, border: '1px solid #bbf7d0', marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                  <div style={{ fontSize: 12, fontWeight: 700, color: '#166534' }}>
                    🟢 Server酱 微信推送配置指引：
                  </div>
                  <a
                    href="https://sct.ftqq.com"
                    target="_blank"
                    rel="noreferrer"
                    style={{ fontSize: 12, fontWeight: 600, color: '#2563eb' }}
                  >
                    前往 sct.ftqq.com ↗
                  </a>
                </div>
                <div style={{ fontSize: 12, color: '#15803d', lineHeight: 1.6 }}>
                  1. 手机微信打开并扫码登录 <a href="https://sct.ftqq.com" target="_blank" rel="noreferrer" style={{ fontWeight: 600, color: '#166534' }}>sct.ftqq.com</a>；<br />
                  2. 关注官方服务号，在「Key&API」页面复制您的 <b>SendKey</b>（以 <code>SCT</code> 开头）；<br />
                  3. 粘贴至下方输入框，点击测试，微信公众号将立即弹出提醒！
                </div>
                <Form.Item
                  name="serverchanKey"
                  label={<span style={{ fontSize: 13, color: '#166534', fontWeight: 600 }}>您的 Server酱 SendKey</span>}
                  rules={[
                    {
                      validator: (_, val) => {
                        const wechatEnabled = form.getFieldValue('wechatEnabled')
                        if (wechatEnabled && channelType === 'serverchan' && (!val || !val.trim())) {
                          return Promise.reject(new Error('开启微信通知时请输入 Server酱 SendKey'))
                        }
                        return Promise.resolve()
                      },
                    },
                  ]}
                  style={{ marginTop: 10, marginBottom: 0 }}
                >
                  <Input placeholder="例如: SCT123456Txxxxxxxxxxxxxxxx" allowClear style={{ borderRadius: 8 }} />
                </Form.Item>

                <div style={{ textAlign: 'right', marginTop: 10 }}>
                  <Button
                    size="small"
                    icon={<SendOutlined />}
                    loading={testingServerchan}
                    onClick={handleTestServerchan}
                    style={{ borderRadius: 6 }}
                  >
                    发送微信测试消息
                  </Button>
                </div>
              </div>
            ) : (
              <div style={{ background: '#f8fafc', padding: '12px 14px', borderRadius: 10, border: '1px solid #e2e8f0', marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                  <div style={{ fontSize: 12, fontWeight: 700, color: '#1e293b' }}>
                    🍎 Bark iOS 原生通知指引（免费、开源、秒级触达）：
                  </div>
                  <a
                    href="https://apps.apple.com/cn/app/bark-customed-notifications/id1403753865"
                    target="_blank"
                    rel="noreferrer"
                    style={{ fontSize: 12, fontWeight: 600, color: '#2563eb' }}
                  >
                    App Store 下载 Bark ↗
                  </a>
                </div>
                <div style={{ fontSize: 12, color: '#475569', lineHeight: 1.6 }}>
                  1. 在 iPhone / iPad 上安装 <b>Bark</b> App 并打开；<br />
                  2. 复制 Bark 首页顶部的<b>设备 Key</b> 或完整推送链接（如 <code>https://api.day.app/YOUR_KEY</code>）；<br />
                  3. 粘贴至下方，点击测试，手机屏幕顶部将直接弹出系统原生通知！
                </div>
                <Form.Item
                  name="barkUrl"
                  label={<span style={{ fontSize: 13, color: '#1e293b', fontWeight: 600 }}>Bark 设备 Key 或完整推送 URL</span>}
                  rules={[
                    {
                      validator: (_, val) => {
                        const wechatEnabled = form.getFieldValue('wechatEnabled')
                        if (wechatEnabled && channelType === 'bark' && (!val || !val.trim())) {
                          return Promise.reject(new Error('开启 Bark 通知时请输入设备 Key 或链接'))
                        }
                        return Promise.resolve()
                      },
                    },
                  ]}
                  style={{ marginTop: 10, marginBottom: 0 }}
                >
                  <Input placeholder="输入 Bark 设备 Key 或 https://api.day.app/YOUR_KEY" allowClear style={{ borderRadius: 8 }} />
                </Form.Item>

                <div style={{ textAlign: 'right', marginTop: 10 }}>
                  <Button
                    size="small"
                    icon={<SendOutlined />}
                    loading={testingBark}
                    onClick={handleTestBark}
                    style={{ borderRadius: 6 }}
                  >
                    发送 Bark 测试通知
                  </Button>
                </div>
              </div>
            )}
          </Card>

          {/* 个性化偏好 */}
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
