import React, { useCallback, useState } from 'react'
import {
  AuthContext,
  TOKEN_KEY,
  USER_KEY,
  ROLE_KEY,
  VIP_KEY,
  WATCHLIST_CACHE_KEY,
} from './auth-context'

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string>(() => localStorage.getItem(TOKEN_KEY) ?? '')
  const [username, setUsername] = useState<string>(() => localStorage.getItem(USER_KEY) ?? '')
  const [role, setRole] = useState<string>(() => localStorage.getItem(ROLE_KEY) ?? 'USER')
  const [isVip, setIsVip] = useState<boolean>(() => localStorage.getItem(VIP_KEY) === 'true')

  const login = useCallback((t: string, u: string, r?: string, v?: boolean) => {
    const userRole = r ?? 'USER'
    const userVip = v ?? false
    localStorage.setItem(TOKEN_KEY, t)
    localStorage.setItem(USER_KEY, u)
    localStorage.setItem(ROLE_KEY, userRole)
    localStorage.setItem(VIP_KEY, String(userVip))
    setToken(t)
    setUsername(u)
    setRole(userRole)
    setIsVip(userVip)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(ROLE_KEY)
    localStorage.removeItem(VIP_KEY)
    localStorage.removeItem(WATCHLIST_CACHE_KEY)
    setToken('')
    setUsername('')
    setRole('USER')
    setIsVip(false)
  }, [])

  const isAdmin = role === 'ADMIN'

  return (
    <AuthContext.Provider value={{ token, username, role, isVip, isAdmin, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
