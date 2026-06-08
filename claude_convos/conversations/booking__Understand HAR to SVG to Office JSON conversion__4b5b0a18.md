# Understand HAR to SVG to Office JSON conversion

**Project:** booking  
**Session:** `4b5b0a18-ba1a-43cf-af2f-dd4f7515069f`  
**Date:** 2026-06-08  

---

### You _10:51:34_

explain how the .har to svg to office json represenation works. 

i want to eventually rewrite this in java

---

### Claude _10:51:37_

Let me explore the codebase to understand this pipeline.

---

### Claude _10:51:37_

[Tool call: Bash]
```json
{
  "command": "eza --tree /Users/kapilan.cholan1/othergit/desk-booking --level=3 2>/dev/null"
}
```

---

### Claude _10:51:38_

[Tool call: Bash]
```json
{
  "command": "eza /Users/kapilan.cholan1/othergit/desk-booking/kadence/ -la 2>/dev/null"
}
```

---

### You _10:51:39_

[Tool result]
```
/Users/kapilan.cholan1/othergit/desk-booking
├── __pycache__
│   └── solver.cpython-314.pyc
├── algo.md
├── bin
│   └── main
│       └── deskbooking
├── build
│   ├── classes
│   │   └── java
│   ├── generated
│   │   └── sources
│   ├── resources
│   │   ├── main
│   │   └── test
│   └── tmp
│       └── compileJava
├── build.gradle
├── CLAUDE.md
├── demo.py
├── desk_booking
│   ├── __init__.py
│   ├── __pycache__
│   │   ├── __init__.cpython-314.pyc
│   │   ├── data.cpython-314.pyc
│   │   ├── metrics.cpython-314.pyc
│   │   └── solver.cpython-314.pyc
│   ├── data.py
│   ├── metrics.py
│   └── solver.py
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── kadence
│   ├── floor-map-5th.html
│   ├── floor-map-5th.json
│   ├── floor-plan-5th.svg
│   ├── floor_pipeline.py
│   └── ot.har
├── pyproject.toml
├── README.md
├── settings.gradle
├── simulate.py
├── src
│   └── main
│       └── java
├── uv.lock
└── workday
    ├── CLAUDE.md
    ├── orgchart.csv
    ├── orgchart.json
    └── orgchart.xlsx
```

---

### You _10:51:39_

[Tool result]
```
.rw-r--r--@  52k kapilan.cholan1 18 May 17:16 floor-map-5th.html
.rw-r--r--@  84k kapilan.cholan1 18 May 17:12 floor-map-5th.json
.rw-r--r--@ 323k kapilan.cholan1 18 May 17:01 floor-plan-5th.svg
.rw-r--r--@  16k kapilan.cholan1  5 Jun 17:07 floor_pipeline.py
.rw-------@ 4.9M kapilan.cholan1 18 May 16:57 ot.har
```

---

