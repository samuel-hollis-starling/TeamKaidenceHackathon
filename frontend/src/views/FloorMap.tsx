import { useEffect, useRef, useState, useMemo } from 'react'
import type { Desk, Employee, OrgNode, AssignmentCollection } from '../types'

const VIEW_W = 6736
const VIEW_H = 4290
const LABEL_SIZE = 60
const CHAR_W = LABEL_SIZE * 0.52
const LABEL_H = LABEL_SIZE * 1.5
const LABEL_HIDE_RADIUS = 400

export type Transform = { scale: number; tx: number; ty: number }

const DESK_R = 22

function lineContrastColor(hex: string): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return (0.299 * r + 0.587 * g + 0.114 * b) > 128 ? '#18181b' : '#ffffff'
}


function neighbourhoodLabels(desks: Desk[]): { name: string; x: number; y: number }[] {
  const groups: Record<string, { sumX: number; sumY: number; count: number }> = {}
  for (const d of desks) {
    if (!d.neighborhood) continue
    if (!groups[d.neighborhood]) groups[d.neighborhood] = { sumX: 0, sumY: 0, count: 0 }
    groups[d.neighborhood].sumX += d.x
    groups[d.neighborhood].sumY += d.y
    groups[d.neighborhood].count++
  }
  const labels = Object.entries(groups).map(([name, { sumX, sumY, count }]) => ({
    name, x: sumX / count, y: sumY / count,
  }))
  for (let iter = 0; iter < 20; iter++) {
    let moved = false
    for (let i = 0; i < labels.length; i++) {
      for (let j = i + 1; j < labels.length; j++) {
        const a = labels[i], b = labels[j]
        const overlapX = (a.name.length + b.name.length) * CHAR_W / 2 - Math.abs(a.x - b.x)
        if (overlapX <= 0) continue
        const overlapY = LABEL_H - Math.abs(a.y - b.y)
        if (overlapY <= 0) continue
        const push = overlapY / 2 + 10
        if (a.y <= b.y) { a.y -= push; b.y += push } else { a.y += push; b.y -= push }
        moved = true
      }
    }
    if (!moved) break
  }
  return labels
}

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
}

