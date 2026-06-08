# Identify unconnected backend code pieces

**Project:** TeamKaidenceHackathon  
**Session:** `17353605-3a03-4a78-87e0-ecb80a735ab1`  
**Date:** 2026-06-08  

---

### You _13:17:37_

explore the codebase – other than the frontend, any unconnected pieces of code in the backend? multiple devs are working on this, so we're wiring things up

---

### Claude _13:17:44_

[Tool call: Agent]
```json
{
  "description": "Explore backend for unwired/disconnected code",
  "subagent_type": "Explore",
  "prompt": "Explore the Java backend in /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon to find pieces of code that exist but aren't wired up or connected yet. Focus on:\n\n1. Services/classes that are implemented but not bound in AppModule.java or not injected anywhere\n2. Resource classes (Jersey endpoints) that exist but aren't registered in Main.java\n3. Interfaces that have implementations not bound in Guice\n4. Any stub/TODO methods in resource classes\n5. Any models or utilities that are defined but not used\n\nKey files to check: AppModule.java, Main.java, and all *Resource.java files, *Service*.java files, *ServiceImpl.java files.\n\nReport: for each disconnected piece, what it is, where it lives, and what it would need to be wired to. Be specific with file paths and class names."
}
```

---

### You _13:19:03_

[Tool result]
```
Excellent! Now I have a complete picture. Let me compile my findings:

## Summary of Findings

Based on my thorough exploration of the Java backend in `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon`, here are the disconnected pieces of code that exist but aren't fully wired up or are unused:

### **1. MISSING BINDING: AssignmentState**

**What it is:** A singleton class that stores the last computed assignment result.

**Where it lives:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/service/AssignmentState.java`

**Current status:** 
- NOT bound in `AppModule.java` 
- IS manually retrieved in `Main.java` via `injector.getInstance(AssignmentState.class)` (line 41)
- IS used by `AssignmentResource.java` (injected as constructor parameter)

**What it needs:** Should be explicitly bound in AppModule.java with:
```java
bind(AssignmentState.class).toInstance(new AssignmentState());
// OR
bind(AssignmentState.class).in(Singleton.class);
```

Currently, Guice will auto-instantiate it because it's marked `@Singleton`, but explicit binding is better practice.

---

### **2. UNUSED MODEL CLASS: Office**

**What it is:** A data model class designed to aggregate desks, employees, and organizational nodes.

**Where it lives:** `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/model/Office.java`

**Current status:** 
- Defined with constructor and getters
- NOT imported or instantiated anywhere in the codebase
- No resources expose it as an endpoint

**What it would need to be wired:** Either:
1. Delete it if not needed, OR
2. Create a new resource endpoint (e.g., `OfficeResource`) that returns `Office` data and register it in `Main.java`

---

### **3. HELPER CLASSES - Properly Wired But Not as DI Services**

These are utility/parser classes that are NOT bound in AppModule but are instantiated directly as needed. This is appropriate for their use case:

- **`HarParser`** - `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/parser/HarParser.java`
  - Instantiated directly in `FloorMapParser` (no DI needed)
  
- **`SvgParser`** - `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/parser/SvgParser.java`
  - Instantiated directly in `FloorMapParser` (no DI needed)
  
- **`FloorMapParser`** - `/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/parser/FloorMapParser.java`
  - Instantiated directly in `FloorMapServiceImpl` and `FloorMapResource` (no DI needed)

Status: These are intentionally NOT DI-managed since they're stateless utilities used for one-time parsing.

---

### **Complete Wiring Status Summary**

**All Services & Implementations - PROPERLY WIRED:**
- ✅ `HelloService` → `HelloServiceImpl` (bound in AppModule, registered in Main)
- ✅ `FloorMapService` → `FloorMapServiceImpl` (bound in AppModule, registered in Main)
- ✅ `OrgChartService` → `OrgChartServiceImpl` (bound in AppModule, registered in Main)
- ✅ `AssignmentService` → `SimulatedAnnealingAssignmentService` (bound in AppModule, registered in Main)
- ✅ `ScoringService` → `ScoringServiceImpl` (bound in AppModule, registered in Main)
- ✅ `BookingService` → `BookingServiceImpl` (bound in AppModule, registered in Main)

