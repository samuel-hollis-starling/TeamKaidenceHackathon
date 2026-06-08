# Architecture

## Overview

Desk booking system for a hybrid office. Employees declare they're coming in with preferences; a single algorithm run assigns everyone to a desk for the day.

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Java 17, Jersey 3.1.5 (JAX-RS), Guice 7.0.0, Grizzly embedded HTTP |
| Frontend | React, Vite, TypeScript |
| Persistence | In-memory + JSON file dump (MVP, no DB) |
| API contract | TypeScript types generated from Jersey resources via `cz.habarta.typescript-generator` |

## Dev split

- **Dev 1** — Frontend: floor map renderer, booking form, score dashboard
- **Dev 2** — Backend core: services, data loading, REST resources, Guice wiring
- **Dev 3** — Algorithm & scoring: `AssignmentService`, `ScoringService`, relationship weight function

## Data flow

```
input-data/floor-map-5th.json  ──► FloorMapService ──► DeskResource      GET /api/desks
input-data/orgchart.json       ──► OrgChartService ──► EmployeeResource  GET /api/employees
                                                     ──► AssignmentResource
User submits booking ──────────► BookingService ────► BookingResource    POST /api/bookings
"Run assignment" ──────────────► AssignmentService ──► AssignmentResource POST /api/assignments/run
                                 ScoringService ─────► AssignmentResource GET /api/assignments/score

REST API ──► ./gradlew generateTypeScript ──► frontend/src/generated/api.ts ──► React app
```

## Ports

- Backend: `http://localhost:8080`
- Frontend dev server: `http://localhost:5173`
- CORS is open (`*`) so the Vite dev server can call the backend directly

## Key constraint

MVP is single-day only. No date routing, no multi-day state. Capacity is fixed at 191 (number of desks on the 5th floor).
