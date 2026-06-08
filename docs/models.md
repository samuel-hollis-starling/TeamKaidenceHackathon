# Models

All model classes live in `com.starlingbank.model`. Jackson serialisation: no-arg constructor + getters. No Lombok.

## Core data models

### `Desk`
Represents a physical desk on the floor.
```
id           String   — unique desk ID (ULID format)
name         String   — human-readable code, e.g. "UK-LDN-LFW-5-100A"
neighborhood String?  — area name, nullable (e.g. "Tech South C", "Banking")
x            double   — x coordinate on floor plan (~0–3500 range)
y            double   — y coordinate on floor plan (~0–4000 range)
rotation     double   — desk orientation in degrees
```

### `Employee`
Personal data for a person. Deliberately separated from org tree structure.
```
id       String  — matches keys in orgchart.json (e.g. "0_David_Sproul")
name     String
role     String
location String  — e.g. "London", "Remote - United Kingdom"
```

### `OrgNode`
Tree position of an employee. Kept separate from `Employee` to avoid coupling personal data with org structure.
```
employeeId   String        — FK to Employee.id
parentId     String?       — null for root (CEO)
childrenIds  List<String>  — direct report IDs
depth        int           — 0 = root
orgPath      List<String>  — IDs from root → this node (inclusive)
```

### `Office`
Combined static input for the assignment algorithm. Not exposed as an API response.
```
desks          List<Desk>
employeesById  Map<String, Employee>   — keyed by Employee.id
orgById        Map<String, OrgNode>    — keyed by employeeId
```

### `SocialPreference` (enum)
```
TALK_TO_ME       — seat near other sociable people
DONT_TALK_TO_ME  — prefer isolation
NONE             — no preference
```

## API request/response models

### `BookingRequest`
What a user submits when booking a desk for the day.
```
employeeId        String
socialPreference  SocialPreference
feelingLucky      boolean  — stubbed; sit next to highest-ranking person present
```

### `BookingCollection`
Response for `GET /api/bookings`.
```
bookings       List<BookingRequest>
totalCapacity  int  — always 191
remaining      int  — bookings remaining before office is full
```

### `AssignmentCollection`
Output of the assignment algorithm. Both maps are provided so the floor map (desk-first iteration) and the booking form (employee-first lookup) can both query efficiently.
```
deskByEmployeeId  Map<String, String>  — employeeId → deskId
employeeByDeskId  Map<String, String>  — deskId → employeeId
```

### `AssignmentScore`
Scoring metrics for the current assignment. All values 0–100. See [algorithm.md](algorithm.md) for definitions.
```
totalQapCost        double  — normalised QAP objective (lower = better assignment)
teamCohesion        double
managerProximity    double
socialSatisfaction  double
windowHitRate       double
```
