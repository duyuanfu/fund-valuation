import type {
  EstimateResult,
  FundDetailView,
  NoticeView,
  UserView,
  CalendarHolidaysResponse,
  SystemStats,
  FundView,
} from './types'

const TOKEN_KEY = 'fund-valuation-token'

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
      localStorage.removeItem('fund-valuation-username')
      localStorage.removeItem('fund-valuation-role')
      localStorage.removeItem('fund-valuation-is-vip')
      localStorage.removeItem('fund-valuation-cached-watchlist')
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

  register: (username: string, password: string) =>
    request<{ token: string; username: string; role: string; isVip: boolean }>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  listWatchlist: () => request<EstimateResult[]>('/api/watchlist'),

  addFund: (fundCode: string) =>
    request<unknown>('/api/watchlist', { method: 'POST', body: JSON.stringify({ fundCode }) }),

  removeFund: (fundCode: string) =>
    request<unknown>(`/api/watchlist/${encodeURIComponent(fundCode)}`, { method: 'DELETE' }),

  reorder: (fundCodes: string[]) =>
    request<unknown>('/api/watchlist', { method: 'PUT', body: JSON.stringify({ fundCodes }) }),

  fundDetail: (code: string) =>
    request<FundDetailView>(`/api/fund/${encodeURIComponent(code)}`),

  getNotice: () => request<NoticeView | null>('/api/notice'),

  // 管理员后台接口
  listUsers: () => request<UserView[]>('/api/admin/users'),

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
}

export function sseUrl(): string {
  const token = localStorage.getItem(TOKEN_KEY)
  return `/api/watchlist/stream?token=${encodeURIComponent(token ?? '')}`
}
