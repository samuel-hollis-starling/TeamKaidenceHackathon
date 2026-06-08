import { useState, useEffect, useMemo } from 'react'
import type { Desk, Employee, OrgNode, AssignmentCollection } from '../types'
import { getDesks, getEmployees, getOrgNodes, getAssignments } from '../api'
import FloorMap, { hashHue } from './FloorMap'
import type { Transform } from './FloorMap'
import ScoreDashboard from './ScoreDashboard'

function orgColor(node: OrgNode): string {
  const branch = node.orgPath[1] ?? node.orgPath[0]
  return `hsl(${hashHue(branch)},65%,${45 + node.depth * 8}%)`
}

export default function MapView() {
  const [desks, setDesks] = useState<Desk[]>([])
  const [empById, setEmpById] = useState<Record<string, Employee>>({})
  const [orgById, setOrgById] = useState<Record<string, OrgNode>>({})
  const [assignments, setAssignments] = useState<AssignmentCollection>({ deskByEmployeeId: {}, employeeByDeskId: {} })
  const [transform, setTransform] = useState<Transform>({ scale: 0.18, tx: 20, ty: 20 })

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
  }

  const branches = useMemo(() => {
    const seen = new Map<string, string>()
    for (const node of Object.values(orgById)) {
      const branch = node.orgPath[1] ?? node.orgPath[0]
      if (!seen.has(branch)) seen.set(branch, `hsl(${hashHue(branch)},65%,50%)`)
    }
    return [...seen.entries()].sort((a, b) => a[0].localeCompare(b[0]))
  }, [orgById])

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
          const color = org ? orgColor(org) : '#d1d5db'
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
        />
      </div>

      <div className="score-panel">
        <ScoreDashboard />
      </div>
    </div>
  )
}
