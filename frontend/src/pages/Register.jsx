import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import Field from '../components/Field.jsx'

const PHONE = /^(09\d{9}|\+639\d{9})$/
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validate(f) {
  const e = {}
  if (!f.fullName.trim()) e.fullName = 'Full name is required.'
  if (!f.email.trim()) e.email = 'Email address is required.'
  else if (!EMAIL.test(f.email.trim())) e.email = 'Enter a valid email address.'
  if (!f.phoneNumber.trim()) e.phoneNumber = 'Mobile number is required.'
  else if (!PHONE.test(f.phoneNumber.trim())) e.phoneNumber = 'Use a Philippine mobile number like 09171234567 or +639171234567.'
  if (!f.password) e.password = 'Password is required.'
  else if (f.password.length < 8) e.password = 'Password must be at least 8 characters.'
  if (f.confirm !== f.password) e.confirm = 'Passwords do not match.'
  if (!f.barangayId) e.barangayId = 'Please select your barangay.'
  return e
}

export default function Register() {
  const { kind } = useParams()
  const navigate = useNavigate()
  const [barangays, setBarangays] = useState([])
  const [form, setForm] = useState({ fullName: '', email: '', phoneNumber: '', password: '', confirm: '', barangayId: '' })
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api('/barangays').then(setBarangays).catch(() => setMessage('Could not load the barangay list. Is the server running?'))
  }, [])

  if (kind !== 'resident' && kind !== 'officer') return <Navigate to="/register/resident" replace />
  const isOfficer = kind === 'officer'
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    const fe = validate(form)
    setErrors(fe); setMessage('')
    if (Object.keys(fe).length) return
    setBusy(true)
    try {
      await api(`/auth/register/${kind}`, {
        method: 'POST',
        body: {
          fullName: form.fullName.trim(), email: form.email.trim(), phoneNumber: form.phoneNumber.trim(),
          password: form.password, barangayId: Number(form.barangayId),
        },
      })
      navigate('/login', {
        replace: true,
        state: { notice: isOfficer
          ? 'Officer account created. You can log in once an administrator approves it.'
          : 'Account created. You can now log in.' },
      })
    } catch (err) {
      setErrors(err.fieldErrors || {})
      setMessage(err.message)
    } finally { setBusy(false) }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card wide" onSubmit={submit} noValidate>
        <h1 className="brand big">RiverWatch <span>Cebu</span></h1>
        <div className="tabs" role="tablist">
          <Link role="tab" aria-selected={!isOfficer} className={!isOfficer ? 'on' : ''} to="/register/resident">Household resident</Link>
          <Link role="tab" aria-selected={isOfficer} className={isOfficer ? 'on' : ''} to="/register/officer">BDRRM officer</Link>
        </div>
        {isOfficer && <div className="banner info">Officer accounts must be approved by a System Administrator before you can log in.</div>}
        {message && <div className="banner err" role="alert">{message}</div>}

        <Field label="Full name" name="fullName" error={errors.fullName}>
          <input id="fullName" autoComplete="name" value={form.fullName} onChange={set('fullName')} />
        </Field>
        <Field label="Email address" name="email" error={errors.email}>
          <input id="email" type="email" autoComplete="email" value={form.email} onChange={set('email')} />
        </Field>
        <Field label="Mobile number" name="phoneNumber" error={errors.phoneNumber}>
          <input id="phoneNumber" inputMode="tel" placeholder="09171234567" value={form.phoneNumber} onChange={set('phoneNumber')} />
        </Field>
        <Field label="Barangay" name="barangayId" error={errors.barangayId}>
          <select id="barangayId" value={form.barangayId} onChange={set('barangayId')}>
            <option value="">Select your barangay…</option>
            {barangays.map((b) => <option key={b.barangayId} value={b.barangayId}>{b.name}, {b.cityMunicipality}</option>)}
          </select>
        </Field>
        <div className="two">
          <Field label="Password (min. 8 characters)" name="password" error={errors.password}>
            <input id="password" type="password" autoComplete="new-password" value={form.password} onChange={set('password')} />
          </Field>
          <Field label="Confirm password" name="confirm" error={errors.confirm}>
            <input id="confirm" type="password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} />
          </Field>
        </div>
        <button className="btn primary" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button>
        <p className="muted small">Already have an account? <Link to="/login">Log in</Link></p>
      </form>
    </div>
  )
}
