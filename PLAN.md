# Desk Booking Hackathon — Project Plan

## Concept

On-demand desk assignment for a hybrid office. Employees declare they're coming in and set preferences; a single algorithm run assigns everyone to a desk for the day. The UI shows a live floor map and a scoring dashboard measuring assignment quality.

**Office**: 5th floor, 191 desks across 15 neighborhoods.
**Capacity**: first-come-first-served up to 191 bookings; after that, no more.

---

## Tech Stack

- **Backend**: Java, Jersey (JAX-RS), Guice, TypeScript generation from annotations
- **Frontend**: React, renders floor map from JSON coordinates (not the SVG file)
- **Persistence**: JSON file on disk for demo purposes; no real DB

---

## Data Inputs

### Floor Map (`floor-map-5th.json`)
Each desk has: `id`, `name`, `neighborhood`, `x`, `y`, `rotation`
- Window desks = desks at the perimeter (near min/max x or y bounds of the floor plan). Office is described as a square with windows all around, so perimeter detection uses coordinate bounds.
- 15 neighborhoods used for visual grouping on the map.

### Org Chart (`orgchart.json`)
Each employee has: `id`, `name`, `role`, `location`, `org`, `depth`, `orgPath`, `parent`, `children`
- Tree rooted at the CEO.
- Team relationship weight between two people = `1 / (tree_distance between them)`, where `tree_distance` is the number of hops through the LCA (lowest common ancestor).
- Direct siblings (same manager) = highest weight; same dept but further up = lower weight.

---

## User Preferences (per booking)

| Preference | Description |
|---|---|
| **Team auto-clustering** | Always on. Org chart used to compute relationship weights. |
| **Window seat** | Prefer desks near the building perimeter. |
| **Talk to me** | Prefer to be seated near other sociable people. |
| **Don't talk to me** | Prefer isolation; seat away from high-traffic / social clusters. |
| **I'm feeling lucky** | Seat the user next to the highest-ranking person in the office that day. |

---

## System Components

### Backend Services

#### `FloorMapService`
- Loads desks from `floor-map-5th.json` at startup
- Provides: `List<Desk> getDesks()`, `double distance(deskA, deskB)` (Euclidean)
- Identifies perimeter desks (window seats) using coordinate bounding box

#### `OrgChartService`
- Loads `orgchart.json` at startup
- Provides: `double relationshipWeight(employeeA, employeeB)` using tree distance via LCA
- Provides: `Employee getHighestRanking(List<Employee> present)` for "feeling lucky"

#### `BookingService`
- Manages bookings for the current day (in-memory + JSON file dump)
- `POST /bookings` — employee books in with preferences
- `GET /bookings` — list all bookings for today
- Enforces 191-person capacity cap

#### `AssignmentService`
- Single method: `Map<EmployeeId, DeskId> assign(List<Booking> bookings, List<Desk> desks)`
- **Stub v0**: random shuffle (initial implementation)
- **V1**: Random (baseline, for scoring comparison)
- **V2** (stretch): Local search / simulated annealing on the QAP objective

#### `ScoringService`
- Computes all heuristics against a completed assignment
- Returns a structured `AssignmentScore` object

---

## Assignment Algorithm — The Problem

This is a **Quadratic Assignment Problem (QAP)**:

> Minimise `∑_{i≠j} weight(i,j) × distance(desk(i), desk(j))`

Where:
- `weight(i,j)` = relationship strength between employees i and j (see below)
- `distance` = Euclidean distance between assigned desk coordinates

### Relationship Weight Function

```
weight(i, j) = team_weight(i, j)
             + social_bonus(i, j)
             + window_adjustment(i, j)
             + lucky_affinity(i, j)
```

- **team_weight**: `1 / tree_distance(i, j)` — siblings = 1.0, cousins = 0.5, etc.
- **social_bonus**: both `talkToMe` → positive bonus
- **social_penalty**: either `dontTalkToMe` → negative weight (push apart)
- **lucky_affinity**: for the "feeling lucky" employee, strong positive weight toward the highest-ranked person present

---

## Scoring Heuristics (Score Dashboard)

| Metric | Definition |
|---|---|
| **Total QAP Cost** | Raw `∑ weight × distance`, normalised 0–100 (lower = better) |
| **Team Cohesion** | Avg Euclidean distance between each person and their nearest teammate |
| **Manager Proximity** | Avg distance between each employee and their direct manager (if both present) |
| **Social Satisfaction** | % of talk/don't-talk preferences correctly honoured |
| **Window Hit Rate** | % of window-preference users assigned a perimeter desk |

Show scores for: Random baseline vs. current algorithm — so the improvement is visible.

---

## REST API (Jersey / JAX-RS)

```
GET  /desks                    — all 191 desks with coordinates + neighborhood
GET  /employees                — all employees from org chart
POST /bookings                 — submit a booking with preferences
GET  /bookings                 — list today's bookings
POST /assignments/run          — trigger assignment algorithm (idempotent, re-runnable)
GET  /assignments              — get current desk assignments (employeeId → deskId)
GET  /assignments/score        — get scoring metrics for current assignment
```

TypeScript types generated from Jersey annotations for the React client.

---

## Frontend (React)

### Views

#### 1. Booking Page
- Employee picker (searchable dropdown from org chart)
- Date selector (defaults to today)
- Preference toggles: Window, Talk to me / Don't talk to me, I'm Feeling Lucky
- Submit button — calls `POST /bookings`
- Shows capacity remaining (e.g. "143 / 191 desks available")

