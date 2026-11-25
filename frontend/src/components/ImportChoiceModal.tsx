import { Modal, Typography, Space } from 'antd'
import {
  CameraOutlined,
  EditOutlined,
  ThunderboltFilled,
  RightOutlined,
} from '@ant-design/icons'

const { Title, Text } = Typography

interface ImportChoiceModalProps {
  open: boolean
  onClose: () => void
  onChooseScreenshot: () => void
  onChooseManual: () => void
}

export function ImportChoiceModal({
  open,
  onClose,
  onChooseScreenshot,
  onChooseManual,
}: ImportChoiceModalProps) {
  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      width={460}
      centered
      destroyOnClose
      styles={{
        body: {
          borderRadius: 16,
          padding: '24px 20px',
        },
      }}
    >
      <div style={{ textAlign: 'center', marginBottom: 20 }}>
        <Title level={4} style={{ margin: 0, fontWeight: 650, color: '#0f172a' }}>
          选择持仓录入方式
        </Title>
        <Text type="secondary" style={{ fontSize: 13 }}>
          支持截屏自动结构化提取，或手动逐条录入
        </Text>
      </div>

      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        {/* 选项 1: 截图智能导入 (推荐) */}
        <div
          onClick={() => {
            onClose()
            onChooseScreenshot()
          }}
          style={{
            display: 'flex',
            alignItems: 'center',
            padding: '16px 18px',
            borderRadius: 14,
            border: '1.5px solid #bae6fd',
            background: 'linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%)',
            cursor: 'pointer',
            transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            boxShadow: '0 2px 8px rgba(2, 132, 199, 0.08)',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.borderColor = '#0284c7'
            e.currentTarget.style.transform = 'translateY(-1px)'
            e.currentTarget.style.boxShadow = '0 6px 16px rgba(2, 132, 199, 0.15)'
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = '#bae6fd'
            e.currentTarget.style.transform = 'none'
            e.currentTarget.style.boxShadow = '0 2px 8px rgba(2, 132, 199, 0.08)'
          }}
        >
          <div
            style={{
              width: 44,
              height: 44,
              borderRadius: 12,
              background: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
              boxShadow: '0 3px 8px rgba(2, 132, 199, 0.3)',
            }}
          >
            <CameraOutlined style={{ fontSize: 22, color: '#ffffff' }} />
          </div>

          <div style={{ marginLeft: 14, flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ fontSize: 15, fontWeight: 650, color: '#0f172a' }}>
                支付宝截图智能导入
              </span>
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 3,
                  fontSize: 11,
                  fontWeight: 600,
                  color: '#0284c7',
                  background: '#e0f2fe',
                  padding: '2px 7px',
                  borderRadius: 10,
                }}
              >
                <ThunderboltFilled style={{ fontSize: 10 }} /> 推荐
              </span>
            </div>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 3 }}>
              上传或粘贴持仓截图，AI 秒级识别全部基金
            </div>
          </div>

          <RightOutlined style={{ color: '#94a3b8', fontSize: 13 }} />
        </div>

        {/* 选项 2: 手工逐条录入 */}
        <div
          onClick={() => {
            onClose()
            onChooseManual()
          }}
          style={{
            display: 'flex',
            alignItems: 'center',
            padding: '16px 18px',
            borderRadius: 14,
            border: '1.5px solid #e2e8f0',
            background: '#ffffff',
            cursor: 'pointer',
            transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.borderColor = '#cbd5e1'
            e.currentTarget.style.background = '#f8fafc'
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = '#e2e8f0'
            e.currentTarget.style.background = '#ffffff'
          }}
        >
          <div
            style={{
              width: 44,
              height: 44,
              borderRadius: 12,
              background: '#f1f5f9',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <EditOutlined style={{ fontSize: 20, color: '#475569' }} />
          </div>

          <div style={{ marginLeft: 14, flex: 1 }}>
            <div style={{ fontSize: 15, fontWeight: 600, color: '#0f172a' }}>
              手动录入持仓
            </div>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 3 }}>
              手动输入 6 位基金代码、金额与成本收益
            </div>
          </div>

          <RightOutlined style={{ color: '#94a3b8', fontSize: 13 }} />
        </div>
      </Space>
    </Modal>
  )
}
