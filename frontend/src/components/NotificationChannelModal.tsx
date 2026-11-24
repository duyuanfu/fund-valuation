import { useState, useEffect } from 'react'
import {
  Modal,
  Form,
  Input,
  Switch,
  Radio,
  Button,
  Space,
  Alert,
  message,
  Card,
} from 'antd'
import {
  BellOutlined,
  MailOutlined,
  WechatOutlined,
  SendOutlined,
  SafetyOutlined,
  QrcodeOutlined,
  LinkOutlined,
} from '@ant-design/icons'
import { api } from '../api'
import type { NotificationChannelView } from '../api/types'

interface NotificationChannelModalProps {
  open: boolean
  onClose: () => void
}

export function NotificationChannelModal({ open, onClose }: NotificationChannelModalProps) {
  const [form] = Form.useForm<NotificationChannelView>()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [testingEmail, setTestingEmail] = useState(false)
  const [testingWechat, setTestingWechat] = useState(false)
  const [wechatType, setWechatType] = useState('wxpusher')
  const [channelData, setChannelData] = useState<NotificationChannelView | null>(null)

  const loadChannel = async () => {
    setLoading(true)
    try {
      const data = await api.getNotificationChannel()
      if (data) {
        setChannelData(data)
        form.setFieldsValue({
          email: data.email || '',
          emailEnabled: data.emailEnabled ?? true,
          wechatChannelType: data.wechatChannelType || 'wxpusher',
          wechatUid: data.wechatUid || '',
          wechatAppToken: data.wechatAppToken || '',
          wechatWebhookUrl: data.wechatWebhookUrl || '',
          wechatEnabled: data.wechatEnabled ?? true,
          remindEnabled: data.remindEnabled ?? true,
          pushScope: data.pushScope || 'ALL_WATCHLIST',
          hideAmount: data.hideAmount ?? false,
        })
        setWechatType(data.wechatChannelType || 'wxpusher')
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
      message.success('通知渠道设置已成功保存！交易日 14:30 将准时为您推送。')
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
      // 自动保存当前表单参数
      const currentValues = form.getFieldsValue()
      await api.updateNotificationChannel(currentValues).catch(() => {})
      const res = await api.testNotificationEmail(emailVal.trim())
      message.success(res.message || '测试邮件已发出，请检查收件箱')
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

  const handleTestWechat = async () => {
    try {
      if (wechatType === 'wxpusher') {
        await form.validateFields(['wechatUid'])
      } else {
        await form.validateFields(['wechatWebhookUrl'])
      }
      setTestingWechat(true)
      const currentValues = form.getFieldsValue()
      await api.updateNotificationChannel(currentValues).catch(() => {})
      const res = await api.testNotificationWechat()
      message.success(res.message || '微信/Webhook 测试消息已发出！')
    } catch (e: any) {
      if (e?.errorFields) {
        message.error(e.errorFields[0]?.errors[0] || '请完善微信推送参数')
      } else {
        message.error(e?.message || '测试推送失败，请检查 UID 或 Webhook 地址')
      }
    } finally {
      setTestingWechat(false)
    }
  }

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 16 }}>
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 6px rgba(37, 99, 235, 0.3)',
            }}
          >
            <BellOutlined style={{ color: '#ffffff', fontSize: 16 }} />
          </div>
          <div>
            <span style={{ fontWeight: 700, color: '#0f172a' }}>每日 14:30 盘中关键提醒 · 邮件与微信绑定</span>
          </div>
        </div>
      }
      open={open}
      onCancel={onClose}
      width={620}
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
      <div style={{ padding: '6px 0' }}>
        <Alert
          type="info"
          showIcon
          message="为什么是 14:30？"
          description="A股与场外公募基金以每日 15:00 为申购/赎回确认截止点。系统在交易日 14:30 准时测算最新收益走势，无需您主动打开网页，直接推送到您的手机，助您把握盘尾黄金调仓时机！"
          style={{ marginBottom: 16, borderRadius: 10 }}
        />

        <Form form={form} layout="vertical" disabled={loading}>
          {/* 总开关 */}
          <Card
            size="small"
            style={{
              borderRadius: 10,
              background: '#f8fafc',
              marginBottom: 16,
              border: '1px solid #e2e8f0',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontWeight: 650, color: '#1e293b' }}>开启交易日 14:30 盘中提醒</div>
                <div style={{ fontSize: 12, color: '#64748b' }}>休市日与节假日将自动静默，不打扰您的休息</div>
              </div>
              <Form.Item name="remindEnabled" valuePropName="checked" noStyle>
                <Switch checkedChildren="已开启" unCheckedChildren="已关闭" />
              </Form.Item>
            </div>
          </Card>

          {/* 渠道 1: 电子邮箱 */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Space>
                  <MailOutlined style={{ color: '#2563eb' }} />
                  <span style={{ fontWeight: 650 }}>电子邮箱接收 (推荐，排版精美)</span>
                </Space>
                <Form.Item name="emailEnabled" valuePropName="checked" noStyle>
                  <Switch size="small" checkedChildren="开启" unCheckedChildren="关闭" />
                </Form.Item>
              </div>
            }
            size="small"
            style={{ borderRadius: 10, marginBottom: 16 }}
          >
            <Form.Item
              name="email"
              label="电子邮箱地址"
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
              extra="支持 QQ邮箱、163网易、Gmail、Outlook 等任何常用邮箱，手机端将收到标准邮件提醒。"
            >
              <Input placeholder="例如: yourname@qq.com" allowClear prefix={<MailOutlined style={{ color: '#94a3b8' }} />} />
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

          {/* 渠道 2: 微信 / Webhook */}
          <Card
            title={
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Space>
                  <WechatOutlined style={{ color: '#16a34a' }} />
                  <span style={{ fontWeight: 650 }}>微信通知 (支持扫码个人接收或企业微信)</span>
                </Space>
                <Form.Item name="wechatEnabled" valuePropName="checked" noStyle>
                  <Switch size="small" checkedChildren="开启" unCheckedChildren="关闭" />
                </Form.Item>
              </div>
            }
            size="small"
            style={{ borderRadius: 10, marginBottom: 16 }}
          >
            <Form.Item name="wechatChannelType" label="微信推送模式">
              <Radio.Group
                onChange={(e) => setWechatType(e.target.value)}
                buttonStyle="solid"
                style={{ width: '100%' }}
              >
                <Radio.Button value="webhook" style={{ width: '50%', textAlign: 'center' }}>
                  <LinkOutlined /> 微信免App直推 (PushPlus/企微)
                </Radio.Button>
                <Radio.Button value="wxpusher" style={{ width: '50%', textAlign: 'center' }}>
                  <QrcodeOutlined /> WxPusher (需安装App)
                </Radio.Button>
              </Radio.Group>
            </Form.Item>

            {wechatType === 'wxpusher' ? (
              <div style={{ background: '#f0fdf4', padding: '12px 14px', borderRadius: 8, border: '1px solid #bbf7d0', marginBottom: 12 }}>
                <div style={{ fontSize: 12, color: '#166534', lineHeight: 1.6 }}>
                  <b>WxPusher 接收指南（官方已停止微信直接推送，需配合官方 App）：</b><br />
                  1. 关注 WxPusher 服务号或下载 WxPusher 客户端，复制您的 <code>UID_xxxx</code>；<br />
                  2. <b>关键步骤：</b>请确保微信已扫描本应用（或您自建应用）专属关注二维码；<br />
                  {channelData?.systemWxpusherQrUrl ? (
                    <div style={{ margin: '6px 0', padding: '6px 10px', background: '#dcfce7', borderRadius: 6 }}>
                      👉 <b>系统通知应用关注入口：</b>
                      <a href={channelData.systemWxpusherQrUrl} target="_blank" rel="noreferrer" style={{ marginLeft: 6, fontWeight: 600 }}>
                        点击此处打开关注二维码链接
                      </a>
                    </div>
                  ) : null}
                  3. 点击下方「发送测试微信」，手机将立即收到确认卡片！
                </div>
                <Form.Item
                  name="wechatUid"
                  label="您的 WxPusher UID"
                  rules={[
                    {
                      validator: (_, val) => {
                        const wechatEnabled = form.getFieldValue('wechatEnabled')
                        if (wechatEnabled && wechatType === 'wxpusher' && (!val || !val.trim())) {
                          return Promise.reject(new Error('开启微信通知时请输入 WxPusher UID'))
                        }
                        return Promise.resolve()
                      },
                    },
                  ]}
                  style={{ marginTop: 10, marginBottom: 8 }}
                >
                  <Input placeholder="例如: UID_a1b2c3d4e5f6g7h8..." allowClear />
                </Form.Item>
                <Form.Item
                  name="wechatAppToken"
                  label="个人自定义 AppToken (可选)"
                  extra="以 AT_ 开头。留空时默认使用系统后台统一配置的 AppToken。如果您在 wxpusher.zjiecode.com 创建了个人应用，请在此填入您的 AppToken。"
                  style={{ marginBottom: 0 }}
                >
                  <Input placeholder="例如: AT_xxxxxxxxxxxx (留空使用系统默认)" allowClear />
                </Form.Item>
              </div>
            ) : (
              <div style={{ background: '#f8fafc', padding: '12px 14px', borderRadius: 8, border: '1px solid #e2e8f0', marginBottom: 12 }}>
                <div style={{ fontSize: 12, color: '#334155', lineHeight: 1.6, marginBottom: 10 }}>
                  <b style={{ color: '#0f172a' }}>微信直接接收（无需下载 App）推荐通道：</b><br />
                  1. <b>PushPlus 推送加 (最推荐)</b>：微信扫码关注「pushplus推送加」公众号或登录 <a href="https://www.pushplus.plus" target="_blank" rel="noreferrer" style={{ fontWeight: 600, color: '#2563eb' }}>pushplus.plus</a>，在首页复制 <b>Token</b>（32位字符）直接填入下方即可！交易日 14:30 消息将由该公众号直接推送。<br />
                  2. <b>企业微信群机器人</b>：在企微群添加机器人，将 Webhook 链接粘贴在下方，开启微信插件后，个人微信将实时接收弹窗。<br />
                  3. <b>Server酱</b>：直接粘贴 <code>SCTxxxx</code> SendKey 即可。<br />
                  4. <b>iOS 用户</b>：支持 Bark 推送链接直接通知手机。
                </div>
                <Form.Item
                  name="wechatWebhookUrl"
                  label="微信免App推送 Token 或 Webhook 地址"
                  rules={[
                    {
                      validator: (_, val) => {
                        const wechatEnabled = form.getFieldValue('wechatEnabled')
                        if (wechatEnabled && wechatType === 'webhook' && (!val || !val.trim())) {
                          return Promise.reject(new Error('开启微信免App通道时请输入 Token 或 Webhook 地址'))
                        }
                        return Promise.resolve()
                      },
                    },
                  ]}
                  style={{ marginBottom: 0 }}
                >
                  <Input placeholder="输入 PushPlus Token(32位字符) 或 Server酱(SCTxxxx) 或 企业微信Webhook" allowClear />
                </Form.Item>
              </div>
            )}

            <div style={{ textAlign: 'right' }}>
              <Button
                size="small"
                icon={<SendOutlined />}
                loading={testingWechat}
                onClick={handleTestWechat}
                style={{ borderRadius: 6 }}
              >
                发送测试微信/Webhook
              </Button>
            </div>
          </Card>

          {/* 偏好设置 */}
          <Card title="个性化推送偏好" size="small" style={{ borderRadius: 10 }}>
            <Form.Item name="pushScope" label="推送内容范围">
              <Radio.Group>
                <Radio value="ALL_WATCHLIST">自选与持仓全部推送</Radio>
                <Radio value="PORTFOLIO_ONLY">仅推送实际持仓基金 (VIP)</Radio>
              </Radio.Group>
            </Form.Item>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 }}>
              <div>
                <div style={{ fontWeight: 600, fontSize: 13, color: '#334155' }}>
                  <SafetyOutlined style={{ marginRight: 6, color: '#64748b' }} />
                  隐藏具体资产金额 (隐私脱敏)
                </div>
                <div style={{ fontSize: 12, color: '#64748b' }}>
                  开启后在邮件和微信中仅显示百分比涨跌幅，不显示持仓总额与盈亏具体金额
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