export default function FloorMap({
  desks, empById, orgById, assignments, transform, onTransformChange,
  selectedDeskId, nodeColors, onViewInOrg,
  hoveredEmpId, onHoverEmployee,
}: FloorMapProps) {
  const [tooltip, setTooltip] = useState<{ desk: Desk; x: number; y: number } | null>(null)
  const [svgMouse, setSvgMouse] = useState<{ x: number; y: number } | null>(null)
  const [devMode, setDevMode] = useState(false)
  const dragging = useRef<{ startX: number; startY: number; startTx: number; startTy: number } | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const transformRef = useRef(transform)

  const labels = useMemo(() => neighbourhoodLabels(desks), [desks])

  // Spiderweb connections for hovered employee
  const connections = useMemo(() => {
    if (!hoveredEmpId) return null
    const hoveredDeskId = assignments.deskByEmployeeId[hoveredEmpId]
    const hoveredDesk = hoveredDeskId ? desks.find(d => d.id === hoveredDeskId) : null
    if (!hoveredDesk) return null
    const org = orgById[hoveredEmpId]
    if (!org) return null

    const color = nodeColors.get(hoveredEmpId) ?? '#873DAD'

    const pos = (empId: string): { x: number; y: number } | null => {
      const dId = assignments.deskByEmployeeId[empId]
      if (!dId) return null
      const d = desks.find(dk => dk.id === dId)
      return d ? { x: d.x, y: d.y } : null
    }

    const from = { x: hoveredDesk.x, y: hoveredDesk.y }

    let manager: Seg | null = null
    if (org.parentId) {
      const to = pos(org.parentId)
      if (to) manager = { x1: from.x, y1: from.y, x2: to.x, y2: to.y }
    }

    const siblings: Seg[] = []
    if (org.parentId) {
      const parentOrg = orgById[org.parentId]
      if (parentOrg) {
        for (const sibId of parentOrg.childrenIds) {
          if (sibId === hoveredEmpId) continue
          const to = pos(sibId)
          if (to) siblings.push({ x1: from.x, y1: from.y, x2: to.x, y2: to.y })
        }
      }
    }

    const children: Seg[] = []
    for (const childId of org.childrenIds) {
      const to = pos(childId)
      if (to) children.push({ x1: from.x, y1: from.y, x2: to.x, y2: to.y })
    }

    return { color, manager, siblings, children }
  }, [hoveredEmpId, orgById, assignments, desks, nodeColors])

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
    const rect = containerRef.current?.getBoundingClientRect()
    if (rect) {
      setSvgMouse({
        x: (e.clientX - rect.left - transform.tx) / transform.scale,
        y: (e.clientY - rect.top - transform.ty) / transform.scale,
      })
    }
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
          <p className="map-hint">Scroll to zoom · drag to pan · hover a desk or name for connections</p>
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
        onMouseLeave={() => { onMouseUp(); setTooltip(null); setSvgMouse(null); onHoverEmployee?.(null) }}
      >
        <svg width="100%" height="100%" style={{ cursor: dragging.current ? 'grabbing' : 'grab' }}>
          <g transform={`translate(${transform.tx},${transform.ty}) scale(${transform.scale})`}>
            <image href="/floor-plan.svg" x={0} y={0} width={VIEW_W} height={VIEW_H} />

            {desks.map(desk => {
              const empId = assignments.employeeByDeskId[desk.id]
              const deskOrg = empId ? orgById[empId] : null
              const color = (empId ? nodeColors.get(empId) : null) ?? '#d1d5db'
              const isHovered = !!hoveredEmpId && empId === hoveredEmpId
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
                >
                  {deskOrg && (
                    <circle
                      cx={0} cy={0}
                      r={ringR}
                      fill="none"
                      stroke={color}
                      strokeWidth={ringStrokeW}
                      strokeOpacity={0.55}
                    />
                  )}
                  <circle
                    cx={0} cy={0}
                    r={DESK_R}
                    fill={color}
                    stroke={isHovered ? '#321e37' : '#fff'}
                    strokeWidth={isHovered ? sw(4) : sw(1.5)}
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
              const lc = lineContrastColor(connections.color)
              return (
                <g style={{ pointerEvents: 'none' }}>
                  {connections.siblings.map((s, i) => (
                    <g key={i}>
                      <line x1={s.x1} y1={s.y1} x2={s.x2} y2={s.y2} stroke="#18181b" strokeWidth={sw(4)} strokeOpacity={0.25} strokeDasharray={da(10, 7)} strokeLinecap="round" />
                      <line x1={s.x1} y1={s.y1} x2={s.x2} y2={s.y2} stroke={lc} strokeWidth={sw(2)} strokeOpacity={0.8} strokeDasharray={da(10, 7)} strokeLinecap="round" />
                    </g>
                  ))}
                  {connections.children.map((c, i) => (
                    <g key={i}>
                      <line x1={c.x1} y1={c.y1} x2={c.x2} y2={c.y2} stroke="#18181b" strokeWidth={sw(5)} strokeOpacity={0.25} strokeLinecap="round" />
                      <line x1={c.x1} y1={c.y1} x2={c.x2} y2={c.y2} stroke={lc} strokeWidth={sw(3)} strokeOpacity={0.9} strokeLinecap="round" />
                    </g>
                  ))}
                  {connections.manager && (
                    <g>
                      <line x1={connections.manager.x1} y1={connections.manager.y1} x2={connections.manager.x2} y2={connections.manager.y2} stroke="#18181b" strokeWidth={sw(7)} strokeOpacity={0.25} strokeLinecap="round" />
                      <line x1={connections.manager.x1} y1={connections.manager.y1} x2={connections.manager.x2} y2={connections.manager.y2} stroke={lc} strokeWidth={sw(5)} strokeOpacity={0.95} strokeLinecap="round" />
                    </g>
                  )}
                </g>
              )
            })()}

            {labels.map(l => {
              const nearCursor = svgMouse != null && Math.hypot(svgMouse.x - l.x, svgMouse.y - l.y) < LABEL_HIDE_RADIUS
              return (
                <g
                  key={l.name}
                  style={{ pointerEvents: 'none', userSelect: 'none', opacity: nearCursor ? 0 : 1, transition: 'opacity 0.25s' }}
                >
                  <text
                    x={l.x}
                    y={l.y}
                    textAnchor="middle"
                    dominantBaseline="middle"
                    fontSize={LABEL_SIZE}
                    fontWeight="700"
                    fill="#2A1F52"
                    stroke="rgba(255,255,255,0.75)"
                    strokeWidth={14}
                    paintOrder="stroke"
                  >
                    {l.name}
                  </text>
                </g>
              )
            })}
          </g>
        </svg>

        {tooltip && (
          <div className="map-tooltip" style={{ left: tooltip.x + 12, top: tooltip.y - 8 }}>
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
        )}
      </div>
    </div>
  )
}
