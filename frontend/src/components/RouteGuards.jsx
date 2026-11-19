import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { HOME, useAuth } from '../auth/AuthContext.jsx'

export function Splash() { return <div className="splash">Loading RiverWatch…</div> }

/** Any signed-in user. */
export function RequireAuth() {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <Splash />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

/** Only the listed roles; everyone else is sent to the 403 page. */
export function RequireRole({ roles }) {
  const { user } = useAuth()
  if (!user || !roles.includes(user.role)) return <Navigate to="/forbidden" replace />
  return <Outlet />
}

/** Login and registration pages: signed-in users go straight to their own dashboard. */
export function GuestOnly() {
  const { user, loading } = useAuth()
  if (loading) return <Splash />
  if (user) return <Navigate to={HOME[user.role]} replace />
  return <Outlet />
}
