import { Modal, Typography, Space, Button, message } from 'antd'
import {
  CustomerServiceOutlined,
  MailOutlined,
  QqOutlined,
  CopyOutlined,
  SendOutlined,
} from '@ant-design/icons'

const { Text, Paragraph } = Typography

interface ContactModalProps {
  open: boolean
  onClose: () => void
}

export function ContactModal({ open, onClose }: ContactModalProps) {
  const email = 'duyuanfu@yeah.net'
  const qq = '2860421826'

  const copyToClipboard = (text: string, label: string) => {
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
              width: 28,
              height: 28,
              borderRadius: 8,
              background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <CustomerServiceOutlined style={{ color: '#ffffff', fontSize: 14 }} />
          </div>
          <span style={{ fontWeight: 650, color: '#0f172a' }}>联系管理员 / 客户支持</span>
        </div>
      }
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="close" type="primary" onClick={onClose} style={{ borderRadius: 8, minWidth: 80 }}>
          我知道了
        </Button>,
      ]}
      width={460}
      centered
      destroyOnClose
    >
      <div style={{ padding: '8px 0 4px 0' }}>
        <Paragraph style={{ color: '#64748b', fontSize: 13, marginBottom: 16, lineHeight: 1.6 }}>
          如有新账号审核授权、开通/续费 VIP 会员、系统使用疑问或功能建议，欢迎随时通过以下方式与管理员取得联系：
        </Paragraph>

        {/* 电子邮箱卡片 */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 14px',
            borderRadius: 12,
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            marginBottom: 12,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 10,
                background: '#eff6ff',
                color: '#2563eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 18,
              }}
            >
              <MailOutlined />
            </div>
            <div>
              <div style={{ fontSize: 12, color: '#64748b', marginBottom: 2 }}>电子邮箱 (Email)</div>
              <Text strong style={{ fontSize: 15, color: '#0f172a', fontFamily: 'monospace' }}>
                {email}
              </Text>
            </div>
          </div>
          <Space size={6}>
            <Button
              size="small"
              icon={<CopyOutlined />}
              onClick={() => copyToClipboard(email, '邮箱地址')}
              style={{ borderRadius: 6 }}
            >
              复制
            </Button>
            <Button
              size="small"
              type="primary"
              ghost
              icon={<SendOutlined />}
              href={`mailto:${email}`}
              target="_blank"
              style={{ borderRadius: 6 }}
            >
              发信
            </Button>
          </Space>
        </div>

        {/* QQ 卡片 */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 14px',
            borderRadius: 12,
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            marginBottom: 16,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 10,
                background: '#e0f2fe',
                color: '#0284c7',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 18,
              }}
            >
              <QqOutlined />
            </div>
            <div>
              <div style={{ fontSize: 12, color: '#64748b', marginBottom: 2 }}>客服支持 QQ</div>
              <Text strong style={{ fontSize: 15, color: '#0f172a', fontFamily: 'monospace' }}>
                {qq}
              </Text>
            </div>
          </div>
          <Button
            size="small"
            icon={<CopyOutlined />}
            onClick={() => copyToClipboard(qq, 'QQ号码')}
            style={{ borderRadius: 6 }}
          >
            复制号码
          </Button>
        </div>

        {/* 提示信息 */}
        <div
          style={{
            padding: '10px 12px',
            borderRadius: 8,
            background: '#fffbeb',
            border: '1px solid #fef3c7',
            fontSize: 12,
            color: '#b45309',
            lineHeight: 1.5,
          }}
        >
          <b>💡 快速授权提示：</b>新用户注册后，可直接发送邮件或 QQ 告知您的注册用户名，我们将尽快为您开通使用权限。
        </div>
      </div>
    </Modal>
  )
}
