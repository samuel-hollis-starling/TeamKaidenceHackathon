import { useState, useEffect } from 'react'
import type { Employee, SocialPreference, BookingCollection } from '../types'
import { getEmployees, getBookings, addBooking } from '../api'

export default function BookingForm() {
  const [employees, setEmployees] = useState<Employee[]>([])
  const [collection, setCollection] = useState<BookingCollection | null>(null)
  const [search, setSearch] = useState('')
  const [selectedId, setSelectedId] = useState('')
  const [socialPref, setSocialPref] = useState<SocialPreference>('NONE')
  const [feelingLucky, setFeelingLucky] = useState(false)
  const [confirmed, setConfirmed] = useState<string | null>(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    getEmployees().then(setEmployees)
    getBookings().then(setCollection)
  }, [])

  const filtered = employees.filter(e =>
    e.name.toLowerCase().includes(search.toLowerCase())
  )

  const selected = employees.find(e => e.id === selectedId)

  function pickEmployee(e: Employee) {
    setSelectedId(e.id)
    setSearch(e.name)
    setOpen(false)
  }

  async function submit(ev: React.FormEvent) {
    ev.preventDefault()
    if (!selectedId) return
    await addBooking({ employeeId: selectedId, socialPreference: socialPref, feelingLucky })
    const updated = await getBookings()
    setCollection(updated)
    setConfirmed(selected?.name ?? selectedId)
    setSelectedId('')
    setSearch('')
  }

  const remaining = collection?.remaining ?? 191
  const total = collection?.totalCapacity ?? 191
  const pct = Math.round(((total - remaining) / total) * 100)

  return (
    <div className="view booking-form">
      <h2>Book a Desk</h2>

      <div className="capacity-bar">
        <div className="capacity-fill" style={{ width: `${pct}%` }} />
        <span className="capacity-label">{remaining} of {total} desks remaining</span>
      </div>

      {confirmed && (
        <div className="confirmation">
          Booking registered for <strong>{confirmed}</strong>. Desk assigned on arrival.
        </div>
      )}

      <form onSubmit={submit}>
        <label>Employee</label>
        <div className="dropdown-wrap">
          <input
            className="dropdown-input"
            placeholder="Search name…"
            value={search}
            onFocus={() => setOpen(true)}
            onChange={e => { setSearch(e.target.value); setOpen(true); setSelectedId('') }}
          />
          {open && filtered.length > 0 && (
            <ul className="dropdown-list">
              {filtered.slice(0, 12).map(e => (
                <li key={e.id} onMouseDown={() => pickEmployee(e)}>
                  <span className="emp-name">{e.name}</span>
                  <span className="emp-role">{e.role}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <label>Social preference</label>
        <div className="radio-group">
          {(['TALK_TO_ME', 'NONE', 'DONT_TALK_TO_ME'] as SocialPreference[]).map(v => (
            <label key={v} className={`radio-pill ${socialPref === v ? 'active' : ''}`}>
              <input type="radio" name="social" value={v} checked={socialPref === v} onChange={() => setSocialPref(v)} />
              {v === 'TALK_TO_ME' ? '💬 Talk to me' : v === 'NONE' ? '— No preference' : '🤫 Don\'t talk to me'}
            </label>
          ))}
        </div>

        <div className="toggle-row">
          <label className="toggle">
            <input type="checkbox" checked={feelingLucky} onChange={e => setFeelingLucky(e.target.checked)} />
            <span>Feeling lucky 🍀</span>
          </label>
        </div>

        <button type="submit" disabled={!selectedId}>Register attendance</button>
      </form>
    </div>
  )
}
