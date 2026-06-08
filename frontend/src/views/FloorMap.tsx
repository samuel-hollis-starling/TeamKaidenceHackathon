import { useState, useEffect, useRef, useMemo } from 'react'
import type { Desk, Employee, OrgNode, AssignmentCollection } from '../types'
import { getDesks, getEmployees, getOrgNodes, getAssignments } from '../api'

const VIEW_W = 6736
const VIEW_H = 4290

function hashHue(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) & 0xffffffff
  return Math.abs(h) % 360
}

function deskColor(desk: Desk, employeeByDeskId: Record<string, string>, orgByEmployeeId: Record<string, OrgNode>): string {
  const empId = employeeByDeskId[desk.id]
  if (!empId) return '#d1d5db'
  const node = orgByEmployeeId[empId]
  if (!node) return '#94a3b8'
  const branch = node.orgPath[1] ?? node.orgPath[0]
  const hue = hashHue(branch)
  const lightness = 45 + node.depth * 8
  return `hsl(${hue},65%,${lightness}%)`
}

function neighbourhoodLabels(desks: Desk[]): { name: string; x: number; y: number }[] {
  const groups: Record<string, { minY: number; maxX: number; minX: number }> = {}
  for (const d of desks) {
    if (!d.neighborhood) continue
    if (!groups[d.neighborhood]) groups[d.neighborhood] = { minY: Infinity, maxX: -Infinity, minX: Infinity }
    const g = groups[d.neighborhood]
    if (d.y < g.minY) g.minY = d.y
    if (d.x > g.maxX) g.maxX = d.x
    if (d.x < g.minX) g.minX = d.x
  }
  return Object.entries(groups).map(([name, { minY, minX, maxX }]) => ({
    name,
    x: (minX + maxX) / 2,
    y: minY - 80,
  }))
}

type Transform = { scale: number; tx: number; ty: number }

export default function FloorMap() {
  const [desks, setDesks] = useState<Desk[]>([])
  const [empById, setEmpById] = useState<Record<string, Employee>>({})
  const [orgById, setOrgById] = useState<Record<string, OrgNode>>({})
  const [assignments, setAssignments] = useState<AssignmentCollection>({ deskByEmployeeId: {}, employeeByDeskId: {} })
  const [transform, setTransform] = useState<Transform>({ scale: 0.18, tx: 20, ty: 20 })
  const [tooltip, setTooltip] = useState<{ desk: Desk; x: number; y: number } | null>(null)
  const dragging = useRef<{ startX: number; startY: number; startTx: number; startTy: number } | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

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

  const labels = useMemo(() => neighbourhoodLabels(desks), [desks])

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    function onWheel(e: WheelEvent) {
      e.preventDefault()
      const factor = e.deltaY < 0 ? 1.04 : 0.96
      const rect = el!.getBoundingClientRect()
      const mx = e.clientX - rect.left
      const my = e.clientY - rect.top
      setTransform(t => {
        const newScale = Math.max(0.05, Math.min(5, t.scale * factor))
        const ratio = newScale / t.scale
        return { scale: newScale, tx: mx - (mx - t.tx) * ratio, ty: my - (my - t.ty) * ratio }
      })
    }
    el.addEventListener('wheel', onWheel, { passive: false })
    return () => el.removeEventListener('wheel', onWheel)
  }, [])

  function onMouseDown(e: React.MouseEvent) {
    if (e.button !== 0) return
    dragging.current = { startX: e.clientX, startY: e.clientY, startTx: transform.tx, startTy: transform.ty }
  }

  function onMouseMove(e: React.MouseEvent) {
    if (!dragging.current) return
    setTransform(t => ({
      ...t,
      tx: dragging.current!.startTx + e.clientX - dragging.current!.startX,
      ty: dragging.current!.startTy + e.clientY - dragging.current!.startY,
    }))
  }

  function onMouseUp() { dragging.current = null }

  const tooltipEmp = tooltip ? empById[assignments.employeeByDeskId[tooltip.desk.id]] : null
  const tooltipOrg = tooltipEmp ? orgById[tooltipEmp.id] : null

  // Label font size in SVG units — large enough to read at typical zoom
  const labelSize = 60

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
        onMouseLeave={() => { onMouseUp(); setTooltip(null) }}
      >
        <svg width="100%" height="100%" style={{ cursor: dragging.current ? 'grabbing' : 'grab' }}>
          <g transform={`translate(${transform.tx},${transform.ty}) scale(${transform.scale})`}>
            {/* Floor plan walls — SVG stripped of desk elements */}
            <image href="/floor-plan.svg" x={0} y={0} width={VIEW_W} height={VIEW_H} />

            {/* Desk dots */}
            {desks.map(desk => (
              <circle
                key={desk.id}
                cx={desk.x}
                cy={desk.y}
                r={28}
                fill={deskColor(desk, assignments.employeeByDeskId, orgById)}
                stroke="#fff"
                strokeWidth={4}
                style={{ cursor: 'pointer' }}
                onMouseEnter={e => showTooltip(desk, e)}
                onMouseLeave={() => setTooltip(null)}
              />
            ))}

            {/* Neighbourhood labels — rendered last so they sit above desk circles */}
            {labels.map(l => (
              <g key={l.name} style={{ pointerEvents: 'none', userSelect: 'none' }}>
                <rect
                  x={l.x - labelSize * l.name.length * 0.3}
                  y={l.y - labelSize * 0.8}
                  width={labelSize * l.name.length * 0.6}
                  height={labelSize * 1.1}
                  rx={8}
                  fill="#fff"
                  fillOpacity={0.72}
                />
                <text
                  x={l.x}
                  y={l.y}
                  textAnchor="middle"
                  dominantBaseline="middle"
                  fontSize={labelSize}
                  fontWeight="600"
                  fill="#374151"
                >
                  {l.name}
                </text>
              </g>
            ))}
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

  function showTooltip(desk: Desk, e: React.MouseEvent) {
    setTooltip({ desk, x: e.clientX, y: e.clientY })
  }
}
