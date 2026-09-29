import { Card, Typography, Space } from 'antd'
import { CrownOutlined } from '@ant-design/icons'
import { useAuth } from '../hooks/useAuth'

const { Title, Paragraph } = Typography

interface VipGuardProps {
  children: React.ReactNode
  featureName?: string
  fallback?: React.ReactNode
}

/**
 * 通用 VIP 专属特权守卫组件:
 * 将未来推出的高级功能直接用 <VipGuard featureName="高级持仓穿透分析">{...}</VipGuard> 包裹即可。
 */
export function VipGuard({ children, featureName = '高级特色功能', fallback }: VipGuardProps) {
  const { isVip, isAdmin } = useAuth()

  if (isVip || isAdmin) {
    return <>{children}</>
  }

  if (fallback) {
    return <>{fallback}</>
  }

  return (
    <Card
      style={{
        borderRadius: 16,
        textAlign: 'center',
        padding: '36px 20px',
        border: '1px dashed #f59e0b',
        backgroundColor: '#fffbeb',
      }}
    >
      <Space orientation="vertical" size={14} align="center">
        <div
          style={{
            width: 52,
            height: 52,
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 14px rgba(245, 158, 11, 0.35)',
          }}
        >
          <CrownOutlined style={{ fontSize: 26, color: '#ffffff' }} />
        </div>
        <Title level={4} style={{ margin: 0, color: '#92400e', fontWeight: 700 }}>
          👑 VIP 会员专享特权
        </Title>
        <Paragraph style={{ color: '#b45309', margin: 0, maxWidth: 360, fontSize: 13 }}>
          【{featureName}】为系统 VIP 会员专属特性。如需体验，请联系管理员为您开通或续期 VIP 权限。
        </Paragraph>
      </Space>
    </Card>
  )
}