### Claude _10:51:41_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/desk-booking/kadence/floor_pipeline.py"
}
```

---

### You _10:51:41_

[Tool result]
```
1	#!/usr/bin/env python3
2	"""
3	Floor map pipeline: SVG + desk API metadata → floor-map-5th.json + floor-map-5th.html
4	
5	Usage:
6	    uv run python kadence/floor_pipeline.py          # parse + render
7	    uv run python kadence/floor_pipeline.py --render  # render only (from existing JSON)
8	"""
9	
10	import json
11	import re
12	import sys
13	import xml.etree.ElementTree as ET
14	from pathlib import Path
15	
16	BASE = Path(__file__).parent
17	
18	LANDMARK_IDS = {"KITCHEN", "RECEPTION", "STAIRS", "ELEVATOR", "RESTROOM", "WORKPLACETECHBAR"}
19	UNAVAILABLE_IDS = {"unavailable", "unavailable-", "Unavailable"}
20	
21	PALETTE = [
22	    "#4e79a7", "#f28e2b", "#e15759", "#76b7b2", "#59a14f",
23	    "#edc948", "#b07aa1", "#ff9da7", "#9c755f", "#bab0ac",
24	    "#d37295", "#fabfd2", "#8cd17d", "#86bcb6", "#499894",
25	    "#f1ce63", "#a0cbe8", "#ffbe7d", "#b6992d", "#79706e",
26	]
27	
28	# ── SVG helpers ───────────────────────────────────────────────────────────────
29	
30	def _strip_ns(tag):
31	    return tag.split("}")[1] if "}" in tag else tag
32	
33	
34	def _parse_translate(transform_str):
35	    """Return (x, y) of the first translate() in a transform string, or (None, None)."""
36	    if not transform_str:
37	        return None, None
38	    m = re.search(r"translate\(([^)]+)\)", transform_str)
39	    if not m:
40	        return None, None
41	    parts = [p.strip() for p in m.group(1).split(",")]
42	    return round(float(parts[0]), 2), round(float(parts[1]) if len(parts) > 1 else 0.0, 2)
43	
44	
45	def _parse_rotation(transform_str):
46	    if not transform_str:
47	        return 0
48	    m = re.search(r"rotate\(([^)]+)\)", transform_str)
49	    return round(float(m.group(1).split(",")[0].strip()), 2) if m else 0
50	
51	
52	def _parse_polygon_points(points_str, dx=0.0, dy=0.0):
53	    nums = re.findall(r"[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?", points_str)
54	    return [
55	        [round(float(nums[i]) + dx, 2), round(float(nums[i + 1]) + dy, 2)]
56	        for i in range(0, len(nums) - 1, 2)
57	    ]
58	
59	
60	# ── Parse ─────────────────────────────────────────────────────────────────────
61	
62	def parse_svg(svg_path: Path) -> dict:
63	    """Extract desks, pods, walls, landmarks, and unavailable spaces from the SVG."""
64	    tree = ET.parse(svg_path)
65	    root = tree.getroot()
66	
67	    parent_map = {child: parent for parent in tree.iter() for child in parent}
68	
69	    vb = root.get("viewBox", "").split()
70	    viewbox = (
71	        {"width": float(vb[2]), "height": float(vb[3])}
72	        if len(vb) == 4
73	        else {"width": float(root.get("width", 0)), "height": float(root.get("height", 0))}
74	    )
75	
76	    desks, pods, walls, landmarks, unavailable = [], [], [], [], []
77	
78	    for elem in root.iter():
79	        eid = elem.get("id", "")
80	        transform = elem.get("transform", "")
81	        tag = _strip_ns(elem.tag)
82	
83	        if eid.startswith("space::desk::"):
84	            x, y = _parse_translate(transform)
85	            if x is not None:
86	                desks.append({"id": eid[len("space::desk::"):], "x": x, "y": y, "rotation": _parse_rotation(transform)})
87	
88	        elif eid.startswith("space::pod::"):
89	            x, y = _parse_translate(transform)
90	            if x is not None:
91	                pods.append({"id": eid[len("space::pod::"):], "x": x, "y": y, "rotation": _parse_rotation(transform)})
92	
93	        elif eid == "walls" and tag == "g":
94	            dx, dy = _parse_translate(transform)
95	            dx, dy = dx or 0.0, dy or 0.0
96	            for child in elem:
97	                if _strip_ns(child.tag) == "polygon":
98	                    walls.append({"id": child.get("id", ""), "points": _parse_polygon_points(child.get("points", ""), dx, dy)})
99	
100	        elif eid in LANDMARK_IDS:
101	            parent = parent_map.get(elem)
102	            if parent is not None:
103	                px, py = _parse_translate(parent.get("transform", ""))
104	                if px is not None:
105	                    landmarks.append({"type": eid, "x": px, "y": py})
106	
107	        elif eid in UNAVAILABLE_IDS and tag == "g":
108	            x, y = _parse_translate(transform)
109	            if x is not None:
110	                unavailable.append({"x": x, "y": y, "rotation": _parse_rotation(transform), "shape": "round"})
111	
112	    return {"viewbox": viewbox, "desks": desks, "pods": pods, "walls": walls, "landmarks": landmarks, "unavailable": unavailable}
113	
114	
115	def build_floor_map(svg_path: Path, desks_path: Path) -> dict:
116	    """Merge SVG geometry with desk API metadata into a unified floor map dict."""
117	    with open(desks_path) as f:
118	        desk_meta = {d["id"]: d for d in json.load(f)}
119	
120	    neighborhoods = sorted({d["neighborhood"] for d in desk_meta.values() if d.get("neighborhood")})
121	    svg = parse_svg(svg_path)
122	
123	    matched, unmatched = [], []
124	    for desk in svg["desks"]:
125	        meta = desk_meta.get(desk["id"])
126	        if meta:
127	            matched.append({"id": desk["id"], "name": meta["name"], "neighborhood": meta.get("neighborhood"),
128	                            "x": desk["x"], "y": desk["y"], "rotation": desk["rotation"]})
129	        else:
130	            unmatched.append(desk)
131	
132	    print(f"desks:              {len(matched)} matched, {len(unmatched)} SVG-only")
133	    print(f"pods:               {len(svg['pods'])}")
134	    print(f"walls:              {len(svg['walls'])}")
135	    print(f"landmarks:          {len(svg['landmarks'])}")
136	    print(f"unavailable spaces: {len(svg['unavailable'])}")
137	    print(f"neighborhoods:      {neighborhoods}")
138	
139	    return {
140	        "floor": {"id": "01G6X15J9J81YY4RJRDCHWR27V", "name": "5th Floor",
141	                  "building": "London Fruit & Wool Exchange", "viewBox": svg["viewbox"]},
142	        "neighborhoods": neighborhoods,
143	        "spaces": {"desks": matched, "pods": svg["pods"]},
144	        "walls": svg["walls"],
145	        "landmarks": svg["landmarks"],
146	        "unavailableSpaces": svg["unavailable"],
147	    }
148	
149	
150	# ── Render ────────────────────────────────────────────────────────────────────
151	
152	def render_html(floor_map: dict, output_path: Path) -> None:
153	    """Write a self-contained HTML canvas visualiser for the floor map."""
154	    map_json = json.dumps(floor_map, separators=(",", ":"))
155	    neighbourhoods = floor_map["neighborhoods"]
156	    colour_map_json = json.dumps({n: PALETTE[i % len(PALETTE)] for i, n in enumerate(neighbourhoods)})
157	
158	    html = f"""<!DOCTYPE html>
159	<html lang="en">
160	<head>
161	<meta charset="UTF-8">
162	<title>5th Floor — London Fruit &amp; Wool Exchange</title>
163	<style>
164	  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
165	  body {{ background: #1a1a2e; font-family: system-ui, sans-serif; overflow: hidden; }}
166	  canvas {{ display: block; cursor: grab; }}
167	  canvas.dragging {{ cursor: grabbing; }}
168	
169	  #legend {{
170	    position: fixed; top: 12px; right: 12px;
171	    background: rgba(20,20,40,0.92); border: 1px solid #333;
172	    border-radius: 8px; padding: 12px 14px; color: #ddd;
173	    font-size: 11px; max-height: calc(100vh - 24px); overflow-y: auto;
174	    backdrop-filter: blur(4px);
175	  }}
176	  #legend h3 {{ font-size: 12px; margin-bottom: 8px; color: #fff; }}
177	  .lrow {{ display: flex; align-items: center; gap: 6px; margin: 3px 0; }}
178	  .dot {{ width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }}
179	
180	  #tooltip {{
181	    position: fixed; pointer-events: none;
182	    background: rgba(20,20,40,0.95); color: #fff;
183	    border: 1px solid #444; border-radius: 6px;
184	    padding: 6px 10px; font-size: 12px; line-height: 1.5;
185	    display: none; white-space: nowrap;
186	  }}
187	
188	  #controls {{
189	    position: fixed; bottom: 12px; left: 12px;
190	    color: #666; font-size: 11px;
191	  }}
192	</style>
193	</head>
194	<body>
195	<canvas id="c"></canvas>
196	<div id="legend">
197	  <h3>Neighbourhoods</h3>
198	  <div id="leg-items"></div>
199	  <h3 style="margin-top:10px">Other</h3>
200	  <div class="lrow"><div class="dot" style="background:#6c757d;border-radius:2px"></div>Pod</div>
201	  <div class="lrow"><div class="dot" style="background:#555"></div>Unavailable</div>
202	  <div class="lrow"><div class="dot" style="background:#e9c46a;border-radius:0"></div>Landmark</div>
203	</div>
204	<div id="tooltip"></div>
205	<div id="controls">Scroll to zoom · Drag to pan</div>
206	
207	<script>
208	const MAP = {map_json};
209	const COLOURS = {colour_map_json};
210	
211	const canvas = document.getElementById("c");
212	const ctx = canvas.getContext("2d");
213	
214	function resize() {{ canvas.width = window.innerWidth; canvas.height = window.innerHeight; draw(); }}
215	window.addEventListener("resize", resize);
216	
217	const VB = MAP.floor.viewBox;
218	let scale, ox, oy;
219	
220	function fitToWindow() {{
221	  const margin = 40;
222	  scale = Math.min((window.innerWidth - margin * 2) / VB.width, (window.innerHeight - margin * 2) / VB.height);
223	  ox = (window.innerWidth  - VB.width  * scale) / 2;
224	  oy = (window.innerHeight - VB.height * scale) / 2;
225	}}
226	
227	const tx = x => ox + x * scale;
228	const ty = y => oy + y * scale;
229	
230	function circleHit(cx, cy, r, mx, my) {{
231	  const dx = cx - mx, dy = cy - my;
232	  return Math.sqrt(dx * dx + dy * dy) < r;
233	}}
234	
235	function polyHit(points, mx, my) {{
236	  let inside = false;
237	  for (let i = 0, j = points.length - 1; i < points.length; j = i++) {{
238	    const xi = points[i][0], yi = points[i][1], xj = points[j][0], yj = points[j][1];
239	    if ((yi > my) !== (yj > my) && mx < (xj - xi) * (my - yi) / (yj - yi) + xi) inside = !inside;
240	  }}
241	  return inside;
242	}}
243	
244	function hitTest(mx, my) {{
245	  for (const d of MAP.spaces.desks)      if (circleHit(d.x, d.y, 22, mx, my)) return {{ kind: "desk",        item: d }};
246	  for (const p of MAP.spaces.pods)       if (circleHit(p.x, p.y, 28, mx, my)) return {{ kind: "pod",         item: p }};
247	  for (const u of MAP.unavailableSpaces) if (circleHit(u.x, u.y, 35, mx, my)) return {{ kind: "unavailable", item: u }};
248	  for (const lm of MAP.landmarks)        if (circleHit(lm.x, lm.y, 14, mx, my)) return {{ kind: "landmark",  item: lm }};
249	  for (const w of MAP.walls)             if (polyHit(w.points, mx, my))          return {{ kind: "wall",      item: w }};
250	  return null;
251	}}
252	
253	let hovered = null;
254	
255	function drawPolygon(points, fill, stroke, lineWidth = 1) {{
256	  if (!points.length) return;
257	  ctx.beginPath();
258	  ctx.moveTo(tx(points[0][0]), ty(points[0][1]));
259	  for (let i = 1; i < points.length; i++) ctx.lineTo(tx(points[i][0]), ty(points[i][1]));
260	  ctx.closePath();
261	  if (fill)   {{ ctx.fillStyle = fill; ctx.fill(); }}
262	  if (stroke) {{ ctx.strokeStyle = stroke; ctx.lineWidth = lineWidth; ctx.stroke(); }}
263	}}
264	
265	function drawDesk(desk) {{
266	  const isHit = hovered?.kind === "desk" && hovered.item === desk;
267	  const r = Math.max(16 * scale, 2);
268	  const colour = COLOURS[desk.neighborhood] ?? "#888";
269	  ctx.beginPath();
270	  ctx.arc(tx(desk.x), ty(desk.y), r, 0, Math.PI * 2);
271	  ctx.fillStyle = isHit ? "#fff" : colour;
272	  ctx.fill();
273	  if (isHit) {{ ctx.strokeStyle = colour; ctx.lineWidth = 2; ctx.stroke(); }}
274	}}
275	
276	function drawPod(pod) {{
277	  const isHit = hovered?.kind === "pod" && hovered.item === pod;
278	  const r = Math.max(20 * scale, 2);
279	  ctx.beginPath();
280	  ctx.arc(tx(pod.x), ty(pod.y), r, 0, Math.PI * 2);
281	  ctx.fillStyle = isHit ? "#fff" : "#6c757d";
282	  ctx.fill();
283	  ctx.strokeStyle = isHit ? "#6c757d" : "#aaa";
284	  ctx.lineWidth = isHit ? 2 : 1;
285	  ctx.stroke();
286	}}
287	
288	function drawUnavailable(u) {{
289	  const isHit = hovered?.kind === "unavailable" && hovered.item === u;
290	  const r = Math.max(30 * scale, 2);
291	  ctx.beginPath();
292	  ctx.arc(tx(u.x), ty(u.y), r, 0, Math.PI * 2);
293	  ctx.fillStyle = isHit ? "#4a4a6a" : "#333344";
294	  ctx.fill();
295	  ctx.strokeStyle = isHit ? "#aaa" : "#555";
296	  ctx.lineWidth = isHit ? 2 : 1;
297	  ctx.stroke();
298	}}
299	
300	function drawLandmark(lm) {{
301	  const isHit = hovered?.kind === "landmark" && hovered.item === lm;
302	  const x = tx(lm.x), y = ty(lm.y), sz = isHit ? 8 : 5;
303	  ctx.fillStyle = isHit ? "#fff" : "#e9c46a";
304	  ctx.fillRect(x - sz, y - sz, sz * 2, sz * 2);
305	  if (isHit) {{ ctx.strokeStyle = "#e9c46a"; ctx.lineWidth = 2; ctx.strokeRect(x - sz, y - sz, sz * 2, sz * 2); }}
306	  if (scale > 0.06) {{
307	    ctx.fillStyle = isHit ? "#fff" : "#e9c46a";
308	    ctx.font = `${{Math.round(10 + scale * 10)}}px system-ui`;
309	    ctx.fillText(lm.type, x + sz + 3, y + 4);
310	  }}
311	}}
312	
313	function draw() {{
314	  ctx.clearRect(0, 0, canvas.width, canvas.height);
315	  ctx.fillStyle = "#23243a";
316	  ctx.fillRect(0, 0, canvas.width, canvas.height);
317	  for (const wall of MAP.walls) {{
318	    const isHit = hovered?.kind === "wall" && hovered.item === wall;
319	    drawPolygon(wall.points, isHit ? "#4a4c70" : "#2e3050", isHit ? "#8888cc" : "#3a3c5a", isHit ? 2 : 1);
320	  }}
321	  for (const u of MAP.unavailableSpaces) drawUnavailable(u);
322	  for (const pod of MAP.spaces.pods)     drawPod(pod);
323	  for (const desk of MAP.spaces.desks)   drawDesk(desk);
324	  for (const lm of MAP.landmarks)        drawLandmark(lm);
325	}}
326	
327	const legEl = document.getElementById("leg-items");
328	for (const [name, colour] of Object.entries(COLOURS)) {{
329	  const row = document.createElement("div");
330	  row.className = "lrow";
331	  row.innerHTML = `<div class="dot" style="background:${{colour}}"></div>${{name}}`;
332	  legEl.appendChild(row);
333	}}
334	
335	const tooltip = document.getElementById("tooltip");
336	
337	function tooltipHTML({{ kind, item }}) {{
338	  const row = (label, val) => val != null
339	    ? `<tr><td style="color:#999;padding-right:10px">${{label}}</td><td>${{val}}</td></tr>` : "";
340	  const rows = {{
341	    desk:        row("name", `<b>${{item.name}}</b>`) + row("id", `<code style="font-size:10px">${{item.id}}</code>`) + row("neighbourhood", item.neighborhood) + row("coords", `${{item.x}}, ${{item.y}}`),
342	    pod:         row("type", "<b>Pod</b>") + row("id", `<code style="font-size:10px">${{item.id}}</code>`) + row("coords", `${{item.x}}, ${{item.y}}`),
343	    unavailable: row("type", "<b>Unavailable space</b>") + row("shape", item.shape) + row("coords", `${{item.x}}, ${{item.y}}`),
344	    landmark:    row("type", `<b>${{item.type}}</b>`) + row("coords", `${{item.x}}, ${{item.y}}`),
345	    wall:        row("type", "<b>Wall</b>") + row("id", `<code style="font-size:10px">${{item.id}}</code>`) + row("points", item.points.length),
346	  }}[kind] ?? "";
347	  return `<table style="border-collapse:collapse">${{rows}}</table>`;
348	}}
349	
350	canvas.addEventListener("mousemove", e => {{
351	  if (isDragging) return;
352	  const mx = (e.clientX - ox) / scale, my = (e.clientY - oy) / scale;
353	  const hit = hitTest(mx, my);
354	  if (hit !== hovered) {{ hovered = hit; draw(); }}
355	  if (hit) {{
356	    tooltip.style.display = "block";
357	    tooltip.style.left = (e.clientX + 14) + "px";
358	    tooltip.style.top  = (e.clientY - 10) + "px";
359	    tooltip.innerHTML  = tooltipHTML(hit);
360	  }} else {{
361	    tooltip.style.display = "none";
362	  }}
363	}});
364	
365	canvas.addEventListener("mouseleave", () => {{ tooltip.style.display = "none"; hovered = null; draw(); }});
366	
367	let isDragging = false, lastX, lastY;
368	canvas.addEventListener("mousedown", e => {{ isDragging = true; lastX = e.clientX; lastY = e.clientY; canvas.classList.add("dragging"); }});
369	window.addEventListener("mouseup",   () => {{ isDragging = false; canvas.classList.remove("dragging"); }});
370	window.addEventListener("mousemove", e => {{
371	  if (!isDragging) return;
372	  ox += e.clientX - lastX; oy += e.clientY - lastY;
373	  lastX = e.clientX; lastY = e.clientY;
374	  draw();
375	}});
376	
377	canvas.addEventListener("wheel", e => {{
378	  e.preventDefault();
379	  const factor = e.deltaY < 0 ? 1.1 : 0.9;
380	  ox = e.clientX - (e.clientX - ox) * factor;
381	  oy = e.clientY - (e.clientY - oy) * factor;
382	  scale *= factor;
383	  draw();
384	}}, {{ passive: false }});
385	
386	fitToWindow();
387	resize();
388	</script>
389	</body>
390	</html>"""
391	
392	    output_path.write_text(html)
393	    print(f"Written: {output_path}")
394	
395	
396	# ── Main ──────────────────────────────────────────────────────────────────────
397	
398	def main():
399	    render_only = "--render" in sys.argv
400	
401	    json_path = BASE / "floor-map-5th.json"
402	    html_path = BASE / "floor-map-5th.html"
403	
404	    if render_only:
405	        with open(json_path) as f:
406	            floor_map = json.load(f)
407	    else:
408	        floor_map = build_floor_map(
409	            svg_path=BASE / "floor-plan-5th.svg",
410	            desks_path=BASE / "desks-5th.json",
411	        )
412	        with open(json_path, "w") as f:
413	            json.dump(floor_map, f, indent=2)
414	        print(f"Written: {json_path}")
415	
416	    render_html(floor_map, html_path)
417	
418	
419	if __name__ == "__main__":
420	    main()
421
```

---

### Claude _10:51:47_

Now let me look at how the HAR file was used to get desk metadata, and inspect the output JSON structure.

---

### Claude _10:51:48_

[Tool call: Bash]
```json
{
  "command": "rg -l \"har\\|ot\\.har\" /Users/kapilan.cholan1/othergit/desk-booking/ 2>/dev/null"
}
```

---

### Claude _10:51:49_

[Tool call: Bash]
```json
{
  "command": "jq 'keys' /Users/kapilan.cholan1/othergit/desk-booking/kadence/floor-map-5th.json 2>/dev/null"
}
```

---

### You _10:51:49_

[Tool result]
```
[
  "floor",
  "landmarks",
  "neighborhoods",
  "spaces",
  "unavailableSpaces",
  "walls"
]
```

---

### You _10:51:49_

[Tool result]
```
(Bash completed with no output)
```

---

### Claude _10:51:51_

[Tool call: Bash]
```json
{
  "command": "jq '{floor, neighborhoods, spaces_keys: (.spaces | keys), desk_count: (.spaces.desks | length), pod_count: (.spaces.pods | length), wall_count: (.walls | length), landmark_count: (.landmarks | length), sample_desk: .spaces.desks[0]}' /Users/kapilan.cholan1/othergit/desk-booking/kadence/floor-map-5th.json"
}
```

---

### Claude _10:51:53_

[Tool call: Bash]
```json
{
  "command": "# Check if there's a desks-5th.json (the desk API data extracted from HAR)\neza /Users/kapilan.cholan1/othergit/desk-booking/kadence/ -la 2>/dev/null\n# Also check git log for any scripts that processed the HAR\ngit -C /Users/kapilan.cholan1/othergit/desk-booking log --oneline --all 2>/dev/null | head -20"
}
```

---

### You _10:51:53_

[Tool result]
```
{
  "floor": {
    "id": "01G6X15J9J81YY4RJRDCHWR27V",
    "name": "5th Floor",
    "building": "London Fruit & Wool Exchange",
    "viewBox": {
      "width": 6736.0,
      "height": 4290.0
    }
  },
  "neighborhoods": [
    "Agile Workspaces",
    "Banking",
    "Desk Pods",
    "EMBER",
    "Facilities",
    "Red Team",
    "Tech Central",
    "Tech North A",
    "Tech North B",
    "Tech North C",
    "Tech South A",
    "Tech South B",
    "Tech South B2",
    "Tech South C",
    "Technology - Priority",
    "Workplace Technology",
    "Workplace Technology Workbench"
  ],
  "spaces_keys": [
    "desks",
    "pods"
  ],
  "desk_count": 191,
  "pod_count": 8,
  "wall_count": 16,
  "landmark_count": 9,
  "sample_desk": {
    "id": "01G6X1AXT33V1KY943F8CXP442",
    "name": "UK-LDN-LFW-5-100A",
    "neighborhood": "Tech South C",
    "x": 453.16,
    "y": 2883.9,
    "rotation": 90.0
  }
}
```

---

### You _10:53:33_

[Tool result]
```
.rw-r--r--@  52k kapilan.cholan1 18 May 17:16 floor-map-5th.html
.rw-r--r--@  84k kapilan.cholan1 18 May 17:12 floor-map-5th.json
.rw-r--r--@ 323k kapilan.cholan1 18 May 17:01 floor-plan-5th.svg
.rw-r--r--@  16k kapilan.cholan1  5 Jun 17:07 floor_pipeline.py
.rw-------@ 4.9M kapilan.cholan1 18 May 16:57 ot.har
```

---

### Claude _10:53:36_

[Tool call: Bash]
```json
{
  "command": "# Look at the HAR file structure - extract a sample of the desk API response\njq '.log.entries[] | select(.request.url | contains(\"desk\")) | {url: .request.url, status: .response.status} | first' /Users/kapilan.cholan1/othergit/desk-booking/kadence/ot.har 2>/dev/null | head -50"
}
```

---

### Claude _10:53:37_

[Tool call: Bash]
```json
{
  "command": "# Find what API calls are in the HAR\njq '[.log.entries[].request.url] | unique[]' /Users/kapilan.cholan1/othergit/desk-booking/kadence/ot.har 2>/dev/null | head -40"
}
```

---

### You _10:53:37_

[Tool result]
```
"https://api.onkadence.co/v1/bookings/check-concurrent-limit"
"https://api.onkadence.co/v1/closures?building.id=01G6X15CCK3H03X450NQPCHFM1"
"https://api.onkadence.co/v1/contextual-bookable-spaces?itemsPerPage=500&onlyAvailable=true&floor.id=01G6X15J9J81YY4RJRDCHWR27V&guestBooking=false&capacity=1&userId=01GRG9H6HDS5B8B0ATYAZVNQXH&startDate=2026-05-18T15%3A45%3A00&endDate=2026-05-18T15%3A59%3A59&spaceType=desk&page=0&pagination=false"
"https://api.onkadence.co/v1/floors/01G6X15J9J81YY4RJRDCHWR27V"
"https://api.onkadence.co/v1/floors/01G6X15J9J81YY4RJRDCHWR27V/amenities?page=1"
"https://api.onkadence.co/v1/floors?itemsPerPage=500&building.id=01G6X15CCK3H03X450NQPCHFM1&order[name]=asc&page=1"
"https://api.stonly.com/api/v2/widget/integration?widgetId=677d2a43-9e3c-11ed-871a-0a52ff1ec764&stonlyAnonymousId=28be23c1-5773-46f4-83dd-d622ec82de99&url=https%3A%2F%2Fstarling-bank.onkadence.co%2Fexplore%2Fspace-selection&customerUserId=01GRG9H6HDS5B8B0ATYAZVNQXH"
"https://s.clarity.ms/collect"
"https://static.onkadence.co/lightning/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/floor-plan/0199100b-6672-0e17-573f-6cc6d47b7415.svg"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/018278df-e3e4-52fd-5f39-fe30554d6eeb.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/0182912d-df84-d2a4-c33d-bb2ddab05d08.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/01847a6c-cc42-18e5-2ad1-7e1b05292a89.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/01876ab9-7dfe-416d-60fa-58316ea11d4a.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/018fa0a0-7e15-8138-8ab2-7fba44f2c45e.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/0190eec8-33ac-0af9-dec6-fca5a5366557.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/01912be3-d0d9-081f-28ae-d7b44179b006.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/0191f9ea-206a-3fe3-bf17-e9acc59bdd57.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/0196d0c4-ad96-cd56-2f34-cc6378d44566.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/01971776-c720-4d50-a356-3cc92a9b3c48.jpeg/200x200-cover.webp"
"https://static.onkadence.co/resize/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/profile/019c65b3-1c58-c061-67b2-1b4055d5c6c3.jpeg/200x200-cover.webp"
```

---

### You _10:53:37_

[Tool result]
```
(Bash completed with no output)
```

---

### Claude _10:53:40_

[Tool call: Bash]
```json
{
  "command": "# Extract the contextual-bookable-spaces response (the desk list API)\njq '.log.entries[] | select(.request.url | contains(\"contextual-bookable-spaces\")) | .response.content.text' /Users/kapilan.cholan1/othergit/desk-booking/kadence/ot.har 2>/dev/null | head -5"
}
```

---

### Claude _10:53:41_

[Tool call: Bash]
```json
{
  "command": "# Get the SVG URL from HAR\njq '.log.entries[] | select(.request.url | contains(\".svg\")) | {url: .request.url, size: .response.content.size}' /Users/kapilan.cholan1/othergit/desk-booking/kadence/ot.har 2>/dev/null"
}
```

---

### You _10:53:41_

[Tool result]
```
{
  "url": "https://static.onkadence.co/lightning/017b15fb-3e2d-3c3e-9ba0-8d72311f0a13/floor-plan/0199100b-6672-0e17-573f-6cc6d47b7415.svg",
  "size": 322927
}
```

---

### You _10:53:41_

[Tool result]
```
<persisted-output>
Output too large (316.3KB). Full output saved to: /Users/kapilan.cholan1/.claude/projects/-Users-kapilan-cholan1-othergit-desk-booking/4b5b0a18-ba1a-43cf-af2f-dd4f7515069f/tool-results/blrzkmuy2.txt

