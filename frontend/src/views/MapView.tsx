import { useState, useEffect, useMemo } from 'react'
import type { Desk, Employee, OrgNode, AssignmentCollection } from '../types'
import { getDesks, getEmployees, getOrgNodes, getAssignments } from '../api'
import FloorMap from './FloorMap'
import type { Transform } from './FloorMap'
import ScoreDashboard from './ScoreDashboard'

// Starling chromatic families, shades 400→800 (light to dark), readable on map backgrounds.
const STARLING_FAMILIES = [
  ['#68C482', '#4AB067', '#349C51', '#238940', '#187532'],  // green
  ['#35E4D0', '#22C9B6', '#16AE9C', '#0E9383', '#0A7669'],  // teal
  ['#68B0C4', '#4A9AB0', '#34869C', '#237389', '#186175'],  // tealBlue
  ['#829AFF', '#6482FF', '#4563E0', '#2E4BC1', '#1E38A2'],  // blue
  ['#B979DA', '#9F57C3', '#873DAD', '#722996', '#5F1C80'],  // purple
  ['#C468A5', '#B04A8E', '#9C347A', '#892367', '#751856'],  // pink
  ['#E27373', '#D85555', '#CE3D3D', '#B22929', '#961B1B'],  // red
  ['#EC984D', '#E18637', '#D17728', '#BD681D', '#A35815'],  // orange
  ['#ECD24D', '#E1C537', '#D1B528', '#BDA21D', '#A38C15'],  // yellow
] as const

function buildColors(
  assignments: AssignmentCollection,
  orgById: Record<string, OrgNode>
): Map<string, string> {
  const bookedEmpIds = new Set(Object.keys(assignments.deskByEmployeeId))
  const bookedNodes = Object.values(orgById).filter(n => bookedEmpIds.has(n.employeeId))
  if (bookedNodes.length === 0) return new Map()

  // Find the lowest common ancestor of all booked employees by longest common orgPath prefix.
  // orgPath excludes self, so orgPath.length === node.depth.
  const paths = bookedNodes.map(n => n.orgPath)
  const minLen = Math.min(...paths.map(p => p.length))
  let lcaIdx = 0
  while (lcaIdx + 1 < minLen && paths.every(p => p[lcaIdx + 1] === paths[0][lcaIdx + 1])) {
    lcaIdx++
  }
  // Color groups are the LCA's direct children — one level below the LCA.
  const divIdx = lcaIdx + 1

  const divAncestorIds = [...new Set(
    bookedNodes.flatMap(n =>
      n.orgPath.length > divIdx ? [n.orgPath[divIdx]] :
      n.orgPath.length === divIdx ? [n.employeeId] : []
    )
  )].sort()

  // Each division group gets a Starling color family, cycling if there are more groups than families.
  const divFamily = new Map<string, readonly string[]>(
    divAncestorIds.map((id, i) => [id, STARLING_FAMILIES[i % STARLING_FAMILIES.length]])
  )

  // Sibling index/count from the full org so shade positions are stable.
  const sibIdx = new Map<string, number>()
  const sibCnt = new Map<string, number>()
  for (const node of Object.values(orgById)) {
    for (let i = 0; i < node.childrenIds.length; i++) {
      sibIdx.set(node.childrenIds[i], i)
      sibCnt.set(node.childrenIds[i], node.childrenIds.length)
    }
  }

  const colors = new Map<string, string>()
  for (const node of bookedNodes) {
    const path = node.orgPath
    if (path.length < divIdx) { colors.set(node.employeeId, '#636363'); continue }
    const divKey = path.length > divIdx ? path[divIdx] : node.employeeId
    const family = divFamily.get(divKey)
    if (!family) { colors.set(node.employeeId, '#94a3b8'); continue }

    // Compute a shade position (0–1) from sibling indices at deeper org levels.
    // Successive levels nudge the position with diminishing weight.
    let pos = 0.5
    const nudge = (depthOffset: number, weight: number) => {
      if (path.length <= divIdx + depthOffset) return
      const idx = sibIdx.get(path[divIdx + depthOffset]) ?? 0
      const cnt = sibCnt.get(path[divIdx + depthOffset]) ?? 1
      if (cnt > 1) pos += (idx / (cnt - 1) - 0.5) * weight
    }
    nudge(1, 0.6)
    nudge(2, 0.3)
    nudge(3, 0.15)
    nudge(4, 0.08)

    pos = Math.max(0, Math.min(1, pos))
    const shadeIdx = Math.round(pos * (family.length - 1))
    colors.set(node.employeeId, family[shadeIdx])
  }
  return colors
}

export default function MapView() {
  const [desks, setDesks] = useState<Desk[]>([])
  const [empById, setEmpById] = useState<Record<string, Employee>>({})
  const [orgById, setOrgById] = useState<Record<string, OrgNode>>({})
  const [assignments, setAssignments] = useState<AssignmentCollection>({ deskByEmployeeId: {}, employeeByDeskId: {} })
  const [transform, setTransform] = useState<Transform>({ scale: 0.18, tx: 20, ty: 20 })
  const [selectedDeskId, setSelectedDeskId] = useState<string | null>(null)
  const [hoveredEmpId, setHoveredEmpId] = useState<string | null>(null)
  const [clickedEmpId, setClickedEmpId] = useState<string | null>(null)
  const [sidebarHoveredEmpId, setSidebarHoveredEmpId] = useState<string | null>(null)

  const activeSpiderEmpId = clickedEmpId ?? sidebarHoveredEmpId

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

  const nodeColors = useMemo(
    () => buildColors(assignments, orgById),
    [assignments, orgById]
  )

  const employees = useMemo(
    () => Object.values(empById).sort((a, b) => a.name.localeCompare(b.name)),
    [empById]
  )

  return (
    <div className="map-view-layout">
      <div className="people-panel">
        <div className="panel-section-title">People</div>
        {employees
          .filter(emp => assignments.deskByEmployeeId[emp.id])
          .map(emp => {
            const deskId = assignments.deskByEmployeeId[emp.id]
            const color = nodeColors.get(emp.id) ?? '#d1d5db'
            return (
              <button
                key={emp.id}
                className={`person-row${hoveredEmpId === emp.id ? ' hovered' : ''}`}
                onClick={() => { panToDesk(deskId); setClickedEmpId(emp.id) }}
                onMouseEnter={() => { setHoveredEmpId(emp.id); setSidebarHoveredEmpId(emp.id) }}
                onMouseLeave={() => { setHoveredEmpId(null); setSidebarHoveredEmpId(null) }}
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
          nodeColors={nodeColors}
          hoveredEmpId={hoveredEmpId}
          onHoverEmployee={setHoveredEmpId}
          clickedEmpId={activeSpiderEmpId}
          onClickEmployee={setClickedEmpId}
        />
      </div>

      <div className="score-panel">
        <ScoreDashboard />
      </div>
    </div>
  )
}
