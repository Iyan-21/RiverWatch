import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth/AuthContext.jsx'
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
  return e
}

export default function Profile() {
  const { setUser } = useAuth()
  const [me, setMe] = useState(null)
  const [form, setForm] = useState({ fullName: '', email: '', phoneNumber: '' })
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [saved, setSaved] = useState('')
  const [busy, setBusy] = useState(false)

  function fill(u) {
    setMe(u)
    setForm({ fullName: u.fullName, email: u.email, phoneNumber: u.phoneNumber || '' })
  }

  useEffect(() => { api('/profile').then(fill).catch((e) => setMessage(e.message)) }, [])

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    const fe = validate(form)
    setErrors(fe); setMessage(''); setSaved('')
    if (Object.keys(fe).length) return
    setBusy(true)
    try {
      const updated = await api('/profile', { method: 'PUT', body: {
        fullName: form.fullName.trim(), email: form.email.trim(), phoneNumber: form.phoneNumber.trim(),
      } })
      fill(updated)
      setUser(updated) // keeps the name in the top bar up to date
      setSaved('Profile saved.')
    } catch (err) {
      setErrors(err.fieldErrors || {})
      setMessage(err.message)
    } finally { setBusy(false) }
  }

  if (!me) return message ? <div className="banner err">{message}</div> : <p className="muted">Loading…</p>

  return (
    <>
      <h2>My profile</h2>
      <form className="card form-card" onSubmit={submit} noValidate>
        {saved && <div className="banner ok" role="status">{saved}</div>}
        {message && <div className="banner err" role="alert">{message}</div>}
        <Field label="Full name" name="fullName" error={errors.fullName}>
          <input id="fullName" value={form.fullName} onChange={set('fullName')} />
        </Field>
        <Field label="Email address" name="email" error={errors.email}>
          <input id="email" type="email" value={form.email} onChange={set('email')} />
        </Field>
        <Field label="Mobile number" name="phoneNumber" error={errors.phoneNumber}>
          <input id="phoneNumber" inputMode="tel" value={form.phoneNumber} onChange={set('phoneNumber')} />
        </Field>
        <dl className="details-grid">
          <dt>Role</dt><dd>{me.role}</dd>
          <dt>Account status</dt><dd>{me.status}</dd>
          <dt>Barangay</dt><dd>{me.barangay ? `${me.barangay.name}, ${me.barangay.cityMunicipality}` : 'All barangays'}</dd>
        </dl>
        <button className="btn primary" disabled={busy}>{busy ? 'Saving…' : 'Save changes'}</button>
      </form>
    </>
  )
}
