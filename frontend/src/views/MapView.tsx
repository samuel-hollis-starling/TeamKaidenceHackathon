import { useState, useEffect, useMemo } from 'react'
import type { Desk, Employee, OrgNode, AssignmentCollection } from '../types'
import { getDesks, getEmployees, getOrgNodes, getAssignments } from '../api'
import FloorMap from './FloorMap'
import type { Transform } from './FloorMap'
import ScoreDashboard from './ScoreDashboard'

function makeBranchColors(orgById: Record<string, OrgNode>): Map<string, string> {
  const branches = [...new Set(
    Object.values(orgById).map(n => n.orgPath[1] ?? n.orgPath[0])
  )].sort()
  return new Map(branches.map((b, i) => [b, `hsl(${Math.round((i / branches.length) * 360)}, 72%, 52%)`]))
}

function nodeColor(node: OrgNode, branchColors: Map<string, string>): string {
  return branchColors.get(node.orgPath[1] ?? node.orgPath[0]) ?? '#d1d5db'
}

export default function MapView() {
  const [desks, setDesks] = useState<Desk[]>([])
  const [empById, setEmpById] = useState<Record<string, Employee>>({})
  const [orgById, setOrgById] = useState<Record<string, OrgNode>>({})
  const [assignments, setAssignments] = useState<AssignmentCollection>({ deskByEmployeeId: {}, employeeByDeskId: {} })
  const [transform, setTransform] = useState<Transform>({ scale: 0.18, tx: 20, ty: 20 })
  const [selectedDeskId, setSelectedDeskId] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([getDesks(), getEmployees(), getOrgNodes(), getAssignments()]).then(
      ([d, emps, nodes, ass]) => {
        setDesks(d)
        setEmpById(Object.fromEntries(emps.map(e => [e.id, e])))
        setOrgById(Object.fromEntries(nodes.map(n => [n.employeeId, n])))
        setAssignments(ass)
      }
    )
  }, [])

  function panToDesk(deskId: string) {
    const desk = desks.find(d => d.id === deskId)
    if (!desk) return
    const scale = 0.55
    const areaW = window.innerWidth - 220 - 320
    const areaH = window.innerHeight - 60
    setTransform({ scale, tx: areaW / 2 - desk.x * scale, ty: areaH / 2 - desk.y * scale })
    setSelectedDeskId(deskId)
  }

  const branchColors = useMemo(() => makeBranchColors(orgById), [orgById])
  const branches = useMemo(() => [...branchColors.entries()].sort((a, b) => a[0].localeCompare(b[0])), [branchColors])

  const employees = useMemo(
    () => Object.values(empById).sort((a, b) => a.name.localeCompare(b.name)),
    [empById]
  )

  return (
    <div className="map-view-layout">
      <div className="people-panel">
        {branches.length > 0 && (
          <>
            <div className="panel-section-title">Teams</div>
            {branches.map(([name, color]) => (
              <div key={name} className="key-row">
                <span className="person-dot" style={{ background: color }} />
                <span className="key-label">{name}</span>
              </div>
            ))}
          </>
        )}

        <div className="panel-section-title">People</div>
        {employees.map(emp => {
          const deskId = assignments.deskByEmployeeId[emp.id]
          const org = orgById[emp.id]
          const color = org ? nodeColor(org, branchColors) : '#d1d5db'
          return (
            <button
              key={emp.id}
              className={`person-row${deskId ? '' : ' no-desk'}`}
              onClick={() => deskId && panToDesk(deskId)}
              disabled={!deskId}
              title={emp.role}
            >
              <span className="person-dot" style={{ background: color }} />
              <span className="person-name">{emp.name}</span>
            </button>
          )
        })}
      </div>

      <div className="map-area">
        <FloorMap
          desks={desks}
          empById={empById}
          orgById={orgById}
          assignments={assignments}
          transform={transform}
          onTransformChange={setTransform}
          selectedDeskId={selectedDeskId}
          branchColors={branchColors}
        />
      </div>

      <div className="score-panel">
        <ScoreDashboard />
      </div>
    </div>
  )
}
