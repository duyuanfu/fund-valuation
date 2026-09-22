import { createContext, useCallback, useContext, useState } from 'react'

const TOKEN_KEY = 'fund-valuation-token'
const USER_KEY = 'fund-valuation-username'

interface AuthContextValue {
  token: string
  username: string
  login: (token: string, username: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue>({
  token: '',
  username: '',
  login: () => {},
  logout: () => {},
})

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string>(() => localStorage.getItem(TOKEN_KEY) ?? '')
  const [username, setUsername] = useState<string>(() => localStorage.getItem(USER_KEY) ?? '')

  const login = useCallback((t: string, u: string) => {
    localStorage.setItem(TOKEN_KEY, t)
    localStorage.setItem(USER_KEY, u)
    setToken(t)
    setUsername(u)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem('fund-valuation-cached-watchlist')
    setToken('')
    setUsername('')
  }, [])

  return (
    <AuthContext.Provider value={{ token, username, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
