import type {
  EstimateResult,
  FundDetailView,
  FundPerformanceView,
  NoticeView,
  UserView,
  CalendarHolidaysResponse,
  SystemStats,
  FundView,
  AddFundResult,
  PortfolioView,
  PositionSaveRequest,
  VipConfigView,
  RegisterResponse,
  UserProfileView,
  VipOrderView,
  NotificationChannelView,
  EmailConfigView,
} from './types'
import {
  TOKEN_KEY,
  USER_KEY,
  ROLE_KEY,
  VIP_KEY,
  WATCHLIST_CACHE_KEY,
} from '../context/auth-context'

function authHeader(): Record<string, string> {
  const token = localStorage.getItem(TOKEN_KEY)
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    ...options,
  })

  let errorMsg = `请求失败 (${res.status})`
  if (!res.ok) {
    try {
      const body = await res.json()
      if (body && body.error) errorMsg = body.error
    } catch {
      /* ignore */
    }

    // 针对登录/注册接口，直接抛出后端错误信息（如“账号已被管理员停用，请联系管理员”）供登录页拦截展示
    if (url.startsWith('/api/auth/')) {
      throw new Error(errorMsg)
    }

    // 针对业务接口（如 /api/watchlist），若收到 401（未登录）或 403（账号被封禁停用）
    if (res.status === 401 || res.status === 403) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(ROLE_KEY)
      localStorage.removeItem(VIP_KEY)
      localStorage.removeItem(WATCHLIST_CACHE_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
      throw new Error(errorMsg)
    }

    throw new Error(errorMsg)
  }

  const text = await res.text()
  return (text ? JSON.parse(text) : null) as T
}

