import { useEffect, useRef, useState, useMemo } from 'react'
import type { Employee, OrgNode } from '../types'
import { getEmployees, getOrgNodes, getBookings } from '../api'

const VIVID = ['#349C51','#22C9B6','#34869C','#4563E0','#873DAD','#9C347A','#CE3D3D','#D17728','#D1B528']
const LIGHT = ['#D0F5DA','#B9FFF7','#BCE1EB','#CAD4FF','#E8C9F8','#EBBCDB','#F5C2C2','#FBCAA0','#FBECA0']

const NW = 168, NH = 56, HG = 12, VG = 68, SG = 44, MC = 8

type Transform = { scale: number; tx: number; ty: number }

interface LayoutNode {
  id: string; name: string; role: string
  x: number; y: number
  vivid: string; light: string; isManager: boolean; booked: boolean
}

interface Conn {
  x1: number; y1: number; x2: number; y2: number; color: string
}

interface OrgChartProps {
  focusId?: string | null
}

const trunc = (s: string, n: number) => s.length > n ? s.slice(0, n - 1) + '…' : s

function computeLayout(employees: Employee[], orgNodes: OrgNode[], bookedIds: Set<string>) {
  const empById = Object.fromEntries(employees.map(e => [e.id, e]))

  const minDepth = Math.min(...orgNodes.map(n => n.depth))

  const managers = orgNodes
    .filter(n => n.depth === minDepth)
    .sort((a, b) => (empById[a.employeeId]?.name ?? '').localeCompare(empById[b.employeeId]?.name ?? ''))

  const nodes: LayoutNode[] = []
  const conns: Conn[] = []
  let curX = 0

  for (let mi = 0; mi < managers.length; mi++) {
    const mgr = managers[mi]
    const emp = empById[mgr.employeeId]
    if (!emp) continue

    const vivid = VIVID[mi % VIVID.length]
    const light = LIGHT[mi % LIGHT.length]

    // All descendants of this manager, sorted by depth then name
    const children = orgNodes
      .filter(n => n.depth > minDepth && n.orgPath[minDepth] === mgr.employeeId)
      .map(n => ({ org: n, emp: empById[n.employeeId] }))
      .filter((c): c is { org: OrgNode; emp: Employee } => !!c.emp)
      .sort((a, b) => a.org.depth !== b.org.depth
        ? a.org.depth - b.org.depth
        : a.emp.name.localeCompare(b.emp.name))

    const cols = Math.min(Math.max(children.length, 1), MC)
    const subtreeW = children.length === 0 ? NW : cols * NW + (cols - 1) * HG

    const mgrX = curX + (subtreeW - NW) / 2
    nodes.push({ id: mgr.employeeId, name: emp.name, role: emp.role, x: mgrX, y: 0, vivid, light, isManager: true, booked: bookedIds.has(mgr.employeeId) })

    const rows: { org: OrgNode; emp: Employee }[][] = []
    for (let i = 0; i < children.length; i += MC) rows.push(children.slice(i, i + MC))

    for (let ri = 0; ri < rows.length; ri++) {
      const row = rows[ri]
      const rowW = row.length * NW + (row.length - 1) * HG
      const rowStartX = curX + (subtreeW - rowW) / 2
      const rowY = NH + VG + ri * (NH + VG)

      for (let ci = 0; ci < row.length; ci++) {
        const { org, emp: cEmp } = row[ci]
        const childX = rowStartX + ci * (NW + HG)
        nodes.push({ id: org.employeeId, name: cEmp.name, role: cEmp.role, x: childX, y: rowY, vivid, light, isManager: false, booked: bookedIds.has(org.employeeId) })
      }
    }

    curX += subtreeW + SG
  }

  return { nodes, conns }
}

