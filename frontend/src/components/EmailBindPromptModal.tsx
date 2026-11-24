import { useState } from 'react'
import { Modal, Input, Button, message } from 'antd'
import { MailOutlined, SettingOutlined } from '@ant-design/icons'
import { api } from '../api'

interface EmailBindPromptModalProps {
  open: boolean
  onClose: () => void
  onOpenFullSettings: () => void
  onSuccess?: (email: string) => void
}

export function EmailBindPromptModal({
  open,
  onClose,
  onOpenFullSettings,
  onSuccess,
}: EmailBindPromptModalProps) {
  const [email, setEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleBind = async () => {
    if (!email || !email.trim()) {
      message.warning('请输入您的电子邮箱地址')
      return
    }
    const clean = email.trim()
    const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/
    if (!emailRegex.test(clean)) {
      message.error('请输入格式正确的电子邮箱地址')
      return
    }

    setSubmitting(true)
    try {
      await api.bindNotificationEmail(clean)
      message.success('🎉 邮箱绑定成功！已自动开启交易日盘中估值提醒。')
      sessionStorage.setItem('dismiss_email_bind_prompt', '1')
      if (onSuccess) onSuccess(clean)
      onClose()
    } catch (e: any) {
      message.error(e?.message || '邮箱绑定失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDismiss = () => {
    sessionStorage.setItem('dismiss_email_bind_prompt', '1')
    onClose()
  }

  const handleGoFullSettings = () => {
    sessionStorage.setItem('dismiss_email_bind_prompt', '1')
    onClose()
    onOpenFullSettings()
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
              background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 6px rgba(37, 99, 235, 0.25)',
              flexShrink: 0,
            }}
          >
            <MailOutlined style={{ color: '#ffffff', fontSize: 16 }} />
          </div>
          <span style={{ fontWeight: 700, color: '#0f172a' }}>绑定邮箱 · 开启盘中估值提醒</span>
        </div>
      }
      open={open}
      onCancel={handleDismiss}
      width={460}
      centered
      destroyOnClose
      footer={null}
    >
      <div style={{ padding: '6px 0' }}>
        <p style={{ margin: '0 0 12px 0', fontSize: 13, color: '#475569', lineHeight: 1.6 }}>
          检测到您尚未绑定电子邮箱。绑定后，系统将在每个 A 股交易日盘中（默认 <b>14:30</b>）准时向您发送<b>持仓盈亏总览</b>与<b>自选基金异动播报</b>，助您在 15:00 闭市前从容决策！
        </p>

        <div style={{ marginBottom: 16 }}>
          <Input
            size="large"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onPressEnter={handleBind}
            placeholder="请输入您的电子邮箱 (例如: name@163.com)"
            prefix={<MailOutlined style={{ color: '#94a3b8', marginRight: 4 }} />}
            allowClear
            style={{ borderRadius: 10, fontSize: 14 }}
          />
        </div>

        <Button
          type="primary"
          size="large"
          block
          loading={submitting}
          onClick={handleBind}
          style={{
            height: 42,
            borderRadius: 10,
            background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
            fontWeight: 650,
            fontSize: 14,
            boxShadow: '0 4px 12px rgba(37, 99, 235, 0.25)',
            marginBottom: 10,
          }}
        >
          立即绑定并开启提醒
        </Button>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 4 }}>
          <Button
            type="link"
            size="small"
            icon={<SettingOutlined />}
            onClick={handleGoFullSettings}
            style={{ color: '#2563eb', padding: 0, fontSize: 12 }}
          >
            使用微信(Server酱)或Bark推送 ↗
          </Button>

          <Button
            type="text"
            size="small"
            onClick={handleDismiss}
            style={{ color: '#94a3b8', fontSize: 12 }}
          >
            稍后再说
          </Button>
        </div>
      </div>
    </Modal>
  )
}
