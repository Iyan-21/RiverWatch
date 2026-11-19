import { useDashboard } from '../components/useDashboard.js'
import { Stat, Upcoming } from '../components/Tiles.jsx'

export default function ResidentDashboard() {
  const { data, error } = useDashboard('/resident/dashboard')
  if (error) return <div className="banner err">{error}</div>
  if (!data) return <p className="muted">Loading…</p>
  const b = data.user.barangay
  return (
    <>
      <h2>Welcome, {data.user.fullName}</h2>
      <p className="muted">Resident of {b.name}, {b.cityMunicipality}</p>
      <div className="grid">
        <Stat label="Active BDRRM officers in your barangay" value={data.stats.activeOfficersInMyBarangay} />
      </div>
      <Upcoming items={['Household profile and alert channel (Telegram or SMS)', 'Flood alerts for your barangay', 'Evacuation check-in']} />
    </>
  )
}