export default function OrgChart({ focusId }: OrgChartProps) {
  const [employees, setEmployees] = useState<Employee[]>([])
  const [orgNodes, setOrgNodes] = useState<OrgNode[]>([])
  const [bookedIds, setBookedIds] = useState<Set<string>>(new Set())
  const [transform, setTransform] = useState<Transform>({ scale: 0.35, tx: 40, ty: 80 })
  const containerRef = useRef<HTMLDivElement>(null)
  const dragging = useRef<{ startX: number; startY: number; startTx: number; startTy: number } | null>(null)

  useEffect(() => {
    Promise.all([getEmployees(), getOrgNodes(), getBookings()]).then(([emps, nodes, bookings]) => {
      setEmployees(emps)
      setOrgNodes(nodes)
      // When nobody has booked yet show everyone in colour; grey-out only kicks in once bookings exist
      const ids = bookings.bookings.map(b => b.employeeId)
      setBookedIds(ids.length > 0 ? new Set(ids) : new Set(emps.map(e => e.id)))
    })
  }, [])

  const { nodes, conns } = useMemo(() => computeLayout(employees, orgNodes, bookedIds), [employees, orgNodes, bookedIds])

  // Pan to and highlight focusId when nodes are loaded
  useEffect(() => {
    if (!focusId || nodes.length === 0) return
    const target = nodes.find(n => n.id === focusId)
    if (!target || !containerRef.current) return
    const w = containerRef.current.clientWidth
    const h = containerRef.current.clientHeight
    const s = 1.2
    setTransform({
      scale: s,
      tx: w / 2 - (target.x + NW / 2) * s,
      ty: h / 2 - (target.y + NH / 2) * s,
    })
  }, [focusId, nodes])

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const onWheel = (e: WheelEvent) => {
      e.preventDefault()
      const factor = e.deltaY < 0 ? 1.08 : 0.93
      const rect = el.getBoundingClientRect()
      const mx = e.clientX - rect.left
      const my = e.clientY - rect.top
      setTransform(t => {
        const newScale = Math.max(0.1, Math.min(5, t.scale * factor))
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
    const drag = dragging.current
    if (!drag) return
    const { startTx, startTy, startX, startY } = drag
    const cx = e.clientX, cy = e.clientY
    setTransform(t => ({ ...t, tx: startTx + cx - startX, ty: startTy + cy - startY }))
  }

  function onMouseUp() { dragging.current = null }

  const focusNode = focusId ? nodes.find(n => n.id === focusId) : null

  return (
    <div className="view org-chart-view">
      <h2>Org Chart</h2>
      <p className="map-hint">Scroll to zoom · drag to pan · vivid = manager, light = report</p>
      <div
        ref={containerRef}
        className="map-container"
        style={{ cursor: 'grab' }}
        onMouseDown={onMouseDown}
        onMouseMove={onMouseMove}
        onMouseUp={onMouseUp}
        onMouseLeave={onMouseUp}
      >
        <svg width="100%" height="100%">
          <g transform={`translate(${transform.tx},${transform.ty}) scale(${transform.scale})`}>
            {conns.map((c, i) => {
              const ctrlY = c.y1 + (c.y2 - c.y1) * 0.5
              return (
                <path
                  key={i}
                  d={`M${c.x1},${c.y1} C${c.x1},${ctrlY} ${c.x2},${ctrlY} ${c.x2},${c.y2}`}
                  fill="none"
                  stroke={c.color}
                  strokeWidth={2}
                  strokeOpacity={0.4}
                />
              )
            })}

            {nodes.map(n => {
              const fill = n.booked
                ? (n.isManager ? n.vivid : n.light)
                : (n.isManager ? '#94a3b8' : '#f1f5f9')
              const stroke = n.booked ? n.vivid : '#cbd5e1'
              const nameColor = n.booked ? (n.isManager ? '#fff' : '#171B1F') : '#94a3b8'
              const roleColor = n.booked ? (n.isManager ? 'rgba(255,255,255,0.78)' : '#636363') : '#b0bcc8'
              return (
                <g key={n.id} transform={`translate(${n.x},${n.y})`}>
                  <rect
                    width={NW}
                    height={NH}
                    rx={9}
                    fill={fill}
                    stroke={stroke}
                    strokeWidth={n.isManager ? 0 : 1.5}
                  />
                  <text
                    x={NW / 2}
                    y={21}
                    textAnchor="middle"
                    dominantBaseline="middle"
                    fontSize={11}
                    fontWeight="600"
                    fill={nameColor}
                  >
                    {trunc(n.name, 22)}
                  </text>
                  <text
                    x={NW / 2}
                    y={38}
                    textAnchor="middle"
                    dominantBaseline="middle"
                    fontSize={9}
                    fill={roleColor}
                  >
                    {trunc(n.role, 28)}
                  </text>
                </g>
              )
            })}

            {/* Focus ring around the linked-to node */}
            {focusNode && (
              <rect
                className="focus-ring"
                x={focusNode.x - 5}
                y={focusNode.y - 5}
                width={NW + 10}
                height={NH + 10}
                rx={13}
                fill="none"
                stroke="#321e37"
                strokeWidth={3}
              />
            )}
          </g>
        </svg>
      </div>
    </div>
  )
}
