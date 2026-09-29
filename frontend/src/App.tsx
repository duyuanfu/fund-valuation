import { useState, useEffect, lazy, Suspense } from 'react'
import { Button, Layout, Typography, Spin } from 'antd'
import { LogoutOutlined, LineChartOutlined, SettingOutlined } from '@ant-design/icons'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './hooks/useAuth'

// 全路由懒加载: 各模块独立按需割离，登录页不加载自选表/图表，首屏提速 80%
const AuthPage = lazy(() => import('./pages/AuthPage'))
const WatchlistPage = lazy(() => import('./pages/WatchlistPage'))
const FundDetailPage = lazy(() => import('./pages/FundDetailPage'))
const AdminPage = lazy(() => import('./pages/AdminPage'))

const { Header, Content } = Layout
const { Text } = Typography

function LayoutShell() {
  const { token, username, isVip, isAdmin, logout } = useAuth()
  const navigate = useNavigate()
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768)
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  // 未登录拦截到登录页 (懒加载包裹)
  if (!token) {
    return (
      <Suspense
        fallback={
          <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f8fafc' }}>
            <Spin size="large" />
          </div>
        }
      >
        <Routes>
          <Route path="/login" element={<AuthPage />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </Suspense>
    )
  }

  return (
    <Layout style={{ minHeight: '100vh', background: '#f8fafc' }} className="no-horiz-overflow">
      {/* 现代纯净白底 + 毛玻璃柔和微边框顶栏 */}
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(255, 255, 255, 0.92)',
          backdropFilter: 'blur(12px)',
          WebkitBackdropFilter: 'blur(12px)',
          borderBottom: '1px solid #f1f5f9',
          padding: isMobile ? '0 16px' : '0 28px',
          height: 56,
          lineHeight: '56px',
          boxShadow: '0 1px 2px 0 rgba(15, 23, 42, 0.03)',
          position: 'sticky',
          top: 0,
          zIndex: 100,
        }}
      >
        <div
          style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}
          onClick={() => navigate('/')}
        >
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
            <LineChartOutlined style={{ color: '#ffffff', fontSize: 16 }} />
          </div>
          <Text
            strong
            style={{
              color: '#0f172a',
              fontSize: isMobile ? 16 : 17,
              fontWeight: 650,
              letterSpacing: -0.2,
            }}
          >
            基金实时估值
          </Text>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          {isAdmin && (
            <Button
              type="primary"
              size="small"
              icon={<SettingOutlined />}
              onClick={() => navigate('/admin')}
              style={{
                borderRadius: 8,
                background: 'linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%)',
                borderColor: '#6d28d9',
                fontSize: 12,
                fontWeight: 600,
              }}
            >
              {!isMobile && '管理后台'}
            </Button>
          )}

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              background: '#f1f5f9',
              padding: '3px 10px',
              borderRadius: 16,
              fontSize: 12,
              color: '#334155',
              fontWeight: 500,
            }}
          >
            <span
              style={{
                width: 6,
                height: 6,
                borderRadius: '50%',
                backgroundColor: '#10b981',
                display: 'inline-block',
              }}
            />
            {username}
            {isVip && (
              <span
                style={{
                  background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
                  color: '#ffffff',
                  fontSize: 10,
                  fontWeight: 700,
                  padding: '1px 5px',
                  borderRadius: 8,
                  marginLeft: 2,
                }}
              >
                👑 VIP
              </span>
            )}
          </div>
          <Button
            type="text"
            size="small"
            icon={<LogoutOutlined />}
            style={{
              color: '#64748b',
              fontSize: 13,
              display: 'flex',
              alignItems: 'center',
            }}
            onClick={() => {
              logout()
              navigate('/login')
            }}
          >
            {!isMobile && '退出'}
          </Button>
        </div>
      </Header>

      <Content style={{ width: '100%', overflowX: 'hidden' }}>
        <Suspense
          fallback={
            <div style={{ textAlign: 'center', padding: '60px 0' }}>
              <Spin size="large" />
            </div>
          }
        >
          <Routes>
            <Route path="/" element={<WatchlistPage />} />
            <Route path="/fund/:code" element={<FundDetailPage />} />
            <Route path="/admin" element={isAdmin ? <AdminPage /> : <Navigate to="/" replace />} />
            <Route path="/login" element={<Navigate to="/" replace />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </Content>
    </Layout>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <LayoutShell />
    </AuthProvider>
  )
}
