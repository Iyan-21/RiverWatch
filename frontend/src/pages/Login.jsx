import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { HOME, useAuth } from '../auth/AuthContext.jsx'
import Field from '../components/Field.jsx'

export default function Login() {
  const { login, flash } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const notice = location.state?.notice || flash

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    const fe = {}
    if (!form.email.trim()) fe.email = 'Email address is required.'
    if (!form.password) fe.password = 'Password is required.'
    setErrors(fe); setMessage('')
    if (Object.keys(fe).length) return
    setBusy(true)
    try {
      const user = await login(form.email, form.password)
      navigate(HOME[user.role], { replace: true }) // each role lands on its own dashboard
    } catch (err) {
      setErrors(err.fieldErrors || {})
      setMessage(err.message)
    } finally { setBusy(false) }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={submit} noValidate>
        <h1 className="brand big">RiverWatch <span>Cebu</span></h1>
        <p className="muted">Log in to your account</p>
        {notice && <div className="banner ok" role="status">{notice}</div>}
        {message && <div className="banner err" role="alert">{message}</div>}
        <Field label="Email address" name="email" error={errors.email}>
          <input id="email" type="email" autoComplete="username" value={form.email} onChange={set('email')} />
        </Field>
        <Field label="Password" name="password" error={errors.password}>
          <input id="password" type="password" autoComplete="current-password" value={form.password} onChange={set('password')} />
        </Field>
        <button className="btn primary" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>
        <p className="muted small">
          New here? <Link to="/register/resident">Register as a resident</Link> or{' '}
          <Link to="/register/officer">as a BDRRM officer</Link>.
        </p>
      </form>
    </div>
  )
}
