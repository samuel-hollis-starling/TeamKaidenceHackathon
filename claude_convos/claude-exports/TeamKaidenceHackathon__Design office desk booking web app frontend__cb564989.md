# Design office desk booking web app frontend

**Project:** TeamKaidenceHackathon  
**Session:** `cb564989-73d4-4aea-b0f3-43a51244f0ad`  
**Date:** 2026-06-08  

---

### You _10:53:29_

Hello I am brand new to front end development, I am creating a web app front end for a desk booking system similar to Kadence for an office. Two other devs are working on the backend. We have 6 hours and it's a hackathon so I need the most impressive front end design ever. What do you suggest?

---

### Claude _10:53:37_

Let me take a quick look at what's already in the project before making recommendations.

---

### Claude _10:53:38_

[Tool call: Bash]
```json
{
  "command": "find /Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon -type f | head -60 && echo \"---\" && ls /Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon",
  "description": "Survey project structure"
}
```

---

### You _10:53:40_

[Tool result]
```
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/gradlew
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.gitignore
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/build.gradle.kts
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/settings.gradle.kts
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/PLAN.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/gradlew.bat
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/input-data/orgchart.csv
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/input-data/floor-map-5th.json
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/input-data/orgchart.json
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/input-data/floor-plan-5th.svg
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/input-data/floor-map-5th.html
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/tsconfig.node.json
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/index.html
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/tsconfig.app.json
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/README.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/.gitignore
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/package-lock.json
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/package.json
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/tsconfig.json
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/eslint.config.js
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/vite.config.ts
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/architecture.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/api.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/floor-map.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/backend.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/index.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/models.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/org-chart.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/algorithm.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/frontend.md
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.git/ORIG_HEAD
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.git/config
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.git/HEAD
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.git/index
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.git/packed-refs
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.git/FETCH_HEAD
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.idea/go.imports.xml
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.idea/gradle.xml
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.idea/vcs.xml
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.idea/.gitignore
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.idea/workspace.xml
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.idea/modules.xml
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.idea/TeamKaidenceHackathon.iml
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.idea/misc.xml
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/src/main/java/com/starlingbank/AppModule.java
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/src/main/java/com/starlingbank/HelloServiceImpl.java
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/src/main/java/com/starlingbank/HelloResource.java
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/src/main/java/com/starlingbank/Main.java
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/src/main/java/com/starlingbank/HelloService.java
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/public/icons.svg
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/public/favicon.svg
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/src/App.tsx
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/src/main.tsx
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/src/App.css
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/src/index.css
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/gradle/wrapper/gradle-wrapper.jar
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/gradle/wrapper/gradle-wrapper.properties
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.git/logs/HEAD
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.git/hooks/post-commit
/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/.git/hooks/commit-msg
---
build.gradle.kts
docs
frontend
gradle
gradlew
gradlew.bat
input-data
PLAN.md
settings.gradle.kts
src
```

---

### Claude _10:53:42_

[Tool call: Read]
```json
{
  "file_path": "/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/frontend/package.json"
}
```

---

### Claude _10:53:42_

[Tool call: Read]
```json
{
  "file_path": "/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/PLAN.md"
}
```

---

### Claude _10:53:42_

[Tool call: Read]
```json
{
  "file_path": "/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/frontend.md"
}
```

---

### Claude _10:53:43_

[Tool call: Read]
```json
{
  "file_path": "/Users/verity.bennett/IdeaProjects/TeamKaidenceHackathon/docs/api.md"
}
```

---

### You _10:53:43_

