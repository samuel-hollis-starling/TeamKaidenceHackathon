# Run smoke test

**Project:** TeamKaidenceHackathon  
**Session:** `9a0fc88c-8c7d-4490-bc9a-ab1af90c3682`  
**Date:** 2026-06-08  

---

### You _14:18:04_

run smoke test

---

### You _14:18:33_

run smoke test

---

### Claude _14:18:35_

Looking for the smoke test script.

---

### Claude _14:18:35_

[Tool call: Bash]
```json
{
  "command": "fd smoke test . --type f 2>/dev/null || fd -t f smoke . 2>/dev/null || rg -l \"smoke\" . --max-depth 3",
  "description": "Find smoke test script"
}
```

---

### You _14:18:37_

[Tool result]
```
./smoke_test.py
```

---

### Claude _14:18:40_

[Tool call: Read]
```json
{
  "file_path": "/Users/kapilan.cholan1/othergit/TeamKaidenceHackathon/smoke_test.py"
}
```

---

### You _14:18:40_

[Tool result]
```
1	#!/usr/bin/env python3
2	"""
3	Smoke test: load attendees, book them all, run assignment, print scores.
4	Usage: python3 smoke_test.py [--base-url http://localhost:8080]
5	"""
6	
7	import argparse
8	import json
9	import sys
10	import time
11	import urllib.request
12	import urllib.error
13	from pathlib import Path
14	
15	BASE_URL = "http://localhost:8080"
16	ATTENDEES_FILE = Path(__file__).parent / "input-data" / "har-attendees-2026-06-03.json"
17	
18	
19	def post(url: str, body: dict) -> dict:
20	    data = json.dumps(body).encode()
21	    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
22	    with urllib.request.urlopen(req) as resp:
23	        return json.loads(resp.read())
24	
25	
26	def get(url: str) -> dict:
27	    with urllib.request.urlopen(url) as resp:
28	        return json.loads(resp.read())
29	
30	
31	def main():
32	    parser = argparse.ArgumentParser()
33	    parser.add_argument("--base-url", default=BASE_URL)
34	    args = parser.parse_args()
35	    base = args.base_url.rstrip("/")
36	
37	    attendees = json.loads(ATTENDEES_FILE.read_text())
38	    print(f"Loaded {len(attendees)} attendees from {ATTENDEES_FILE.name}")
39	
40	    # --- 1. POST bookings ---
41	    print(f"\nPOSTing bookings to {base}/api/bookings ...")
42	    failures = []
43	    for i, person in enumerate(attendees, 1):
44	        payload = {
45	            "employeeId": person["id"],
46	            "socialPreference": "NONE",
47	            "windowSeat": False,
48	            "feelingLucky": False,
49	        }
50	        try:
51	            post(f"{base}/api/bookings", payload)
52	            print(f"  [{i:>2}/{len(attendees)}] {person['name']}")
53	        except urllib.error.URLError as e:
54	            print(f"  [{i:>2}/{len(attendees)}] FAILED {person['name']}: {e}")
55	            failures.append(person["id"])
56	
57	    if failures:
58	        print(f"\nWARN: {len(failures)} bookings failed: {failures}")
59	
60	    # --- 2. GET bookings ---
61	    print(f"\nGET {base}/api/bookings ...")
62	    bookings = get(f"{base}/api/bookings")
63	    booked = bookings.get("bookings", [])
64	    print(f"  Server has {len(booked)} bookings confirmed.")
65	
66	    # --- 3. POST assignments/run ---
67	    print(f"\nPOST {base}/api/assignments/run  (SA — this takes a few seconds) ...")
68	    t0 = time.time()
69	    assignments = post(f"{base}/api/assignments/run", {})
70	    elapsed = time.time() - t0
71	    assigned = assignments.get("deskByEmployeeId", {})
72	    print(f"  Done in {elapsed:.1f}s — {len(assigned)} employees assigned.")
73	
74	    # --- 4. GET assignments/score ---
75	    print(f"\nGET {base}/api/assignments/score ...")
76	    score = get(f"{base}/api/assignments/score")
77	
78	    print("\n" + "=" * 50)
79	    print("  ASSIGNMENT SCORE REPORT")
80	    print("=" * 50)
81	    for key, val in score.items():
82	        label = key.replace("_", " ").title()
83	        if isinstance(val, float):
84	            print(f"  {label:<30} {val:>10.4f}")
85	        else:
86	            print(f"  {label:<30} {val!s:>10}")
87	    print("=" * 50)
88	
89	
90	if __name__ == "__main__":
91	    main()
92
```

