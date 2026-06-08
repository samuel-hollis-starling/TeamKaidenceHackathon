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
  orgById: Record<string, OrgNode>
): {
  nodeColors: Map<string, string>           // employeeId → color
} {
  const allNodes = Object.values(orgById)
  const minDepth = allNodes.length > 0 ? Math.min(...allNodes.map(n => n.depth)) : 2

  const divAncestorIds = [...new Set(
    allNodes.flatMap(n => n.orgPath.length > minDepth ? [n.orgPath[minDepth]] : [])
  )].sort()

  const divHue = new Map<string, number>(
    divAncestorIds.map((id, i) => [id, STARLING_HUES[i % STARLING_HUES.length]])
  )

  // Sibling position for each node (used to spread hue within a branch)
  const sibIdx = new Map<string, number>()
  const sibCnt = new Map<string, number>()
  for (const node of allNodes) {
    for (let i = 0; i < node.childrenIds.length; i++) {
      sibIdx.set(node.childrenIds[i], i)
      sibCnt.set(node.childrenIds[i], node.childrenIds.length)
    }
  }

  const nodeColors = new Map<string, string>()
  for (const node of allNodes) {
    const path = node.orgPath
    if (path.length <= minDepth) {
      nodeColors.set(node.employeeId, '#636363')
      continue
    }
    const baseHue = divHue.get(path[minDepth])
    if (baseHue === undefined) {
      nodeColors.set(node.employeeId, '#94a3b8')
      continue
    }

    let hue = baseHue

    // One level below division: ±15° spread across siblings
    if (path.length >= minDepth + 2) {
      const idx = sibIdx.get(path[minDepth + 1]) ?? 0
      const cnt = sibCnt.get(path[minDepth + 1]) ?? 1
      if (cnt > 1) hue += (idx / (cnt - 1) - 0.5) * 30
    }

    // Two levels below division: ±6° spread within sub-group
    if (path.length >= minDepth + 3) {
      const idx = sibIdx.get(path[minDepth + 2]) ?? 0
      const cnt = sibCnt.get(path[minDepth + 2]) ?? 1
      if (cnt > 1) hue += (idx / (cnt - 1) - 0.5) * 12
    }

    hue = ((hue % 360) + 360) % 360
    nodeColors.set(node.employeeId, `hsl(${hue.toFixed(0)}, ${PALETTE_SAT}%, ${PALETTE_LUM}%)`)
  }

  return { nodeColors }
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

  const { nodeColors } = useMemo(
    () => buildColors(orgById),
    [orgById]
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
          onViewInOrg={onViewInOrg}
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
