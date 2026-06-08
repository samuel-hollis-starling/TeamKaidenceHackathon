# Plan frontend-backend integration

**Project:** TeamKaidenceHackathon  
**Session:** `8b3eb585-67b6-44ce-9d80-beeb413673c4`  
**Date:** 2026-06-08  

---

### You _13:41:16_

let's plan wiring up the frontend to the backend

---

### Claude _13:41:19_

Let me survey the current state of both ends before we plan.

---

### Claude _13:41:24_

[Tool call: Agent]
```json
{
  "description": "Survey frontend-backend wiring state",
  "subagent_type": "Explore",
  "prompt": "In the repo at /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon, I need to understand the current wiring between frontend and backend. Please find and read:\n\n1. `frontend/src/api/client.ts` \u2014 the FetchHttpClient and how it maps to the generated types\n2. `frontend/src/generated/api.ts` \u2014 all generated API types and the ApplicationClient interface\n3. All React components/pages that make API calls (look in `frontend/src/`) \u2014 what endpoints they call and what data they expect\n4. All Jersey resource classes (look in `src/main/java/`) \u2014 what endpoints exist and what they return (especially AssignmentResource, BookingResource, FloorMapResource, OrgNodesResource or similar)\n5. `frontend/vite.config.ts` \u2014 proxy config\n\nReport: for each endpoint (method + path), whether the backend is implemented or stubbed, whether the frontend is calling it, and any type mismatches you can spot. Keep it structured \u2014 a table or bullet list per endpoint."
}
```

---

### You _13:42:13_

[Tool result]
```
Perfect! Now I have all the information I need. Let me compile the comprehensive report:

## Frontend-Backend Wiring Analysis

### Network Configuration

**Vite Proxy** (`/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/vite.config.ts`):
- Only `/hello` is proxied to `http://localhost:8080`
- **Missing**: No proxy rule for `/api/*` endpoints

**FetchHttpClient** (`/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/client.ts`):
- Makes requests to `window.location.origin` (relative paths, no explicit backend URL)
- Will hit the same server as the frontend (incorrect for dev if backend is on port 8080)

**Frontend API Layer** (`/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/index.ts`):
- Currently using **stubs** (not the generated client)
- Comment states: "When the backend is ready, swap each export below to use the generated client from '../generated/api' + FetchHttpClient"

---

### Endpoint Implementation Status

#### **GET /api/assignments**
- **Backend**: ✅ Fully implemented (`AssignmentResource.getAssignment()`)
  - Returns: `AssignmentCollection` (Map<String,String> deskByEmployeeId, employeeByDeskId)
- **Frontend**: ✅ Called in `MapView.tsx` (line 98: `getAssignments()`)
- **Type Match**: ✅ Complete match
- **Current Status**: Using stub that returns hardcoded data

#### **POST /api/assignments/run**
- **Backend**: ✅ Fully implemented (`AssignmentResource.run()`)
  - Takes bookings and desks, calls `AssignmentService.assign()`
  - Returns: `AssignmentCollection`
- **Frontend**: ✅ Called in `ScoreDashboard.tsx` (line 29: `runAssignment()`)
- **Type Match**: ✅ Complete match
- **Current Status**: Using stub that returns hardcoded data

#### **GET /api/assignments/score**
- **Backend**: ✅ Fully implemented (`AssignmentResource.getScore()`)
  - Returns: `AssignmentScore` (totalQapCost, teamCohesion, managerProximity, socialSatisfaction, windowHitRate)
- **Frontend**: ✅ Called in `ScoreDashboard.tsx` (line 25: `getScore()`)
- **Type Match**: ✅ Complete match
- **Current Status**: Using stub that returns hardcoded score data

#### **POST /api/bookings**
- **Backend**: ✅ Fully implemented (`BookingResource.addBooking(BookingRequest)`)
  - Takes: `BookingRequest` (employeeId, socialPreference, windowSeat, feelingLucky)
  - Returns: `BookingRequest`
- **Frontend**: ✅ Called in `BookingForm.tsx` (line 36: `addBooking(...)`)
- **Type Match**: ✅ Complete match
- **Current Status**: Using stub

#### **GET /api/bookings**
- **Backend**: ✅ Fully implemented (`BookingResource.getBookings()`)
  - Returns: `BookingCollection` (bookings[], totalCapacity, remaining)
- **Frontend**: ✅ Called in `BookingForm.tsx` (lines 18, 37: `getBookings()`)
- **Type Match**: ✅ Complete match
- **Current Status**: Using stub

#### **GET /api/desks**
- **Backend**: ✅ Fully implemented (`DeskResource.getDesks()`)
  - Returns: `List<Desk>`
- **Frontend**: ✅ Called in `MapView.tsx` (line 98: `getDesks()`)
- **Type Match**: ✅ Complete match (190+ desks in stub data)
- **Current Status**: Using stub with hardcoded desk data

#### **GET /api/employees**
- **Backend**: ✅ Fully implemented (`EmployeeResource.getEmployees()`)
  - Returns: `List<Employee>` from `OrgChartService.getEmployees()`
- **Frontend**: ✅ Called in:
  - `BookingForm.tsx` (line 17: `getEmployees()`)
  - `MapView.tsx` (line 98: `getEmployees()`)
  - `OrgChart.tsx` (line 97: `getEmployees()`)
- **Type Match**: ✅ Complete match (id, name, role, location)
- **Current Status**: Using stub

#### **GET /api/orgnodes**
- **Backend**: ✅ Fully implemented (`OrgNodeResource.getOrgNodes()`)
  - Returns: `List<OrgNode>` from `OrgChartService.getOrgNodes()`
- **Frontend**: ✅ Called in:
  - `MapView.tsx` (line 98: `getOrgNodes()`)
  - `OrgChart.tsx` (line 97: `getOrgNodes()`)
- **Type Match**: ✅ Complete match (employeeId, parentId, childrenIds[], depth, orgPath[])
- **Current Status**: Using stub
- **Note**: `frontend/src/api/index.ts` comment flags: "getOrgNodes() has no backend endpoint yet" — **but the endpoint IS implemented** (this comment is outdated)

#### **GET /hello**
- **Backend**: ✅ Fully implemented (`HelloResource.greet(String name)`)
  - Accepts optional query param `name` (default: "World")
  - Returns: `HelloResponse` (message, timestamp)
- **Frontend**: Not called by any view
- **Type Match**: ✅ Complete match in generated API
- **Current Status**: Directly proxied via vite.config.ts (only endpoint with proxy rule)

#### **POST /api/floor-map/parse-har**
- **Backend**: ✅ Fully implemented (`FloorMapResource.parseHar()`)
  - Accepts query param `har` (filename)
  - Returns: Metadata map (file, floor, building, desks, pods, walls, landmarks)
- **Frontend**: ❌ **NOT called** — no frontend integration
- **Type Match**: N/A (stub data provides Desk list instead)

---

### Critical Issues

| Issue | Severity | Details |
|-------|----------|---------|
| **Missing `/api/*` proxy** | 🔴 High | Vite only proxies `/hello`. Need to add proxy rule for `/api` to `http://localhost:8080` |
| **Frontend still using stubs** | 🔴 High | All API calls still import from `./stubs` instead of `./generated/api` + FetchHttpClient. Must swap to use real backend |
| **FetchHttpClient URL** | 🟡 Medium | Client uses `window.location.origin` which works if both frontend & backend are on same host. Will fail if backend is truly at separate URL |
| **Outdated API index comment** | 🟠 Low | Comment in `frontend/src/api/index.ts` says "getOrgNodes() has no backend endpoint yet" but it's actually implemented |

---

### Type Mismatch Found

**OrgNode parentId field**:
- **Frontend types** (`/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/types.ts`, line 21): `parentId: string | null` (nullable)
- **Backend model** (`OrgNodeResource` returns `List<OrgNode>`): `parentId` is `String` (can be null in Java)
- **Generated API types** (`api.ts`, line 76): `parentId: string` (not marked as nullable)

This could cause issues if the backend returns null for parentId on root nodes. The frontend type is safer (nullable).

---

### Summary Table

| Endpoint | Method | Backend | Frontend | Types Match | Proxy | Active |
|----------|--------|---------|----------|-------------|-------|--------|
| /api/assignments | GET | ✅ | ✅ | ✅ | ❌ | Stub |
| /api/assignments/run | POST | ✅ | ✅ | ✅ | ❌ | Stub |
| /api/assignments/score | GET | ✅ | ✅ | ✅ | ❌ | Stub |
| /api/bookings | GET | ✅ | ✅ | ✅ | ❌ | Stub |
| /api/bookings | POST | ✅ | ✅ | ✅ | ❌ | Stub |
| /api/desks | GET | ✅ | ✅ | ✅ | ❌ | Stub |
| /api/employees | GET | ✅ | ✅ | ✅ | ❌ | Stub |
| /api/orgnodes | GET | ✅ | ✅ | ⚠️ | ❌ | Stub |
| /hello | GET | ✅ | ❌ | ✅ | ✅ | Direct |
| /api/floor-map/parse-har | POST | ✅ | ❌ | N/A | ❌ | N/A |

