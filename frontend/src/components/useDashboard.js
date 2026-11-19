import { useEffect, useState } from 'react'
import { api } from '../api.js'

export function useDashboard(path) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { api(path).then(setData).catch((e) => setError(e.message)) }, [path])
  return { data, error }
}
