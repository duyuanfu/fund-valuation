import { useState } from 'react'
import { Button, Card, Form, Input, Tabs, Typography, message } from 'antd'
import { LineChartOutlined, LockOutlined, UserOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { api } from '../api'

const { Title, Paragraph, Text } = Typography

export default function AuthPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [submitting, setSubmitting] = useState(false)

  const submit = async (values: { username: string; password: string }) => {
    setSubmitting(true)
    try {
      const res =
        mode === 'login'
          ? await api.login(values.username, values.password)
          : await api.register(values.username, values.password)
      login(res.token, res.username, res.role, res.isVip)
      message.success(mode === 'login' ? '欢迎回来' : '账号注册成功，已自动登录')
      navigate('/', { replace: true })
    } catch (e) {
      message.error((e as Error).message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        width: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(ellipse at 50% -20%, rgba(219, 234, 254, 0.7) 0%, rgba(248, 250, 252, 0.9) 60%, #f8fafc 100%)',
        padding: '24px 16px',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* 极简淡雅微环境光斑 */}
      <div
        style={{
          position: 'absolute',
          top: '-10%',
          right: '-5%',
          width: '380px',
          height: '380px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(186, 230, 253, 0.35) 0%, transparent 70%)',
          pointerEvents: 'none',
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: '-12%',
          left: '-8%',
          width: '420px',
          height: '420px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(224, 231, 255, 0.35) 0%, transparent 70%)',
          pointerEvents: 'none',
        }}
      />

      <div style={{ width: '100%', maxWidth: 390, position: 'relative', zIndex: 1 }}>
        {/* 品牌精质标志 */}
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <div
            style={{
              width: 50,
              height: 50,
              borderRadius: 14,
              background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 8px 20px -3px rgba(37, 99, 235, 0.3)',
              marginBottom: 14,
            }}
          >
            <LineChartOutlined style={{ fontSize: 24, color: '#ffffff' }} />
          </div>
          <Title
            level={3}
            style={{
              color: '#0f172a',
              margin: '0 0 6px 0',
              fontWeight: 700,
              letterSpacing: -0.5,
              fontSize: 22,
            }}
          >
            基金实时估值
          </Title>
          <Paragraph style={{ color: '#64748b', fontSize: 13, margin: 0, fontWeight: 400 }}>
            盘中持仓动态估算 · 债券利率模型驱动
          </Paragraph>
        </div>

        {/* 高端纯净卡片 */}
        <Card
          style={{
            borderRadius: 18,
            boxShadow: '0 10px 30px -8px rgba(15, 23, 42, 0.08), 0 4px 12px -2px rgba(15, 23, 42, 0.03)',
            border: '1px solid #f1f5f9',
            backgroundColor: '#ffffff',
          }}
          styles={{ body: { padding: '28px 22px' } }}
        >
          <Tabs
            centered
            activeKey={mode}
            onChange={(k) => setMode(k as 'login' | 'register')}
            items={[
              { key: 'login', label: <span style={{ fontSize: 15, fontWeight: 600, padding: '0 12px' }}>账号登录</span> },
              { key: 'register', label: <span style={{ fontSize: 15, fontWeight: 600, padding: '0 12px' }}>新用户注册</span> },
            ]}
          />

          <div
            style={{
              marginTop: 12,
              marginBottom: 20,
              backgroundColor: '#f8fafc',
              border: '1px solid #f1f5f9',
              borderRadius: 10,
              padding: '8px 12px',
              fontSize: 12,
              color: '#64748b',
              lineHeight: 1.5,
            }}
          >
            老用户注册请保持用户名与原 <Text code style={{ color: '#2563eb' }}>userId</Text> 一致，即可自动归集原有自选。
          </div>

          <Form onFinish={submit} layout="vertical" size="large">
            <Form.Item
              name="username"
              rules={[
                { required: true, message: '请输入用户名' },
                { min: 2, max: 20, message: '用户名长度在 2-20 个字符' },
              ]}
              style={{ marginBottom: 16 }}
            >
              <Input
                prefix={<UserOutlined style={{ color: '#94a3b8', marginRight: 4 }} />}
                placeholder="用户名 / userId"
                autoComplete="username"
                style={{
                  borderRadius: 10,
                  fontSize: 14,
                  borderColor: '#e2e8f0',
                  padding: '9px 12px',
                }}
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码长度至少需 6 位' },
              ]}
              style={{ marginBottom: 22 }}
            >
              <Input.Password
                prefix={<LockOutlined style={{ color: '#94a3b8', marginRight: 4 }} />}
                placeholder="密码 (至少6位)"
                autoComplete="current-password"
                style={{
                  borderRadius: 10,
                  fontSize: 14,
                  borderColor: '#e2e8f0',
                  padding: '9px 12px',
                }}
              />
            </Form.Item>

            <Button
              type="primary"
              htmlType="submit"
              block
              loading={submitting}
              style={{
                height: 44,
                borderRadius: 10,
                fontWeight: 650,
                fontSize: 15,
                background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
                boxShadow: '0 4px 12px rgba(37, 99, 235, 0.25)',
              }}
            >
              {mode === 'login' ? '立即登录' : '立即注册并体验'}
            </Button>
          </Form>
        </Card>

        {/* 底部极简版权声明 */}
        <div style={{ textAlign: 'center', marginTop: 22 }}>
          <Text type="secondary" style={{ fontSize: 12, color: '#94a3b8' }}>
            估值仅供盘中决策参考 · 不构成绝对投资建议
          </Text>
        </div>
      </div>
    </div>
  )
}