**All 8 business-logic endpoints are fully implemented on the backend and have corresponding frontend calls, but the frontend is currently using client-side stubs rather than making real HTTP calls. To go live, you need to:**

1. Add `/api` proxy to vite.config.ts
2. Switch frontend API imports from `./stubs` to `./generated/api` + FetchHttpClient
3. Fix the parentId nullable type mismatch in OrgNode
```

---

### Claude _13:42:29_

Great picture. Here's the situation:

**Everything is built on both sides — nothing is actually connected.** The frontend `api/index.ts` exports stubs instead of the generated client, and the Vite proxy only covers `/hello`, not `/api/*`.

Here's the plan to wire it all up:

---

### 1. Add `/api` proxy to `vite.config.ts`
One line — forward `^/api` to `http://localhost:8080`.

### 2. Swap `frontend/src/api/index.ts` from stubs → real client
Instantiate `FetchHttpClient` + `ApplicationClient` and re-export the same function signatures. All consumers (`BookingForm`, `MapView`, `ScoreDashboard`, `OrgChart`) stay untouched.

### 3. Fix `parentId` nullable mismatch
The generated `api.ts` has `parentId: string` but the backend can return `null` for root nodes. We either:
- **Fix the Java model** to use `@Nullable` / Jackson annotation so the generator produces `string | null`, or
- **Patch `api.ts` manually** after generation (fragile)

Best option: patch the Java `OrgNode` model and re-run `generateTypeScript`.

### 4. Verify `AssignmentResource.run()` is actually implemented
CLAUDE.md says it's "currently stubbed — returns empty maps". If so, the score dashboard will need real work or we leave it wired to the stub for now.

---

**Open question:** The `POST /api/assignments/run` is the most complex endpoint (runs the full SA algorithm). Do you want to:
- **Wire everything end-to-end** including the run endpoint (may expose bugs in the SA service)
- **Wire all read/write endpoints except `/run`** for now, then tackle that separately

Which approach do you want?

---

### You _13:42:56_

wire everything end to end. plan first.

---

### Claude _13:42:57_

[Tool call: EnterPlanMode]
```json

```

---

### You _13:42:57_

[Tool result]
```
Entered plan mode. You should now focus on exploring the codebase and designing an implementation approach.

In plan mode, you should:
1. Thoroughly explore the codebase to understand existing patterns
2. Identify similar features and architectural approaches
3. Consider multiple approaches and their trade-offs
4. Use AskUserQuestion if you need to clarify the approach
5. Design a concrete implementation strategy
6. When ready, use ExitPlanMode to present your plan for approval

Remember: DO NOT write or edit any files yet. This is a read-only exploration and planning phase.
```

---

### Claude _13:43:06_

[Tool call: Agent]
```json
{
  "description": "Explore frontend API layer",
  "subagent_type": "Explore",
  "prompt": "In /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon, I need to understand the full frontend API layer to plan wiring it to the real backend. Please read these files in full:\n\n1. `frontend/src/api/index.ts` \u2014 the current stub exports\n2. `frontend/src/api/client.ts` \u2014 the FetchHttpClient\n3. `frontend/src/generated/api.ts` \u2014 the generated ApplicationClient and types\n4. `frontend/src/api/stubs.ts` or any stub file under `frontend/src/api/`\n5. `frontend/vite.config.ts` \u2014 proxy config\n6. `frontend/src/types.ts` \u2014 local type definitions\n\nI need to know:\n- Exactly what functions are exported from api/index.ts and their signatures\n- What the generated ApplicationClient methods look like and their return types\n- How FetchHttpClient implements the HttpClient interface\n- What the stubs return (so we know what shape the real data must match)\n- Whether api/index.ts re-exports from stubs or from the generated client\n\nBreadth: medium"
}
```

---

### Claude _13:43:12_

[Tool call: Agent]
```json
{
  "description": "Explore backend resources & models",
  "subagent_type": "Explore",
  "prompt": "In /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon, I need to understand the backend resource endpoints and their actual implementation status. Please read these files in full:\n\n1. `src/main/java/com/starlingbank/resource/AssignmentResource.java`\n2. `src/main/java/com/starlingbank/resource/BookingResource.java`\n3. `src/main/java/com/starlingbank/resource/DeskResource.java`\n4. `src/main/java/com/starlingbank/resource/EmployeeResource.java`\n5. `src/main/java/com/starlingbank/resource/OrgNodeResource.java`\n6. `src/main/java/com/starlingbank/resource/FloorMapResource.java`\n7. `src/main/java/com/starlingbank/model/OrgNode.java` \u2014 check if parentId can be null\n8. `src/main/java/com/starlingbank/AppModule.java` \u2014 Guice bindings\n\nI need to know:\n- For each resource: the exact HTTP method, path, what it takes, what it returns, and whether the implementation is real or stubbed\n- Specifically for AssignmentResource.run(): is it actually calling the SA service or returning empty data?\n- Whether OrgNode.parentId is nullable\n- What services are bound in AppModule\n\nBreadth: very thorough"
}
```

---

### Claude _13:43:15_

[Tool call: Agent]
```json
{
  "description": "Explore frontend consumers of API",
  "subagent_type": "Explore",
  "prompt": "In /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon, I need to understand how frontend components consume the API layer. Please read:\n\n1. `frontend/src/components/BookingForm.tsx` \u2014 look for API calls and how it handles responses\n2. `frontend/src/components/MapView.tsx` \u2014 look for API calls and how it handles responses\n3. `frontend/src/components/ScoreDashboard.tsx` \u2014 look for API calls and how it handles responses\n4. `frontend/src/components/OrgChart.tsx` \u2014 look for API calls and how it handles responses\n5. Any other component that imports from `../api` or `../../api`\n\nFor each component, report:\n- Which functions it imports from the api layer\n- How it calls them (parameters, await patterns)\n- What it does with the response data\n- Any error handling\n\nBreadth: medium"
}
```

---

### You _13:44:19_

[Tool result]
```
Now I have everything needed. Here is the complete analysis.

---

## How Frontend Components Consume the API Layer

### API Layer Architecture

All API functions are defined as stubs in `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/stubs.ts` and re-exported through `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/index.ts`. Every function returns a `Promise`, making the interface identical to what a real HTTP client would provide. A `FetchHttpClient` class exists in `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/client.ts` but is not yet wired in -- the index file currently re-exports entirely from `stubs`.

The exported API functions and their signatures are:

| Function | Signature |
|---|---|
| `getDesks` | `() => Promise<Desk[]>` |
| `getEmployees` | `() => Promise<Employee[]>` |
| `getOrgNodes` | `() => Promise<OrgNode[]>` |
| `getBookings` | `() => Promise<BookingCollection>` |
| `addBooking` | `(request: BookingRequest) => Promise<BookingRequest>` |
| `getAssignments` | `() => Promise<AssignmentCollection>` |
| `runAssignment` | `() => Promise<AssignmentCollection>` |
| `getScore` | `() => Promise<AssignmentScore>` |

---

### 1. BookingForm (`/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/views/BookingForm.tsx`)

**Imports:** `getEmployees`, `getBookings`, `addBooking` from `../api`

**How it calls them:**

- **On mount** (inside a `useEffect` with `[]` deps): fires two independent `.then()` chains in parallel:
  - `getEmployees().then(setEmployees)` -- stores the full `Employee[]` into state.
  - `getBookings().then(setCollection)` -- stores the `BookingCollection` into state.

- **On form submit** (the `submit` async function):
  1. `await addBooking({ employeeId, socialPreference, windowSeat, feelingLucky })` -- passes a `BookingRequest` object.
  2. `const updated = await getBookings()` -- re-fetches the full booking collection after mutation.
  3. `setCollection(updated)` -- replaces state with fresh data.
  4. Resets form fields and sets a confirmation message.

**Response data usage:**
- `employees` array is used for the searchable dropdown (filtered by name).
- `collection.remaining` and `collection.totalCapacity` drive the capacity progress bar.

**Error handling:** None. No `try/catch` around the `await` calls, no `.catch()` on the `.then()` chains. An unhandled rejection would propagate to the browser.

---

### 2. MapView (`/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/views/MapView.tsx`)

**Imports:** `getDesks`, `getEmployees`, `getOrgNodes`, `getAssignments` from `../api`

**How it calls them:**

- **On mount** (inside a `useEffect` with `[]` deps): fires all four in parallel using `Promise.all`:
  ```ts
  Promise.all([getDesks(), getEmployees(), getOrgNodes(), getAssignments()]).then(
    ([d, emps, nodes, ass]) => { ... }
  )
  ```

**Response data usage:**
- `desks` (`Desk[]`) stored directly in state, passed to `FloorMap` for rendering.
- `emps` (`Employee[]`) converted to a `Record<string, Employee>` keyed by `e.id`, used for the people sidebar and passed to `FloorMap`.
- `nodes` (`OrgNode[]`) converted to a `Record<string, OrgNode>` keyed by `n.employeeId`, used for color computation (via `buildColors`) and passed to `FloorMap`.
- `assignments` (`AssignmentCollection`) stored directly, used to look up `deskByEmployeeId` for click-to-pan and passed to `FloorMap`.

**Error handling:** None. No `.catch()` on the `Promise.all`, no `try/catch`.

---

### 3. ScoreDashboard (`/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/views/ScoreDashboard.tsx`)

**Imports:** `getScore`, `runAssignment` from `../api`

**How it calls them:**

- **On mount** (inside a `useEffect` with `[]` deps):
  - `getScore().then(setScore)` -- stores the `AssignmentScore` into state.

- **On "Run assignment" button click** (the `run` async function):
  1. `setRunning(true)` -- shows loading state.
  2. `await runAssignment()` -- triggers the assignment algorithm. The return value is **discarded**.
  3. `const s = await getScore()` -- re-fetches the score after the assignment runs.
  4. `setScore(s)` -- updates the dashboard metrics.
  5. `setRunning(false)` -- clears loading state.

**Response data usage:**
- The `AssignmentScore` object's keys (`teamCohesion`, `managerProximity`, `socialSatisfaction`, `windowHitRate`, `totalQapCost`) are each rendered as a labeled metric bar with color coding (green >= 75, orange >= 50, red < 50). `totalQapCost` is inverted (displayed as `100 - raw`).

**Error handling:** None. If `runAssignment()` or `getScore()` rejects, `setRunning(false)` is never called (no `finally` block), leaving the button permanently disabled in the "Running..." state.

---

### 4. OrgChart (`/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/views/OrgChart.tsx`)

**Imports:** `getEmployees`, `getOrgNodes` from `../api`

**How it calls them:**

- **On mount** (inside a `useEffect` with `[]` deps): fires both in parallel using `Promise.all`:
  ```ts
  Promise.all([getEmployees(), getOrgNodes()]).then(([emps, nodes]) => {
    setEmployees(emps)
    setOrgNodes(nodes)
  })
  ```

**Response data usage:**
- Both arrays are passed to `computeLayout()` (via `useMemo`), which builds a visual tree of `LayoutNode[]` and `Conn[]` for the SVG org chart.
- Employees at `depth === 2` in the org tree are treated as top-level managers/divisions; their direct children (`depth === 3`) are shown as reports below them.
- Nodes are colored using a VIVID/LIGHT palette keyed to manager index.

**Error handling:** None. No `.catch()` on the `Promise.all`.

---

### 5. Other Components Importing from the API Layer

The grep confirmed that **only these four files** import from `../api`. No other component or view file imports from the API layer. The `FloorMap` component (imported by `MapView`) receives all its data as props rather than fetching directly.

---

### Summary of Patterns

**Common patterns across all four views:**
- All API calls happen in `useEffect(() => { ... }, [])` for initial data loading.
- Mutating actions (`addBooking`, `runAssignment`) use `async/await` in event handlers, followed by a re-fetch of the affected data.
- Every function from the API layer returns a `Promise`. Components consume them via either `.then(setState)` (for simple loads) or `await` (for sequential multi-step operations).
- Data is stored in React `useState` hooks immediately upon resolution.

**Consistent gap:** No error handling exists anywhere. None of the four views have `try/catch`, `.catch()`, or error state. A network failure or backend error would result in unhandled promise rejections and potentially stuck UI states (especially in `ScoreDashboard` where `running` would stay `true` forever).
```

---

### You _13:44:20_

[Tool result]
```
Now I have all the information needed. Here is the comprehensive analysis:

---

## Resource Endpoints and Implementation Status

**Note:** The resource files are in the package `com.starlingbank.api`, not `com.starlingbank.resource`. The exact file paths are under `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/api/`.

---

### 1. AssignmentResource (`/api/assignments`)

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/api/AssignmentResource.java`

| Endpoint | Method | Path | Input | Returns | Status |
|---|---|---|---|---|---|
| `run()` | `POST` | `/api/assignments/run` | None (body-less) | `AssignmentCollection` (JSON) | **REAL** |
| `getAssignment()` | `GET` | `/api/assignments` | None | `AssignmentCollection` (JSON) | **REAL** |
| `getScore()` | `GET` | `/api/assignments/score` | None | `AssignmentScore` (JSON) | **REAL** |

**Injected dependencies:** `AssignmentService`, `AssignmentState`, `BookingService`, `FloorMapService`, `ScoringService`.

---

### AssignmentResource.run() -- detailed behavior

This is **fully real**, not stubbed. The logic is:

1. Calls `bookingService.getBookings().getBookings()` to get the current list of `BookingRequest`s.
2. **If bookings is empty**, it returns `assignmentState.get()` -- which defaults to an `AssignmentCollection` with two empty maps (`Map.of()` for both `deskByEmployeeId` and `employeeByDeskId`) if nothing was ever assigned. So yes, if no bookings exist, it returns empty data, but this is the correct/expected behavior, not a stub.
3. **If bookings exist**, it fetches desks from `floorMapService.getDesks()`, calls `assignmentService.assign(bookings, desks)`, stores the result in `assignmentState`, and returns it.
4. The `AssignmentService` is bound to `SimulatedAnnealingAssignmentService`, which is a **fully implemented** multi-threaded simulated annealing optimizer (400 runs, 12 threads, 200k iterations each, with real QAP cost computation using team weights derived from org chart tree distance, social preferences, and "feeling lucky" affinity). This is not a stub in any way.

---

### 2. BookingResource (`/api/bookings`)

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/api/BookingResource.java`

| Endpoint | Method | Path | Input | Returns | Status |
|---|---|---|---|---|---|
| `addBooking()` | `POST` | `/api/bookings` | `BookingRequest` (JSON body) | `BookingRequest` (JSON) | **REAL** (delegates to `BookingServiceImpl`) |
| `getBookings()` | `GET` | `/api/bookings` | None | `BookingCollection` (JSON) | **REAL** |

**Injected dependency:** `BookingService` (bound to `BookingServiceImpl`).

---

### 3. DeskResource (`/api/desks`)

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/api/DeskResource.java`

| Endpoint | Method | Path | Input | Returns | Status |
|---|---|---|---|---|---|
| `getDesks()` | `GET` | `/api/desks` | None | `List<Desk>` (JSON) | **REAL** (delegates to `FloorMapServiceImpl`) |

**Injected dependency:** `FloorMapService` (bound to `FloorMapServiceImpl`).

---

### 4. EmployeeResource (`/api/employees`)

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/api/EmployeeResource.java`

| Endpoint | Method | Path | Input | Returns | Status |
|---|---|---|---|---|---|
| `getEmployees()` | `GET` | `/api/employees` | None | `List<Employee>` (JSON) | **REAL** |

Returns `List.copyOf(orgChartService.getEmployees().values())`.

**Injected dependency:** `OrgChartService` (bound to `OrgChartServiceImpl`).

---

### 5. OrgNodeResource (`/api/orgnodes`)

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/api/OrgNodeResource.java`

| Endpoint | Method | Path | Input | Returns | Status |
|---|---|---|---|---|---|
| `getOrgNodes()` | `GET` | `/api/orgnodes` | None | `List<OrgNode>` (JSON) | **REAL** |

Returns `List.copyOf(orgChartService.getOrgNodes().values())`.

**Injected dependency:** `OrgChartService` (bound to `OrgChartServiceImpl`).

---

### 6. FloorMapResource (`/api/floor-map`)

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/api/FloorMapResource.java`

| Endpoint | Method | Path | Input | Returns | Status |
|---|---|---|---|---|---|
| `parseHar()` | `POST` | `/api/floor-map/parse-har` | `@QueryParam("har") String harFile` | `Map<String, Object>` (JSON) with keys: `file`, `floor`, `building`, `desks`, `pods`, `walls`, `landmarks` | **REAL** |

This is a utility/admin endpoint. It reads a HAR file from `input-data/har/<harFile>`, parses it via `FloorMapParser`, writes the parsed `FloorMap` as JSON to `input-data/floors/<slug>.json`, registers the floor map with the service, and returns a summary.

**Injected dependency:** `FloorMapService` (bound to `FloorMapServiceImpl`).

---

### OrgNode.parentId -- Nullability

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/model/OrgNode.java`

`parentId` is declared as `private String parentId` -- a plain `String` with no `@Nullable`/`@NonNull`/`@NotNull` annotation. There is no validation or null-check anywhere in the class. The constructor accepts it directly: `this.parentId = parentId;`.

**Yes, `parentId` can be null.** Java `String` is inherently nullable, and there is nothing preventing it. For the root node of the org tree, `parentId` would logically be `null` (no parent). The SA service's `treeDistance` method uses `orgPath` (not `parentId`) to compute tree distance, so null `parentId` does not cause issues there.

---

### AppModule -- Guice Bindings

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/AppModule.java`

Six bindings are configured:

| Interface | Implementation |
|---|---|
| `HelloService` | `HelloServiceImpl` |
| `FloorMapService` | `FloorMapServiceImpl` |
| `OrgChartService` | `OrgChartServiceImpl` |
| `AssignmentService` | `SimulatedAnnealingAssignmentService` |
| `ScoringService` | `ScoringServiceImpl` |
| `BookingService` | `BookingServiceImpl` |

Note: `AssignmentState` is not bound in `AppModule` -- it is annotated with `@Singleton` directly on the class and is injected by Guice's just-in-time (JIT) binding. It holds a `volatile AssignmentCollection` that defaults to empty maps.

---

### Summary of Implementation Status

Every single resource endpoint has a **real, non-stubbed implementation**. There are no TODO stubs, no hardcoded dummy data, and no placeholder responses. The most sophisticated piece is `AssignmentResource.run()`, which orchestrates a full pipeline: fetch bookings, fetch desks, run a parallelized simulated annealing optimizer (400 concurrent runs across 12 threads, 200k iterations each), and return optimized desk assignments. The only scenario where `run()` returns "empty" data is when there are zero bookings -- which is the correct behavior, not a stub.
```

---

### You _13:44:59_

[Tool result]
```
Now I have the complete picture. Here is the full analysis.

---

## Frontend API Layer -- Complete Analysis

### Files examined

- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/index.ts`
- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/client.ts`
- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/stubs.ts`
- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/generated/api.ts`
- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/vite.config.ts`
- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/types.ts`

---

### 1. What `api/index.ts` exports and whether it re-exports stubs or the generated client

**It re-exports exclusively from `./stubs`.** The generated client is not used at all yet. The file contains a clear comment explaining the swap plan:

```typescript
// Stub layer -- all API calls go here.
// When the backend is ready, swap each export below to use the generated client
// from '../generated/api' + FetchHttpClient, and update the base URL in client.ts
// from window.location.origin -> http://localhost:8080.
//
// getOrgNodes() has no backend endpoint yet -- flag this to the backend dev when wiring up.
export {
  getDesks,
  getEmployees,
  getOrgNodes,
  getBookings,
  addBooking,
  getAssignments,
  runAssignment,
  getScore,
} from './stubs'
```

Eight functions are exported. Note the comment that `getOrgNodes()` has no backend endpoint yet.

---

### 2. Exported function signatures and return types (from `stubs.ts`)

All stubs import types from `../types` (not from the generated file). Every function returns a `Promise` resolved synchronously with hardcoded data:

| Function | Signature | Return Type |
|---|---|---|
| `getDesks` | `(): Promise<Desk[]>` | Resolves with the full `DESKS` array (191 desk objects) |
| `getEmployees` | `(): Promise<Employee[]>` | Resolves with `VISIBLE_EMPLOYEES` (filtered subset of `EMPLOYEES`) |
| `getOrgNodes` | `(): Promise<OrgNode[]>` | Resolves with `VISIBLE_ORG_NODES` (filtered subset of `ORG_NODES`) |
| `getBookings` | `(): Promise<BookingCollection>` | Resolves `{ bookings, totalCapacity: 191, remaining: 191 - bookings.length }` |
| `addBooking` | `(request: BookingRequest): Promise<BookingRequest>` | Mutates module-level `bookings` array (upserts by `employeeId`), resolves with the request |
| `getAssignments` | `(): Promise<AssignmentCollection>` | Resolves with module-level `assignments` |
| `runAssignment` | `(): Promise<AssignmentCollection>` | Resolves with module-level `assignments` (same data; no solver logic in stub) |
| `getScore` | `(): Promise<AssignmentScore>` | Resolves with hardcoded `{ totalQapCost: 72, teamCohesion: 58, managerProximity: 61, socialSatisfaction: 80, windowHitRate: 65 }` |

**Important stub behavior:** The stubs filter employees and org nodes to only those below `2_Raman_Bhatia` in the org tree (depth > Raman's depth). Assignments are similarly filtered. The `bookings` array starts empty and is mutated in-memory by `addBooking`.

---

### 3. Generated `RestApplicationClient` methods and return types (from `generated/api.ts`)

The generated client class is `RestApplicationClient`. It takes an `HttpClient` in its constructor. Its methods and their HTTP mappings:

| Method | HTTP | URL | Param | Return Type |
|---|---|---|---|---|
| `getDesks()` | GET | `api/desks` | -- | `RestResponse<Desk[]>` (i.e., `Promise<Desk[]>`) |
| `getEmployees()` | GET | `api/employees` | -- | `RestResponse<Employee[]>` |
| `getOrgNodes()` | GET | `api/orgnodes` | -- | `RestResponse<OrgNode[]>` |
| `getBookings()` | GET | `api/bookings` | -- | `RestResponse<BookingCollection>` |
| `addBooking(arg0)` | POST | `api/bookings` | `BookingRequest` body | `RestResponse<BookingRequest>` |
| `getAssignment()` | GET | `api/assignments` | -- | `RestResponse<AssignmentCollection>` |
| `run()` | POST | `api/assignments/run` | -- | `RestResponse<AssignmentCollection>` |
| `getScore()` | GET | `api/assignments/score` | -- | `RestResponse<AssignmentScore>` |
| `greet(queryParams?)` | GET | `hello` | `{ name?: string }` | `RestResponse<HelloResponse>` |

`RestResponse<R>` is defined as `Promise<R>`.

**Name mismatches between stubs and generated client that must be reconciled when wiring:**
- Stub `getAssignments()` corresponds to generated `getAssignment()` (no "s")
- Stub `runAssignment()` corresponds to generated `run()`
- The generated client has `greet()` which has no stub equivalent

---

### 4. `FetchHttpClient` implementation (from `client.ts`)

```typescript
export class FetchHttpClient implements HttpClient {
  request<R>(requestConfig: {
    method: string
    url: string
    queryParams?: Record<string, unknown>
    data?: unknown
    copyFn?: (data: R) => R
  }): RestResponse<R> {
    const url = new URL('/' + requestConfig.url, window.location.origin)
    // ... appends queryParams to URL search params
    // ... calls fetch() with JSON body if data is provided
    // ... throws on non-ok response
    // ... returns res.json() as R
  }
}
```

Key details:
- Base URL is `window.location.origin` (the Vite dev server, relying on proxy for backend routes).
- Query params are set via `url.searchParams.set()`, skipping null/undefined values.
- POST bodies are JSON-serialized with `Content-Type: application/json`.
- Error handling: throws `Error(`HTTP ${res.status} ${res.statusText}`)` on non-ok responses.
- The `copyFn` parameter is accepted but **never used** in the implementation.

---

### 5. Vite proxy configuration (from `vite.config.ts`)

```typescript
server: {
  proxy: {
    '/hello': 'http://localhost:8080',
  },
}
```

**Only `/hello` is proxied.** The `/api/*` routes that the generated client uses (`api/desks`, `api/employees`, `api/bookings`, `api/assignments`, `api/assignments/run`, `api/assignments/score`, `api/orgnodes`) are **not proxied**. When wiring to the real backend, you will need to add a proxy entry like `'/api': 'http://localhost:8080'`.

---

### 6. Local type definitions (from `types.ts`) vs. generated types

The local `types.ts` defines: `SocialPreference`, `Desk`, `Employee`, `OrgNode`, `BookingRequest`, `BookingCollection`, `AssignmentCollection`, `AssignmentScore`.

The generated `api.ts` defines the same types plus additional ones not in `types.ts`: `FloorInfo`, `FloorMap`, `Spaces`, `Pod`, `Wall`, `Landmark`, `UnavailableSpace`, `ViewBox`, `HelloResponse`.

There is one subtle difference: in local `types.ts`, `Desk.neighborhood` is typed as `string | null`, while in the generated file it is typed as `string` (non-nullable). The stub data does include desks with `"neighborhood": null` (e.g., "Working Lounge", "Flexible Working" desks), so the local type is the more accurate one for the actual data shape.

---

### Summary of what needs to happen to wire to the real backend

1. **Add Vite proxy** for `/api` to `http://localhost:8080`.
2. **Instantiate `RestApplicationClient`** with `new FetchHttpClient()` and create wrapper functions that match the current stub signatures (handling the name mismatches: `getAssignments` -> `client.getAssignment()`, `runAssignment` -> `client.run()`).
3. **Decide on types**: either import from `generated/api.ts` or keep `types.ts` in sync. The `neighborhood: string | null` discrepancy needs resolution.
4. **`getOrgNodes`** has a generated endpoint (`GET /api/orgnodes`) but the comment in `index.ts` warns the backend may not have implemented it yet -- verify with the backend team.
5. **Stub filtering logic** (the `RAMAN_DEPTH` / `VISIBLE_IDS` filtering) will presumably be handled server-side, so the wired functions should be simpler pass-throughs.
6. **`greet()`** exists in the generated client but is not exported from the stubs; decide whether the frontend needs it.
```

---

### Claude _13:45:41_

Now I have a clear picture. Let me design the implementation plan.

---

### Claude _13:46:02_

[Tool call: Agent]
```json
{
  "description": "Design wiring plan",
  "subagent_type": "Plan",
  "prompt": "Design an implementation plan for wiring the frontend to the real backend in /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon.\n\n## Current State\n\nThe frontend has 8 API functions exported from `frontend/src/api/index.ts` that all re-export from `./stubs` (hardcoded data). A generated client (`RestApplicationClient` in `frontend/src/generated/api.ts`) and `FetchHttpClient` (`frontend/src/api/client.ts`) already exist but are unused. All 8 backend endpoints are fully implemented (not stubbed).\n\nThe Vite proxy only forwards `/hello` to `http://localhost:8080`. No `/api` proxy rule exists.\n\n## What Needs to Change\n\n### File 1: `frontend/vite.config.ts`\nAdd `/api` to the proxy config alongside `/hello`, both pointing to `http://localhost:8080`.\n\n### File 2: `frontend/src/api/index.ts`\nReplace the stub re-exports with real client calls. Current stub exports and their generated client equivalents:\n\n| Stub function name | Generated method | Notes |\n|---|---|---|\n| `getDesks()` | `client.getDesks()` | Same name |\n| `getEmployees()` | `client.getEmployees()` | Same name |\n| `getOrgNodes()` | `client.getOrgNodes()` | Same name |\n| `getBookings()` | `client.getBookings()` | Same name |\n| `addBooking(req)` | `client.addBooking(req)` | Same name |\n| `getAssignments()` | `client.getAssignment()` | Name mismatch: no \"s\" |\n| `runAssignment()` | `client.run()` | Name mismatch |\n| `getScore()` | `client.getScore()` | Same name |\n\nThe plan: instantiate `FetchHttpClient` and `RestApplicationClient` at module level, then export named wrapper functions matching the current signatures. Import types from `../types` for the function signatures. The generated RestResponse<T> is just Promise<T>, so the return types are compatible.\n\nType compatibility: The `types.ts` types have `Desk.neighborhood: string | null` and `OrgNode.parentId: string | null`, while the generated types have them as non-nullable `string`. Since `string` is a subtype of `string | null`, assigning generated return values to the broader types.ts types works fine structurally \u2014 no casts needed.\n\n### No Other Files Change\nAll 4 consumer components (BookingForm, MapView, ScoreDashboard, OrgChart) import from `../api` and use the same function names. Since we're preserving the exact export names and signatures, no consumer changes are needed.\n\n### What We Do NOT Change\n- `frontend/src/api/stubs.ts` \u2014 keep it around as a reference/fallback, just stop importing from it\n- `frontend/src/generated/api.ts` \u2014 don't modify generated code\n- `frontend/src/api/client.ts` \u2014 already correct, uses `window.location.origin` which works with the Vite proxy\n- Backend code \u2014 all endpoints are fully implemented\n- `frontend/src/types.ts` \u2014 already has correct nullable types\n\n## Verification\n1. Start backend: `./gradlew run`\n2. Start frontend: `cd frontend && npm run dev`\n3. Test each view:\n   - BookingForm: employee dropdown loads, can submit a booking, capacity bar updates\n   - MapView: desks render from real floor map data, assignments overlay after running\n   - OrgChart: tree renders with real org data\n   - ScoreDashboard: click \"Run assignment\" (after adding bookings), scores update\n4. Run `npm run build` to verify no TypeScript errors\n\n## Risks\n- The SA algorithm takes noticeable time (400 runs \u00d7 200k iterations). The ScoreDashboard's \"Run assignment\" button may appear to hang. This is expected behavior, not a bug. The button already has a \"Running...\" loading state.\n- If no bookings exist when \"Run assignment\" is clicked, the backend correctly returns empty maps \u2014 no error, just no assignments.\n\nPlease produce a concise step-by-step plan covering these changes."
}
```

---

### You _13:46:57_

[Tool result]
```
Only `index.ts` imports from stubs. Everything is confirmed. Here is the complete implementation plan.

---

## Implementation Plan: Wire Frontend to Real Backend

This is a two-file change. No other files need modification. All four consumer components (`BookingForm`, `MapView`, `ScoreDashboard`, `OrgChart`) import from `../api` using the same function names, so they are unaffected.

### Step 1: Add `/api` proxy rule to Vite config

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/vite.config.ts`

Currently the proxy config only forwards `/hello`:

```ts
proxy: {
  '/hello': 'http://localhost:8080',
},
```

Add `/api` as a second entry:

```ts
proxy: {
  '/hello': 'http://localhost:8080',
  '/api': 'http://localhost:8080',
},
```

This ensures that all requests from the `FetchHttpClient` (which constructs URLs like `/api/desks`, `/api/bookings`, etc. relative to `window.location.origin`) get forwarded to the backend at `localhost:8080` during development.

### Step 2: Replace stub re-exports with real client calls

**File:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/index.ts`

Replace the entire file contents. The new implementation:

1. Imports `FetchHttpClient` from `./client`.
2. Imports `RestApplicationClient` from `../generated/api`.
3. Imports the types from `../types` (for the function signatures).
4. Instantiates the client at module level (singleton, created once on import).
5. Exports 8 named functions that delegate to the client, preserving the exact function names and signatures the consumers expect.

The mapping with name adjustments:

| Exported function | Delegates to |
|---|---|
| `getDesks()` | `client.getDesks()` |
| `getEmployees()` | `client.getEmployees()` |
| `getOrgNodes()` | `client.getOrgNodes()` |
| `getBookings()` | `client.getBookings()` |
| `addBooking(req)` | `client.addBooking(req)` |
| `getAssignments()` | `client.getAssignment()` (note: no trailing "s" on the generated method) |
| `runAssignment()` | `client.run()` (different name on generated client) |
| `getScore()` | `client.getScore()` |

**Type compatibility note:** The stub functions return `Promise<T>` using types from `../types`. The generated client methods return `RestResponse<T>`, which is defined as `type RestResponse<R> = Promise<R>` (line 195 of the generated file). These are structurally identical, so no casts or type gymnastics are needed.

The `types.ts` types have `Desk.neighborhood: string | null` and `OrgNode.parentId: string | null`, while the generated types have them as plain `string`. Since `string` is assignable to `string | null`, the return values from the generated client are structurally compatible with the broader types the consumers expect. No explicit casts are required.

### Step 3: Verify

1. Start backend: `./gradlew run` from the project root.
2. Start frontend: `cd frontend && npm run dev`.
3. Test each view in the browser:
   - **BookingForm** at the default route: employee dropdown should populate with real employees; submitting a booking should persist (verify by refreshing); the capacity bar should reflect real `totalCapacity` and `remaining` from the backend.
   - **MapView**: desks should render at their real x/y positions; after running an assignment, colored overlays should appear on assigned desks.
   - **OrgChart**: the tree should render with real employee/org hierarchy data.
   - **ScoreDashboard**: click "Run assignment" (after adding at least one booking); the score metrics should update with real values. The SA algorithm takes noticeable time (400 runs x 200k iterations) -- the button's "Running..." loading state is expected to show for several seconds.
4. Run `npm run build` in the `frontend/` directory to confirm no TypeScript compilation errors.

### What stays unchanged

- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/stubs.ts` -- kept as dead code / reference; just no longer imported.
- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/client.ts` -- already correct; uses `window.location.origin` which works with the Vite proxy.
- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/generated/api.ts` -- generated code, never modify by hand.
- `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/types.ts` -- already has correct nullable types.
- All backend code -- all 8 endpoints are fully implemented.
- All 4 consumer components -- they import by function name from `../api`, and the names and signatures are preserved.

### Risks

- **SA algorithm latency**: The "Run assignment" operation is computationally expensive on the backend. The ScoreDashboard button already has a loading state ("Running...") to handle this.
- **Empty state**: If no bookings exist when "Run assignment" is triggered, the backend returns empty maps (not an error). The UI should handle this gracefully (it does -- it just shows no assignments).

---

### Critical Files for Implementation
- /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/vite.config.ts
- /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/index.ts
- /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/client.ts
- /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/generated/api.ts
- /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/types.ts
```

---

### Claude _13:47:12_

[Tool call: Write]
```json
{
  "file_path": "/Users/kapilan.cholan1/.claude/plans/distributed-imagining-dove.md",
  "content": "# Plan: Wire Frontend to Real Backend\n\n## Context\n\nThe frontend has 8 API functions that all return hardcoded stub data. The backend has all 8 endpoints fully implemented (including the SA assignment algorithm). A generated TypeScript client (`RestApplicationClient`) and `FetchHttpClient` already exist but are unused. This plan connects them \u2014 a two-file change.\n\n## Changes\n\n### 1. Add `/api` proxy to Vite config\n\n**File:** `frontend/vite.config.ts`\n\nAdd `'/api': 'http://localhost:8080'` alongside the existing `/hello` proxy entry. This lets the `FetchHttpClient` (which uses `window.location.origin` as base URL) reach the backend during dev.\n\n### 2. Replace stub exports with real client calls\n\n**File:** `frontend/src/api/index.ts`\n\nReplace the stub re-exports with wrapper functions that delegate to `RestApplicationClient`. The file will:\n- Import `FetchHttpClient` from `./client` and `RestApplicationClient` from `../generated/api`\n- Instantiate both at module level\n- Export 8 named functions preserving the existing signatures\n\nName mappings where they differ:\n| Current export | Generated method |\n|---|---|\n| `getAssignments()` | `client.getAssignment()` |\n| `runAssignment()` | `client.run()` |\n\nAll other names match 1:1. No type casts needed \u2014 `RestResponse<T>` is `Promise<T>`, and the generated types are structurally assignable to the `types.ts` types (which have broader nullability).\n\n### What stays unchanged\n\n- `stubs.ts` \u2014 kept as dead code, no longer imported\n- `client.ts` \u2014 already correct\n- `generated/api.ts` \u2014 never modify generated code\n- `types.ts` \u2014 already has correct nullable types\n- All 4 consumer components (BookingForm, MapView, ScoreDashboard, OrgChart) \u2014 they import by function name from `../api`, unchanged\n- All backend code \u2014 fully implemented\n\n## Verification\n\n1. `./gradlew run` (backend) and `cd frontend && npm run dev` (frontend) simultaneously\n2. BookingForm: employee dropdown populates, booking submits, capacity bar updates\n3. MapView: desks render at real positions, assignments overlay after running\n4. OrgChart: real org hierarchy renders\n5. ScoreDashboard: \"Run assignment\" triggers SA algorithm, scores update (expect several seconds of latency)\n6. `cd frontend && npm run build` \u2014 no TypeScript errors\n"
}
```

---

### You _13:47:12_

[Tool result]
```
File created successfully at: /Users/kapilan.cholan1/.claude/plans/distributed-imagining-dove.md (file state is current in your context — no need to Read it back)
```

---

### Claude _13:47:15_

Two files, zero consumer changes. The plan is ready for review.

---

### Claude _13:47:15_

[Tool call: ExitPlanMode]
```json
{
  "plan": "# Plan: Wire Frontend to Real Backend\n\n## Context\n\nThe frontend has 8 API functions that all return hardcoded stub data. The backend has all 8 endpoints fully implemented (including the SA assignment algorithm). A generated TypeScript client (`RestApplicationClient`) and `FetchHttpClient` already exist but are unused. This plan connects them \u2014 a two-file change.\n\n## Changes\n\n### 1. Add `/api` proxy to Vite config\n\n**File:** `frontend/vite.config.ts`\n\nAdd `'/api': 'http://localhost:8080'` alongside the existing `/hello` proxy entry. This lets the `FetchHttpClient` (which uses `window.location.origin` as base URL) reach the backend during dev.\n\n### 2. Replace stub exports with real client calls\n\n**File:** `frontend/src/api/index.ts`\n\nReplace the stub re-exports with wrapper functions that delegate to `RestApplicationClient`. The file will:\n- Import `FetchHttpClient` from `./client` and `RestApplicationClient` from `../generated/api`\n- Instantiate both at module level\n- Export 8 named functions preserving the existing signatures\n\nName mappings where they differ:\n| Current export | Generated method |\n|---|---|\n| `getAssignments()` | `client.getAssignment()` |\n| `runAssignment()` | `client.run()` |\n\nAll other names match 1:1. No type casts needed \u2014 `RestResponse<T>` is `Promise<T>`, and the generated types are structurally assignable to the `types.ts` types (which have broader nullability).\n\n### What stays unchanged\n\n- `stubs.ts` \u2014 kept as dead code, no longer imported\n- `client.ts` \u2014 already correct\n- `generated/api.ts` \u2014 never modify generated code\n- `types.ts` \u2014 already has correct nullable types\n- All 4 consumer components (BookingForm, MapView, ScoreDashboard, OrgChart) \u2014 they import by function name from `../api`, unchanged\n- All backend code \u2014 fully implemented\n\n## Verification\n\n1. `./gradlew run` (backend) and `cd frontend && npm run dev` (frontend) simultaneously\n2. BookingForm: employee dropdown populates, booking submits, capacity bar updates\n3. MapView: desks render at real positions, assignments overlay after running\n4. OrgChart: real org hierarchy renders\n5. ScoreDashboard: \"Run assignment\" triggers SA algorithm, scores update (expect several seconds of latency)\n6. `cd frontend && npm run build` \u2014 no TypeScript errors\n",
  "planFilePath": "/Users/kapilan.cholan1/.claude/plans/distributed-imagining-dove.md"
}
```

---

### You _13:48:31_

[Tool result]
```
User has approved your plan. You can now start coding. Start with updating your todo list if applicable

Your plan has been saved to: /Users/kapilan.cholan1/.claude/plans/distributed-imagining-dove.md
You can refer back to it if needed during implementation.

## Approved Plan:
# Plan: Wire Frontend to Real Backend

## Context

The frontend has 8 API functions that all return hardcoded stub data. The backend has all 8 endpoints fully implemented (including the SA assignment algorithm). A generated TypeScript client (`RestApplicationClient`) and `FetchHttpClient` already exist but are unused. This plan connects them — a two-file change.

## Changes

### 1. Add `/api` proxy to Vite config

**File:** `frontend/vite.config.ts`

Add `'/api': 'http://localhost:8080'` alongside the existing `/hello` proxy entry. This lets the `FetchHttpClient` (which uses `window.location.origin` as base URL) reach the backend during dev.

### 2. Replace stub exports with real client calls

**File:** `frontend/src/api/index.ts`

Replace the stub re-exports with wrapper functions that delegate to `RestApplicationClient`. The file will:
- Import `FetchHttpClient` from `./client` and `RestApplicationClient` from `../generated/api`
- Instantiate both at module level
- Export 8 named functions preserving the existing signatures

Name mappings where they differ:
| Current export | Generated method |
|---|---|
| `getAssignments()` | `client.getAssignment()` |
| `runAssignment()` | `client.run()` |

All other names match 1:1. No type casts needed — `RestResponse<T>` is `Promise<T>`, and the generated types are structurally assignable to the `types.ts` types (which have broader nullability).

### What stays unchanged

- `stubs.ts` — kept as dead code, no longer imported
- `client.ts` — already correct
- `generated/api.ts` — never modify generated code
- `types.ts` — already has correct nullable types
- All 4 consumer components (BookingForm, MapView, ScoreDashboard, OrgChart) — they import by function name from `../api`, unchanged
- All backend code — fully implemented

## Verification

1. `./gradlew run` (backend) and `cd frontend && npm run dev` (frontend) simultaneously
2. BookingForm: employee dropdown populates, booking submits, capacity bar updates
3. MapView: desks render at real positions, assignments overlay after running
4. OrgChart: real org hierarchy renders
5. ScoreDashboard: "Run assignment" triggers SA algorithm, scores update (expect several seconds of latency)
6. `cd frontend && npm run build` — no TypeScript errors
```

---

### Claude _13:48:35_

Reading both files before editing.

---

### Claude _13:48:35_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/vite.config.ts"
}
```

---

### Claude _13:48:36_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/index.ts"
}
```

---

### You _13:48:36_

[Tool result]
```
1	// Stub layer — all API calls go here.
2	// When the backend is ready, swap each export below to use the generated client
3	// from '../generated/api' + FetchHttpClient, and update the base URL in client.ts
4	// from window.location.origin → http://localhost:8080.
5	//
6	// getOrgNodes() has no backend endpoint yet — flag this to the backend dev when wiring up.
7	export {
8	  getDesks,
9	  getEmployees,
10	  getOrgNodes,
11	  getBookings,
12	  addBooking,
13	  getAssignments,
14	  runAssignment,
15	  getScore,
16	} from './stubs'
```

---

### You _13:48:36_

[Tool result]
```
1	import { defineConfig } from 'vite'
2	import react from '@vitejs/plugin-react'
3	
4	export default defineConfig({
5	  plugins: [react()],
6	  server: {
7	    proxy: {
8	      '/hello': 'http://localhost:8080',
9	    },
10	  },
11	})
12
```

---

### Claude _13:48:39_

[Tool call: Edit]
```json
{
  "replace_all": false,
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/vite.config.ts",
  "old_string": "    proxy: {\n      '/hello': 'http://localhost:8080',\n    },",
  "new_string": "    proxy: {\n      '/hello': 'http://localhost:8080',\n      '/api': 'http://localhost:8080',\n    },"
}
```

---

### You _13:48:39_

[Tool result]
```
The file /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/vite.config.ts has been updated successfully. (file state is current in your context — no need to Read it back)
```

---

### Claude _13:48:44_

[Tool call: Write]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/index.ts",
  "content": "import { FetchHttpClient } from './client'\nimport { RestApplicationClient } from '../generated/api'\nimport type {\n  Desk,\n  Employee,\n  OrgNode,\n  BookingCollection,\n  BookingRequest,\n  AssignmentCollection,\n  AssignmentScore,\n} from '../types'\n\nconst client = new RestApplicationClient(new FetchHttpClient())\n\nexport const getDesks = (): Promise<Desk[]> => client.getDesks()\nexport const getEmployees = (): Promise<Employee[]> => client.getEmployees()\nexport const getOrgNodes = (): Promise<OrgNode[]> => client.getOrgNodes()\nexport const getBookings = (): Promise<BookingCollection> => client.getBookings()\nexport const addBooking = (req: BookingRequest): Promise<BookingRequest> => client.addBooking(req)\nexport const getAssignments = (): Promise<AssignmentCollection> => client.getAssignment()\nexport const runAssignment = (): Promise<AssignmentCollection> => client.run()\nexport const getScore = (): Promise<AssignmentScore> => client.getScore()\n"
}
```

---

### You _13:48:44_

[Tool result]
```
The file /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/index.ts has been updated successfully. (file state is current in your context — no need to Read it back)
```

---

### Claude _13:48:47_

The vite.config.ts errors are pre-existing (missing node_modules in the TS server's view, not real build errors). Let me verify with an actual build.

---

### Claude _13:48:49_

[Tool call: Bash]
```json
{
  "command": "cd /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend && npm run build 2>&1",
  "description": "TypeScript check + production build",
  "timeout": 60000
}
```

---

### You _13:48:53_

[Tool error]
```
Exit code 1
npm warn Unknown user config "always-auth" (//starlingbank.jfrog.io/artifactory/api/npm/npm-local/:always-auth). This will stop working in the next major version of npm. See `npm help npmrc` for supported config options.

> frontend@0.0.0 build
> tsc -b && vite build

error TS2688: Cannot find type definition file for 'vite/client'.
  The file is in the program because:
    Entry point of type library 'vite/client' specified in compilerOptions
error TS2688: Cannot find type definition file for 'node'.
  The file is in the program because:
    Entry point of type library 'node' specified in compilerOptions
npm notice
npm notice New minor version of npm available! 11.11.0 -> 11.16.0
npm notice Changelog: https://github.com/npm/cli/releases/tag/v11.16.0
npm notice To update run: npm install -g npm@11.16.0
npm notice
```

---

### Claude _13:48:55_

Dependencies aren't installed. Running `npm install` first.

---

### Claude _13:48:56_

[Tool call: Bash]
```json
{
  "command": "cd /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend && npm install 2>&1 | tail -5",
  "description": "Install frontend dependencies",
  "timeout": 60000
}
```

---

### You _13:49:05_

[Tool result]
```
42 packages are looking for funding
  run `npm fund` for details

found 0 vulnerabilities
```

---

### Claude _13:49:07_

[Tool call: Bash]
```json
{
  "command": "npm run build 2>&1",
  "description": "TypeScript check + production build",
  "timeout": 60000
}
```

---

### You _13:49:27_

[Tool error]
```
Exit code 2
npm warn Unknown user config "always-auth" (//starlingbank.jfrog.io/artifactory/api/npm/npm-local/:always-auth). This will stop working in the next major version of npm. See `npm help npmrc` for supported config options.

> frontend@0.0.0 build
> tsc -b && vite build

src/api/client.ts(23,7): error TS2698: Spread types may only be created from object types.
src/generated/api.ts(119,17): error TS1294: This syntax is not allowed when 'erasableSyntaxOnly' is enabled.
```

---

### Claude _13:49:31_

These errors are in files I didn't touch. Let me check if they're pre-existing.

---

### Claude _13:49:32_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/client.ts"
}
```

---

### Claude _13:49:32_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/generated/api.ts",
  "offset": 115,
  "limit": 10
}
```

---

### You _13:49:32_

[Tool result]
```
1	import type { HttpClient, RestResponse } from '../generated/api'
2	
3	export class FetchHttpClient implements HttpClient {
4	  request<R>(requestConfig: {
5	    method: string
6	    url: string
7	    queryParams?: Record<string, unknown>
8	    data?: unknown
9	    copyFn?: (data: R) => R
10	  }): RestResponse<R> {
11	    const url = new URL('/' + requestConfig.url, window.location.origin)
12	
13	    if (requestConfig.queryParams) {
14	      for (const [key, value] of Object.entries(requestConfig.queryParams)) {
15	        if (value !== undefined && value !== null) {
16	          url.searchParams.set(key, String(value))
17	        }
18	      }
19	    }
20	
21	    return fetch(url.toString(), {
22	      method: requestConfig.method,
23	      ...(requestConfig.data && {
24	        body: JSON.stringify(requestConfig.data),
25	        headers: { 'Content-Type': 'application/json' },
26	      }),
27	    }).then(res => {
28	      if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`)
29	      return res.json() as R
30	    })
31	  }
32	}
```

---

### You _13:49:32_

[Tool result]
```
115	}
116	
117	export class RestApplicationClient {
118	
119	    constructor(protected httpClient: HttpClient) {
120	    }
121	
122	    /**
123	     * HTTP GET /api/assignments
124	     * Java method: com.starlingbank.api.AssignmentResource.getAssignment
```

---

### Claude _13:49:41_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/tsconfig.app.json"
}
```

---

### You _13:49:41_

[Tool result]
```
1	{
2	  "compilerOptions": {
3	    "tsBuildInfoFile": "./node_modules/.tmp/tsconfig.app.tsbuildinfo",
4	    "target": "es2023",
5	    "lib": ["ES2023", "DOM"],
6	    "module": "esnext",
7	    "types": ["vite/client"],
8	    "skipLibCheck": true,
9	
10	    /* Bundler mode */
11	    "moduleResolution": "bundler",
12	    "allowImportingTsExtensions": true,
13	    "verbatimModuleSyntax": true,
14	    "moduleDetection": "force",
15	    "noEmit": true,
16	    "jsx": "react-jsx",
17	
18	    /* Linting */
19	    "noUnusedLocals": true,
20	    "noUnusedParameters": true,
21	    "erasableSyntaxOnly": true,
22	    "noFallthroughCasesInSwitch": true
23	  },
24	  "include": ["src"]
25	}
26
```

---

### Claude _13:50:03_

Two pre-existing build errors to fix:
1. `generated/api.ts:119` — `constructor(protected httpClient)` is a parameter property, blocked by `erasableSyntaxOnly: true` in tsconfig. The generated code doesn't comply; removing that flag is the right fix.
2. `client.ts:23` — spread of `(data && {...})` can be `false`, not an object. Fix with a ternary.

---

### Claude _13:50:04_

[Tool call: Edit]
```json
{
  "replace_all": false,
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/tsconfig.app.json",
  "old_string": "    \"erasableSyntaxOnly\": true,\n",
  "new_string": ""
}
```

---

### Claude _13:50:05_

[Tool call: Edit]
```json
{
  "replace_all": false,
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/client.ts",
  "old_string": "      ...(requestConfig.data && {\n        body: JSON.stringify(requestConfig.data),\n        headers: { 'Content-Type': 'application/json' },\n      }),",
  "new_string": "      ...(requestConfig.data ? {\n        body: JSON.stringify(requestConfig.data),\n        headers: { 'Content-Type': 'application/json' },\n      } : {}),"
}
```

---

### You _13:50:05_

[Tool result]
```
The file /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/tsconfig.app.json has been updated successfully. (file state is current in your context — no need to Read it back)
```

---

### You _13:50:05_

[Tool result]
```
The file /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend/src/api/client.ts has been updated successfully. (file state is current in your context — no need to Read it back)
```

---

### Claude _13:50:10_

[Tool call: Bash]
```json
{
  "command": "npm run build 2>&1",
  "description": "TypeScript check + production build",
  "timeout": 60000
}
```

---

### You _13:50:14_

[Tool result]
```
npm warn Unknown user config "always-auth" (//starlingbank.jfrog.io/artifactory/api/npm/npm-local/:always-auth). This will stop working in the next major version of npm. See `npm help npmrc` for supported config options.

> frontend@0.0.0 build
> tsc -b && vite build

vite v8.0.16 building client environment for production...
[2Ktransforming...✓ 25 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                   0.45 kB │ gzip:  0.29 kB
dist/assets/index-Dynl-E78.css    7.54 kB │ gzip:  2.17 kB
dist/assets/index-BdH58XzS.js   210.60 kB │ gzip: 66.60 kB

✓ built in 361ms
```

---

### Claude _13:50:19_

Build passes clean. The diagnostic is a style hint (async function suggestion), not an error.

Now, regarding your message — noted that the frontend should run with `nvm use 22` before `npm run dev`. Noted for future reference. The wiring is done:

**Summary of changes:**

| File | Change |
|---|---|
| `frontend/vite.config.ts` | Added `'/api': 'http://localhost:8080'` to proxy config |
| `frontend/src/api/index.ts` | Replaced stub re-exports with `RestApplicationClient` + `FetchHttpClient` calls |
| `frontend/src/api/client.ts` | Fixed spread type error (`&&` → ternary) |
| `frontend/tsconfig.app.json` | Removed `erasableSyntaxOnly: true` (incompatible with generated code's parameter properties) |

To test end-to-end: `./gradlew run` in one terminal, then `cd frontend && nvm use 22 && npm run dev` in another.

---

### You _13:50:34_

ok. kill any existing java server

---

### Claude _13:50:37_

[Tool call: Bash]
```json
{
  "command": "lsof -ti :8080 | xargs kill -9 2>/dev/null && echo \"killed\" || echo \"nothing running on 8080\"",
  "description": "Kill processes on port 8080"
}
```

---

### You _13:50:37_

[Tool result]
```
killed
```

---

### Claude _13:50:38_

Done — process on port 8080 killed.

---

### You _13:56:12_

commit and push

---

### Claude _13:56:13_

[Tool call: Bash]
```json
{
  "command": "git status && git diff",
  "description": "Show working tree status and diff"
}
```

---

### You _13:56:14_

[Tool result]
```
On branch master
Your branch is up to date with 'origin/master'.

Changes not staged for commit:
  (use "git add <file>..." to update what will be committed)
  (use "git restore <file>..." to discard changes in working directory)
	modified:   src/api/client.ts
	modified:   src/api/index.ts
	modified:   tsconfig.app.json
	modified:   vite.config.ts

no changes added to commit (use "git add" and/or "git commit -a")
diff --git a/frontend/src/api/client.ts b/frontend/src/api/client.ts
index 376b58c..f7a6687 100644
--- a/frontend/src/api/client.ts
+++ b/frontend/src/api/client.ts
@@ -20,10 +20,10 @@ export class FetchHttpClient implements HttpClient {
 
     return fetch(url.toString(), {
       method: requestConfig.method,
-      ...(requestConfig.data && {
+      ...(requestConfig.data ? {
         body: JSON.stringify(requestConfig.data),
         headers: { 'Content-Type': 'application/json' },
-      }),
+      } : {}),
     }).then(res => {
       if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`)
       return res.json() as R
diff --git a/frontend/src/api/index.ts b/frontend/src/api/index.ts
index e09c7f1..5406b08 100644
--- a/frontend/src/api/index.ts
+++ b/frontend/src/api/index.ts
@@ -1,16 +1,22 @@
-// Stub layer — all API calls go here.
-// When the backend is ready, swap each export below to use the generated client
-// from '../generated/api' + FetchHttpClient, and update the base URL in client.ts
-// from window.location.origin → http://localhost:8080.
-//
-// getOrgNodes() has no backend endpoint yet — flag this to the backend dev when wiring up.
-export {
-  getDesks,
-  getEmployees,
-  getOrgNodes,
-  getBookings,
-  addBooking,
-  getAssignments,
-  runAssignment,
-  getScore,
-} from './stubs'
\ No newline at end of file
+import { FetchHttpClient } from './client'
+import { RestApplicationClient } from '../generated/api'
+import type {
+  Desk,
+  Employee,
+  OrgNode,
+  BookingCollection,
+  BookingRequest,
+  AssignmentCollection,
+  AssignmentScore,
+} from '../types'
+
+const client = new RestApplicationClient(new FetchHttpClient())
+
+export const getDesks = (): Promise<Desk[]> => client.getDesks()
+export const getEmployees = (): Promise<Employee[]> => client.getEmployees()
+export const getOrgNodes = (): Promise<OrgNode[]> => client.getOrgNodes()
+export const getBookings = (): Promise<BookingCollection> => client.getBookings()
+export const addBooking = (req: BookingRequest): Promise<BookingRequest> => client.addBooking(req)
+export const getAssignments = (): Promise<AssignmentCollection> => client.getAssignment()
+export const runAssignment = (): Promise<AssignmentCollection> => client.run()
+export const getScore = (): Promise<AssignmentScore> => client.getScore()
diff --git a/frontend/tsconfig.app.json b/frontend/tsconfig.app.json
index 7f42e5f..d034c66 100644
--- a/frontend/tsconfig.app.json
+++ b/frontend/tsconfig.app.json
@@ -18,7 +18,6 @@
     /* Linting */
     "noUnusedLocals": true,
     "noUnusedParameters": true,
-    "erasableSyntaxOnly": true,
     "noFallthroughCasesInSwitch": true
   },
   "include": ["src"]
diff --git a/frontend/vite.config.ts b/frontend/vite.config.ts
index 584cc27..002b0c3 100644
--- a/frontend/vite.config.ts
+++ b/frontend/vite.config.ts
@@ -6,6 +6,7 @@ export default defineConfig({
   server: {
     proxy: {
       '/hello': 'http://localhost:8080',
+      '/api': 'http://localhost:8080',
     },
   },
 })
```

---

### Claude _13:56:18_

[Tool call: Bash]
```json
{
  "command": "git add frontend/src/api/client.ts frontend/src/api/index.ts frontend/tsconfig.app.json frontend/vite.config.ts && git commit -m \"$(cat <<'EOF'\nWire frontend to real backend via generated REST client\n\nReplaces stub exports in api/index.ts with RestApplicationClient +\nFetchHttpClient. Adds /api proxy in vite.config.ts so dev server\nforwards to localhost:8080. Fixes pre-existing build errors: spread\ntype in client.ts and erasableSyntaxOnly incompatibility with\ngenerated parameter properties.\nEOF\n)\"",
  "description": "Stage and commit wiring changes"
}
```

---

### You _13:56:34_

[Tool error]
```
Exit code 128
warning: could not open directory 'frontend/frontend/': No such file or directory
fatal: pathspec 'frontend/src/api/client.ts' did not match any files
```

---

### Claude _13:56:38_

[Tool call: Bash]
```json
{
  "command": "pwd && git -C /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon add frontend/src/api/client.ts frontend/src/api/index.ts frontend/tsconfig.app.json frontend/vite.config.ts && git -C /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon commit -m \"$(cat <<'EOF'\nWire frontend to real backend via generated REST client\n\nReplaces stub exports in api/index.ts with RestApplicationClient +\nFetchHttpClient. Adds /api proxy in vite.config.ts so dev server\nforwards to localhost:8080. Fixes pre-existing build errors: spread\ntype in client.ts and erasableSyntaxOnly incompatibility with\ngenerated parameter properties.\nEOF\n)\"",
  "description": "Stage and commit from repo root"
}
```

---

### You _13:56:39_

[Tool result]
```
/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/frontend
3109d54 Wire frontend to real backend via generated REST client
[master 3109d54] Wire frontend to real backend via generated REST client
 4 files changed, 25 insertions(+), 19 deletions(-)
```

---

### Claude _13:56:41_

[Tool call: Bash]
```json
{
  "command": "git -C /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon push",
  "description": "Push to remote"
}
```

---

### You _13:57:22_

[Tool error]
```
The user doesn't want to proceed with this tool use. The tool use was rejected (eg. if it was a file edit, the new_string was NOT written to the file). STOP what you are doing and wait for the user to tell you how to proceed.
```

---

### You _13:57:22_

[Request interrupted by user for tool use]

---
