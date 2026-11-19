import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import * as client from '../api.js'

const AuthContext = createContext(null)

export const HOME = { RESIDENT: '/app/resident', OFFICER: '/app/officer', ADMIN: '/app/admin' }

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [flash, setFlash] = useState('')

  useEffect(() => {
    client.setSessionHandler(setUser)
    // On page load, try to restore the session from the refresh cookie.
    client.refreshSession().finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (email, password) => {
    const u = await client.login(email, password)
    setFlash('')
    setUser(u)
    return u
  }, [])

  const logout = useCallback(async () => {
    try { await client.logout() } finally { setFlash('You have been logged out.'); setUser(null) }
  }, [])

  const value = useMemo(() => ({ user, loading, flash, login, logout }), [user, loading, flash, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() { return useContext(AuthContext) }
