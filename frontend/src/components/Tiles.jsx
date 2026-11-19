export function Stat({ label, value }) {
  return <div className="card stat"><div className="num">{value}</div><div className="muted">{label}</div></div>
}

export function Upcoming({ items }) {
  return (
    <div className="card">
      <h3>Planned for later increments</h3>
      <ul className="upcoming">{items.map((i) => <li key={i}>{i}</li>)}</ul>
    </div>
  )
}
