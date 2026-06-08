import { useState, useEffect, useMemo } from 'react'
import type { Desk, Employee, OrgNode, AssignmentCollection } from '../types'
import { getDesks, getEmployees, getOrgNodes, getAssignments } from '../api'
import FloorMap from './FloorMap'
import type { Transform } from './FloorMap'
import ScoreDashboard from './ScoreDashboard'

// Starling-brand hues: anchored on plum (285°) and teal (174°), spread across the wheel
const STARLING_HUES = [285, 265, 246, 225, 205, 185, 174, 157, 135, 90, 40, 20, 340]
const PALETTE_SAT = 62
const PALETTE_LUM = 48

function buildColors(
  orgById: Record<string, OrgNode>,
  empById: Record<string, Employee>
): {
  divisionColors: Array<[string, string]>  // [divId, color], sorted by name for sidebar
  divisionNames: Map<string, string>
  nodeColors: Map<string, string>           // employeeId → color
} {
  // Real grouping is at depth 2 (C-suite divisions, direct reports to CEO)
  const divisions = Object.values(orgById)
    .filter(n => n.depth === 2)
    .sort((a, b) => a.employeeId.localeCompare(b.employeeId))

  const divHue = new Map<string, number>(
    divisions.map((d, i) => [d.employeeId, STARLING_HUES[i % STARLING_HUES.length]])
  )
  const divisionNames = new Map<string, string>(
    divisions.map(d => [d.employeeId, empById[d.employeeId]?.name ?? d.employeeId])
  )
  const divisionColors: Array<[string, string]> = divisions
    .map((d, i) => [d.employeeId, `hsl(${STARLING_HUES[i % STARLING_HUES.length]}, ${PALETTE_SAT}%, ${PALETTE_LUM}%)`] as [string, string])
    .sort((a, b) => (divisionNames.get(a[0]) ?? '').localeCompare(divisionNames.get(b[0]) ?? ''))

  // Sibling position for each node (used to spread hue within a branch)
  const sibIdx = new Map<string, number>()
  const sibCnt = new Map<string, number>()
  for (const node of Object.values(orgById)) {
    for (let i = 0; i < node.childrenIds.length; i++) {
      sibIdx.set(node.childrenIds[i], i)
      sibCnt.set(node.childrenIds[i], node.childrenIds.length)
    }
  }

  const nodeColors = new Map<string, string>()
  for (const node of Object.values(orgById)) {
    const path = node.orgPath
    if (path.length < 3) {
      nodeColors.set(node.employeeId, '#636363')
      continue
    }
    const baseHue = divHue.get(path[2])
    if (baseHue === undefined) {
      nodeColors.set(node.employeeId, '#94a3b8')
      continue
    }

    let hue = baseHue

    // Depth-3 (department head siblings): ±15° spread within the division
    if (path.length >= 4) {
      const idx = sibIdx.get(path[3]) ?? 0
      const cnt = sibCnt.get(path[3]) ?? 1
      if (cnt > 1) hue += (idx / (cnt - 1) - 0.5) * 30
    }

    // Depth-4 (team lead siblings): ±6° spread within the department
    if (path.length >= 5) {
      const idx = sibIdx.get(path[4]) ?? 0
      const cnt = sibCnt.get(path[4]) ?? 1
      if (cnt > 1) hue += (idx / (cnt - 1) - 0.5) * 12
    }

    // Depth 5+ (ICs and below): no further spread — whole team shares one colour

    hue = ((hue % 360) + 360) % 360
    nodeColors.set(node.employeeId, `hsl(${hue.toFixed(0)}, ${PALETTE_SAT}%, ${PALETTE_LUM}%)`)
  }

  return { divisionColors, divisionNames, nodeColors }
}

interface MapViewProps {
  onViewInOrg?: (employeeId: string) => void
}

export default function MapView({ onViewInOrg }: MapViewProps) {
  const [desks, setDesks] = useState<Desk[]>([])
  const [empById, setEmpById] = useState<Record<string, Employee>>({})
  const [orgById, setOrgById] = useState<Record<string, OrgNode>>({})
  const [assignments, setAssignments] = useState<AssignmentCollection>({ deskByEmployeeId: {}, employeeByDeskId: {} })
  const [transform, setTransform] = useState<Transform>({ scale: 0.18, tx: 20, ty: 20 })
  const [selectedDeskId, setSelectedDeskId] = useState<string | null>(null)
  const [hoveredEmpId, setHoveredEmpId] = useState<string | null>(null)

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

  const { divisionColors, divisionNames, nodeColors } = useMemo(
    () => buildColors(orgById, empById),
    [orgById, empById]
  )

  const employees = useMemo(
    () => Object.values(empById).sort((a, b) => a.name.localeCompare(b.name)),
    [empById]
  )

  return (
    <div className="map-view-layout">
      <div className="people-panel">
        {divisionColors.length > 0 && (
          <>
            <div className="panel-section-title">Divisions</div>
            {divisionColors.map(([divId, color]) => (
              <div key={divId} className="key-row">
                <span className="person-dot" style={{ background: color }} />
                <span className="key-label">{divisionNames.get(divId)}</span>
              </div>
            ))}
          </>
        )}

        <div className="panel-section-title">People</div>
        {employees.map(emp => {
          const deskId = assignments.deskByEmployeeId[emp.id]
          const color = nodeColors.get(emp.id) ?? '#d1d5db'
          return (
            <button
              key={emp.id}
              className={`person-row${deskId ? '' : ' no-desk'}${hoveredEmpId === emp.id ? ' hovered' : ''}`}
              onClick={() => deskId && panToDesk(deskId)}
              onMouseEnter={() => setHoveredEmpId(emp.id)}
              onMouseLeave={() => setHoveredEmpId(null)}
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
          nodeColors={nodeColors}
          onViewInOrg={onViewInOrg}
          hoveredEmpId={hoveredEmpId}
          onHoverEmployee={setHoveredEmpId}
        />
      </div>

      <div className="score-panel">
        <ScoreDashboard />
      </div>
    </div>
  )
}
