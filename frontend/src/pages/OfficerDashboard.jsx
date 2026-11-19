import { Link } from 'react-router-dom'
import { useDashboard } from '../components/useDashboard.js'
import { Stat, Upcoming } from '../components/Tiles.jsx'

export default function OfficerDashboard() {
  const { data, error } = useDashboard('/officer/dashboard')
  if (error) return <div className="banner err">{error}</div>
  if (!data) return <p className="muted">Loading…</p>
  const b = data.user.barangay
  return (
    <>
      <h2>BDRRM Officer dashboard</h2>
      <p className="muted">{data.user.fullName} · {b.name}, {b.cityMunicipality}</p>
      <div className="grid">
        <Stat label="Registered residents in your barangay" value={data.stats.registeredResidents} />
        <div className="card"><h3>Residents</h3><p className="muted">See the residents registered in your barangay.</p><Link className="btn primary" to="/app/officer/residents">Open list</Link></div>
      </div>
      <Upcoming items={['Rainfall thresholds', 'Alert history', 'Evacuation events and center capacity', 'Barangay map']} />
    </>
  )
}