Preview (first 2KB):
"{\"@context\":\"\\/v1\\/contexts\\/ContextualBookableSpace\",\"@id\":\"\\/v1\\/contextual-bookable-spaces\",\"@type\":\"hydra:Collection\",\"hydra:totalItems\":207,\"hydra:member\":[{\"@id\":\"\\/v1\\/contextual-bookable-spaces\\/01G6X1AXT33V1KY943F8CXP442\",\"@type\":\"ContextualBookableSpace\",\"id\":\"01G6X1AXT33V1KY943F8CXP442\",\"space\":{\"@id\":\"\\/v1\\/spaces\\/01G6X1AXT33V1KY943F8CXP442\",\"@type\":\"Space\",\"id\":\"01G6X1AXT33V1KY943F8CXP442\",\"name\":\"UK-LDN-LFW-5-100A\",\"type\":\"desk\",\"state\":\"booked\",\"neighborhood\":{\"@id\":\"\\/v1\\/neighborhoods\\/01KBM9G995EJDD5ZBNM2JRBPDQ\",\"@type\":\"Neighborhood\",\"id\":\"01KBM9G995EJDD5ZBNM2JRBPDQ\",\"name\":\"Tech South C\",\"maxBookingDurationEnabled\":false,\"maxBookingDurationInMinutes\":60},\"spaceAmenities\":[{\"@id\":\"\\/v1\\/space-amenities\\/01GQFFW7B95J6M017K1RMKGGGC\",\"@type\":\"SpaceAmenity\",\"id\":\"01GQFFW7B95J6M017K1RMKGGGC\",\"amenity\":{\"@id\":\"\\/v1\\/amenities\\/01GQFFV9MXHSY8TDSF5WYPSGGB\",\"@type\":\"Amenity\",\"id\":\"01GQFFV9MXHSY8TDSF5WYPSGGB\",\"name\":\"Windows\",\"faName\":\"window-frame\",\"faStyle\":\"fas\"},\"quantity\":1}],\"capacity\":1,\"assignedToMe\":false,\"resource_type\":\"desk\"},\"restrictions\":[{\"@type\":\"TransientSpaceRestriction\",\"@id\":\"\\/.well-known\\/genid\\/f9770aa40ad85aeb5d9f\",\"restriction\":\"booked\",\"overridable\":false}],\"bookings\":[{\"@id\":\"\\/v1\\/bookings\\/01KQVMCX5ZSZPRP9HD49H5KYA5\",\"@type\":\"Booking\",\"id\":\"01KQVMCX5ZSZPRP9HD49H5KYA5\",\"startDate\":\"2026-05-18T08:00:00+00:00\",\"endDate\":\"2026-05-18T16:00:00+00:00\",\"incognito\":false,\"user\":{\"@id\":\"\\/v1\\/users\\/01HPB1WXM9WA9C6MW1ZA1RWV7T\",\"@type\":\"User\",\"id\":\"01HPB1WXM9WA9C6MW1ZA1RWV7T\",\"firstName\":\"Daniel\",\"lastName\":\"Cosentino\",\"incognito\":false,\"monogram\":\"DC\",\"starred\":false,\"starredRequest\":false,\"starredMeRequest\":false,\"teammate\":true},\"status\":\"checked-in\",\"type\":\"space_desk\",\"guestBooking\":false,\"hasG
...
</persisted-output>
```

---

### Claude _10:53:46_

[Tool call: Bash]
```json
{
  "command": "# See a cleaner sample of a desk from the API response\njq '.log.entries[] | select(.request.url | contains(\"contextual-bookable-spaces\")) | .response.content.text' /Users/kapilan.cholan1/othergit/desk-booking/kadence/ot.har 2>/dev/null | python3 -c \"import sys, json; data = json.loads(json.load(sys.stdin)); print(json.dumps(data['hydra:member'][0], indent=2))\" 2>/dev/null | head -60"
}
```

---

### Claude _10:53:47_

[Tool call: Bash]
```json
{
  "command": "# Look at SVG structure near the top to understand desk element format\nrg -n \"space::desk::\" /Users/kapilan.cholan1/othergit/desk-booking/kadence/floor-plan-5th.svg | head -10"
}
```

---

### You _10:53:51_

[Tool result]
```
(Bash completed with no output)
```

---

### You _10:53:51_

[Tool result]
```
51:    <g id="space::desk::01G6X1AXT33V1KY943F8CXP442" transform="translate(453.1643, 2883.9011) rotate(90) translate(-453.1643, -2883.9011)translate(424.1643, 2839.9011)" fill="#979797">
55:    <g id="space::desk::01G6X1AY3F5B6WWHP50X600DM1" transform="translate(551.1643, 2883.9011) rotate(90) translate(-551.1643, -2883.9011)translate(522.1643, 2839.9011)" fill="#979797">
59:    <g id="space::desk::01G6X1AYE4YSY1M3TD2CY9N4FQ" transform="translate(649.1643, 2883.9011) rotate(90) translate(-649.1643, -2883.9011)translate(620.1643, 2839.9011)" fill="#979797">
63:    <g id="space::desk::01G6X1AYQE0JFGQQPRPBZXP1PH" transform="translate(453.1643, 2951.9011) rotate(-90) translate(-453.1643, -2951.9011)translate(424.1643, 2907.9011)" fill="#979797">
67:    <g id="space::desk::01G6X1AZ2A6HXBYE77FH9CSB58" transform="translate(551.1643, 2951.9011) rotate(-90) translate(-551.1643, -2951.9011)translate(522.1643, 2907.9011)" fill="#979797">
71:    <g id="space::desk::01G6X1AZBEFZYCSEQ69RHWDXEZ" transform="translate(649.1643, 2951.9011) rotate(-90) translate(-649.1643, -2951.9011)translate(620.1643, 2907.9011)" fill="#979797">
75:    <g id="space::desk::01G6X1AZNVQQ7HTS0AJ8QVHFMM" transform="translate(453.1643, 3119.9011) rotate(90) translate(-453.1643, -3119.9011)translate(424.1643, 3075.9011)" fill="#979797">
79:    <g id="space::desk::01G6X1AZZE2BAKZ4BTA8Z8HQF7" transform="translate(551.1643, 3119.9011) rotate(90) translate(-551.1643, -3119.9011)translate(522.1643, 3075.9011)" fill="#979797">
83:    <g id="space::desk::01G6X1B09PGF9JSXFBZ3Q5SKB0" transform="translate(649.1643, 3119.9011) rotate(90) translate(-649.1643, -3119.9011)translate(620.1643, 3075.9011)" fill="#979797">
87:    <g id="space::desk::01G6X1B0M7E1HTXCPNZ0DXJM8B" transform="translate(453.1643, 3187.9011) rotate(-90) translate(-453.1643, -3187.9011)translate(424.1643, 3143.9011)" fill="#979797">
```

---

### Claude _10:53:53_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/desk-booking/kadence/floor-plan-5th.svg",
  "limit": 100
}
```

