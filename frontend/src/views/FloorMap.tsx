import { useEffect, useRef, useState, useMemo } from 'react'
import type { Desk, Employee, OrgNode, AssignmentCollection } from '../types'

const VIEW_W = 6736
const VIEW_H = 4290
const LABEL_SIZE = 60
const CHAR_W = LABEL_SIZE * 0.52
const LABEL_H = LABEL_SIZE * 1.5
const LABEL_HIDE_RADIUS = 400

export type Transform = { scale: number; tx: number; ty: number }

function deskColor(desk: Desk, employeeByDeskId: Record<string, string>, orgByEmployeeId: Record<string, OrgNode>, branchColors: Map<string, string>): string {
  const empId = employeeByDeskId[desk.id]
  if (!empId) return '#d1d5db'
  const node = orgByEmployeeId[empId]
  if (!node) return '#94a3b8'
  const branch = node.orgPath[1] ?? node.orgPath[0]
  return branchColors.get(branch) ?? '#94a3b8'
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

interface FloorMapProps {
  desks: Desk[]
  empById: Record<string, Employee>
  orgById: Record<string, OrgNode>
  assignments: AssignmentCollection
  transform: Transform
  onTransformChange: (updater: (prev: Transform) => Transform) => void
  selectedDeskId?: string | null
  branchColors: Map<string, string>
}

export default function FloorMap({ desks, empById, orgById, assignments, transform, onTransformChange, selectedDeskId, branchColors }: FloorMapProps) {
  const [tooltip, setTooltip] = useState<{ desk: Desk; x: number; y: number } | null>(null)
  const [svgMouse, setSvgMouse] = useState<{ x: number; y: number } | null>(null)
  const dragging = useRef<{ startX: number; startY: number; startTx: number; startTy: number } | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const transformRef = useRef(transform)

  const labels = useMemo(() => neighbourhoodLabels(desks), [desks])

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
    if (!dragging.current) return
    onTransformChange(t => ({
      ...t,
      tx: dragging.current!.startTx + e.clientX - dragging.current!.startX,
      ty: dragging.current!.startTy + e.clientY - dragging.current!.startY,
    }))
  }

  function onMouseUp() { dragging.current = null }

  const tooltipEmp = tooltip ? empById[assignments.employeeByDeskId[tooltip.desk.id]] : null
  const tooltipOrg = tooltipEmp ? orgById[tooltipEmp.id] : null

  return (
    <div className="view floor-map-view">
      <h2>Floor Map — 5th Floor</h2>
      <p className="map-hint">Scroll to zoom · drag to pan · hover a desk for details</p>
      <div
        ref={containerRef}
        className="map-container"
        onMouseDown={onMouseDown}
        onMouseMove={onMouseMove}
        onMouseUp={onMouseUp}
        onMouseLeave={() => { onMouseUp(); setTooltip(null); setSvgMouse(null) }}
      >
        <svg width="100%" height="100%" style={{ cursor: dragging.current ? 'grabbing' : 'grab' }}>
          <g transform={`translate(${transform.tx},${transform.ty}) scale(${transform.scale})`}>
            <image href="/floor-plan.svg" x={0} y={0} width={VIEW_W} height={VIEW_H} />

            {desks.map(desk => (
              <circle
                key={desk.id}
                cx={desk.x}
                cy={desk.y}
                r={28}
                fill={deskColor(desk, assignments.employeeByDeskId, orgById, branchColors)}
                stroke="#fff"
                strokeWidth={4}
                style={{ cursor: 'pointer' }}
                onMouseEnter={e => setTooltip({ desk, x: e.clientX, y: e.clientY })}
                onMouseLeave={() => setTooltip(null)}
              />
            ))}

            {labels.map(l => {
              const halfW = (l.name.length * CHAR_W) / 2 + 20
              const nearCursor = svgMouse != null && Math.hypot(svgMouse.x - l.x, svgMouse.y - l.y) < LABEL_HIDE_RADIUS
              return (
                <g
                  key={l.name}
                  style={{
                    pointerEvents: 'none',
                    userSelect: 'none',
                    opacity: nearCursor ? 0 : 1,
                    transition: 'opacity 0.25s',
                  }}
                >
                  <rect
                    x={l.x - halfW}
                    y={l.y - LABEL_SIZE * 0.75}
                    width={halfW * 2}
                    height={LABEL_SIZE * 1.1}
                    rx={10}
                    fill="#fff"
                    fillOpacity={0.92}
                    stroke="#e4e0d8"
                    strokeWidth={4}
                  />
                  <text
                    x={l.x}
                    y={l.y}
                    textAnchor="middle"
                    dominantBaseline="middle"
                    fontSize={LABEL_SIZE}
                    fontWeight="700"
                    fill="#2A1F52"
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
                {tooltipOrg && <div className="tt-depth">Depth {tooltipOrg.depth}</div>}
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
