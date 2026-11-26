// Small fetch wrapper. The access token lives only in memory (never localStorage);
// the refresh token is an HttpOnly cookie that JavaScript cannot read.
let accessToken = null
let onSession = () => {}
let refreshing = null

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export function setSessionHandler(fn) { onSession = fn }
export function setAccessToken(t) { accessToken = t }

async function parse(res) {
  if (res.status === 204) return null
  const text = await res.text()
  try { return text ? JSON.parse(text) : null } catch { return null }
}

function toError(res, data) {
  return new ApiError(res.status, data?.message || 'Something went wrong. Please try again.', data?.fieldErrors || {})
}

/** One refresh at a time, so parallel callers never reuse a rotated refresh cookie. */
export function refreshSession() {
  if (!refreshing) {
    refreshing = fetch('/api/v1/auth/refresh', { method: 'POST', credentials: 'include' })
      .then(async (res) => {
        const data = await parse(res)
        if (!res.ok) { accessToken = null; onSession(null); return null }
        accessToken = data.accessToken
        onSession(data.user)
        return data
      })
      .catch(() => { accessToken = null; onSession(null); return null })
      .finally(() => { refreshing = null })
  }
  return refreshing
}

async function send(path, { method = 'GET', body } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`
  return fetch(`/api/v1${path}`, {
    method, headers, credentials: 'include',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
}

export async function api(path, options = {}) {
  let res = await send(path, options)
  if (res.status === 401 && accessToken && path !== '/auth/login' && path !== '/auth/refresh') {
    const renewed = await refreshSession() // access token expired: try the refresh cookie once
    if (renewed) res = await send(path, options)
  }
  const data = await parse(res)
  if (!res.ok) throw toError(res, data)
  return data
}

export async function login(email, password) {
  const res = await send('/auth/login', { method: 'POST', body: { email, password } })
  const data = await parse(res)
  if (!res.ok) throw toError(res, data)
  accessToken = data.accessToken
  return data.user
}

export async function logout() {
  try { await send('/auth/logout', { method: 'POST' }) } finally { accessToken = null }
}