---

### You _10:53:53_

[Tool error]
```
File content (25028 tokens) exceeds maximum allowed tokens (25000). Use offset and limit parameters to read specific portions of the file, or search for specific content instead of reading the whole file.
```

---

### Claude _10:53:56_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/desk-booking/kadence/floor-plan-5th.svg",
  "limit": 90
}
```

---

### You _10:53:56_

[Tool result]
```
1	<?xml version="1.0" encoding="UTF-8"?>
2	<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="6736px" height="4290px" viewBox="0 0 6736 4290" version="1.1">
3	  <title>London_5th</title>
4	  <g id="London_5th" stroke="none" fill="none" fill-rule="evenodd" stroke-width="1">
5	    <g id="floor" transform="translate(355.1643, 342.9011)" fill="#FFFFFF">
6	      <rect id="Rectangle" fill-rule="nonzero" x="0" y="0" width="3094" height="3214"></rect>
7	    </g>
8	    <g id="walls" transform="translate(342, 337)" fill="#E5E7EC">
9	      <polygon id="Path-36" fill-rule="nonzero" points="487.434141 1.13535891e-22 3093.90822 1.13535891e-22 3093.90822 3213.58621 -8.58732036e-14 3213.58621 0 2324.35017 476.367587 46.798558 515.962576 46.798558 46.2329162 2324.35017 46.2329162 3167.31461 3015.17158 3167.31461 3015.17158 2400.43352 3039.70801 2400.43352 3039.70801 915.509927 3039.70801 46.798558 1836.76189 46.798558 1836.76189 241.166585 1796.16426 241.166585 1796.16426 46.798558 1472.3633 46.798558 1472.3633 241.166585 1439.0432 241.166585 1439.0432 46.798558 476.367587 46.798558"></polygon>
10	      <polygon id="Path-37" fill-rule="nonzero" points="1109.02895 133.633408 1251.33324 133.633408 1251.33324 715.102651 1241.56359 715.102651 1241.56359 675.582644 1010.98783 675.582644 1010.98783 715.102651 998.887236 715.102651 998.887236 645.534173 1010.98783 645.534173 1010.98783 662.948587 1199.08414 662.948587 1199.08414 504.211201 1042.23335 504.211201 1019.32338 593.412115 1010.98783 593.412115 1042.23335 472.210142 1049.39954 472.210142 1045.49987 493.188925 1199.08414 493.188925 1199.08414 345.438264 1075.52946 345.438264 1057.40461 419.896258 1049.39954 416.251517 1075.52946 317.16756 1081.3002 317.16756 1078.35401 333.246289 1199.08414 333.246289 1199.08414 181.080238 1109.02895 181.080238 1090.80329 259.762543 1083.57272 257.523607 1104.66803 172.813296 1104.66803 133.633408"></polygon>
11	      <polygon id="Path-38" fill-rule="nonzero" points="760.533166 587.443464 903.202083 618.680828 893.610477 669.759197 883.687351 666.2275 890.988361 627.242204 809.780756 609.802673 784.423255 724.51838 866.768523 739.048851 873.024112 707.781369 883.687351 711.123454 864.430107 797.849597 854.000368 794.192014 862.081252 756.559745 780.496148 742.616917 760.533166 851.194216 838.794316 868.439062 844.894309 833.739291 857.963084 837.16801 838.794316 928.429229 827.649309 925.260815 835.226731 885.367033 757.28041 868.439062 732.630609 981.677853 812.159925 998.951155 820.948346 961.941669 831.192164 964.09721 817.916346 1017.92466 683.413455 989.138462 743.342922 685.204877 780.496148 692.460664 799.131751 609.802673 757.28041 600.528846"></polygon>
12	      <polygon id="Path-39" fill-rule="nonzero" points="426.876767 1223.18478 1042.4001 1351.72326 985.470661 1617.14953 958.4497 1617.14953 958.4497 1600.38153 973.371868 1600.38153 1023.46774 1361.46481 476.76279 1247.66136 426.876767 1491.82988 903.202083 1589.40111 903.202083 1606.79311 371.927098 1499.64405"></polygon>
13	      <polygon id="Path-40" fill-rule="nonzero" points="303.170468 1617.14953 627.699525 1617.14953 627.699525 1630.199 584.626795 1630.199 584.626795 1725.30558 627.699525 1725.30558 627.699525 1880.07889 584.626795 1880.07889 584.626795 1976.41857 627.699525 1976.41857 627.699525 1988.21955 562.548114 1988.21955 562.548114 1970.6586 574.978398 1970.6586 574.978398 1630.199 355.523516 1630.199 355.523516 1976.41857 509.833464 1976.41857 509.833464 1988.21955 303.170468 1988.21955"></polygon>
14	      <polygon id="Path-41" fill-rule="nonzero" points="316.149627 2126.93867 492.074316 2090.56187 496.561366 2108.75027 378.080928 2132.69016 419.081096 2327.38487 597.027189 2289.9116 556.187729 2095.75318 548.711091 2098.21768 544.60433 2082.12523 572.705499 2075.5807 577.817559 2090.56187 570.180274 2090.56187 609.781948 2286.08271 789.205759 2251.59732 749.989556 2052.43781 629.76307 2077.69962 626.01553 2065.4536 760.594704 2036.31773 826.161237 2353.50988 783.063023 2363.67162 779.438776 2351.13028 810.458426 2343.60982 792.592007 2269.41008 681.214614 2289.9116 699.438483 2369.36889 731.52861 2360.67693 734.593016 2373.90442 648.445846 2395.62857 645.836904 2382.58071 681.214614 2373.90442 664.391497 2292.68451 551.396613 2317.67229 566.607404 2397.84426 604.362894 2389.44893 607.121304 2402.70459 520.050929 2422.03789 517.178585 2409.62912 551.396613 2400.52636 536.093447 2320.57793 422.432971 2343.60982 439.178535 2422.03789 478.022393 2414.6286 481.273371 2429.49274 383.58043 2448.89785"></polygon>
15	      <polygon id="Path-42" fill-rule="nonzero" points="972.815153 798.124845 1353.30164 798.124845 1353.30164 822.867928 1243.87152 822.867928 1243.87152 917.973275 1429.52001 917.973275 1429.52001 822.867928 1408.09241 822.867928 1408.09241 798.124845 1452.9091 798.124845 1452.9091 822.867928 1442.44121 822.867928 1442.44121 917.973275 1508.62472 917.973275 1505.97093 798.124845 1525.99244 798.124845 1525.99244 822.867928 1519.19042 822.867928 1519.19042 917.973275 1585.27102 917.973275 1582.21585 798.124845 1756.53953 798.124845 1756.53953 822.867928 1599.14121 822.867928 1599.14121 917.973275 1826.22225 917.973275 1826.22225 822.867928 1805.47016 822.867928 1805.47016 798.124845 1830.01516 798.124845 1830.01516 718.277019 1449.78344 718.277019 1449.78344 737.155602 1433.78532 737.155602 1433.78532 600.34804 1852.94609 600.34804 1852.94609 614.088839 1839.0268 614.088839 1839.0268 798.124845 1861.51374 798.124845 1861.51374 822.867928 1839.0268 822.867928 1839.0268 917.973275 2024.06755 917.973275 2024.06755 822.867928 1909.92378 822.867928 1909.92378 798.124845 1988.71953 798.124845 1988.71953 614.088839 1905.79761 614.088839 1905.79761 600.34804 2139.53379 600.34804 2139.53379 737.155602 2127.90399 737.155602 2127.90399 614.088839 2002.92143 614.088839 2002.92143 798.124845 2050.23579 798.124845 2050.23579 822.867928 2036.17526 822.867928 2036.17526 917.973275 2127.90399 917.973275 2127.90399 822.867928 2101.50579 822.867928 2101.50579 798.124845 2139.53379 798.124845 2139.53379 935.53721 2136.51591 935.53721 2136.51591 1145.65311 2127.90399 1145.65311 2127.90399 935.53721 1861.51374 935.53721 1861.51374 1209.80738 2127.90399 1209.80738 2127.90399 1199.91967 2139.53379 1199.91967 2139.53379 1233.51724 2127.90399 1233.51724 2127.90399 1222.73251 1959.09963 1222.73251 1959.09963 1500.94835 2127.90399 1500.94835 2127.90399 1312.28869 2139.53379 1312.28869 2139.53379 1740.52717 2131.04227 1740.52717 2131.04227 1693.3534 2058.24521 1693.3534 2058.24521 1670.72657 2113.31851 1670.72657 2113.31851 1526.97704 1728.38085 1526.97704 1728.38085 1670.72657 1786.59627 1670.72657 1786.59627 1693.3534 1713.01214 1693.3534 1713.01214 1737.55665 1703.17054 1737.55665 1703.17054 1693.3534 1489.5853 1693.3534 1489.5853 1677.16113 1703.17054 1677.16113 1703.17054 1606.79311 1519.19042 1606.79311 1519.19042 1626.1607 1505.97093 1626.1607 1505.97093 1589.40111 1519.19042 1589.40111 1519.19042 1595.8971 1703.17054 1595.8971 1703.17054 1518.70753 1519.19042 1518.70753 1519.19042 1545.52086 1505.97093 1545.52086 1505.97093 1482.01486 1519.19042 1482.01486 1519.19042 1507.94316 1649.31253 1507.94316 1649.31253 1471.36684 1666.26532 1471.36684 1666.26532 1507.94316 1740.22493 1507.94316 1740.22493 1392.86367 1666.26532 1392.86367 1666.26532 1419.16261 1649.31253 1419.16261 1649.31253 1375.21616 1756.53953 1375.21616 1756.53953 1505.08684 1940.16364 1505.08684 1940.16364 1222.73251 1585.27102 1222.73251 1585.27102 1277.5637 1519.19042 1277.5637 1519.19042 1419.16261 1505.97093 1419.16261 1505.97093 1392.86367 1482.97791 1392.86367 1482.97791 1375.21616 1505.97093 1375.21616 1505.97093 1242.38192 1482.97791 1242.38192 1482.97791 1222.73251 1505.97093 1222.73251 1505.97093 1218.09544 1519.19042 1218.09544 1519.19042 1263.88411 1573.14279 1263.88411 1573.14279 1177.10628 1519.19042 1177.10628 1519.19042 1186.50147 1508.62472 1186.50147 1508.62472 1160.60988 1585.27102 1160.60988 1585.27102 1209.80738 1843.76347 1209.80738 1843.76347 1070.37846 1774.75552 1070.37846 1774.75552 963.403068 1405.276 963.403068 1408.09241 1082.67292 1390.3402 1082.67292 1390.3402 1066.53124 1270.94555 1066.53124 1270.94555 1160.60988 1390.3402 1160.60988 1390.3402 1133.92148 1405.276 1133.92148 1408.09241 1222.73251 1420.73775 1222.73251 1420.73775 1242.38192 1408.09241 1242.38192 1408.09241 1307.04407 1390.3402 1307.04407 1390.3402 1177.10628 1270.94555 1177.10628 1270.94555 1375.21616 1390.3402 1375.21616 1390.3402 1355.01268 1405.276 1355.01268 1405.276 1375.21616 1417.97012 1375.21616 1417.97012 1392.86367 1405.276 1392.86367 1405.276 1419.16261 1390.3402 1419.16261 1390.3402 1392.86367 1270.94555 1392.86367 1270.94555 1444.14415 1142.59869 1444.14415 1142.59869 1569.86645 1258.76864 1569.86645 1258.76864 1493.03417 1270.94555 1493.03417 1270.94555 1500.94835 1390.3402 1500.94835 1390.3402 1482.01486 1405.276 1482.01486 1405.276 1677.16113 1417.97012 1677.16113 1417.97012 1693.3534 1358.6154 1693.3534 1358.6154 1677.16113 1381.75122 1677.16113 1381.75122 1526.97704 1270.94555 1526.97704 1270.94555 1677.16113 1289.4807 1677.16113 1289.4807 1693.3534 1142.59869 1693.3534 1142.59869 1740.52717 1126.57786 1740.52717 1126.57786 1670.72657 1142.59869 1670.72657 1142.59869 1677.16113 1258.76864 1677.16113 1258.76864 1581.58987 1142.59869 1581.58987 1142.59869 1617.14953 1126.57786 1617.14953 1126.57786 1282.28547 879.747405 1233.51724 884.3772 1209.80738 896.685994 1209.80738 893.055513 1222.73251 1126.57786 1268.56099 1126.57786 1259.22828 1142.59869 1259.22828 1142.59869 1292.21706 1258.76864 1292.21706 1258.76864 1066.53124 1142.59869 1066.53124 1142.59869 1126.66695 1126.57786 1126.66695 1126.57786 977.880037 945.294309 977.880037 907.681124 1160.60988 896.685994 1157.34104 939.546738 945.685039 949.83629 949.096509 949.83629 966.283322 1126.57786 963.403068 1125.11024 811.423484 978.626407 811.423484 962.568186 893.676102 952.500234 889.998907"></polygon>
16	      <polygon id="Path-43" fill-rule="nonzero" points="1125.4604 1853.92299 1139.96059 1853.92299 1139.96059 1894.37071 1371.01608 1894.37071 1371.01608 1910.0752 1351.40007 1910.0752 1351.40007 2284.13309 1359.74672 2284.13309 1359.74672 2296.4715 1351.40007 2296.4715 1351.40007 2319.5394 1397.81552 2319.5394 1397.81552 2296.4715 1386.41499 2296.4715 1386.41499 2284.13309 1433.3804 2284.13309 1433.3804 2296.4715 1410.783 2296.4715 1410.783 2353.52712 1502.39411 2353.52712 1502.39411 2296.4715 1482.21496 2296.4715 1482.21496 2284.13309 1625.27702 2284.13309 1625.27702 2230.37525 1563.33997 2230.37525 1563.33997 2205.86217 1575.72763 2205.86217 1575.72763 2214.30171 1658.53152 2214.30171 1658.53152 2340.6957 2105.10173 2340.6957 2105.10173 2089.55259 1502.39411 2089.55259 1502.39411 1910.0752 1488.5444 1910.0752 1488.5444 1894.37071 1520.28288 1894.37071 1520.28288 2062.55015 1701.55783 2062.55015 1701.55783 1910.0752 1575.72763 1910.0752 1575.72763 1894.37071 1701.55783 1894.37071 1701.55783 1853.92299 1713.14887 1853.92299 1713.14887 1894.37071 1783.65531 1894.37071 1783.65531 1922.4848 1726.34402 1922.4848 1726.34402 2062.55015 2112.83391 2062.55015 2112.83391 1922.4848 2050.63147 1922.4848 2050.63147 1894.37071 2128.53375 1894.37071 2128.53375 1853.92299 2139.53379 1853.92299 2139.53379 2086.16796 2330.09076 2086.16796 2330.09076 2035.48605 2344.57115 2035.48605 2344.57115 2162.88461 2296.50203 2162.88461 2296.50203 2099.91328 2139.53379 2099.91328 2139.53379 2225.36481 2344.57115 2225.36481 2344.57115 2238.27783 2139.53379 2238.27783 2139.53379 2383.17216 2330.09076 2383.17216 2330.09076 2296.4715 2344.57115 2296.4715 2344.57115 2402.97686 2330.09076 2402.97686 2330.09076 2393.00021 2139.53379 2393.00021 2139.53379 2510.47227 2117.4716 2510.47227 2117.4716 2673.64405 2330.09076 2673.64405 2330.09076 2460.06943 2344.57115 2460.06943 2344.57115 2735.3493 2330.09076 2735.3493 2330.09076 2683.2644 2117.4716 2683.2644 2117.4716 2735.3493 1969.9039 2735.3493 1969.9039 2715.48084 1982.30804 2715.48084 1982.30804 2721.94655 2105.10173 2721.94655 2105.10173 2626.02651 1982.30804 2626.02651 1982.30804 2665.11903 1969.9039 2665.11903 1969.9039 2571.012 1982.30804 2571.012 1982.30804 2614.71174 2105.10173 2614.71174 2105.10173 2510.47227 1921.40155 2510.47227 1921.40155 2494.61065 2026.62653 2494.61065 2026.62653 2373.42496 1837.4017 2373.42496 1837.4017 2494.61065 1870.33135 2494.61065 1870.33135 2510.47227 1830.49238 2510.47227 1830.49238 2725.36645 1870.33135 2725.36645 1870.33135 2735.3493 1804.62433 2735.3493 1804.62433 2725.36645 1816.24363 2725.36645 1816.24363 2510.47227 1792.82542 2510.47227 1792.82542 2494.61065 1827.37084 2494.61065 1827.37084 2373.42496 1644.20329 2373.42496 1644.20329 2494.61065 1738.96604 2494.61065 1738.96604 2510.47227 1639.90566 2510.47227 1639.90566 2725.36645 1756.54353 2725.36645 1756.54353 2735.3493 1543.37243 2735.3493 1543.37243 2725.36645 1628.93988 2725.36645 1628.93988 2510.47227 1559.87844 2510.47227 1559.87844 2494.61065 1628.93988 2494.61065 1628.93988 2373.42496 1394.87397 2373.42496 1394.87397 2494.61065 1506.90518 2494.61065 1506.90518 2510.47227 1477.19508 2510.47227 1477.19508 2725.36645 1488.5444 2725.36645 1488.5444 2735.3493 1449.86326 2735.3493 1449.86326 2725.36645 1464.20717 2725.36645 1464.20717 2510.47227 1376.50174 2510.47227 1376.50174 2494.61065 1383.33643 2494.61065 1383.33643 2353.52712 1397.81552 2353.52712 1397.81552 2333.55301 1326.05219 2333.55301 1326.05219 2510.47227 1304.92022 2510.47227 1304.92022 2494.61065 1312.34896 2494.61065 1312.34896 2333.55301 1144.07238 2333.55301 1144.07238 2494.61065 1252.19552 2494.61065 1252.19552 2725.36645 1397.81552 2725.36645 1397.81552 2735.3493 1006.57813 2735.3493 1006.57813 2725.36645 1242.52371 2725.36645 1242.52371 2510.47227 936.254627 2510.47227 936.254627 2725.36645 951.455132 2725.36645 951.455132 2735.3493 926.367862 2735.3493 926.367862 2296.4715 940.471735 2296.4715 940.471735 2482.70744 1125.4604 2482.70744 1125.4604 2238.27783 926.367862 2238.27783 926.367862 2073.41042 940.471735 2073.41042 943.420788 2225.36481 1125.4604 2225.36481 1125.4604 1973.55003 926.367862 2018.98157 926.367862 1999.364 1125.4604 1956.42066"></polygon>
17	      <polygon id="Path-44" fill-rule="nonzero" points="2488.71254 2031.53611 2516.19033 2036.81378 2512.6737 2049.6423 2499.32583 2045.81329 2467.00156 2205.14667 2712.22118 2258.48269 2747.18335 2097.26541 2564.87478 2058.46581 2568.1425 2045.81329 2790.99027 2093.07131 2786.85556 2105.5375 2760.32729 2100.91755 2705.98288 2351.87579 2735.61549 2357.47097 2731.25926 2371.91038 2653.15165 2357.47097 2659.81394 2341.59591 2692.03798 2347.45287 2705.98288 2270.63375 2595.86934 2248.37299 2578.61768 2329.336 2617.14207 2335.53408 2611.67262 2351.87579 2524.08937 2335.53408 2529.68531 2318.0255 2559.88138 2324.40769 2578.61768 2244.52932 2463.88173 2218.47008 2447.05003 2302.01072 2483.79473 2308.44569 2479.19873 2322.75126 2430.41879 2312.63359"></polygon>
18	      <polygon id="Path-45" fill-rule="nonzero" points="2258.20036 1641.98188 2342.68514 1626.38084 2342.68514 1912.63044 2258.20036 1890.51481"></polygon>
19	      <polygon id="Path-46" fill-rule="nonzero" points="2509.41033 1570.91455 2597.60522 1570.91455 2597.60522 1961.94918 2509.41033 1961.94918"></polygon>
20	      <polygon id="Path-47" fill-rule="nonzero" points="2435.98442 1233.61946 2819.122 1156.70793 2870.13573 1404.59344 2566.98777 1468.51119 2566.98777 1456.93294 2819.122 1404.59344 2771.17961 1179.13898 2456.73537 1244.13839 2501.86714 1468.51119 2516.22608 1465.06611 2516.22608 1477.81585 2490.70627 1484.24194"></polygon>
21	      <polygon id="Path-48" fill-rule="nonzero" points="3039.54431 333.186968 2879.66426 333.186968 2879.66426 501.694766 2924.16426 501.694766 2924.16426 489.352271 2890.22866 489.352271 2890.22866 346.09564 3039.54431 346.09564"></polygon>
22	      <polygon id="Path-49" fill-rule="nonzero" points="3039.54431 487.005386 2977.66426 487.005386 2977.66426 501.694766 3039.54431 501.694766"></polygon>
23	      <polygon id="Path-50" fill-rule="nonzero" points="707.163601 1678.77514 762.304881 1678.77514 762.304881 1911.81802 707.163601 1911.81802"></polygon>
24	      <polygon id="Path-51" fill-rule="nonzero" points="2541.37263 382.897484 2696.72972 382.897484 2696.72972 456.657523 2541.37263 456.901105"></polygon>
25	    </g>
26	    <g id="layer" transform="translate(2595.0593, 1586.5231)" fill="#52566C">
27	      <path d="M7.854,15.378 L11.44,4.994 L15.004,15.378 L18.26,15.378 L22.836,0.264 L19.316,0.264 L16.522,10.406 L13.178,0.264 L9.658,0.264 L6.314,10.406 L3.52,0.264 L0,0.264 L4.576,15.378 L7.854,15.378 Z M32.208,15.642 C36.586,15.642 40.238,12.056 40.238,7.81 C40.238,3.564 36.586,0 32.208,0 C27.874,0 24.178,3.564 24.178,7.81 C24.178,12.056 27.874,15.642 32.208,15.642 Z M32.208,12.694 C29.678,12.694 27.522,10.45 27.522,7.81 C27.522,5.192 29.678,2.948 32.208,2.948 C34.76,2.948 36.916,5.192 36.916,7.81 C36.916,10.45 34.76,12.694 32.208,12.694 Z M45.87,15.378 L45.87,10.472 L48.268,10.472 L51.722,15.378 L55.418,15.378 L51.7,10.098 C53.768,9.372 55.132,7.612 55.132,5.368 C55.132,2.376 52.712,0.264 49.434,0.264 L42.636,0.264 L42.636,15.378 L45.87,15.378 Z M49.082,7.502 L45.87,7.502 L45.87,3.234 L49.082,3.234 C50.688,3.234 51.788,4.07 51.788,5.368 C51.788,6.666 50.688,7.502 49.082,7.502 Z M60.896,15.378 L60.896,11.154 L62.788,9.13 L67.43,15.378 L71.368,15.378 L65.054,6.82 L70.95,0.264 L67.012,0.264 L60.896,7.194 L60.896,0.264 L57.662,0.264 L57.662,15.378 L60.896,15.378 Z M76.164,15.378 L76.164,10.472 L79.728,10.472 C83.006,10.472 85.426,8.36 85.426,5.368 C85.426,2.376 83.006,0.264 79.728,0.264 L72.93,0.264 L72.93,15.378 L76.164,15.378 Z M79.376,7.502 L76.164,7.502 L76.164,3.234 L79.376,3.234 C80.982,3.234 82.082,4.07 82.082,5.368 C82.082,6.666 80.982,7.502 79.376,7.502 Z M98.12,15.378 L98.12,12.452 L90.596,12.452 L90.596,0.264 L87.34,0.264 L87.34,15.378 L98.12,15.378 Z M102.63,15.378 L103.598,13.002 L110.33,13.002 L111.298,15.378 L114.95,15.378 L108.79,0.264 L105.16,0.264 L99,15.378 L102.63,15.378 Z M109.208,10.252 L104.72,10.252 L106.964,4.73 L109.208,10.252 Z M124.168,15.642 C126.456,15.642 128.568,14.652 130.042,13.112 L127.688,11 C126.808,12.012 125.554,12.694 124.168,12.694 C121.638,12.694 119.482,10.45 119.482,7.81 C119.482,5.192 121.638,2.948 124.168,2.948 C125.554,2.948 126.808,3.63 127.688,4.642 L130.042,2.53 C128.568,0.968 126.456,0 124.168,0 C119.834,0 116.138,3.564 116.138,7.81 C116.138,12.056 119.834,15.642 124.168,15.642 Z M143.682,15.378 L143.682,12.452 L135.3,12.452 L135.3,9.13 L142.89,9.13 L142.89,6.204 L135.3,6.204 L135.3,3.19 L143.682,3.19 L143.682,0.264 L132.044,0.264 L132.044,15.378 L143.682,15.378 Z M24.387,41.378 L24.387,29.19 L29.095,29.19 L29.095,26.264 L16.423,26.264 L16.423,29.19 L21.131,29.19 L21.131,41.378 L24.387,41.378 Z M42.911,41.378 L42.911,38.452 L34.529,38.452 L34.529,35.13 L42.119,35.13 L42.119,32.204 L34.529,32.204 L34.529,29.19 L42.911,29.19 L42.911,26.264 L31.273,26.264 L31.273,41.378 L42.911,41.378 Z M52.811,41.642 C55.099,41.642 57.211,40.652 58.685,39.112 L56.331,37 C55.451,38.012 54.197,38.694 52.811,38.694 C50.281,38.694 48.125,36.45 48.125,33.81 C48.125,31.192 50.281,28.948 52.811,28.948 C54.197,28.948 55.451,29.63 56.331,30.642 L58.685,28.53 C57.211,26.968 55.099,26 52.811,26 C48.477,26 44.781,29.564 44.781,33.81 C44.781,38.056 48.477,41.642 52.811,41.642 Z M63.921,41.378 L63.921,35.13 L70.807,35.13 L70.807,41.378 L74.041,41.378 L74.041,26.264 L70.807,26.264 L70.807,32.204 L63.921,32.204 L63.921,26.264 L60.687,26.264 L60.687,41.378 L63.921,41.378 Z M91.245,41.378 C94.127,41.378 96.195,39.706 96.195,37.286 C96.195,35.614 95.183,34.206 93.423,33.414 C94.765,32.71 95.535,31.588 95.535,30.048 C95.535,27.848 93.665,26.264 90.959,26.264 L83.303,26.264 L83.303,41.378 L91.245,41.378 Z M90.145,32.358 L86.537,32.358 L86.537,29.036 L90.145,29.036 C91.355,29.036 92.213,29.74 92.213,30.686 C92.213,31.654 91.355,32.358 90.145,32.358 Z M90.651,38.606 L86.537,38.606 L86.537,34.91 L90.651,34.91 C91.949,34.91 92.873,35.68 92.873,36.758 C92.873,37.836 91.949,38.606 90.651,38.606 Z M100.969,41.378 L101.937,39.002 L108.669,39.002 L109.637,41.378 L113.289,41.378 L107.129,26.264 L103.499,26.264 L97.339,41.378 L100.969,41.378 Z M107.547,36.252 L103.059,36.252 L105.303,30.73 L107.547,36.252 Z M118.305,41.378 L118.305,36.472 L120.703,36.472 L124.157,41.378 L127.853,41.378 L124.135,36.098 C126.203,35.372 127.567,33.612 127.567,31.368 C127.567,28.376 125.147,26.264 121.869,26.264 L115.071,26.264 L115.071,41.378 L118.305,41.378 Z M121.517,33.502 L118.305,33.502 L118.305,29.234 L121.517,29.234 C123.123,29.234 124.223,30.07 124.223,31.368 C124.223,32.666 123.123,33.502 121.517,33.502 Z" id="WORKPLACETECHBAR" fill-rule="nonzero"></path>
28	    </g>
29	    <g id="layer2" transform="translate(1633.1643, 746.9011)" fill="#52566C">
30	      <path d="M7.99615385,0.593804155 C8.02692308,0.777397393 8.61538462,4.2852001 8.61538462,5.62504011 C8.61538462,7.66800316 7.54615385,9.12503035 5.96538462,9.71096621 L6.46153846,19.0117215 C6.48846154,19.5468763 6.06923077,20 5.53846154,20 L3.07692308,20 C2.55,20 2.12692308,19.5507825 2.15384615,19.0117215 L2.65,9.71096621 C1.06538462,9.12503035 0,7.66409692 0,5.62504011 C0,4.28129386 0.588461538,0.777397393 0.619230769,0.593804155 C0.742307692,-0.199162382 2.36153846,-0.2108811 2.46153846,0.636772785 L2.46153846,6.15238239 C2.51153846,6.28519452 3.04230769,6.27738204 3.07692308,6.15238239 C3.13076923,5.1641039 3.38076923,0.714897567 3.38461538,0.61333535 C3.51153846,-0.199162382 5.10384615,-0.199162382 5.22692308,0.61333535 C5.23461538,0.718803806 5.48076923,5.1641039 5.53461538,6.15238239 C5.56923077,6.27738204 6.10384615,6.28519452 6.15,6.15238239 L6.15,0.636772785 C6.25,-0.20697486 7.87307692,-0.199162382 7.99615385,0.593804155 Z M12.5807692,11.7539293 L12.0038462,18.9843778 C11.9576923,19.5312513 12.3846154,20 12.9230769,20 L15.0769231,20 C15.5884615,20 16,19.5820324 16,19.0625026 L16,0.937553196 C16,0.421929635 15.5884615,5.58119561e-05 15.0769231,5.58119561e-05 C11.9038462,5.58119561e-05 6.56153846,6.9726926 12.5807692,11.7539293 Z" id="Shape" fill-rule="nonzero"></path>
31	      <path d="M29.3254848,18.7299578 L29.3254848,14.4092827 L31.2710065,12.3389592 L36.0443213,18.7299578 L40.0937211,18.7299578 L33.601108,9.97609001 L39.6638966,3.27004219 L35.6144968,3.27004219 L29.3254848,10.3586498 L29.3254848,3.27004219 L26,3.27004219 L26,18.7299578 L29.3254848,18.7299578 Z M45.0932595,18.7299578 L45.0932595,3.27004219 L41.7451524,3.27004219 L41.7451524,18.7299578 L45.0932595,18.7299578 Z M55.5674054,18.7299578 L55.5674054,6.26300985 L60.4085873,6.26300985 L60.4085873,3.27004219 L47.3781163,3.27004219 L47.3781163,6.26300985 L52.2192982,6.26300985 L52.2192982,18.7299578 L55.5674054,18.7299578 Z M70.2945522,19 C72.6472761,19 74.8190212,17.9873418 76.3347184,16.4120956 L73.9141274,14.2517581 C73.0092336,15.2869198 71.7197599,15.9845288 70.2945522,15.9845288 C67.6929825,15.9845288 65.4759926,13.6891702 65.4759926,10.9887482 C65.4759926,8.31082982 67.6929825,6.01547117 70.2945522,6.01547117 C71.7197599,6.01547117 73.0092336,6.71308017 73.9141274,7.74824191 L76.3347184,5.58790436 C74.8190212,3.99015471 72.6472761,3 70.2945522,3 C65.8379501,3 62.0373961,6.64556962 62.0373961,10.9887482 C62.0373961,15.3319269 65.8379501,19 70.2945522,19 Z M81.7188366,18.7299578 L81.7188366,12.3389592 L88.7996307,12.3389592 L88.7996307,18.7299578 L92.1251154,18.7299578 L92.1251154,3.27004219 L88.7996307,3.27004219 L88.7996307,9.34599156 L81.7188366,9.34599156 L81.7188366,3.27004219 L78.3933518,3.27004219 L78.3933518,18.7299578 L81.7188366,18.7299578 Z M107.168975,18.7299578 L107.168975,15.7369902 L98.5498615,15.7369902 L98.5498615,12.3389592 L106.354571,12.3389592 L106.354571,9.34599156 L98.5498615,9.34599156 L98.5498615,6.26300985 L107.168975,6.26300985 L107.168975,3.27004219 L95.2017544,3.27004219 L95.2017544,18.7299578 L107.168975,18.7299578 Z M113.163897,18.7299578 L113.163897,8.73839662 L120.651893,18.7299578 L124,18.7299578 L124,3.27004219 L120.651893,3.27004219 L120.651893,13.2616034 L113.163897,3.27004219 L109.838412,3.27004219 L109.838412,18.7299578 L113.163897,18.7299578 Z" id="KITCHEN" fill-rule="nonzero"></path>
32	    </g>
33	    <g id="layer2" transform="translate(2725.1643, 2102.9011)" fill="#52566C">
34	      <path d="M7.99615385,0.593804155 C8.02692308,0.777397393 8.61538462,4.2852001 8.61538462,5.62504011 C8.61538462,7.66800316 7.54615385,9.12503035 5.96538462,9.71096621 L6.46153846,19.0117215 C6.48846154,19.5468763 6.06923077,20 5.53846154,20 L3.07692308,20 C2.55,20 2.12692308,19.5507825 2.15384615,19.0117215 L2.65,9.71096621 C1.06538462,9.12503035 0,7.66409692 0,5.62504011 C0,4.28129386 0.588461538,0.777397393 0.619230769,0.593804155 C0.742307692,-0.199162382 2.36153846,-0.2108811 2.46153846,0.636772785 L2.46153846,6.15238239 C2.51153846,6.28519452 3.04230769,6.27738204 3.07692308,6.15238239 C3.13076923,5.1641039 3.38076923,0.714897567 3.38461538,0.61333535 C3.51153846,-0.199162382 5.10384615,-0.199162382 5.22692308,0.61333535 C5.23461538,0.718803806 5.48076923,5.1641039 5.53461538,6.15238239 C5.56923077,6.27738204 6.10384615,6.28519452 6.15,6.15238239 L6.15,0.636772785 C6.25,-0.20697486 7.87307692,-0.199162382 7.99615385,0.593804155 Z M12.5807692,11.7539293 L12.0038462,18.9843778 C11.9576923,19.5312513 12.3846154,20 12.9230769,20 L15.0769231,20 C15.5884615,20 16,19.5820324 16,19.0625026 L16,0.937553196 C16,0.421929635 15.5884615,5.58119559e-05 15.0769231,5.58119559e-05 C11.9038462,5.58119559e-05 6.56153846,6.9726926 12.5807692,11.7539293 Z" id="Shape" fill-rule="nonzero"></path>
35	      <path d="M29.3254848,18.7299578 L29.3254848,14.4092827 L31.2710065,12.3389592 L36.0443213,18.7299578 L40.0937211,18.7299578 L33.601108,9.97609001 L39.6638966,3.27004219 L35.6144968,3.27004219 L29.3254848,10.3586498 L29.3254848,3.27004219 L26,3.27004219 L26,18.7299578 L29.3254848,18.7299578 Z M45.0932595,18.7299578 L45.0932595,3.27004219 L41.7451524,3.27004219 L41.7451524,18.7299578 L45.0932595,18.7299578 Z M55.5674054,18.7299578 L55.5674054,6.26300985 L60.4085873,6.26300985 L60.4085873,3.27004219 L47.3781163,3.27004219 L47.3781163,6.26300985 L52.2192982,6.26300985 L52.2192982,18.7299578 L55.5674054,18.7299578 Z M70.2945522,19 C72.6472761,19 74.8190212,17.9873418 76.3347184,16.4120956 L73.9141274,14.2517581 C73.0092336,15.2869198 71.7197599,15.9845288 70.2945522,15.9845288 C67.6929825,15.9845288 65.4759926,13.6891702 65.4759926,10.9887482 C65.4759926,8.31082982 67.6929825,6.01547117 70.2945522,6.01547117 C71.7197599,6.01547117 73.0092336,6.71308017 73.9141274,7.74824191 L76.3347184,5.58790436 C74.8190212,3.99015471 72.6472761,3 70.2945522,3 C65.8379501,3 62.0373961,6.64556962 62.0373961,10.9887482 C62.0373961,15.3319269 65.8379501,19 70.2945522,19 Z M81.7188366,18.7299578 L81.7188366,12.3389592 L88.7996307,12.3389592 L88.7996307,18.7299578 L92.1251154,18.7299578 L92.1251154,3.27004219 L88.7996307,3.27004219 L88.7996307,9.34599156 L81.7188366,9.34599156 L81.7188366,3.27004219 L78.3933518,3.27004219 L78.3933518,18.7299578 L81.7188366,18.7299578 Z M107.168975,18.7299578 L107.168975,15.7369902 L98.5498615,15.7369902 L98.5498615,12.3389592 L106.354571,12.3389592 L106.354571,9.34599156 L98.5498615,9.34599156 L98.5498615,6.26300985 L107.168975,6.26300985 L107.168975,3.27004219 L95.2017544,3.27004219 L95.2017544,18.7299578 L107.168975,18.7299578 Z M113.163897,18.7299578 L113.163897,8.73839662 L120.651893,18.7299578 L124,18.7299578 L124,3.27004219 L120.651893,3.27004219 L120.651893,13.2616034 L113.163897,3.27004219 L109.838412,3.27004219 L109.838412,18.7299578 L113.163897,18.7299578 Z" id="KITCHEN" fill-rule="nonzero"></path>
36	    </g>
37	    <g id="layer2" transform="translate(1334.1643, 2686.9011)" fill="#52566C">
38	      <path d="M7.99615385,0.593804155 C8.02692308,0.777397393 8.61538462,4.2852001 8.61538462,5.62504011 C8.61538462,7.66800316 7.54615385,9.12503035 5.96538462,9.71096621 L6.46153846,19.0117215 C6.48846154,19.5468763 6.06923077,20 5.53846154,20 L3.07692308,20 C2.55,20 2.12692308,19.5507825 2.15384615,19.0117215 L2.65,9.71096621 C1.06538462,9.12503035 0,7.66409692 0,5.62504011 C0,4.28129386 0.588461538,0.777397393 0.619230769,0.593804155 C0.742307692,-0.199162382 2.36153846,-0.2108811 2.46153846,0.636772785 L2.46153846,6.15238239 C2.51153846,6.28519452 3.04230769,6.27738204 3.07692308,6.15238239 C3.13076923,5.1641039 3.38076923,0.714897567 3.38461538,0.61333535 C3.51153846,-0.199162382 5.10384615,-0.199162382 5.22692308,0.61333535 C5.23461538,0.718803806 5.48076923,5.1641039 5.53461538,6.15238239 C5.56923077,6.27738204 6.10384615,6.28519452 6.15,6.15238239 L6.15,0.636772785 C6.25,-0.20697486 7.87307692,-0.199162382 7.99615385,0.593804155 Z M12.5807692,11.7539293 L12.0038462,18.9843778 C11.9576923,19.5312513 12.3846154,20 12.9230769,20 L15.0769231,20 C15.5884615,20 16,19.5820324 16,19.0625026 L16,0.937553196 C16,0.421929635 15.5884615,5.58119559e-05 15.0769231,5.58119559e-05 C11.9038462,5.58119559e-05 6.56153846,6.9726926 12.5807692,11.7539293 Z" id="Shape" fill-rule="nonzero"></path>
39	      <path d="M29.3254848,18.7299578 L29.3254848,14.4092827 L31.2710065,12.3389592 L36.0443213,18.7299578 L40.0937211,18.7299578 L33.601108,9.97609001 L39.6638966,3.27004219 L35.6144968,3.27004219 L29.3254848,10.3586498 L29.3254848,3.27004219 L26,3.27004219 L26,18.7299578 L29.3254848,18.7299578 Z M45.0932595,18.7299578 L45.0932595,3.27004219 L41.7451524,3.27004219 L41.7451524,18.7299578 L45.0932595,18.7299578 Z M55.5674054,18.7299578 L55.5674054,6.26300985 L60.4085873,6.26300985 L60.4085873,3.27004219 L47.3781163,3.27004219 L47.3781163,6.26300985 L52.2192982,6.26300985 L52.2192982,18.7299578 L55.5674054,18.7299578 Z M70.2945522,19 C72.6472761,19 74.8190212,17.9873418 76.3347184,16.4120956 L73.9141274,14.2517581 C73.0092336,15.2869198 71.7197599,15.9845288 70.2945522,15.9845288 C67.6929825,15.9845288 65.4759926,13.6891702 65.4759926,10.9887482 C65.4759926,8.31082982 67.6929825,6.01547117 70.2945522,6.01547117 C71.7197599,6.01547117 73.0092336,6.71308017 73.9141274,7.74824191 L76.3347184,5.58790436 C74.8190212,3.99015471 72.6472761,3 70.2945522,3 C65.8379501,3 62.0373961,6.64556962 62.0373961,10.9887482 C62.0373961,15.3319269 65.8379501,19 70.2945522,19 Z M81.7188366,18.7299578 L81.7188366,12.3389592 L88.7996307,12.3389592 L88.7996307,18.7299578 L92.1251154,18.7299578 L92.1251154,3.27004219 L88.7996307,3.27004219 L88.7996307,9.34599156 L81.7188366,9.34599156 L81.7188366,3.27004219 L78.3933518,3.27004219 L78.3933518,18.7299578 L81.7188366,18.7299578 Z M107.168975,18.7299578 L107.168975,15.7369902 L98.5498615,15.7369902 L98.5498615,12.3389592 L106.354571,12.3389592 L106.354571,9.34599156 L98.5498615,9.34599156 L98.5498615,6.26300985 L107.168975,6.26300985 L107.168975,3.27004219 L95.2017544,3.27004219 L95.2017544,18.7299578 L107.168975,18.7299578 Z M113.163897,18.7299578 L113.163897,8.73839662 L120.651893,18.7299578 L124,18.7299578 L124,3.27004219 L120.651893,3.27004219 L120.651893,13.2616034 L113.163897,3.27004219 L109.838412,3.27004219 L109.838412,18.7299578 L113.163897,18.7299578 Z" id="KITCHEN" fill-rule="nonzero"></path>
40	    </g>
41	    <g id="door-open" transform="translate(1129.1643, 2128.9011)" fill="#52566C">
42	      <path d="M76.275,20.9999943 L72.65,20.9999943 L72.65,5.31793321 C72.65,4.04011828 71.6744219,2.99996005 70.475,2.99996005 L65.4,2.99996005 L65.4,5.99996005 L69.75,5.99996005 L69.75,24 L76.275,24 C76.6755625,24 77,23.6643744 77,23.2499986 L77,21.7499957 C77,21.3356199 76.6755625,20.9999943 76.275,20.9999943 Z M62.148375,0.047298179 L53.448375,2.37886511 C52.8026719,2.55183419 52.35,3.17339788 52.35,3.88683673 L52.35,21 L48.725,21 C48.3244375,21 48,21.3356199 48,21.7499957 L48,23.2499986 C48,23.6643744 48.3244375,24 48.725,24 L63.95,24 L63.95,1.5552698 C63.95,0.543705373 63.0636875,-0.198327288 62.148375,0.047298179 Z M59.9625,13.4999743 C59.3621094,13.4999743 58.875,12.82826 58.875,11.9999743 C58.875,11.1716943 59.3621094,10.4999743 59.9625,10.4999743 C60.5628906,10.4999743 61.05,11.1716943 61.05,11.9999743 C61.05,12.82826 60.5628906,13.4999743 59.9625,13.4999743 Z" id="Shape" fill-rule="nonzero"></path>
43	      <path d="M3.20576855,44.7468354 L3.20576855,40.0421941 L5.58283503,40.0421941 L9.00668308,44.7468354 L12.6704186,44.7468354 L8.98487513,39.6835443 C11.0348224,38.9873418 12.3869152,37.2995781 12.3869152,35.1476793 C12.3869152,32.278481 9.9880408,30.2531646 6.73865635,30.2531646 L0,30.2531646 L0,44.7468354 L3.20576855,44.7468354 Z M6.38972916,37.1940928 L3.20576855,37.1940928 L3.20576855,33.1012658 L6.38972916,33.1012658 C7.98170946,33.1012658 9.07210693,33.9029536 9.07210693,35.1476793 C9.07210693,36.3924051 7.98170946,37.1940928 6.38972916,37.1940928 Z M26.3221949,44.7468354 L26.3221949,41.9409283 L18.0133662,41.9409283 L18.0133662,38.7552743 L25.5371087,38.7552743 L25.5371087,35.9493671 L18.0133662,35.9493671 L18.0133662,33.0590717 L26.3221949,33.0590717 L26.3221949,30.2531646 L14.7857897,30.2531646 L14.7857897,44.7468354 L26.3221949,44.7468354 Z M36.1357721,45 C38.4037988,45 40.4973619,44.0506329 41.9584945,42.5738397 L39.625044,40.5485232 C38.752726,41.5189873 37.5096729,42.1729958 36.1357721,42.1729958 C33.6278579,42.1729958 31.4906789,40.021097 31.4906789,37.4894515 C31.4906789,34.978903 33.6278579,32.8270042 36.1357721,32.8270042 C37.5096729,32.8270042 38.752726,33.4810127 39.625044,34.4514768 L41.9584945,32.4261603 C40.4973619,30.92827 38.4037988,30 36.1357721,30 C31.839606,30 28.1758706,33.4177215 28.1758706,37.4894515 C28.1758706,41.5611814 31.839606,45 36.1357721,45 Z M55.4794231,44.7468354 L55.4794231,41.9409283 L47.1705944,41.9409283 L47.1705944,38.7552743 L54.694337,38.7552743 L54.694337,35.9493671 L47.1705944,35.9493671 L47.1705944,33.0590717 L55.4794231,33.0590717 L55.4794231,30.2531646 L43.9430179,30.2531646 L43.9430179,44.7468354 L55.4794231,44.7468354 Z M61.127682,44.7468354 L61.127682,40.0421941 L64.6605698,40.0421941 C67.9099543,40.0421941 70.3088287,38.0168776 70.3088287,35.1476793 C70.3088287,32.278481 67.9099543,30.2531646 64.6605698,30.2531646 L57.9219135,30.2531646 L57.9219135,44.7468354 L61.127682,44.7468354 Z M64.3116426,37.1940928 L61.127682,37.1940928 L61.127682,33.1012658 L64.3116426,33.1012658 C65.9036229,33.1012658 66.9940204,33.9029536 66.9940204,35.1476793 C66.9940204,36.3924051 65.9036229,37.1940928 64.3116426,37.1940928 Z M79.4245515,44.7468354 L79.4245515,33.0590717 L84.0914527,33.0590717 L84.0914527,30.2531646 L71.5300739,30.2531646 L71.5300739,33.0590717 L76.196975,33.0590717 L76.196975,44.7468354 L79.4245515,44.7468354 Z M89.5216321,44.7468354 L89.5216321,30.2531646 L86.2940556,30.2531646 L86.2940556,44.7468354 L89.5216321,44.7468354 Z M99.880408,45 C104.22019,45 107.84031,41.5611814 107.84031,37.4894515 C107.84031,33.4177215 104.22019,30 99.880408,30 C95.584242,30 91.9205065,33.4177215 91.9205065,37.4894515 C91.9205065,41.5611814 95.584242,45 99.880408,45 Z M99.880408,42.1729958 C97.3724938,42.1729958 95.2353148,40.021097 95.2353148,37.4894515 C95.2353148,34.978903 97.3724938,32.8270042 99.880408,32.8270042 C102.41013,32.8270042 104.547309,34.978903 104.547309,37.4894515 C104.547309,40.021097 102.41013,42.1729958 99.880408,42.1729958 Z M113.553992,44.7468354 L113.553992,35.3797468 L120.772423,44.7468354 L124,44.7468354 L124,30.2531646 L120.772423,30.2531646 L120.772423,39.6202532 L113.553992,30.2531646 L110.348224,30.2531646 L110.348224,44.7468354 L113.553992,44.7468354 Z" id="RECEPTION" fill-rule="nonzero"></path>
44	    </g>
45	    <g id="sofa" transform="translate(2972.9143, 898.1511) rotate(-180) translate(-2972.9143, -898.1511)translate(2892.1643, 864.4011)" fill="#E5E7EC">
46	      <rect id="Rectangle" fill-rule="nonzero" x="11" y="0.5" width="140" height="54" rx="5"></rect>
47	      <rect id="Rectangle" fill-rule="nonzero" x="153.5" y="0" width="8" height="67" rx="2"></rect>
48	      <rect id="Rectangle" fill-rule="nonzero" x="0.5" y="0" width="8" height="67" rx="2"></rect>
49	      <path d="M77,-18.5 L84,-18.5 C85.1045695,-18.5 86,-17.6045695 86,-16.5 L86,140.5 C86,141.604569 85.1045695,142.5 84,142.5 L77,142.5 C75.8954305,142.5 75,141.604569 75,140.5 L75,-16.5 C75,-17.6045695 75.8954305,-18.5 77,-18.5 Z" id="Rectangle" fill-rule="nonzero" transform="translate(80.5, 62) rotate(90) translate(-80.5, -62)"></path>
50	    </g>
51	    <g id="space::desk::01G6X1AXT33V1KY943F8CXP442" transform="translate(453.1643, 2883.9011) rotate(90) translate(-453.1643, -2883.9011)translate(424.1643, 2839.9011)" fill="#979797">
52	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,2.46025486e-13 51.7073171,2.46025486e-13 L21.2926829,2.46025486e-13 C17.8173301,2.46025486e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
53	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
54	    </g>
55	    <g id="space::desk::01G6X1AY3F5B6WWHP50X600DM1" transform="translate(551.1643, 2883.9011) rotate(90) translate(-551.1643, -2883.9011)translate(522.1643, 2839.9011)" fill="#979797">
56	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,-6.63469216e-13 51.7073171,-6.63469216e-13 L21.2926829,-6.63469216e-13 C17.8173301,-6.63469216e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
57	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
58	    </g>
59	    <g id="space::desk::01G6X1AYE4YSY1M3TD2CY9N4FQ" transform="translate(649.1643, 2883.9011) rotate(90) translate(-649.1643, -2883.9011)translate(620.1643, 2839.9011)" fill="#979797">
60	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,-6.63469216e-13 51.7073171,-6.63469216e-13 L21.2926829,-6.63469216e-13 C17.8173301,-6.63469216e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
61	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
62	    </g>
63	    <g id="space::desk::01G6X1AYQE0JFGQQPRPBZXP1PH" transform="translate(453.1643, 2951.9011) rotate(-90) translate(-453.1643, -2951.9011)translate(424.1643, 2907.9011)" fill="#979797">
64	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,2.51903791e-13 51.7073171,2.51903791e-13 L21.2926829,2.51903791e-13 C17.8173301,2.51903791e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
65	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
66	    </g>
67	    <g id="space::desk::01G6X1AZ2A6HXBYE77FH9CSB58" transform="translate(551.1643, 2951.9011) rotate(-90) translate(-551.1643, -2951.9011)translate(522.1643, 2907.9011)" fill="#979797">
68	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,2.51903791e-13 51.7073171,2.51903791e-13 L21.2926829,2.51903791e-13 C17.8173301,2.51903791e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
69	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
70	    </g>
71	    <g id="space::desk::01G6X1AZBEFZYCSEQ69RHWDXEZ" transform="translate(649.1643, 2951.9011) rotate(-90) translate(-649.1643, -2951.9011)translate(620.1643, 2907.9011)" fill="#979797">
72	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,-6.57590911e-13 51.7073171,-6.57590911e-13 L21.2926829,-6.57590911e-13 C17.8173301,-6.57590911e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
73	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
74	    </g>
75	    <g id="space::desk::01G6X1AZNVQQ7HTS0AJ8QVHFMM" transform="translate(453.1643, 3119.9011) rotate(90) translate(-453.1643, -3119.9011)translate(424.1643, 3075.9011)" fill="#979797">
76	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,-6.49018383e-13 51.7073171,-6.49018383e-13 L21.2926829,-6.49018383e-13 C17.8173301,-6.49018383e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
77	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
78	    </g>
79	    <g id="space::desk::01G6X1AZZE2BAKZ4BTA8Z8HQF7" transform="translate(551.1643, 3119.9011) rotate(90) translate(-551.1643, -3119.9011)translate(522.1643, 3075.9011)" fill="#979797">
80	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,-6.49018383e-13 51.7073171,-6.49018383e-13 L21.2926829,-6.49018383e-13 C17.8173301,-6.49018383e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
81	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
82	    </g>
83	    <g id="space::desk::01G6X1B09PGF9JSXFBZ3Q5SKB0" transform="translate(649.1643, 3119.9011) rotate(90) translate(-649.1643, -3119.9011)translate(620.1643, 3075.9011)" fill="#979797">
84	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,-6.49018383e-13 51.7073171,-6.49018383e-13 L21.2926829,-6.49018383e-13 C17.8173301,-6.49018383e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
85	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
86	    </g>
87	    <g id="space::desk::01G6X1B0M7E1HTXCPNZ0DXJM8B" transform="translate(453.1643, 3187.9011) rotate(-90) translate(-453.1643, -3187.9011)translate(424.1643, 3143.9011)" fill="#979797">
88	      <path d="M15,6.28571429 L15,81.7142857 C15,85.1857899 17.8173301,88 21.2926829,88 L51.7073171,88 C55.1826699,88 58,85.1857899 58,81.7142857 L58,6.28571429 C58,2.81421014 55.1826699,-6.43140079e-13 51.7073171,-6.43140079e-13 L21.2926829,-6.43140079e-13 C17.8173301,-6.43140079e-13 15,2.81421014 15,6.28571429 Z" id="table" fill-rule="nonzero"></path>
89	      <path d="M0,28.5 L0,58.5 C0,59.8807119 0.786231659,61 1.75609756,61 L10.2439024,61 C11.2137683,61 12,59.8807119 12,58.5 L12,28.5 C12,27.1192881 11.2137683,26 10.2439024,26 L1.75609756,26 C0.786231659,26 0,27.1192881 0,28.5 Z" id="chair" fill-rule="nonzero"></path>
90	    </g>
```

---

### Claude _10:54:41_

Now I have everything needed. Here's a complete explanation of the pipeline:

---

## HAR → SVG + desk API metadata → `floor-map-5th.json`

The pipeline is in `kadence/floor_pipeline.py`. There are two independent data sources captured in `ot.har`, then merged.

### Step 1 — What's in the HAR file

The `.har` file is a browser network capture made while browsing the Kadence desk booking UI. It captured two things:

**Source A — SVG floor plan** (from `static.onkadence.co/.../floor-plan/...svg`):
Kadence's visual floor map as SVG XML. Each desk is a `<g>` element whose `id` attribute encodes its ULID: `space::desk::01G6X1AXT33V1KY943F8CXP442`. The position is in the `transform` attribute as a chain of SVG transforms:
```
transform="translate(453.16, 2883.90) rotate(90) translate(-453.16, -2883.90) translate(424.16, 2839.90)"
```
The pipeline pulls the **first** `translate(x, y)` — that's the desk's centre position in SVG coordinate space.

**Source B — Kadence API** (`/v1/contextual-bookable-spaces?...floor.id=01G6X15J9J81YY4RJRDCHWR27V`):
A JSON collection with 207 desk objects. Each has `id` (same ULID), `name` (e.g. `UK-LDN-LFW-5-100A`), and `neighborhood.name` (e.g. `Tech South C`). No coordinates — only metadata.

Both sources use the same ULID as the join key.

---

### Step 2 — SVG parsing (`parse_svg` at line 62)

Walks the SVG with `xml.etree.ElementTree`. For every element:
- ID starts with `space::desk::` → extract ULID, parse `translate(x,y)` and `rotate(angle)` from transform → `{id, x, y, rotation}`
- ID starts with `space::pod::` → same treatment
- ID == `"walls"` → extract child `<polygon points="...">` and convert to `[[x,y], ...]` arrays (applying the `walls` group's own translate offset)
- ID in `LANDMARK_IDS` → grab parent element's translate as `{type, x, y}`
- ID in `UNAVAILABLE_IDS` → grab translate as `{x, y, rotation, shape}`

Returns a dict with `viewbox`, `desks`, `pods`, `walls`, `landmarks`, `unavailable`.

---

### Step 3 — Merge (`build_floor_map` at line 115)

Loads the desk API JSON (extracted from the HAR separately and saved as `desks-5th.json`) keyed by ULID. For each SVG desk, looks up the matching API record and merges:

```
SVG gives:        {id, x, y, rotation}
API gives:        {name, neighborhood}
Result:           {id, name, neighborhood, x, y, rotation}
```

Desks that appear in the SVG but not the API (16 in this case) are dropped from the output.

---

### Output — `floor-map-5th.json`

```json
{
  "floor": { "id": "...", "name": "5th Floor", "building": "...", "viewBox": {"width": 6736, "height": 4290} },
  "neighborhoods": ["Agile Workspaces", "Banking", ...],
  "spaces": {
    "desks": [{"id": "01G6X1AXT33V1KY943F8CXP442", "name": "UK-LDN-LFW-5-100A", "neighborhood": "Tech South C", "x": 453.16, "y": 2883.9, "rotation": 90.0}, ...],
    "pods": [...]
  },
  "walls": [{"id": "Path-36", "points": [[x,y], ...]}, ...],
  "landmarks": [{"type": "KITCHEN", "x": ..., "y": ...}, ...],
  "unavailableSpaces": [...]
}
```

---

### Java rewrite notes

The key things to replicate:

| Concern | Detail |
|---|---|
| **SVG parsing** | Use `javax.xml.parsers.DocumentBuilder` or JAXP. Walk elements, check `id` attribute prefix. |
| **Transform parsing** | Regex on the `transform` string: `translate\(([^)]+)\)` and `rotate\(([^)]+)\)`. Extract the **first** translate only. |
| **Polygon points** | Regex `[-+]?\d+(\.\d+)?` to extract all numbers from the `points` attribute, then pair them up, applying the group's offset. |
| **Landmark parent** | You need the parent element's transform, so build a child→parent map before walking (same as `parent_map` at line 67). |
| **HAR extraction** | The HAR is just JSON — use Jackson/Gson to navigate `.log.entries[]`, match `.request.url`, and extract `.response.content.text`. The text field is a JSON string that needs a second parse. |
| **Merge** | Load desk API records into a `Map<String, DeskMeta>` keyed by ULID, then join with SVG desks. |
| **Coordinate system** | x/y are raw SVG pixels from the viewBox (6736×4290). No further transformation needed. |

---

### You _10:56:26_

<local-command-stdout>Copied to clipboard (5025 characters, 84 lines)
Also written to /tmp/claude-502/response.md</local-command-stdout>

---
