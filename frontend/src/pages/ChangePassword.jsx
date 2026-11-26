import { useState } from 'react'
import { api } from '../api.js'
import Field from '../components/Field.jsx'

export default function ChangePassword() {
  const empty = { currentPassword: '', newPassword: '', confirmPassword: '' }
  const [form, setForm] = useState(empty)
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [done, setDone] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    const fe = {}
    if (!form.currentPassword) fe.currentPassword = 'Enter your current password.'
    if (!form.newPassword) fe.newPassword = 'Enter a new password.'
    else if (form.newPassword.length < 8) fe.newPassword = 'Password must be at least 8 characters.'
    else if (form.newPassword === form.currentPassword) fe.newPassword = 'New password must be different from the current password.'
    if (form.confirmPassword !== form.newPassword) fe.confirmPassword = 'Passwords do not match.'
    setErrors(fe); setMessage(''); setDone('')
    if (Object.keys(fe).length) return
    setBusy(true)
    try {
      const res = await api('/auth/change-password', { method: 'POST', body: form })
      setDone(res.message)
      setForm(empty)
    } catch (err) {
      setErrors(err.fieldErrors || {})
      setMessage(err.message)
    } finally { setBusy(false) }
  }

  return (
    <>
      <h2>Change password</h2>
      <form className="card form-card" onSubmit={submit} noValidate>
        {done && <div className="banner ok" role="status">{done}</div>}
        {message && <div className="banner err" role="alert">{message}</div>}
        <Field label="Current password" name="currentPassword" error={errors.currentPassword}>
          <input id="currentPassword" type="password" autoComplete="current-password" value={form.currentPassword} onChange={set('currentPassword')} />
        </Field>
        <Field label="New password (min. 8 characters)" name="newPassword" error={errors.newPassword}>
          <input id="newPassword" type="password" autoComplete="new-password" value={form.newPassword} onChange={set('newPassword')} />
        </Field>
        <Field label="Confirm new password" name="confirmPassword" error={errors.confirmPassword}>
          <input id="confirmPassword" type="password" autoComplete="new-password" value={form.confirmPassword} onChange={set('confirmPassword')} />
        </Field>
        <button className="btn primary" disabled={busy}>{busy ? 'Saving…' : 'Change password'}</button>
      </form>
    </>
  )
}
