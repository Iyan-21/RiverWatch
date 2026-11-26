import { NavLink, Outlet } from 'react-router-dom'
import { HOME, useAuth } from '../auth/AuthContext.jsx'

const NAV = {
  RESIDENT: [['Dashboard', HOME.RESIDENT], ['My profile', '/app/profile'], ['Change password', '/app/change-password']],
  OFFICER: [['Dashboard', HOME.OFFICER], ['Residents', '/app/officer/residents'], ['My profile', '/app/profile'], ['Change password', '/app/change-password']],
  ADMIN: [['Dashboard', HOME.ADMIN], ['Officer approvals', '/app/admin/officers'], ['My profile', '/app/profile'], ['Change password', '/app/change-password']],
}
const ROLE_LABEL = { RESIDENT: 'Household Resident', OFFICER: 'BDRRM Officer', ADMIN: 'System Administrator' }

export default function Layout() {
  const { user, logout } = useAuth()

  async function onLogout() {
    await logout() // clears the session; the route guard then sends the user to /login
  }

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">RiverWatch <span>Cebu</span></div>
        <nav aria-label="Main">
          {NAV[user.role].map(([label, to]) => (
            <NavLink key={to} to={to} end>{label}</NavLink>
          ))}
        </nav>
        <div className="who">
          <div><strong>{user.fullName}</strong><small>{ROLE_LABEL[user.role]}</small></div>
          <button className="btn ghost" onClick={onLogout}>Log out</button>
        </div>
      </header>
      <main className="content"><Outlet /></main>
    </div>
  )
}
