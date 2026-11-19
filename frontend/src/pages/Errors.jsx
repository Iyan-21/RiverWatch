import { Link } from 'react-router-dom'
import { HOME, useAuth } from '../auth/AuthContext.jsx'

export function Forbidden() {
  const { user } = useAuth()
  return (
    <div className="auth-page"><div className="card auth-card center">
      <h1>403</h1><p>You do not have permission to open that page.</p>
      <Link className="btn primary" to={user ? HOME[user.role] : '/login'}>Back to my dashboard</Link>
    </div></div>
  )
}

export function NotFound() {
  return (
    <div className="auth-page"><div className="card auth-card center">
      <h1>404</h1><p>That page does not exist.</p>
      <Link className="btn primary" to="/">Go home</Link>
    </div></div>
  )
}