#### 2. Floor Map View
- **React-rendered** from JSON coordinates — no SVG file, no canvas library needed
- Desks drawn as positioned `<div>` or `<svg rect>` elements at their x/y positions
- Colour-coded by team (derive team colour from org path depth/branch)
- "Feeling lucky" winner gets a special highlight (crown icon, gold desk)
- Unbooked desks shown in muted/grey
- Hover/click: tooltip showing employee name, team, preferences

#### 3. Score Dashboard
- Live metrics panel: 5 heuristics with progress bars / scores
- "Random" vs "Optimised" side-by-side comparison bars
- Updates after each `POST /assignments/run`

### Demo Flow
1. Seed fake bookings (button: "Simulate a full office day") — picks random employees with random preferences
2. "Run Assignment" button triggers `POST /assignments/run`
3. Floor map animates with desk assignments
4. Score dashboard shows results
5. Optionally re-run to show variance / algorithm improvement

---

## What We're Stubbing

- Assignment algorithm body — just random for now, interface is defined
- No auth / no login — employee is just picked from a dropdown
- No multi-day support — just today
- No admin role distinction

---

## File Structure (same repo)

```
/
├── src/main/java/          ← Java backend
│   └── com/starlingbank/
│       ├── model/          ← Desk, Employee, Booking, Assignment, AssignmentScore
│       ├── service/        ← FloorMapService, OrgChartService, BookingService, AssignmentService, ScoringService
│       └── api/            ← Jersey resources (REST endpoints)
├── frontend/               ← React app
│   ├── src/
│   │   ├── components/     ← FloorMap, ScoreDashboard, BookingForm
│   │   └── api/            ← Generated TypeScript client
│   └── package.json
├── input-data/             ← floor-map-5th.json, orgchart.json
└── PLAN.md
```

---

---

## Current Checkpoint (2026-06-08)

### What's done
- **SA algorithm**: `SimulatedAnnealingAssignmentService` fully implemented — 400 parallel SA runs, 200k iterations each, picks best QAP result. Bound in Guice (`AppModule`).
- **Floor map pipeline**: `FloorMapParser` → `HarParser` → `SvgParser` fully wired; `FloorMapServiceImpl` loads from HAR at startup. Bound in Guice + HK2.
- **Models**: All model classes exist (`Desk`, `Employee`, `OrgNode`, `BookingRequest`, `AssignmentCollection`, `AssignmentScore`, etc.).
- **Service interfaces**: `AssignmentService`, `BookingService`, `OrgChartService`, `ScoringService` — all defined.
- **Frontend views**: `BookingForm`, `MapView`/`FloorMap`, `ScoreDashboard` all exist and call the API.

### What's missing / still stubbed
- `OrgChartService` — **no implementation** (SA depends on it via `@Inject`, so SA can't run)
- `BookingService` — **no implementation** (in-memory store needed)
- `ScoringService` — **no implementation**
- `AssignmentResource` — **stubbed**: returns `Map.of()`, doesn't inject any services
- `BookingResource` — **stubbed**: echoes POST back, `GET /bookings` returns hardcoded `List.of()`
- `Main.java` HK2 bridge — only exposes `HelloService` and `FloorMapService` to Jersey; `AssignmentService`, `BookingService`, `OrgChartService` are missing

### Next steps (in order)

1. **Implement `OrgChartService`** — load `input-data/orgchart.json`, build employee + OrgNode maps, expose `getEmployees()` / `getOrgNodes()`
2. **Implement `BookingService`** — in-memory `List<BookingRequest>`, enforce 191-cap, implement `addBooking` / `getBookings`
3. **Implement `ScoringService`** — compute the 5 scoring metrics against a completed assignment
4. **Bind new impls in `AppModule`** — `OrgChartService`, `BookingService`, `ScoringService`
5. **Wire `BookingResource`** — inject `BookingService`, delegate to it
6. **Wire `AssignmentResource`** — inject `AssignmentService` + `BookingService` + `FloorMapService`; `POST /run` calls `assign(bookings, desks)`; `GET /score` calls `ScoringService`
7. **Update `Main.java` HK2 bridge** — add `AssignmentService`, `BookingService`, `OrgChartService`, `ScoringService` so Jersey can inject them
8. **Smoke test end-to-end** — seed bookings, hit `POST /api/assignments/run`, verify floor map and score dashboard light up

---

## Open Questions / To Decide

- [ ] ~~What colour scheme for team grouping on the map?~~ → **Derived from org chart**: each top-level branch gets a hue, shaded by depth

## TypeScript Generation

Uses `cz.habarta.typescript-generator` (Gradle plugin). It reads Jersey resource classes + `com.starlingbank.model.**` and writes `frontend/src/generated/api.ts` (types + a typed JAX-RS client).

**To add a new endpoint:**
1. Add the resource class to `classes` in `build.gradle.kts`
2. Run `./gradlew generateTypeScript` → regenerates `frontend/src/generated/api.ts`
3. The `FetchHttpClient` in `frontend/src/api/client.ts` already implements the generated `HttpClient` interface

## Decided

- Floor map **will have zoom and pan** (coordinate range is ~3000 units, it's necessary)
- "I'm feeling lucky" is **stubbed** — define the interface and return a no-op; implement later if time allows