**All Resource Endpoints - PROPERLY REGISTERED:**
- ✅ `HelloResource` (registered in Main.java line 24)
- ✅ `DeskResource` (registered in Main.java line 27)
- ✅ `EmployeeResource` (registered in Main.java line 28)
- ✅ `BookingResource` (registered in Main.java line 29)
- ✅ `AssignmentResource` (registered in Main.java line 30)
- ✅ `FloorMapResource` (registered in Main.java line 31)
- ✅ `CorsFilter` (registered in Main.java line 26)

**All Model Classes - PROPERLY USED:**
- ✅ All models (Desk, Employee, BookingRequest, AssignmentCollection, etc.) are used in service responses or as request bodies

---

### **Recommendation**

The only genuine issue is the **missing AppModule binding for `AssignmentState`**. While it currently works (Guice auto-instantiates `@Singleton` classes), explicitly binding it would follow dependency injection best practices and make the dependencies clearer.

The **`Office` class** is the only unused artifact—either remove it or create an endpoint to expose it.
```

---

### Claude _13:19:10_

Let me quickly verify the `Office` model to see what's in it.

---

### Claude _13:19:10_

[Tool call: Bash]
```json
{
  "command": "rg -l \"Office\" /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src --include=\"*.java\"",
  "description": "Find all Java files referencing Office"
}
```

---

### You _13:19:10_

[Tool error]
```
Exit code 2
rg: unrecognized flag --include

similar flags that are available: --include-zero
```

---

### Claude _13:19:12_

[Tool call: Bash]
```json
{
  "command": "rg -l \"Office\" /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src -g \"*.java\"",
  "description": "Find all Java files referencing Office"
}
```

---

### Claude _13:19:13_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/model/Office.java"
}
```

---

### You _13:19:13_

[Tool result]
```
1	package com.starlingbank.model;
2	
3	import java.util.List;
4	import java.util.Map;
5	
6	public class Office {
7	    private List<Desk> desks;
8	    private Map<String, Employee> employeesById;
9	    private Map<String, OrgNode> orgById;
10	
11	    public Office() {}
12	
13	    public Office(List<Desk> desks, Map<String, Employee> employeesById, Map<String, OrgNode> orgById) {
14	        this.desks = desks;
15	        this.employeesById = employeesById;
16	        this.orgById = orgById;
17	    }
18	
19	    public List<Desk> getDesks() { return desks; }
20	    public Map<String, Employee> getEmployeesById() { return employeesById; }
21	    public Map<String, OrgNode> getOrgById() { return orgById; }
22	}
23
```

---

### You _13:19:13_

[Tool result]
```
/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/model/Office.java
```

---

### Claude _13:19:19_

The backend is in pretty good shape overall. Here's the summary:

**Only `Office.java` is a dead end** — it's a model class that aggregates desks, employees, and org nodes, but nothing references it anywhere. It was probably started as an aggregate DTO for an endpoint that never got built.

Everything else is properly wired:
- All 6 services are bound in `AppModule` and registered in `Main.java`
- All 5 resource classes (`EmployeeResource`, `BookingResource`, `AssignmentResource`, etc.) are registered
- `AssignmentState` technically works via Guice's auto-instantiation even without an explicit binding

**The `Office` model looks intentional** — it bundles `List<Desk>`, `Map<String, Employee>`, and `Map<String, OrgNode>` into one payload. If someone was planning a "GET /api/office" convenience endpoint that returns everything the frontend needs in one call, this is the model they'd use. Worth asking the team if that was planned.

---

### You _13:20:52_

is the get desks endpoint enough to render desks on the frontend?

---

### You _13:20:56_

ok kill office.java

---

### Claude _13:20:58_

[Tool call: Bash]
```json
{
  "command": "rm /Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/src/main/java/com/starlingbank/model/Office.java",
  "description": "Delete Office.java"
}
```

---

### You _13:20:58_

[Tool result]
```
(Bash completed with no output)
```

---

### Claude _13:21:00_

Done.

---
