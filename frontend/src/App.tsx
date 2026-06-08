import { useState } from 'react'
import BookingForm from './views/BookingForm'
import FloorMap from './views/FloorMap'
import ScoreDashboard from './views/ScoreDashboard'
import './App.css'

type Tab = 'book' | 'map'

const TABS: { id: Tab; label: string }[] = [
  { id: 'book', label: 'Book a Desk' },
  { id: 'map',  label: 'Floor Map' },
]

export default function App() {
  const [tab, setTab] = useState<Tab>('book')

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
        {tab === 'map' && (
          <div className="map-score-layout">
            <FloorMap />
            <div className="score-panel">
              <ScoreDashboard />
            </div>
          </div>
        )}
      </main>
    </div>
  )
}