[Tool result]
```
1	# Desk Booking Hackathon — Project Plan
2	
3	## Concept
4	
5	On-demand desk assignment for a hybrid office. Employees declare they're coming in and set preferences; a single algorithm run assigns everyone to a desk for the day. The UI shows a live floor map and a scoring dashboard measuring assignment quality.
6	
7	**Office**: 5th floor, 191 desks across 15 neighborhoods.
8	**Capacity**: first-come-first-served up to 191 bookings; after that, no more.
9	
10	---
11	
12	## Tech Stack
13	
14	- **Backend**: Java, Jersey (JAX-RS), Guice, TypeScript generation from annotations
15	- **Frontend**: React, renders floor map from JSON coordinates (not the SVG file)
16	- **Persistence**: JSON file on disk for demo purposes; no real DB
17	
18	---
19	
20	## Data Inputs
21	
22	### Floor Map (`floor-map-5th.json`)
23	Each desk has: `id`, `name`, `neighborhood`, `x`, `y`, `rotation`
24	- Window desks = desks at the perimeter (near min/max x or y bounds of the floor plan). Office is described as a square with windows all around, so perimeter detection uses coordinate bounds.
25	- 15 neighborhoods used for visual grouping on the map.
26	
27	### Org Chart (`orgchart.json`)
28	Each employee has: `id`, `name`, `role`, `location`, `org`, `depth`, `orgPath`, `parent`, `children`
29	- Tree rooted at the CEO.
30	- Team relationship weight between two people = `1 / (tree_distance between them)`, where `tree_distance` is the number of hops through the LCA (lowest common ancestor).
31	- Direct siblings (same manager) = highest weight; same dept but further up = lower weight.
32	
33	---
34	
35	## User Preferences (per booking)
36	
37	| Preference | Description |
38	|---|---|
39	| **Team auto-clustering** | Always on. Org chart used to compute relationship weights. |
40	| **Window seat** | Prefer desks near the building perimeter. |
41	| **Talk to me** | Prefer to be seated near other sociable people. |
42	| **Don't talk to me** | Prefer isolation; seat away from high-traffic / social clusters. |
43	| **I'm feeling lucky** | Seat the user next to the highest-ranking person in the office that day. |
44	
45	---
46	
47	## System Components
48	
49	### Backend Services
50	
51	#### `FloorMapService`
52	- Loads desks from `floor-map-5th.json` at startup
53	- Provides: `List<Desk> getDesks()`, `double distance(deskA, deskB)` (Euclidean)
54	- Identifies perimeter desks (window seats) using coordinate bounding box
55	
56	#### `OrgChartService`
57	- Loads `orgchart.json` at startup
58	- Provides: `double relationshipWeight(employeeA, employeeB)` using tree distance via LCA
59	- Provides: `Employee getHighestRanking(List<Employee> present)` for "feeling lucky"
60	
61	#### `BookingService`
62	- Manages bookings for the current day (in-memory + JSON file dump)
63	- `POST /bookings` — employee books in with preferences
64	- `GET /bookings` — list all bookings for today
65	- Enforces 191-person capacity cap
66	
67	#### `AssignmentService`
68	- Single method: `Map<EmployeeId, DeskId> assign(List<Booking> bookings, List<Desk> desks)`
69	- **Stub v0**: random shuffle (initial implementation)
70	- **V1**: Random (baseline, for scoring comparison)
71	- **V2** (stretch): Local search / simulated annealing on the QAP objective
72	
73	#### `ScoringService`
74	- Computes all heuristics against a completed assignment
75	- Returns a structured `AssignmentScore` object
76	
77	---
78	
79	## Assignment Algorithm — The Problem
80	
81	This is a **Quadratic Assignment Problem (QAP)**:
82	
83	> Minimise `∑_{i≠j} weight(i,j) × distance(desk(i), desk(j))`
84	
85	Where:
86	- `weight(i,j)` = relationship strength between employees i and j (see below)
87	- `distance` = Euclidean distance between assigned desk coordinates
88	
89	### Relationship Weight Function
90	
91	```
92	weight(i, j) = team_weight(i, j)
93	             + social_bonus(i, j)
94	             + window_adjustment(i, j)
95	             + lucky_affinity(i, j)
96	```
97	
98	- **team_weight**: `1 / tree_distance(i, j)` — siblings = 1.0, cousins = 0.5, etc.
99	- **social_bonus**: both `talkToMe` → positive bonus
100	- **social_penalty**: either `dontTalkToMe` → negative weight (push apart)
101	- **lucky_affinity**: for the "feeling lucky" employee, strong positive weight toward the highest-ranked person present
102	
103	---
104	
105	## Scoring Heuristics (Score Dashboard)
106	
107	| Metric | Definition |
108	|---|---|
109	| **Total QAP Cost** | Raw `∑ weight × distance`, normalised 0–100 (lower = better) |
110	| **Team Cohesion** | Avg Euclidean distance between each person and their nearest teammate |
111	| **Manager Proximity** | Avg distance between each employee and their direct manager (if both present) |
112	| **Social Satisfaction** | % of talk/don't-talk preferences correctly honoured |
113	| **Window Hit Rate** | % of window-preference users assigned a perimeter desk |
114	
115	Show scores for: Random baseline vs. current algorithm — so the improvement is visible.
116	
117	---
118	
119	## REST API (Jersey / JAX-RS)
120	
121	```
122	GET  /desks                    — all 191 desks with coordinates + neighborhood
123	GET  /employees                — all employees from org chart
124	POST /bookings                 — submit a booking with preferences
125	GET  /bookings                 — list today's bookings
126	POST /assignments/run          — trigger assignment algorithm (idempotent, re-runnable)
127	GET  /assignments              — get current desk assignments (employeeId → deskId)
128	GET  /assignments/score        — get scoring metrics for current assignment
129	```
130	
131	TypeScript types generated from Jersey annotations for the React client.
132	
133	---
134	
135	## Frontend (React)
136	
137	### Views
138	
139	#### 1. Booking Page
140	- Employee picker (searchable dropdown from org chart)
141	- Date selector (defaults to today)
142	- Preference toggles: Window, Talk to me / Don't talk to me, I'm Feeling Lucky
143	- Submit button — calls `POST /bookings`
144	- Shows capacity remaining (e.g. "143 / 191 desks available")
145	
146	#### 2. Floor Map View
147	- **React-rendered** from JSON coordinates — no SVG file, no canvas library needed
148	- Desks drawn as positioned `<div>` or `<svg rect>` elements at their x/y positions
149	- Colour-coded by team (derive team colour from org path depth/branch)
150	- "Feeling lucky" winner gets a special highlight (crown icon, gold desk)
151	- Unbooked desks shown in muted/grey
152	- Hover/click: tooltip showing employee name, team, preferences
153	
154	#### 3. Score Dashboard
155	- Live metrics panel: 5 heuristics with progress bars / scores
156	- "Random" vs "Optimised" side-by-side comparison bars
157	- Updates after each `POST /assignments/run`
158	
159	### Demo Flow
160	1. Seed fake bookings (button: "Simulate a full office day") — picks random employees with random preferences
161	2. "Run Assignment" button triggers `POST /assignments/run`
162	3. Floor map animates with desk assignments
163	4. Score dashboard shows results
164	5. Optionally re-run to show variance / algorithm improvement
165	
166	---
167	
168	## What We're Stubbing
169	
170	- Assignment algorithm body — just random for now, interface is defined
171	- No auth / no login — employee is just picked from a dropdown
172	- No multi-day support — just today
173	- No admin role distinction
174	
175	---
176	
177	## File Structure (same repo)
178	
179	```
180	/
181	├── src/main/java/          ← Java backend
182	│   └── com/starlingbank/
183	│       ├── model/          ← Desk, Employee, Booking, Assignment, AssignmentScore
184	│       ├── service/        ← FloorMapService, OrgChartService, BookingService, AssignmentService, ScoringService
185	│       └── api/            ← Jersey resources (REST endpoints)
186	├── frontend/               ← React app
187	│   ├── src/
188	│   │   ├── components/     ← FloorMap, ScoreDashboard, BookingForm
189	│   │   └── api/            ← Generated TypeScript client
190	│   └── package.json
191	├── input-data/             ← floor-map-5th.json, orgchart.json
192	└── PLAN.md
193	```
194	
195	---
196	
197	## Open Questions / To Decide
198	
199	- [ ] ~~What colour scheme for team grouping on the map?~~ → **Derived from org chart**: each top-level branch gets a hue, shaded by depth
200	
201	## TypeScript Generation
202	
203	Uses `cz.habarta.typescript-generator` (Gradle plugin). It reads Jersey resource classes + `com.starlingbank.model.**` and writes `frontend/src/generated/api.ts` (types + a typed JAX-RS client).
204	
205	**To add a new endpoint:**
206	1. Add the resource class to `classes` in `build.gradle.kts`
207	2. Run `./gradlew generateTypeScript` → regenerates `frontend/src/generated/api.ts`
208	3. The `FetchHttpClient` in `frontend/src/api/client.ts` already implements the generated `HttpClient` interface
209	
210	## Decided
211	
212	- Floor map **will have zoom and pan** (coordinate range is ~3000 units, it's necessary)
213	- "I'm feeling lucky" is **stubbed** — define the interface and return a no-op; implement later if time allows
214
```

