import { createContext, useCallback, useContext, useState } from 'react'

const TOKEN_KEY = 'fund-valuation-token'
const USER_KEY = 'fund-valuation-username'
const ROLE_KEY = 'fund-valuation-role'
const VIP_KEY = 'fund-valuation-is-vip'

interface AuthContextValue {
  token: string
  username: string
  role: string
  isVip: boolean
  isAdmin: boolean
  login: (token: string, username: string, role?: string, isVip?: boolean) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue>({
  token: '',
  username: '',
  role: 'USER',
  isVip: false,
  isAdmin: false,
  login: () => {},
  logout: () => {},
})

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
    localStorage.removeItem('fund-valuation-cached-watchlist')
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

export function useAuth() {
  return useContext(AuthContext)
}
