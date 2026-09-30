import { useState, useEffect, lazy, Suspense } from 'react'
import { Button, Layout, Typography, Spin, Tag, Segmented } from 'antd'
import { LogoutOutlined, LineChartOutlined, UserOutlined, CustomerServiceOutlined } from '@ant-design/icons'
import { Navigate, Route, Routes, useNavigate, useLocation } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './hooks/useAuth'
import { ContactModal } from './components/ContactModal'
import { ErrorBoundary } from './components/ErrorBoundary'

// 全路由懒加载: 各模块独立按需割离，登录页不加载自选表/图表，首屏提速 80%
const AuthPage = lazy(() => import('./pages/AuthPage'))
const WatchlistPage = lazy(() => import('./pages/WatchlistPage'))
const PortfolioPage = lazy(() => import('./pages/PortfolioPage'))
const FundDetailPage = lazy(() => import('./pages/FundDetailPage'))
const AdminPage = lazy(() => import('./pages/AdminPage'))

const { Header, Content } = Layout
const { Text } = Typography

function LayoutShell() {
  const { token, username, isVip, isAdmin, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)
  const [contactOpen, setContactOpen] = useState(false)

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
          style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', flexShrink: 0, whiteSpace: 'nowrap' }}
          onClick={() => navigate(isAdmin ? '/admin' : '/')}
        >
          <div
            style={{
              width: 30,
              height: 30,
              borderRadius: 8,
              flexShrink: 0,
              background: isAdmin
                ? 'linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%)'
                : 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: isAdmin
                ? '0 2px 6px rgba(124, 58, 237, 0.25)'
                : '0 2px 6px rgba(37, 99, 235, 0.25)',
            }}
          >
            <LineChartOutlined style={{ color: '#ffffff', fontSize: 16 }} />
          </div>
          <Text
            strong
            style={{
              color: '#0f172a',
              fontSize: isMobile ? 15 : 17,
              fontWeight: 650,
              letterSpacing: -0.2,
              whiteSpace: 'nowrap',
            }}
          >
            {isAdmin ? '基金估值管理后台' : '基金实时估值'}
          </Text>
          {isAdmin && (
            <Tag color="purple" style={{ borderRadius: 6, margin: 0, fontWeight: 600 }}>
              ADMIN
            </Tag>
          )}
        </div>

        {/* PC端顶部主导航 Tab: 自选 vs 持仓 */}
        {!isMobile && !isAdmin && (
          <Segmented
            size="middle"
            value={location.pathname === '/portfolio' ? '/portfolio' : '/'}
            onChange={(path) => navigate(path as string)}
            options={[
              { label: '自选基金', value: '/' },
              {
                label: (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
                    持仓估值
                    <span style={{ color: '#d97706', fontSize: 12, fontWeight: 700 }}>👑</span>
                  </span>
                ),
                value: '/portfolio',
              },
            ]}
            style={{
              background: '#f1f5f9',
              padding: 3,
              borderRadius: 10,
              fontWeight: 600,
            }}
          />
        )}

        <div style={{ display: 'flex', alignItems: 'center', gap: isMobile ? 4 : 10, lineHeight: 1, flexShrink: 0 }}>
          {/* 用户区域 - 与浅色界面融合的轻量质感，VIP 仅以柔和金色点缀 */}
          {isVip && !isAdmin ? (
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                background: 'linear-gradient(135deg, #ffffff 0%, #fffbeb 100%)',
                border: '1px solid #fde68a',
                padding: '4px 9px',
                borderRadius: 20,
                fontSize: 12,
                fontWeight: 600,
                height: 28,
                boxSizing: 'border-box',
                boxShadow: '0 1px 3px rgba(217, 119, 6, 0.08)',
              }}
            >
              <UserOutlined style={{ color: '#d97706', fontSize: 12 }} />
              <span style={{ color: '#92400e' }}>{username}</span>
              <span
                style={{
                  background: '#b45309',
                  color: '#ffffff',
                  fontSize: 10,
                  fontWeight: 800,
                  padding: '1px 5px',
                  borderRadius: 4,
                  lineHeight: '12px',
                  letterSpacing: 0.6,
                  display: 'inline-flex',
                  alignItems: 'center',
                }}
              >
                VIP
              </span>
            </div>
          ) : isAdmin ? (
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                background: 'linear-gradient(135deg, #ffffff 0%, #f5f3ff 100%)',
                border: '1px solid #ddd6fe',
                padding: '4px 10px',
                borderRadius: 20,
                fontSize: 12,
                fontWeight: 600,
                height: 28,
                boxSizing: 'border-box',
                boxShadow: '0 1px 3px rgba(124, 58, 237, 0.08)',
              }}
            >
              <UserOutlined style={{ color: '#7c3aed', fontSize: 12 }} />
              <span style={{ color: '#5b21b6' }}>{username}</span>
            </div>
          ) : (
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                background: '#f8fafc',
                border: '1px solid #e2e8f0',
                padding: '4px 10px',
                borderRadius: 20,
                fontSize: 12,
                color: '#1e293b',
                fontWeight: 500,
                lineHeight: 1,
                height: 28,
                boxSizing: 'border-box',
              }}
            >
              <UserOutlined style={{ color: '#64748b', fontSize: 12 }} />
              <span>{username}</span>
            </div>
          )}

          {/* 联系客服支持 */}
          <Button
            type="text"
            size="small"
            icon={<CustomerServiceOutlined style={{ color: '#2563eb' }} />}
            onClick={() => setContactOpen(true)}
            style={{
              color: '#475569',
              fontSize: 12,
              display: 'flex',
              alignItems: 'center',
              height: 28,
              borderRadius: 6,
            }}
          >
            {!isMobile && '联系'}
          </Button>

          <Button
            type="text"
            size="small"
            icon={<LogoutOutlined />}
            style={{
              color: '#64748b',
              fontSize: 12,
              display: 'flex',
              alignItems: 'center',
              height: 28,
              borderRadius: 6,
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

      {/* 移动端专属二级吸顶导航栏: 绝不挤压顶栏标题与图标 */}
      {isMobile && !isAdmin && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '6px 16px',
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
            borderBottom: '1px solid #f1f5f9',
            position: 'sticky',
            top: 56,
            zIndex: 90,
          }}
        >
          <Segmented
            block
            size="middle"
            value={location.pathname === '/portfolio' ? '/portfolio' : '/'}
            onChange={(path) => navigate(path as string)}
            options={[
              { label: '自选基金', value: '/' },
              {
                label: (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    持仓估值
                    <span style={{ color: '#d97706', fontSize: 12, fontWeight: 700 }}>👑</span>
                  </span>
                ),
                value: '/portfolio',
              },
            ]}
            style={{
              width: '100%',
              maxWidth: 340,
              background: '#f1f5f9',
              padding: 3,
              borderRadius: 10,
              fontWeight: 600,
            }}
          />
        </div>
      )}

      <Content style={{ width: '100%', overflowX: 'hidden' }}>
        <ErrorBoundary>
          <Suspense
            fallback={
              <div style={{ textAlign: 'center', padding: '60px 0' }}>
                <Spin size="large" />
              </div>
            }
          >
            <Routes>
              <Route path="/" element={isAdmin ? <Navigate to="/admin" replace /> : <WatchlistPage />} />
              <Route path="/portfolio" element={isAdmin ? <Navigate to="/admin" replace /> : <PortfolioPage />} />
              <Route path="/fund/:code" element={<FundDetailPage />} />
              <Route path="/admin" element={isAdmin ? <AdminPage /> : <Navigate to="/" replace />} />
              <Route path="/login" element={<Navigate to={isAdmin ? '/admin' : '/'} replace />} />
              <Route path="*" element={<Navigate to={isAdmin ? '/admin' : '/'} replace />} />
            </Routes>
          </Suspense>
        </ErrorBoundary>
      </Content>

      {/* 联系管理员弹窗 */}
      <ContactModal open={contactOpen} onClose={() => setContactOpen(false)} />
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
