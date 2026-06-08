# Explain renderer input to output workflow

**Project:** booking  
**Session:** `d69f1625-a12a-4057-9198-b4633e295a76`  
**Date:** 2026-06-08  

---

### You _10:33:47_

explain how renderer works – input to ouput

---

### Claude _10:33:50_

Let me explore the project to find the renderer.

---

### Claude _10:33:50_

[Tool call: Bash]
```json
{
  "command": "fd -t f . /Users/kapilan.cholan1/othergit/desk-booking --exclude .git | sort"
}
```

---

### You _10:33:53_

[Tool result]
```
/Users/kapilan.cholan1/othergit/desk-booking/algo.md
/Users/kapilan.cholan1/othergit/desk-booking/bin/main/deskbooking/workday/OrgNode.class
/Users/kapilan.cholan1/othergit/desk-booking/bin/main/deskbooking/workday/OrgTree.class
/Users/kapilan.cholan1/othergit/desk-booking/bin/main/deskbooking/workday/OrgTreeCli.class
/Users/kapilan.cholan1/othergit/desk-booking/build.gradle
/Users/kapilan.cholan1/othergit/desk-booking/CLAUDE.md
/Users/kapilan.cholan1/othergit/desk-booking/demo.py
/Users/kapilan.cholan1/othergit/desk-booking/desk_booking/__init__.py
/Users/kapilan.cholan1/othergit/desk-booking/desk_booking/data.py
/Users/kapilan.cholan1/othergit/desk-booking/desk_booking/metrics.py
/Users/kapilan.cholan1/othergit/desk-booking/desk_booking/solver.py
/Users/kapilan.cholan1/othergit/desk-booking/gradle/wrapper/gradle-wrapper.jar
/Users/kapilan.cholan1/othergit/desk-booking/gradle/wrapper/gradle-wrapper.properties
/Users/kapilan.cholan1/othergit/desk-booking/gradlew
/Users/kapilan.cholan1/othergit/desk-booking/gradlew.bat
/Users/kapilan.cholan1/othergit/desk-booking/kadence/floor_pipeline.py
/Users/kapilan.cholan1/othergit/desk-booking/kadence/floor-map-5th.html
/Users/kapilan.cholan1/othergit/desk-booking/kadence/floor-map-5th.json
/Users/kapilan.cholan1/othergit/desk-booking/kadence/floor-plan-5th.svg
/Users/kapilan.cholan1/othergit/desk-booking/kadence/ot.har
/Users/kapilan.cholan1/othergit/desk-booking/pyproject.toml
/Users/kapilan.cholan1/othergit/desk-booking/README.md
/Users/kapilan.cholan1/othergit/desk-booking/settings.gradle
/Users/kapilan.cholan1/othergit/desk-booking/simulate.py
/Users/kapilan.cholan1/othergit/desk-booking/src/main/java/deskbooking/workday/OrgNode.java
/Users/kapilan.cholan1/othergit/desk-booking/src/main/java/deskbooking/workday/OrgTree.java
/Users/kapilan.cholan1/othergit/desk-booking/src/main/java/deskbooking/workday/OrgTreeCli.java
/Users/kapilan.cholan1/othergit/desk-booking/uv.lock
/Users/kapilan.cholan1/othergit/desk-booking/workday/CLAUDE.md
/Users/kapilan.cholan1/othergit/desk-booking/workday/orgchart.csv
/Users/kapilan.cholan1/othergit/desk-booking/workday/orgchart.json
/Users/kapilan.cholan1/othergit/desk-booking/workday/orgchart.xlsx
```

---

### Claude _10:33:57_

There's no file named "renderer" — let me check if rendering logic lives in `floor_pipeline.py`, the HTML file, or elsewhere.

---

### Claude _10:33:58_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/desk-booking/kadence/floor_pipeline.py"
}
```

---

### Claude _10:33:58_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/desk-booking/demo.py"
}
```

---

