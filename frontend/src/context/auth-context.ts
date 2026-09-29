import { createContext } from 'react'

export const TOKEN_KEY = 'fund-valuation-token'
export const USER_KEY = 'fund-valuation-username'
export const ROLE_KEY = 'fund-valuation-role'
export const VIP_KEY = 'fund-valuation-is-vip'
export const WATCHLIST_CACHE_KEY = 'fund-valuation-cached-watchlist'

export interface AuthContextValue {
  token: string
  username: string
  role: string
  isVip: boolean
  isAdmin: boolean
  login: (token: string, username: string, role?: string, isVip?: boolean) => void
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue>({
  token: '',
  username: '',
  role: 'USER',
  isVip: false,
  isAdmin: false,
  login: () => {},
  logout: () => {},
})
