import { useState, useEffect, lazy, Suspense } from 'react'
import { Layout, Typography, Spin, Tag, Segmented, Dropdown } from 'antd'
import type { MenuProps } from 'antd'
import {
  LogoutOutlined,
  LineChartOutlined,
  UserOutlined,
  CustomerServiceOutlined,
  MailOutlined,
  BellOutlined,
  DownOutlined,
} from '@ant-design/icons'
import { Navigate, Route, Routes, useNavigate, useLocation } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './hooks/useAuth'
import { ContactModal } from './components/ContactModal'
import { NotificationChannelModal } from './components/NotificationChannelModal'
import { EmailBindPromptModal } from './components/EmailBindPromptModal'
import { ErrorBoundary } from './components/ErrorBoundary'
import { api } from './api'
import type { NotificationChannelView } from './api/types'

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
  const [channel, setChannel] = useState<NotificationChannelView | null>(null)
  const [emailPromptOpen, setEmailPromptOpen] = useState(false)
  const [notificationModalOpen, setNotificationModalOpen] = useState(false)

  const refreshChannel = () => {
    if (token && !isAdmin) {
      api.getNotificationChannel()
        .then((ch) => {
          setChannel(ch)
          if (!ch || !ch.email || !ch.email.trim()) {
            const dismissed = sessionStorage.getItem('dismiss_email_bind_prompt')
            if (!dismissed) {
              setEmailPromptOpen(true)
            }
          }
        })
        .catch(() => {})
    }
  }

  useEffect(() => {
    refreshChannel()
  }, [token, isAdmin])

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

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'user-profile',
      disabled: true,
      label: (
        <div style={{ padding: '6px 4px', minWidth: 200, cursor: 'default' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
            <span style={{ fontWeight: 700, color: '#0f172a', fontSize: 14 }}>
              {username}
            </span>
            {isVip && !isAdmin ? (
              <Tag color="gold" style={{ margin: 0, fontWeight: 700, fontSize: 11, borderRadius: 4 }}>
                👑 VIP会员
              </Tag>
            ) : isAdmin ? (
              <Tag color="purple" style={{ margin: 0, fontWeight: 700, fontSize: 11, borderRadius: 4 }}>
                🛠 超级管理员
              </Tag>
            ) : (
              <Tag color="default" style={{ margin: 0, fontSize: 11, borderRadius: 4 }}>
                普通用户
              </Tag>
            )}
          </div>
          {!isAdmin && (
            <div style={{ fontSize: 12, color: '#64748b', display: 'flex', alignItems: 'center', gap: 6, marginTop: 6 }}>
              <MailOutlined style={{ color: channel?.email ? '#2563eb' : '#d97706', fontSize: 13 }} />
              {channel?.email ? (
                <span style={{ color: '#334155', wordBreak: 'break-all' }}>{channel.email}</span>
              ) : (
                <span
                  style={{ color: '#d97706', cursor: 'pointer', textDecoration: 'underline', fontWeight: 600 }}
                  onClick={(e) => {
                    e.stopPropagation()
                    setEmailPromptOpen(true)
                  }}
                >
                  未绑定邮箱 (点击立即绑定)
                </span>
              )}
            </div>
          )}
        </div>
      ),
    },
    { type: 'divider' },
    ...(!isAdmin
      ? [
          {
            key: 'reminder',
            icon: <BellOutlined style={{ color: '#d97706' }} />,
            label: (
              <span style={{ fontWeight: 500 }}>
                盘中提醒设置
                {channel?.remindTime ? (
                  <span style={{ color: '#94a3b8', fontSize: 11, marginLeft: 6 }}>
                    ({channel.remindTime})
                  </span>
                ) : null}
              </span>
            ),
            onClick: () => setNotificationModalOpen(true),
          },
        ]
      : []),
    {
      key: 'contact',
      icon: <CustomerServiceOutlined style={{ color: '#2563eb' }} />,
      label: '联系客服 / 管理员',
      onClick: () => setContactOpen(true),
    },
    { type: 'divider' },
    {
      key: 'logout',
      danger: true,
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: () => {
        logout()
        navigate('/login')
      },
    },
  ]

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

        <div style={{ display: 'flex', alignItems: 'center', gap: 8, lineHeight: 1, flexShrink: 0 }}>
          {/* 我的 · 个人信息与操作下拉菜单 */}
          <Dropdown
            menu={{ items: userMenuItems }}
            trigger={['click']}
            placement="bottomRight"
            arrow={{ pointAtCenter: true }}
          >
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                cursor: 'pointer',
                padding: '4px 10px',
                borderRadius: 20,
                background: isVip && !isAdmin
                  ? 'linear-gradient(135deg, #ffffff 0%, #fffbeb 100%)'
                  : (isAdmin ? 'linear-gradient(135deg, #ffffff 0%, #f5f3ff 100%)' : '#ffffff'),
                border: isVip && !isAdmin
                  ? '1px solid #fde68a'
                  : (isAdmin ? '1px solid #ddd6fe' : '1px solid #e2e8f0'),
                boxShadow: '0 1px 3px rgba(15, 23, 42, 0.06)',
                userSelect: 'none',
                transition: 'all 0.2s',
                height: 30,
                boxSizing: 'border-box',
              }}
            >
              <UserOutlined style={{ color: isVip && !isAdmin ? '#d97706' : (isAdmin ? '#7c3aed' : '#2563eb'), fontSize: 13 }} />
              <span style={{ fontSize: 13, fontWeight: 600, color: '#1e293b', maxWidth: 90, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {username}
              </span>
              {isVip && !isAdmin && (
                <span
                  style={{
                    background: '#b45309',
                    color: '#ffffff',
                    fontSize: 10,
                    fontWeight: 800,
                    padding: '1px 5px',
                    borderRadius: 4,
                    lineHeight: '12px',
                    letterSpacing: 0.5,
                  }}
                >
                  VIP
                </span>
              )}
              {isAdmin && (
                <Tag color="purple" style={{ margin: 0, padding: '0 4px', fontSize: 10, lineHeight: '14px', borderRadius: 4, fontWeight: 700 }}>
                  ADMIN
                </Tag>
              )}
              <DownOutlined style={{ fontSize: 9, color: '#94a3b8', marginLeft: 1 }} />
            </div>
          </Dropdown>
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

      {/* 盘中提醒设置弹窗 */}
      <NotificationChannelModal
        open={notificationModalOpen}
        onClose={() => {
          setNotificationModalOpen(false)
          refreshChannel()
        }}
      />

      {/* 针对老用户等未绑定邮箱的用户弹窗 */}
      <EmailBindPromptModal
        open={emailPromptOpen}
        onClose={() => setEmailPromptOpen(false)}
        onSuccess={(newEmail) => {
          setChannel((prev) => (prev ? { ...prev, email: newEmail } : { email: newEmail } as any))
          refreshChannel()
        }}
        onOpenFullSettings={() => {
          setEmailPromptOpen(false)
          setNotificationModalOpen(true)
        }}
      />

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