---

### Claude _14:18:43_

[Tool call: Bash]
```json
{
  "command": "python3 smoke_test.py",
  "description": "Run smoke test against localhost:8080",
  "timeout": 120000
}
```

---

### You _14:18:52_

[Tool result]
```
Loaded 86 attendees from har-attendees-2026-06-03.json

POSTing bookings to http://localhost:8080/api/bookings ...
  [ 1/86] David Pyhyk
  [ 2/86] James Sawyer
  [ 3/86] David Harvey
  [ 4/86] Yat Cheung
  [ 5/86] Daniel Cosentino
  [ 6/86] Ollie Jamieson
  [ 7/86] Em Pearce
  [ 8/86] Lauren Kenward
  [ 9/86] Toby Hewing
  [10/86] Francesco Malatto
  [11/86] Leyla Azad
  [12/86] Ore Babasina
  [13/86] Jamie Simm
  [14/86] Jacob Rosenskold
  [15/86] Rob Weddell
  [16/86] Fabrice Conchon
  [17/86] Krish Harjani
  [18/86] Ian Riddick
  [19/86] Conor Grocock
  [20/86] Stephan Blakeslee
  [21/86] Maninder Soor
  [22/86] James Barker
  [23/86] Patricia Hudakova
  [24/86] Jimi Lawal
  [25/86] Dan Ashworth
  [26/86] Sophia Georgiou
  [27/86] Louise Tessier
  [28/86] Adam Modzelewski
  [29/86] Nadya Aliakseyeva
  [30/86] Dan Smith
  [31/86] Emily Bate
  [32/86] Joe Jeffries
  [33/86] Kaiman Mehmet
  [34/86] Sheldon White
  [35/86] Bilal Pandor
  [36/86] Becks Simpson
  [37/86] Anna Blesing
  [38/86] Emma Drew
  [39/86] Sara Fakouri
  [40/86] Aaron Tunney
  [41/86] Steven Blyth
  [42/86] Tara Kaul
  [43/86] Chun Ngai Au
  [44/86] Christina Branco
  [45/86] Katy Hemming
  [46/86] Sam Fraser-Barraud
  [47/86] Alex Gray
  [48/86] George Cullinane
  [49/86] James Hunter
  [50/86] Oj Akhigbe
  [51/86] Jenn Philpott
  [52/86] Shaan Master
  [53/86] Jesse Willoughby
  [54/86] Lucie Spurgeon
  [55/86] Erhan Temurkan
  [56/86] Jonathan Knight
  [57/86] Oliver Broughton
  [58/86] Chloe Fong
  [59/86] Nassos Antoniou
  [60/86] Onur Gokcinar
  [61/86] Matthew Salt
  [62/86] Rich Sale
  [63/86] Finley Batchelor
  [64/86] Jonathan Robinson
  [65/86] Mahfuz Murad
  [66/86] Tom Bennett
  [67/86] Eily Lo
  [68/86] Marc Mardare
  [69/86] Maria Walton Campagna
  [70/86] Mark Robson
  [71/86] Marcus Mak
  [72/86] Federico Iaschi
  [73/86] Bill Ahmed
  [74/86] Jacob Turton
  [75/86] Grace Breen
  [76/86] Tim La Roche
  [77/86] Max Jones
  [78/86] Vivien Tang
  [79/86] Nikke San
  [80/86] Gemma Allwright
  [81/86] Alex Maimescu
  [82/86] Darnell Moore
  [83/86] Andrew James Hopkins
  [84/86] Hamish Proudlock
  [85/86] Sophie Wilson
  [86/86] Jordan Payne

GET http://localhost:8080/api/bookings ...
  Server has 86 bookings confirmed.

POST http://localhost:8080/api/assignments/run  (SA — this takes a few seconds) ...
  Done in 4.1s — 86 employees assigned.

GET http://localhost:8080/api/assignments/score ...

==================================================
  ASSIGNMENT SCORE REPORT
==================================================
  Totalqapcost                      41.0151
  Teamcohesion                      72.7838
  Managerproximity                  88.9894
  Socialsatisfaction               100.0000
  Windowhitrate                    100.0000
==================================================
```

---

### Claude _14:18:55_

All 86 bookings posted and confirmed, SA ran in 4.1s, all 86 assigned. Scores look healthy — social satisfaction and window hit rate both at 100%, team cohesion at 72.8%, manager proximity at 88.9%.

---