export const api = {
  login: (username: string, password: string) =>
    request<{ token: string; username: string; role: string; isVip: boolean }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  register: (username: string, password: string, referralSource?: string) =>
    request<RegisterResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, password, referralSource }),
    }),

  listWatchlist: () => request<EstimateResult[]>('/api/watchlist'),

  addFund: (fundCode: string) =>
    request<unknown>('/api/watchlist', { method: 'POST', body: JSON.stringify({ fundCode }) }),

  addFunds: (fundCodes: string[]) =>
    request<AddFundResult[]>('/api/watchlist/batch', {
      method: 'POST',
      body: JSON.stringify({ fundCodes }),
    }),

  removeFund: (fundCode: string) =>
    request<unknown>(`/api/watchlist/${encodeURIComponent(fundCode)}`, { method: 'DELETE' }),

  reorder: (fundCodes: string[]) =>
    request<unknown>('/api/watchlist', { method: 'PUT', body: JSON.stringify({ fundCodes }) }),

  fundDetail: (code: string) =>
    request<FundDetailView>(`/api/fund/${encodeURIComponent(code)}`),

  fundPerformance: (code: string, range: string = '1m') =>
    request<FundPerformanceView>(
      `/api/fund/${encodeURIComponent(code)}/performance?range=${encodeURIComponent(range)}`
    ),

  // 持仓估值与收益预估 (VIP专享)
  getPortfolio: () => request<PortfolioView>('/api/portfolio'),

  savePosition: (data: PositionSaveRequest) =>
    request<{ success: boolean; id: number; fundCode: string }>('/api/portfolio', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  removePosition: (fundCode: string) =>
    request<unknown>(`/api/portfolio/${encodeURIComponent(fundCode)}`, {
      method: 'DELETE',
    }),

  getNotice: () => request<NoticeView | null>('/api/notice'),

  // 管理员后台接口
  listUsers: (keyword?: string) =>
    request<UserView[]>(`/api/admin/users${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`),

  getFundWatchers: (fundCode: string) =>
    request<UserView[]>(`/api/admin/funds/${encodeURIComponent(fundCode)}/watchers`),

  updateUserStatus: (username: string, status: string) =>
    request<unknown>(`/api/admin/users/${encodeURIComponent(username)}/status`, {
      method: 'POST',
      body: JSON.stringify({ status }),
    }),

  updateUserVip: (username: string, isVip: boolean, expireAt?: string | null) =>
    request<unknown>(`/api/admin/users/${encodeURIComponent(username)}/vip`, {
      method: 'POST',
      body: JSON.stringify({ isVip, expireAt }),
    }),

  updateUserRole: (username: string, role: string) =>
    request<unknown>(`/api/admin/users/${encodeURIComponent(username)}/role`, {
      method: 'POST',
      body: JSON.stringify({ role }),
    }),

  deleteUser: (username: string) =>
    request<unknown>(`/api/admin/users/${encodeURIComponent(username)}`, {
      method: 'DELETE',
    }),

  listFunds: () => request<FundView[]>('/api/admin/funds'),

  getUserWatchlist: (username: string) =>
    request<EstimateResult[]>(`/api/admin/users/${encodeURIComponent(username)}/watchlist`),

  getCalendarHolidays: () => request<CalendarHolidaysResponse>('/api/admin/calendar/holidays'),

  addCalendarHoliday: (date: string, description: string) =>
    request<unknown>('/api/admin/calendar/holidays', {
      method: 'POST',
      body: JSON.stringify({ date, description }),
    }),

  deleteCalendarHoliday: (date: string) =>
    request<unknown>(`/api/admin/calendar/holidays/${encodeURIComponent(date)}`, {
      method: 'DELETE',
    }),

  getAdminNotice: () => request<NoticeView | null>('/api/admin/notice'),

  setAdminNotice: (data: { message?: string; type?: string; enabled?: boolean; closable?: boolean }) =>
    request<NoticeView>('/api/admin/notice', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getSystemStats: () => request<SystemStats>('/api/admin/stats'),

  triggerIntraday: (force = true) =>
    request<unknown>(`/api/admin/intraday?force=${force}`, { method: 'POST' }),

  refreshNav: () => request<{ updatedFunds: number }>('/api/admin/refresh-nav', { method: 'POST' }),

  setBondDuration: (fundCode: string, reportQt: string, duration: number) =>
    request<unknown>('/api/admin/bond-duration', {
      method: 'POST',
      body: JSON.stringify({ fundCode, reportQt, duration }),
    }),

  // VIP配置 (Admin)
  getAdminVipConfig: () => request<VipConfigView>('/api/admin/vip-config'),

  setAdminVipConfig: (data: VipConfigView) =>
    request<VipConfigView>('/api/admin/vip-config', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // 注册审批开关 (Admin)
  getRegistrationApproval: () => request<{ requireApproval: boolean }>('/api/admin/registration-approval'),

  setRegistrationApproval: (requireApproval: boolean) =>
    request<{ success: boolean; requireApproval: boolean }>('/api/admin/registration-approval', {
      method: 'POST',
      body: JSON.stringify({ requireApproval }),
    }),

  // 登录用户信息动态同步
  getMe: () => request<UserProfileView>('/api/auth/me'),

  // 前台获取VIP价格与收款配置
  getVipConfig: () => request<VipConfigView>('/api/portfolio/vip-config'),

  // 前台提交VIP充值通知
  notifyVipRecharge: (data: { planKey: string; planName: string; amount: number; payMethod: string }) =>
    request<{ success: boolean; message: string }>('/api/portfolio/recharge-notify', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // 管理后台VIP充值订单列表与审批
  listVipOrders: () => request<VipOrderView[]>('/api/admin/vip-orders'),

  approveVipOrder: (id: number) =>
    request<{ success: boolean; message: string }>(`/api/admin/vip-orders/${id}/approve`, {
      method: 'POST',
    }),

  // 用户通知渠道与14:30盘中提醒
  getNotificationChannel: () => request<NotificationChannelView>('/api/user/notification-channel'),

  updateNotificationChannel: (data: NotificationChannelView) =>
    request<NotificationChannelView>('/api/user/notification-channel', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  testNotificationEmail: (email?: string) =>
    request<{ success: boolean; message: string }>('/api/user/notification-channel/test-email', {
      method: 'POST',
      body: email ? JSON.stringify({ email }) : undefined,
    }),

  testNotificationWechat: (wechatTarget?: string, wechatChannelType?: string) =>
    request<{ success: boolean; message: string }>('/api/user/notification-channel/test-wechat', {
      method: 'POST',
      body: wechatTarget ? JSON.stringify({ wechatTarget, wechatChannelType }) : undefined,
    }),

  // 管理后台系统邮件与提醒运维
  getAdminEmailConfig: () => request<EmailConfigView>('/api/admin/email-config'),

  setAdminEmailConfig: (data: EmailConfigView) =>
    request<EmailConfigView>('/api/admin/email-config', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  testAdminEmail: (to: string) =>
    request<{ success: boolean; message: string }>('/api/admin/email-config/test', {
      method: 'POST',
      body: JSON.stringify({ to }),
    }),

  triggerAdminDailyReminder: (force = true) =>
    request<{ success: boolean; sentCount: number; message: string }>(
      `/api/admin/email-config/trigger-daily-reminder?force=${force}`,
      { method: 'POST' }
    ),
}

export function sseUrl(): string {
  const token = localStorage.getItem(TOKEN_KEY)
  return `/api/watchlist/stream?token=${encodeURIComponent(token ?? '')}`
}
