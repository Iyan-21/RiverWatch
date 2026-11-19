import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'

export default function AdminOfficers() {
  const [list, setList] = useState(null)
  const [error, setError] = useState('')
  const load = useCallback(() => api('/admin/officers/pending').then(setList).catch((e) => setError(e.message)), [])
  useEffect(() => { load() }, [load])

  async function decide(id, action) {
    setError('')
    try { await api(`/admin/officers/${id}/${action}`, { method: 'POST' }); await load() }
    catch (e) { setError(e.message); load() }
  }

  if (!list) return error ? <div className="banner err">{error}</div> : <p className="muted">Loading…</p>
  return (
    <>
      <h2>Officer approvals</h2>
      {error && <div className="banner err">{error}</div>}
      <div className="card">
        {list.length === 0 ? <p className="muted">No officer accounts are waiting for approval.</p> : (
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Mobile</th><th>Barangay</th><th></th></tr></thead>
            <tbody>
              {list.map((o) => (
                <tr key={o.userId}>
                  <td>{o.fullName}</td><td>{o.email}</td><td>{o.phoneNumber}</td>
                  <td>{o.barangay?.name}, {o.barangay?.cityMunicipality}</td>
                  <td className="actions">
                    <button className="btn primary" onClick={() => decide(o.userId, 'approve')}>Approve</button>
                    <button className="btn danger" onClick={() => decide(o.userId, 'reject')}>Reject</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
