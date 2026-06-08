import { useState } from 'react'
import BookingForm from './views/BookingForm'
import MapView from './views/MapView'
import OrgChart from './views/OrgChart'
import './App.css'

type Tab = 'book' | 'map' | 'org'

const TABS: { id: Tab; label: string }[] = [
  { id: 'book', label: 'Book a Desk' },
  { id: 'map',  label: 'Floor Map' },
  { id: 'org',  label: 'Org Chart' },
]

export default function App() {
  const [tab, setTab] = useState<Tab>('book')
  const [focusOrgId, setFocusOrgId] = useState<string | null>(null)

  function viewInOrg(employeeId: string) {
    setFocusOrgId(employeeId)
    setTab('org')
  }

  return (
    <div className="app">
      <header className="app-header">
        <span className="app-logo">kAIdence</span>
        <nav className="app-nav">
          {TABS.map(t => (
            <button
              key={t.id}
              className={`nav-tab ${tab === t.id ? 'active' : ''}`}
              onClick={() => setTab(t.id)}
            >
              {t.label}
            </button>
          ))}
        </nav>
      </header>
      <main className="app-main">
        {tab === 'book' && <BookingForm />}
        {tab === 'map' && <MapView onViewInOrg={viewInOrg} />}
        {tab === 'org' && <OrgChart focusId={focusOrgId} />}
      </main>
    </div>
  )
}
