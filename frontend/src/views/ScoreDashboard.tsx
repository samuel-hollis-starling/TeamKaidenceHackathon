import { useState, useEffect, useRef } from 'react'
import type { AssignmentScore } from '../types'
import { getScore } from '../api'

type Metric = { label: string; key: keyof AssignmentScore; invert?: boolean; description: string; forceColor?: string }

const METRICS: Metric[] = [
  { label: 'Team Cohesion',       key: 'teamCohesion',       description: 'Teammates seated together' },
  { label: 'Manager Proximity',   key: 'managerProximity',   description: 'Reports close to their manager' },
  { label: 'Social Satisfaction', key: 'socialSatisfaction', description: 'Social preferences honoured' },
  { label: 'QAP Cost',            key: 'totalQapCost',       invert: true, description: 'Overall desk assignment efficiency', forceColor: '#349C51' },
]

function scoreColor(value: number) {
  if (value >= 75) return '#349C51'
  if (value >= 50) return '#E18637'
  return '#CE3D3D'
}

function AnimatedNumber({ target }: { target: number }) {
  const [displayed, setDisplayed] = useState(0)
  const raf = useRef<number>(0)
  const start = useRef<number | null>(null)

  useEffect(() => {
    start.current = null
    const duration = 900
    const from = 0
    function step(ts: number) {
      if (start.current === null) start.current = ts
      const t = Math.min((ts - start.current) / duration, 1)
      const eased = 1 - Math.pow(1 - t, 3)
      setDisplayed(Math.round(from + (target - from) * eased))
      if (t < 1) raf.current = requestAnimationFrame(step)
    }
    raf.current = requestAnimationFrame(step)
    return () => cancelAnimationFrame(raf.current)
  }, [target])

  return <>{displayed}</>
}

export default function ScoreDashboard() {
  const [score, setScore] = useState<AssignmentScore | null>(null)

  useEffect(() => { getScore().then(setScore) }, [])

  return (
    <div className="view score-dashboard">
      <h2>Assignment Score</h2>

      <div className="score-metrics">
        {METRICS.map(m => {
          const raw = score?.[m.key] ?? 0
          const display = Math.round(m.invert ? 100 - raw : raw)
          const color = m.forceColor ?? scoreColor(display)
          return (
            <div key={m.key} className="metric-row">
              <div className="metric-header">
                <span className="metric-label">{m.label}</span>
                <span className="metric-value" style={{ color }}>
                  <AnimatedNumber target={display} />
                </span>
              </div>
              <div className="metric-track">
                <div className="metric-fill" style={{ width: `${display}%`, background: color, boxShadow: `0 0 8px ${color}99` }} />
              </div>
              <div className="metric-desc">{m.description}</div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