### Claude _10:33:58_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/desk-booking/simulate.py"
}
```

---

### You _10:33:58_

[Tool result]
```
1	"""Quick demo: solve for 120 random London employees and evaluate."""
2	import random
3	from desk_booking import solve, evaluate, load_nodes
4	
5	
6	def main():
7	    nodes = load_nodes()
8	    london = [eid for eid, n in nodes.items() if n["location"] == "London"]
9	    random.seed(42)
10	    bookers = random.sample(london, 120)
11	
12	    assignments = solve(bookers)
13	    evaluate(assignments, nodes)
14	
15	    print("Sample assignments (first 20):")
16	    print(f"{'Employee':<35} {'Desk':<20} {'Neighbourhood'}")
17	    print("-" * 75)
18	    for pid, desk in list(assignments.items())[:20]:
19	        print(f"{nodes[pid]['name']:<35} {desk['name']:<20} {desk['neighborhood']}")
20	
21	
22	if __name__ == "__main__":
23	    main()
24
```

---

### You _10:33:58_

[Tool result]
```
1	"""Monte Carlo simulation of the desk assignment solver.
2	
3	Each run picks n=191 (full floor) random employees as bookers,
4	solves the assignment, and collects quality metrics.
5	
6	Usage:
7	    uv run python simulate.py                           # all London employees
8	    uv run python simulate.py --subtree 75_Martin_Dow   # engineering only
9	    uv run python simulate.py --sims 50 --out results.csv
10	"""
11	
12	import csv
13	import math
14	import random
15	import time
16	from pathlib import Path
17	from typing import Annotated, Optional
18	
19	import numpy as np
20	import typer
21	
22	from desk_booking import load_desks, load_nodes
23	from desk_booking.metrics import sim_metrics
24	from desk_booking.solver import _desk_distance_matrix, _local_search, _qap_cost, _spectral_assign
25	from workday.org_tree import org_distance
26	
27	app = typer.Typer()
28	
29	
30	def _subtree_ids(nodes, root):
31	    result = []
32	    stack = [root]
33	    while stack:
34	        eid = stack.pop()
35	        result.append(eid)
36	        stack.extend(nodes[eid]["children"])
37	    return result
38	
39	
40	def _build_matrices(people, nodes, alpha):
41	    """Compute flow matrix F and raw org-distance matrix OD in one pass."""
42	    n = len(people)
43	    F = np.zeros((n, n))
44	    OD = np.zeros((n, n), dtype=int)
45	    for i in range(n):
46	        for j in range(i + 1, n):
47	            d = org_distance(nodes, people[i], people[j])
48	            OD[i, j] = OD[j, i] = d
49	            v = math.exp(-alpha * d)
50	            F[i, j] = F[j, i] = v
51	    return F, OD
52	
53	
54	@app.command()
55	def main(
56	    subtree: Annotated[
57	        Optional[str],
58	        typer.Option(help="Root employee ID to filter pool (e.g. 75_Martin_Dow)"),
59	    ] = None,
60	    sims: Annotated[int, typer.Option(help="Number of simulations to run")] = 100,
61	    alpha: Annotated[float, typer.Option(help="Org similarity decay rate")] = 0.7,
62	    k: Annotated[int, typer.Option(help="Spectral embedding dimensions")] = 20,
63	    seed: Annotated[int, typer.Option(help="Random seed")] = 42,
64	    k_neighbours: Annotated[int, typer.Option(help="Nearest neighbours for cohesion metric")] = 5,
65	    org_close_threshold: Annotated[
66	        int, typer.Option(help="Max org distance considered close")
67	    ] = 3,
68	    out: Annotated[Optional[Path], typer.Option(help="Write per-run results to CSV")] = None,
69	):
70	    random.seed(seed)
71	    np.random.seed(seed)
72	
73	    nodes = load_nodes()
74	    desks = load_desks()
75	    n = len(desks)  # 191 — seats == bookings
76	
77	    if subtree:
78	        pool = [e for e in _subtree_ids(nodes, subtree) if nodes[e]["location"] == "London"]
79	        label = f"subtree of {nodes[subtree]['name']} (London only)"
80	    else:
81	        pool = [eid for eid, nd in nodes.items() if nd["location"] == "London"]
82	        label = "all London employees"
83	
84	    assert len(pool) >= n, f"Only {len(pool)} eligible employees but need {n}"
85	
86	    print(f"Desks: {n}  |  Pool: {len(pool)} ({label})  |  Simulations: {sims}")
87	    print(
88	        f"Alpha: {alpha}  k: {k}  K_neighbours: {k_neighbours}"
89	        f"  org_close_threshold: {org_close_threshold}"
90	    )
91	    print()
92	
93	    D = _desk_distance_matrix(desks)
94	    k_embed = min(k, n)
95	
96	    header = (
97	        f"{'Sim':>5}  {'Cost before':>12}  {'Cost after':>12}  {'Improv':>7}"
98	        f"  {'Sweeps':>6}  {'Cohesion':>9}  {'NN org':>6}  {'Worst team':>10}"
99	    )
100	    print(header)
101	    print("-" * len(header))
102	
103	    records = []
104	    t0 = time.time()
105	
106	    for sim in range(1, sims + 1):
107	        people = random.sample(pool, n)
108	        F, OD = _build_matrices(people, nodes, alpha)
109	
110	        assignment = _spectral_assign(F, D, k=k_embed)
111	        cost_before = _qap_cost(assignment, F, D)
112	
113	        assignment, sweeps = _local_search(assignment, F, D)
114	        cost_after = _qap_cost(assignment, F, D)
115	        improvement = 100 * (cost_before - cost_after) / cost_before
116	
117	        metrics = sim_metrics(
118	            assignment, F, OD, D, k_neighbours=k_neighbours, org_close_threshold=org_close_threshold
119	        )
120	
121	        row = {
122	            "sim": sim,
123	            "cost_before": cost_before,
124	            "cost_after": cost_after,
125	            "improvement": improvement,
126	            "sweeps": sweeps,
127	            **metrics,
128	        }
129	        records.append(row)
130	
131	        print(
132	            f"{sim:>5}  {cost_before:>12,.1f}  {cost_after:>12,.1f}  "
133	            f"{improvement:>6.1f}%  {sweeps:>6}  "
134	            f"{metrics['cohesion_mean']:>8.1%}  {metrics['nn_org_median']:>6.0f}  "
135	            f"{metrics['worst_same_team']:>9.0f}px"
136	        )
137	
138	    elapsed = time.time() - t0
139	
140	    # Summary
141	    def stat(key):
142	        vals = [r[key] for r in records]
143	        return np.mean(vals), np.std(vals), np.min(vals), np.max(vals)
144	
145	    print()
146	    print("═" * 70)
147	    print(f"  SUMMARY  ({sims} simulations, {elapsed:.1f}s total, {elapsed/sims:.2f}s/sim)")
148	    print("═" * 70)
149	    print(f"  {'Metric':<28} {'Mean':>10}  {'Std':>8}  {'Min':>10}  {'Max':>10}")
150	    print(f"  {'-'*28}  {'-'*8}  {'-'*8}  {'-'*10}  {'-'*10}")
151	
152	    def summary_row(label, key, fmt=".1f"):
153	        m, s, lo, hi = stat(key)
154	        print(f"  {label:<28} {m:>10{fmt}}  {s:>8{fmt}}  {lo:>10{fmt}}  {hi:>10{fmt}}")
155	
156	    summary_row("QAP cost (before polish)", "cost_before", ",.1f")
157	    summary_row("QAP cost (after polish)", "cost_after", ",.1f")
158	    summary_row("Improvement (%)", "improvement", ".2f")
159	    summary_row("Sweeps", "sweeps", ".1f")
160	    summary_row("Cohesion mean", "cohesion_mean", ".3f")
161	    summary_row("NN org dist (median)", "nn_org_median", ".2f")
162	    summary_row("Worst same-team dist (px)", "worst_same_team", ".1f")
163	    summary_row("Same-team pairs per run", "n_same_team_pairs", ".1f")
164	    print()
165	
166	    if out is not None:
167	        fieldnames = [
168	            "sim",
169	            "cost_before",
170	            "cost_after",
171	            "improvement",
172	            "sweeps",
173	            "cohesion_mean",
174	            "nn_org_median",
175	            "worst_same_team",
176	            "n_same_team_pairs",
177	        ]
178	        with open(out, "w", newline="") as f:
179	            writer = csv.DictWriter(f, fieldnames=fieldnames)
180	            writer.writeheader()
181	            writer.writerows(records)
182	        print(f"Results written to {out}")
183	
184	
185	if __name__ == "__main__":
186	    app()
187
```

---

### You _10:33:58_

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

### Claude _10:34:22_

The renderer is `render_html` in `kadence/floor_pipeline.py`. Here's the full pipeline:

---

## Input → Output

### Stage 1: Parse (`parse_svg`) — SVG → raw geometry dict

`floor-plan-5th.svg` is parsed with `xml.etree.ElementTree`. It scans every element and classifies by `id` prefix:

| SVG `id` pattern | Extracted as |
|---|---|
| `space::desk::*` | desk `{id, x, y, rotation}` from `translate(x,y)` transform |
| `space::pod::*` | pod `{id, x, y, rotation}` |
| `id="walls"` group | wall polygons with absolute point coords (parent translate applied) |
| `KITCHEN`, `STAIRS`, etc. | landmarks — position taken from the *parent* element's transform |
| `unavailable*` | unavailable circles |

Returns a dict with `viewbox`, `desks`, `pods`, `walls`, `landmarks`, `unavailableSpaces`.

---

### Stage 2: Enrich (`build_floor_map`) — geometry + API metadata → floor map JSON

Merges the SVG geometry with `desks-5th.json` (desk API metadata, loaded separately):
- Joins on desk `id` to attach `name` and `neighborhood` to each SVG desk
- Unmatched SVG desks (no API record) are dropped
- Writes `floor-map-5th.json` — the canonical data file used by the solver

---

### Stage 3: Render (`render_html`) — floor map JSON → self-contained HTML

`render_html` takes the floor map dict and produces `floor-map-5th.html` by embedding everything into a single HTML file:

1. **Data injection** — the entire floor map dict is JSON-serialised and injected as `const MAP = {...}` inline in the `<script>` tag. A `COLOURS` map is built by cycling `PALETTE` over neighborhood names.

2. **Canvas setup** — a full-window `<canvas>` element. `fitToWindow()` computes `scale`, `ox`, `oy` so the floor plan fills the viewport with a 40px margin. Coordinate transform helpers: `tx(x) = ox + x*scale`, `ty(y) = oy + y*scale`.

3. **Draw loop** (`draw()`) — called on every interaction. Renders in layered order:
   - Walls (filled polygons)
   - Unavailable spaces (grey circles)
   - Pods (medium grey circles)
   - Desks (coloured circles, colour = neighborhood)
   - Landmarks (yellow squares + label at sufficient zoom)

4. **Hit testing** (`hitTest`) — on `mousemove`, converts mouse coords back to floor coords `(mx, my) = (clientX - ox)/scale`, then does circle/polygon containment tests against every element. The first hit sets `hovered`, triggering a redraw that highlights it and shows a tooltip.

5. **Navigation** — scroll wheel zooms (adjusting `scale`, `ox`, `oy` around the cursor); drag pans (updates `ox`, `oy`).

---

### End-to-end summary

```
floor-plan-5th.svg  ──parse_svg()──▶  raw geometry
desks-5th.json      ─┐
                      ├─build_floor_map()──▶  floor-map-5th.json
                     raw geometry ──────┘
floor-map-5th.json  ──render_html()──▶  floor-map-5th.html
                                        (self-contained, interactive canvas)
```

The `--render` flag skips the parse+enrich stages and goes straight from the existing JSON to HTML, useful when you only want to update the visualiser styling.

---
