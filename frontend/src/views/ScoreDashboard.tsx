import { useState, useEffect } from 'react'
import type { AssignmentScore } from '../types'
import { getScore, runAssignment } from '../api'

type Metric = { label: string; key: keyof AssignmentScore; invert?: boolean; description: string }

const METRICS: Metric[] = [
  { label: 'Team Cohesion',       key: 'teamCohesion',       description: 'How closely teammates sit together' },
  { label: 'Manager Proximity',   key: 'managerProximity',   description: 'How close reports sit to their manager' },
  { label: 'Social Satisfaction', key: 'socialSatisfaction', description: 'Social preferences honoured' },
  { label: 'QAP Cost',            key: 'totalQapCost',       invert: true, description: 'Overall assignment cost — lower is better' },
]

function scoreColor(value: number) {
  if (value >= 75) return '#349C51'  // green 600
  if (value >= 50) return '#E18637'  // orange 500
  return '#CE3D3D'                   // red 600
}

export default function ScoreDashboard() {
  const [score, setScore] = useState<AssignmentScore | null>(null)
  const [running, setRunning] = useState(false)

  useEffect(() => { getScore().then(setScore) }, [])

  async function run() {
    setRunning(true)
    await runAssignment()
    const s = await getScore()
    setScore(s)
    setRunning(false)
  }

  return (
    <div className="view score-dashboard">
      <h2>Assignment Score</h2>
      <p className="score-subtitle">All scores 0–100. Higher is better except QAP Cost.</p>

      <div className="score-metrics">
        {METRICS.map(m => {
          const raw = score?.[m.key] ?? 0
          const display = m.invert ? 100 - raw : raw
          const color = scoreColor(display)
          return (
            <div key={m.key} className="metric-row">
              <div className="metric-header">
                <span className="metric-label">{m.label}</span>
                <span className="metric-value" style={{ color }}>{Math.round(display)}</span>
              </div>
              <div className="metric-track">
                <div className="metric-fill" style={{ width: `${display}%`, background: color }} />
              </div>
              <div className="metric-desc">{m.description}</div>
            </div>
          )
        })}
      </div>

      <button className="run-btn" onClick={run} disabled={running}>
        {running ? 'Running…' : 'Run assignment'}
      </button>
    </div>
  )
}
