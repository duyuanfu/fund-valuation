import type { EstimateResult, FundDetailView } from './types'

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
  if (res.status === 401) {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem('fund-valuation-username')
    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login'
    }
    throw new Error('未登录或登录已过期')
  }
  if (!res.ok) {
    let msg = `请求失败 (${res.status})`
    try {
      const body = await res.json()
      if (body && body.error) msg = body.error
    } catch {
      /* ignore */
    }
    throw new Error(msg)
  }
  const text = await res.text()
  return (text ? JSON.parse(text) : null) as T
}

export const api = {
  login: (username: string, password: string) =>
    request<{ token: string; username: string }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  register: (username: string, password: string) =>
    request<{ token: string; username: string }>('/api/auth/register', {
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
}

export function sseUrl(): string {
  const token = localStorage.getItem(TOKEN_KEY)
  return `/api/watchlist/stream?token=${encodeURIComponent(token ?? '')}`
}
