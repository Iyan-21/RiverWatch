export default function Field({ label, name, error, children }) {
  return (
    <label className={`field${error ? ' has-error' : ''}`} htmlFor={name}>
      <span>{label}</span>
      {children}
      {error && <em role="alert">{error}</em>}
    </label>
  )
}
