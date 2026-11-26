import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import Field from '../components/Field.jsx'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [sent, setSent] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError(''); setSent('')
    if (!email.trim()) { setError('Email address is required.'); return }
    setBusy(true)
    try {
      const res = await api('/auth/forgot-password', { method: 'POST', body: { email: email.trim() } })
      setSent(res.message)
    } catch (err) {
      setError(err.fieldErrors?.email || err.message)
    } finally { setBusy(false) }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={submit} noValidate>
        <h1 className="brand big">RiverWatch <span>Cebu</span></h1>
        <p className="muted">Enter your account email and we will send you a link to reset your password.</p>
        {sent && <div className="banner ok" role="status">{sent}</div>}
        <Field label="Email address" name="email" error={error}>
          <input id="email" type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} />
        </Field>
        <button className="btn primary" disabled={busy}>{busy ? 'Sending…' : 'Send reset link'}</button>
        <p className="muted small"><Link to="/login">Back to log in</Link></p>
      </form>
    </div>
  )
}
