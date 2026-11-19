import { useEffect, useState } from 'react'
import { api } from '../api.js'

export default function OfficerResidents() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { api(`/officer/residents?page=${page}`).then(setData).catch((e) => setError(e.message)) }, [page])
  if (error) return <div className="banner err">{error}</div>
  if (!data) return <p className="muted">Loading…</p>
  const pages = Math.max(1, Math.ceil(data.total / data.size))
  return (
    <>
      <h2>Residents of your barangay</h2>
      <div className="card">
        {data.items.length === 0 ? <p className="muted">No residents have registered yet.</p> : (
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Mobile</th></tr></thead>
            <tbody>{data.items.map((r) => <tr key={r.userId}><td>{r.fullName}</td><td>{r.email}</td><td>{r.phoneNumber}</td></tr>)}</tbody>
          </table>
        )}
        <div className="pager">
          <button className="btn ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span className="muted">Page {page + 1} of {pages} · {data.total} total</span>
          <button className="btn ghost" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      </div>
    </>
  )
}
