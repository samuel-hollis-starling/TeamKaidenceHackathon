# API

Base URL: `http://localhost:8080`. All endpoints produce/consume `application/json`.

## Endpoints

### Desks
| Method | Path | Response | Status |
|---|---|---|---|
| `GET` | `/api/desks` | `Desk[]` | Stub (returns `[]`) |

### Employees
| Method | Path | Response | Status |
|---|---|---|---|
| `GET` | `/api/employees` | `Employee[]` | Stub (returns `[]`) |

### Bookings
| Method | Path | Body | Response | Status |
|---|---|---|---|---|
| `POST` | `/api/bookings` | `BookingRequest` | `BookingRequest` | Stub (echoes input) |
| `GET` | `/api/bookings` | — | `BookingCollection` | Stub (empty, capacity=191) |

### Assignments
| Method | Path | Response | Status |
|---|---|---|---|
| `POST` | `/api/assignments/run` | `AssignmentCollection` | Stub (empty maps) |
| `GET` | `/api/assignments` | `AssignmentCollection` | Stub (empty maps) |
| `GET` | `/api/assignments/score` | `AssignmentScore` | Stub (hardcoded plausible values) |

## TypeScript client

The TypeScript client and all type interfaces are generated from the Jersey resource classes. **Do not hand-edit `frontend/src/generated/api.ts`** — it is overwritten on every generation run.

To regenerate after adding/changing a Java model or endpoint:
```sh
./gradlew generateTypeScript
```

Output: `frontend/src/generated/api.ts`

Usage in React:
```ts
import { DeskResourceApi, FetchHttpClient } from '../generated/api'
import { FetchHttpClient } from '../api/client'

const client = new DeskResourceApi(new FetchHttpClient())
const desks = await client.getDesks()
```

When adding a new Jersey resource, also add it to `classes` in `build.gradle.kts`:
```kotlin
tasks.generateTypeScript {
    classes = mutableListOf(
        "com.starlingbank.api.YourNewResource",
        // ...
    )
}
```

## CORS

All origins are permitted (`Access-Control-Allow-Origin: *`). The Vite dev server at `localhost:5173` can call the backend at `localhost:8080` directly with no proxy needed.
