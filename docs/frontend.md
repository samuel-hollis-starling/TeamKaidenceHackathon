# Frontend

## Stack

- React + TypeScript
- Vite dev server on `http://localhost:5173`
- TypeScript types and API client auto-generated from the Java backend

## Running

```sh
cd frontend
npm install
npm run dev
```

## TypeScript generation

The file `frontend/src/generated/api.ts` is **gitignored and auto-generated**. Run this from the repo root after any Java model or resource change:

```sh
./gradlew generateTypeScript
```

Do not hand-edit `generated/api.ts`. It is overwritten on every run.

`frontend/src/api/client.ts` contains `FetchHttpClient`, which implements the `HttpClient` interface from the generated file. This is the only hand-written API glue.

Example usage:
```ts
import { BookingResourceApi } from '../generated/api'
import { FetchHttpClient } from '../api/client'

const api = new BookingResourceApi(new FetchHttpClient())
const collection = await api.getBookings()
```

## Views

Three main views, all in `frontend/src/`:

### BookingForm
Employee picks their name, sets preferences, submits. Calls `POST /api/bookings`.

Preference controls:
- Employee dropdown (searchable, sourced from `GET /api/employees`)
- Social preference: Talk to me / Don't talk to me / None
- Feeling lucky toggle (UI present, feature stubbed in backend)
- Capacity indicator — shows remaining desks from `BookingCollection.remaining`

### FloorMap
Renders all 191 desks from JSON coordinates. Shows current assignment.

**Key decisions:**
- Built in React from the `Desk` x/y data — does not use `floor-plan-5th.svg`
- Requires **zoom and pan** — coordinate range is ~3500×4000 units
- Desks coloured by org branch (derived from `OrgNode.orgPath[1]` — the second level of the tree = top-level org branch). Each branch gets a hue; lightness varies by depth
- Unbooked desks: muted grey
- Assigned desks: coloured by employee's org branch
- Hover/click: tooltip with employee name, team, preferences
- "Feeling lucky" winner: special highlight (gold/crown) when feature is implemented

Colour derivation: hash `orgPath[1]` (the top-level branch ID) to a hue in HSL. Use `depth` to adjust lightness within the hue.

### ScoreDashboard
Shows `AssignmentScore` metrics as progress bars or gauges. Sources from `GET /api/assignments/score`.

Metrics to display: Total QAP Cost, Team Cohesion, Manager Proximity, Social Satisfaction, Window Hit Rate.
