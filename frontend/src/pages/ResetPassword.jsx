import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'
import Field from '../components/Field.jsx'

export default function ResetPassword() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const navigate = useNavigate()
  const [state, setState] = useState('checking') // checking | ok | invalid
  const [invalidMsg, setInvalidMsg] = useState('')
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api(`/auth/reset-password/validate?token=${encodeURIComponent(token)}`)
      .then(() => setState('ok'))
      .catch((e) => { setInvalidMsg(e.message); setState('invalid') })
  }, [token])

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    const fe = {}
    if (!form.newPassword) fe.newPassword = 'Enter a new password.'
    else if (form.newPassword.length < 8) fe.newPassword = 'Password must be at least 8 characters.'
    if (form.confirmPassword !== form.newPassword) fe.confirmPassword = 'Passwords do not match.'
    setErrors(fe); setMessage('')
    if (Object.keys(fe).length) return
    setBusy(true)
    try {
      await api('/auth/reset-password', { method: 'POST', body: { token, ...form } })
      navigate('/login', { replace: true, state: { notice: 'Password reset. You can now log in with your new password.' } })
    } catch (err) {
      if (err.status === 410) { setInvalidMsg(err.message); setState('invalid') }
      setErrors(err.fieldErrors || {})
      setMessage(err.message)
    } finally { setBusy(false) }
  }

  if (state === 'checking') return <div className="splash">Checking your link…</div>

  if (state === 'invalid') {
    return (
      <div className="auth-page"><div className="card auth-card center">
        <h1 className="brand big">RiverWatch <span>Cebu</span></h1>
        <div className="banner err" role="alert">{invalidMsg || 'This reset link is invalid or has expired.'}</div>
        <Link className="btn primary" to="/forgot-password">Request a new link</Link>
      </div></div>
    )
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={submit} noValidate>
        <h1 className="brand big">RiverWatch <span>Cebu</span></h1>
        <p className="muted">Choose a new password for your account.</p>
        {message && <div className="banner err" role="alert">{message}</div>}
        <Field label="New password (min. 8 characters)" name="newPassword" error={errors.newPassword}>
          <input id="newPassword" type="password" autoComplete="new-password" value={form.newPassword} onChange={set('newPassword')} />
        </Field>
        <Field label="Confirm new password" name="confirmPassword" error={errors.confirmPassword}>
          <input id="confirmPassword" type="password" autoComplete="new-password" value={form.confirmPassword} onChange={set('confirmPassword')} />
        </Field>
        <button className="btn primary" disabled={busy}>{busy ? 'Saving…' : 'Reset password'}</button>
      </form>
    </div>
  )
}
