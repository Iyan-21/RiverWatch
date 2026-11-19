import { useEffect, useState } from 'react'
import { api } from '../api.js'

export default function Profile() {
  const [me, setMe] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { api('/auth/me').then(setMe).catch((e) => setError(e.message)) }, [])
  if (error) return <div className="banner err">{error}</div>
  if (!me) return <p className="muted">Loading…</p>
  const rows = [
    ['Full name', me.fullName], ['Email', me.email], ['Mobile number', me.phoneNumber || '—'],
    ['Role', me.role], ['Account status', me.status],
    ['Barangay', me.barangay ? `${me.barangay.name}, ${me.barangay.cityMunicipality}` : 'All barangays'],
  ]
  return (
    <>
      <h2>My profile</h2>
      <dl className="card details">
        {rows.map(([k, v]) => (<div key={k}><dt>{k}</dt><dd>{v}</dd></div>))}
      </dl>
    </>
  )
}
