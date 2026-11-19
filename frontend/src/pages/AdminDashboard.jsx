import { Link } from 'react-router-dom'
import { useDashboard } from '../components/useDashboard.js'
import { Stat, Upcoming } from '../components/Tiles.jsx'

export default function AdminDashboard() {
  const { data, error } = useDashboard('/admin/dashboard')
  if (error) return <div className="banner err">{error}</div>
  if (!data) return <p className="muted">Loading…</p>
  const s = data.stats
  return (
    <>
      <h2>System Administrator dashboard</h2>
      <div className="grid">
        <Stat label="Registered residents" value={s.residents} />
        <Stat label="Active officers" value={s.activeOfficers} />
        <Stat label="Officers awaiting approval" value={s.pendingOfficers} />
      </div>
      <div className="card"><h3>Officer approvals</h3><p className="muted">Approve or reject BDRRM officer registrations.</p><Link className="btn primary" to="/app/admin/officers">Review requests</Link></div>
      <Upcoming items={['Barangay and flood-zone setup', 'Audit log viewer']} />
    </>
  )
}
