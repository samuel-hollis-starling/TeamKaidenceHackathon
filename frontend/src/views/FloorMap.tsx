import { useEffect, useRef, useState, useMemo } from 'react'
import type { Desk, Employee, OrgNode, AssignmentCollection } from '../types'

const VIEW_W = 6736
const VIEW_H = 4290
export type Transform = { scale: number; tx: number; ty: number }

const DESK_R = 27.5

type Seg = { x1: number; y1: number; x2: number; y2: number }


interface FloorMapProps {
  desks: Desk[]
  empById: Record<string, Employee>
  orgById: Record<string, OrgNode>
  assignments: AssignmentCollection
  transform: Transform
  onTransformChange: (updater: (prev: Transform) => Transform) => void
  selectedDeskId?: string | null
  nodeColors: Map<string, string>
  onViewInOrg?: (employeeId: string) => void
  hoveredEmpId?: string | null
  onHoverEmployee?: (id: string | null) => void
  clickedEmpId?: string | null
  onClickEmployee?: (id: string | null) => void
}

export default function FloorMap({
  desks, empById, orgById, assignments, transform, onTransformChange,
  selectedDeskId, nodeColors, onViewInOrg,
  hoveredEmpId, onHoverEmployee,
  clickedEmpId, onClickEmployee,
}: FloorMapProps) {
  const [tooltip, setTooltip] = useState<{ desk: Desk; x: number; y: number } | null>(null)
  const [devMode, setDevMode] = useState(false)
  const dragging = useRef<{ startX: number; startY: number; startTx: number; startTy: number } | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const transformRef = useRef(transform)

  // Spiderweb connections — prefer hovered employee, fall back to clicked
  const connections = useMemo(() => {
    const activeEmpId = hoveredEmpId ?? clickedEmpId
    if (!activeEmpId) return null
    const activeDeskId = assignments.deskByEmployeeId[activeEmpId]
    const activeDesk = activeDeskId ? desks.find(d => d.id === activeDeskId) : null
    if (!activeDesk) return null
    const org = orgById[activeEmpId]
    if (!org) return null

    const color = nodeColors.get(activeEmpId) ?? '#873DAD'
    const relatedEmpIds = new Set<string>([activeEmpId])

    const pos = (empId: string): { x: number; y: number } | null => {
      const dId = assignments.deskByEmployeeId[empId]
      if (!dId) return null
      const d = desks.find(dk => dk.id === dId)
      return d ? { x: d.x, y: d.y } : null
    }

    const from = { x: activeDesk.x, y: activeDesk.y }

    let manager: Seg | null = null
    if (org.parentId) {
      const to = pos(org.parentId)
      if (to) { manager = { x1: from.x, y1: from.y, x2: to.x, y2: to.y }; relatedEmpIds.add(org.parentId) }
    }

    const siblings: Seg[] = []
    if (org.parentId) {
      const parentOrg = orgById[org.parentId]
      if (parentOrg) {
        for (const sibId of parentOrg.childrenIds) {
          if (sibId === activeEmpId) continue
          const to = pos(sibId)
          if (to) { siblings.push({ x1: from.x, y1: from.y, x2: to.x, y2: to.y }); relatedEmpIds.add(sibId) }
        }
      }
    }

    // BFS all descendants; draw from active employee to each booked descendant, tagged by relative depth.
    const descendants: { seg: Seg; depth: number }[] = []
    const queue: { empId: string; depth: number }[] = org.childrenIds.map(id => ({ empId: id, depth: 1 }))
    while (queue.length > 0) {
      const { empId, depth } = queue.shift()!
      const childOrg = orgById[empId]
      if (!childOrg) continue
      const to = pos(empId)
      if (to) { descendants.push({ seg: { x1: from.x, y1: from.y, x2: to.x, y2: to.y }, depth }); relatedEmpIds.add(empId) }
      for (const grandchildId of childOrg.childrenIds) queue.push({ empId: grandchildId, depth: depth + 1 })
    }

    return { color, manager, siblings, descendants, relatedEmpIds }
  }, [hoveredEmpId, clickedEmpId, orgById, assignments, desks, nodeColors])

  useEffect(() => { transformRef.current = transform }, [transform])

  useEffect(() => {
    if (!selectedDeskId || !containerRef.current) return
    const desk = desks.find(d => d.id === selectedDeskId)
    if (!desk) return
    const t = transformRef.current
    const rect = containerRef.current.getBoundingClientRect()
    setTooltip({ desk, x: rect.left + desk.x * t.scale + t.tx, y: rect.top + desk.y * t.scale + t.ty })
  }, [selectedDeskId, desks])

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    function onWheel(e: WheelEvent) {
      e.preventDefault()
      const factor = e.deltaY < 0 ? 1.04 : 0.96
      const rect = el!.getBoundingClientRect()
      const mx = e.clientX - rect.left
      const my = e.clientY - rect.top
      onTransformChange(t => {
        const newScale = Math.max(0.05, Math.min(5, t.scale * factor))
        const ratio = newScale / t.scale
        return { scale: newScale, tx: mx - (mx - t.tx) * ratio, ty: my - (my - t.ty) * ratio }
      })
    }
    el.addEventListener('wheel', onWheel, { passive: false })
    return () => el.removeEventListener('wheel', onWheel)
  }, [onTransformChange])

  function onMouseDown(e: React.MouseEvent) {
    if (e.button !== 0) return
    dragging.current = { startX: e.clientX, startY: e.clientY, startTx: transform.tx, startTy: transform.ty }
  }

  function onMouseMove(e: React.MouseEvent) {
    const drag = dragging.current
    if (!drag) return
    const { startTx, startTy, startX, startY } = drag
    const cx = e.clientX, cy = e.clientY
    onTransformChange(t => ({ ...t, tx: startTx + cx - startX, ty: startTy + cy - startY }))
  }

  function onMouseUp() { dragging.current = null }

  const tooltipEmp = tooltip ? empById[assignments.employeeByDeskId[tooltip.desk.id]] : null
  const tooltipOrg = tooltipEmp ? orgById[tooltipEmp.id] : null
  const tooltipManager = tooltipOrg?.parentId != null ? (empById[tooltipOrg.parentId] ?? null) : null

  // Scale-invariant stroke helpers: n screen pixels regardless of zoom
  const sw = (n: number) => n / transform.scale
  const da = (on: number, off: number) => `${on / transform.scale} ${off / transform.scale}`

  return (
    <div className="view floor-map-view">
      <div className="map-view-header">
        <div>
          <h2>Floor Map — 5th Floor</h2>
          <p className="map-hint">Scroll to zoom · drag to pan · click a desk to show connections</p>
        </div>
        <button
          className={`dev-toggle${devMode ? ' active' : ''}`}
          onClick={() => setDevMode(d => !d)}
        >
          Dev mode
        </button>
      </div>

      <div
        ref={containerRef}
        className="map-container"
        onMouseDown={onMouseDown}
        onMouseMove={onMouseMove}
        onMouseUp={onMouseUp}
        onMouseLeave={() => { onMouseUp(); setTooltip(null); onHoverEmployee?.(null) }}
      >
        <svg width="100%" height="100%" style={{ cursor: dragging.current ? 'grabbing' : 'grab' }}>
          <g transform={`translate(${transform.tx},${transform.ty}) scale(${transform.scale})`}>
            <rect x={-50000} y={-50000} width={200000} height={200000} fill="transparent" onClick={() => onClickEmployee?.(null)} />
            <image href="/floor-plan.svg" x={0} y={0} width={VIEW_W} height={VIEW_H} />

            {desks.map(desk => {
              const empId = assignments.employeeByDeskId[desk.id]
              const deskOrg = empId ? orgById[empId] : null
              const color = (empId ? nodeColors.get(empId) : null) ?? '#d1d5db'
              const isHovered = !!hoveredEmpId && empId === hoveredEmpId
              const isClicked = !!clickedEmpId && empId === clickedEmpId
              const showRing = !!empId && !!deskOrg && (connections?.relatedEmpIds.has(empId) ?? false)
              const ringR = DESK_R + 10
              const ringStrokeW = deskOrg ? sw(Math.max(1, (9 - deskOrg.depth) * 0.7 + 1)) : 0
              return (
                <g
                  key={desk.id}
                  transform={`translate(${desk.x},${desk.y})`}
                  style={{ cursor: 'pointer' }}
                  onMouseEnter={e => {
                    setTooltip({ desk, x: e.clientX, y: e.clientY })
                    if (empId) onHoverEmployee?.(empId)
                  }}
                  onMouseLeave={() => {
                    setTooltip(null)
                    onHoverEmployee?.(null)
                  }}
                  onClick={e => {
                    e.stopPropagation()
                    if (empId) onClickEmployee?.(isClicked ? null : empId)
                  }}
                >
                  {showRing && (
                    <circle
                      cx={0} cy={0}
                      r={ringR}
                      fill="none"
                      stroke={color}
                      strokeWidth={ringStrokeW}
                      strokeOpacity={0.65}
                    />
                  )}
                  <circle
                    cx={0} cy={0}
                    r={DESK_R}
                    fill={color}
                    stroke={isClicked ? '#321e37' : isHovered ? '#555' : '#fff'}
                    strokeWidth={isClicked ? sw(4) : isHovered ? sw(2.5) : sw(1.5)}
                  />
                  {devMode && deskOrg && (
                    <text
                      x={0} y={0}
                      textAnchor="middle"
                      dominantBaseline="middle"
                      fontSize={14}
                      fontWeight="700"
                      fill="rgba(255,255,255,0.92)"
                      style={{ pointerEvents: 'none' }}
                    >
                      {deskOrg.depth}
                    </text>
                  )}
                </g>
              )
            })}

            {/* Spiderweb overlay — rendered above desks */}
            {connections && (() => {
              const c = connections.color
              return (
                <g style={{ pointerEvents: 'none' }}>
                  {connections.siblings.map((s, i) => (
                    <g key={i}>
                      <line x1={s.x1} y1={s.y1} x2={s.x2} y2={s.y2} stroke="#18181b" strokeWidth={sw(4)} strokeOpacity={0.25} strokeDasharray={da(10, 7)} strokeLinecap="round" />
                      <line x1={s.x1} y1={s.y1} x2={s.x2} y2={s.y2} stroke={c} strokeWidth={sw(2)} strokeOpacity={0.8} strokeDasharray={da(10, 7)} strokeLinecap="round" />
                    </g>
                  ))}
                  {connections.descendants.map(({ seg: ch, depth }, i) => {
                    const w    = depth === 1 ? sw(3) : depth === 2 ? sw(2) : depth === 3 ? sw(1.5) : sw(1)
                    const op   = depth === 1 ? 0.9  : depth === 2 ? 0.6  : depth === 3 ? 0.4   : 0.25
                    const dash = depth >= 3 ? da(depth === 3 ? 10 : 5, depth === 3 ? 6 : 5) : undefined
                    return (
                      <g key={i}>
                        {depth <= 2 && <line x1={ch.x1} y1={ch.y1} x2={ch.x2} y2={ch.y2} stroke="#18181b" strokeWidth={sw(depth === 1 ? 5 : 4)} strokeOpacity={0.2} strokeLinecap="round" />}
                        <line x1={ch.x1} y1={ch.y1} x2={ch.x2} y2={ch.y2} stroke={c} strokeWidth={w} strokeOpacity={op} strokeLinecap="round" strokeDasharray={dash} />
                      </g>
                    )
                  })}
                  {connections.manager && (
                    <g>
                      <line x1={connections.manager.x1} y1={connections.manager.y1} x2={connections.manager.x2} y2={connections.manager.y2} stroke="#18181b" strokeWidth={sw(7)} strokeOpacity={0.25} strokeLinecap="round" />
                      <line x1={connections.manager.x1} y1={connections.manager.y1} x2={connections.manager.x2} y2={connections.manager.y2} stroke={c} strokeWidth={sw(5)} strokeOpacity={0.95} strokeLinecap="round" />
                    </g>
                  )}
                </g>
              )
            })()}

          </g>
        </svg>

        {tooltip && (() => {
          let left = tooltip.x + 12
          let top = tooltip.y - 8
          if (clickedEmpId) {
            const cDeskId = assignments.deskByEmployeeId[clickedEmpId]
            const cDesk = cDeskId ? desks.find(d => d.id === cDeskId) : null
            const rect = containerRef.current?.getBoundingClientRect()
            if (cDesk && rect) {
              const csx = cDesk.x * transform.scale + transform.tx + rect.left
              const csy = cDesk.y * transform.scale + transform.ty + rect.top
              left = tooltip.x + (csx > tooltip.x ? -240 : 12)
              top = tooltip.y + (csy > tooltip.y ? -8 : -120)
            }
          }
          return (
          <div className="map-tooltip" style={{ left, top }}>
            <div className="tt-desk">{tooltip.desk.name}</div>
            {tooltip.desk.neighborhood && <div className="tt-zone">{tooltip.desk.neighborhood}</div>}
            {tooltipEmp ? (
              <>
                <div className="tt-name">{tooltipEmp.name}</div>
                <div className="tt-role">{tooltipEmp.role}</div>
                {devMode ? (
                  <>
                    <div className="tt-depth">Depth {tooltipOrg?.depth}</div>
                    {tooltipOrg && (
                      <div className="tt-path">{tooltipOrg.orgPath.join(' › ')}</div>
                    )}
                  </>
                ) : (
                  tooltipManager && (
                    <div className="tt-manager">↑ {tooltipManager.name}</div>
                  )
                )}
                {onViewInOrg && (
                  <button
                    className="tt-org-btn"
                    onClick={() => onViewInOrg(tooltipEmp.id)}
                  >
                    View in org chart
                  </button>
                )}
              </>
            ) : (
              <div className="tt-empty">Unassigned</div>
            )}
          </div>
          )
        })()}
      </div>
    </div>
  )
}
