# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

A hackathon desk-booking app. Employees declare they're coming in, set preferences, and a single algorithm run assigns everyone to a desk. The backend is Java/Jersey running on port 8080; the frontend is React/Vite on port 5173.

## Commands

### Backend
```bash
# Build and run (from repo root)
./gradlew run

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.starlingbank.service.SimulatedAnnealingAssignmentServiceTest"

# Regenerate TypeScript types from Java models + Jersey resources
./gradlew generateTypeScript
```

### Frontend
```bash
cd frontend
npm install        # first time
npm run dev        # dev server at http://localhost:5173
npm run build      # type-check + production build
npm run lint
```

### sim.py — CLI for bookings and simulation

Requires the backend to be running. Targets `http://localhost:8080` by default; override with `--base-url`.

```bash
# Dry-run: list everyone under a subtree root (no HTTP calls)
python3 sim.py list "Raman Bhatia"
python3 sim.py list "Raman Bhatia" --depth 2

# Book a subtree, run SA, print score
python3 sim.py book "Raman Bhatia"
python3 sim.py book "Engineering" --max 40 --social TALK_TO_ME
python3 sim.py book "Raman Bhatia" --lucky-rate 0.1 --skip-existing

# Smoke test: book all real attendees from input-data/har-attendees-*.json, run SA, print score
python3 sim.py smoke
python3 sim.py smoke --skip-existing

# Print current score without changing anything
python3 sim.py score

# Clear all in-memory bookings
python3 sim.py reset
```

**Flags for `book`:**
| Flag | Default | Description |
|---|---|---|
| `--depth N` | unlimited | Limit subtree depth |
| `--max N` | desk count | Random-sample to N people |
| `--social` | `NONE` | Apply `TALK_TO_ME` or `DONT_TALK_TO_ME` to all |
| `--lucky-rate F` | `0.0` | Fraction of bookings marked feeling lucky |
| `--skip-existing` | off | Skip employees already booked |

### Development setup
Run backend (`./gradlew run`) and frontend (`npm run dev`) simultaneously. The Vite dev server proxies `/hello` to the backend; all other `/api/*` calls go directly to `http://localhost:8080`.

**Note**: The Vite proxy in `frontend/vite.config.ts` currently only proxies `/hello`. If you add frontend API calls that hit `/api/*` paths during local dev, add those paths to the proxy config.

## Architecture

### Data flow
1. **HAR files** (`input-data/*.har`) are parsed at startup by `FloorMapParser` → `HarParser` (extracts SVG and Kadence API JSON) → `SvgParser` (extracts desk x/y coords) + API JSON (provides desk names/neighborhoods). The merged result is a `FloorMap`.
2. **Org chart** (`input-data/orgchart.json`) is loaded by `OrgChartService` into `Employee` + `OrgNode` maps keyed by employee ID.
3. **Bookings** are in-memory only (no DB). `BookingService` holds them for the current session.
4. **Assignments** are computed on demand via `POST /api/assignments/run` by `AssignmentService`.

### Dependency injection
Uses **Guice** for service wiring, bridged into **HK2** (Jersey's built-in DI) in `Main.java`. To inject a new service into a resource class: bind it in `AppModule`, then add `bind(injector.getInstance(...)).to(...)` in the `AbstractBinder` in `Main`.

### Assignment algorithm
`SimulatedAnnealingAssignmentService` solves a **Quadratic Assignment Problem**: minimise `∑ weight(i,j) × distance(desk_i, desk_j)`. It runs 400 parallel SA runs (12 threads) of 200k iterations each and picks the best result. The weight matrix encodes:
- **Team weight**: `1 / treeDistance(i, j)` via LCA on `orgPath`
- **Social bonus/penalty**: both `TALK_TO_ME` or both `DONT_TALK_TO_ME` → attract; mismatched → repel
- **Lucky affinity**: `FEELING_LUCKY` employees get a large positive weight toward the highest-ranked person present

### TypeScript generation
`./gradlew generateTypeScript` reads Jersey resource classes listed in `build.gradle.kts` → writes `frontend/src/generated/api.ts` (types + a typed `ApplicationClient`). To expose a new endpoint to the frontend: add the resource class to `classes` in `build.gradle.kts` and regenerate. The `FetchHttpClient` in `frontend/src/api/client.ts` implements the generated `HttpClient` interface.

### Key files
| File | Role |
|---|---|
| `AppModule.java` | Guice bindings — swap implementations here |
| `Main.java` | Server startup; HK2 bridge for Guice services |
| `FloorMapServiceImpl.java` | Loads floor map from `input-data/kadence-london.har` at startup |
| `SimulatedAnnealingAssignmentService.java` | Full SA implementation |
| `AssignmentResource.java` | Currently stubbed — `run()` returns empty maps |
| `frontend/src/api/client.ts` | `FetchHttpClient` wraps the generated typed client |
| `frontend/vite.config.ts` | Proxy config for local dev |

### Frontend views
- `BookingForm` — employee picker + preference toggles → `POST /api/bookings`
- `MapView` / `FloorMap` — renders desks as positioned elements from JSON coordinates (not the SVG file); floor coord range is ~3000 units so zoom/pan is required
- `ScoreDashboard` — 5 QAP metrics, random baseline vs. optimised side-by-side

### Persistence
No database. Everything is in-memory for the demo session. The floor map and org chart are loaded from `input-data/` at startup; bookings and assignments live only in memory.