---

### You _10:53:43_

[Tool result]
```
1	# API
2	
3	Base URL: `http://localhost:8080`. All endpoints produce/consume `application/json`.
4	
5	## Endpoints
6	
7	### Desks
8	| Method | Path | Response | Status |
9	|---|---|---|---|
10	| `GET` | `/api/desks` | `Desk[]` | Stub (returns `[]`) |
11	
12	### Employees
13	| Method | Path | Response | Status |
14	|---|---|---|---|
15	| `GET` | `/api/employees` | `Employee[]` | Stub (returns `[]`) |
16	
17	### Bookings
18	| Method | Path | Body | Response | Status |
19	|---|---|---|---|---|
20	| `POST` | `/api/bookings` | `BookingRequest` | `BookingRequest` | Stub (echoes input) |
21	| `GET` | `/api/bookings` | — | `BookingCollection` | Stub (empty, capacity=191) |
22	
23	### Assignments
24	| Method | Path | Response | Status |
25	|---|---|---|---|
26	| `POST` | `/api/assignments/run` | `AssignmentCollection` | Stub (empty maps) |
27	| `GET` | `/api/assignments` | `AssignmentCollection` | Stub (empty maps) |
28	| `GET` | `/api/assignments/score` | `AssignmentScore` | Stub (hardcoded plausible values) |
29	
30	## TypeScript client
31	
32	The TypeScript client and all type interfaces are generated from the Jersey resource classes. **Do not hand-edit `frontend/src/generated/api.ts`** — it is overwritten on every generation run.
33	
34	To regenerate after adding/changing a Java model or endpoint:
35	```sh
36	./gradlew generateTypeScript
37	```
38	
39	Output: `frontend/src/generated/api.ts`
40	
41	Usage in React:
42	```ts
43	import { DeskResourceApi, FetchHttpClient } from '../generated/api'
44	import { FetchHttpClient } from '../api/client'
45	
46	const client = new DeskResourceApi(new FetchHttpClient())
47	const desks = await client.getDesks()
48	```
49	
50	When adding a new Jersey resource, also add it to `classes` in `build.gradle.kts`:
51	```kotlin
52	tasks.generateTypeScript {
53	    classes = mutableListOf(
54	        "com.starlingbank.api.YourNewResource",
55	        // ...
56	    )
57	}
58	```
59	
60	## CORS
61	
62	All origins are permitted (`Access-Control-Allow-Origin: *`). The Vite dev server at `localhost:5173` can call the backend at `localhost:8080` directly with no proxy needed.
63
```

---

### You _10:53:43_

[Tool result]
```
1	{
2	  "name": "frontend",
3	  "private": true,
4	  "version": "0.0.0",
5	  "type": "module",
6	  "scripts": {
7	    "dev": "vite",
8	    "build": "tsc -b && vite build",
9	    "lint": "eslint .",
10	    "preview": "vite preview"
11	  },
12	  "dependencies": {
13	    "react": "^19.2.6",
14	    "react-dom": "^19.2.6"
15	  },
16	  "devDependencies": {
17	    "@eslint/js": "^10.0.1",
18	    "@types/node": "^24.12.3",
19	    "@types/react": "^19.2.14",
20	    "@types/react-dom": "^19.2.3",
21	    "@vitejs/plugin-react": "^6.0.1",
22	    "eslint": "^10.3.0",
23	    "eslint-plugin-react-hooks": "^7.1.1",
24	    "eslint-plugin-react-refresh": "^0.5.2",
25	    "globals": "^17.6.0",
26	    "typescript": "~6.0.2",
27	    "typescript-eslint": "^8.59.2",
28	    "vite": "^8.0.12"
29	  }
30	}
31
```

---

### You _10:53:43_

[Tool result]
```
1	# Frontend
2	
3	## Stack
4	
5	- React + TypeScript
6	- Vite dev server on `http://localhost:5173`
7	- TypeScript types and API client auto-generated from the Java backend
8	
9	## Running
10	
11	```sh
12	cd frontend
13	npm install
14	npm run dev
15	```
16	
17	## TypeScript generation
18	
19	The file `frontend/src/generated/api.ts` is **gitignored and auto-generated**. Run this from the repo root after any Java model or resource change:
20	
21	```sh
22	./gradlew generateTypeScript
23	```
24	
25	Do not hand-edit `generated/api.ts`. It is overwritten on every run.
26	
27	`frontend/src/api/client.ts` contains `FetchHttpClient`, which implements the `HttpClient` interface from the generated file. This is the only hand-written API glue.
28	
29	Example usage:
30	```ts
31	import { BookingResourceApi } from '../generated/api'
32	import { FetchHttpClient } from '../api/client'
33	
34	const api = new BookingResourceApi(new FetchHttpClient())
35	const collection = await api.getBookings()
36	```
37	
38	## Views
39	
40	Three main views, all in `frontend/src/`:
41	
42	### BookingForm
43	Employee picks their name, sets preferences, submits. Calls `POST /api/bookings`.
44	
45	Preference controls:
46	- Employee dropdown (searchable, sourced from `GET /api/employees`)
47	- Window seat toggle
48	- Social preference: Talk to me / Don't talk to me / None
49	- Feeling lucky toggle (UI present, feature stubbed in backend)
50	- Capacity indicator — shows remaining desks from `BookingCollection.remaining`
51	
52	### FloorMap
53	Renders all 191 desks from JSON coordinates. Shows current assignment.
54	
55	**Key decisions:**
56	- Built in React from the `Desk` x/y data — does not use `floor-plan-5th.svg`
57	- Requires **zoom and pan** — coordinate range is ~3500×4000 units
58	- Desks coloured by org branch (derived from `OrgNode.orgPath[1]` — the second level of the tree = top-level org branch). Each branch gets a hue; lightness varies by depth
59	- Unbooked desks: muted grey
60	- Assigned desks: coloured by employee's org branch
61	- Hover/click: tooltip with employee name, team, preferences
62	- "Feeling lucky" winner: special highlight (gold/crown) when feature is implemented
63	
64	Colour derivation: hash `orgPath[1]` (the top-level branch ID) to a hue in HSL. Use `depth` to adjust lightness within the hue.
65	
66	### ScoreDashboard
67	Shows `AssignmentScore` metrics as progress bars or gauges. Sources from `GET /api/assignments/score`.
68	
69	Metrics to display: Total QAP Cost, Team Cohesion, Manager Proximity, Social Satisfaction, Window Hit Rate.
70
```

---

### Claude _10:54:16_

Good news — you already have a solid React + Vite + TypeScript foundation. Here's the most impactful 6-hour plan:

---

## The Winning Strategy

**Install 2 things right now:**

1. **Tailwind CSS** — utility classes that make everything look polished without writing custom CSS
2. **Ant Design (`antd`)** — a comprehensive component library with a searchable dropdown (employee picker), toggles, progress bars, and cards already built. One install covers every UI widget you need.

```bash
cd frontend
npm install antd @ant-design/icons
npm install -D tailwindcss @tailwindcss/vite
```

---

## What Will Win the Hackathon

The **floor map is your killer feature**. 191 desks rendered as SVG `<rect>` elements, colour-coded by team (hashed org branch → HSL hue), with zoom/pan and hover tooltips — nothing in a competing team's demo will look as impressive. Make this the centrepiece.

**Prioritise your 6 hours like this:**

| Time | Work |
|---|---|
| 0–30 min | Tailwind + Ant Design setup, dark theme, 3-tab layout |
| 30 min–3 hr | **Floor map** — SVG with zoom/pan, team colours, hover tooltip |
| 3–4.5 hr | Booking form (employee picker, preference toggles, capacity counter) |
| 4.5–5.5 hr | Score dashboard (progress bars, Random vs Optimised comparison) |
| 5.5–6 hr | "Simulate full office day" button + "Run Assignment" animation |

---

## Visual Direction

Go **dark theme**: near-black background (`#0f0f1a`), team colours as vivid HSL hues against the dark canvas. This looks dramatically more impressive in a demo room than a light UI.

The floor map with 191 glowing coloured dots that animate into position when you click "Run Assignment" will get a reaction from the room.

---

Want me to get started? I can:
- Set up Tailwind + Ant Design with a dark theme
- Build the floor map SVG component with zoom/pan and team colouring
- Wire up all three views with the API stubs

Just say the word and I'll start building.

---
