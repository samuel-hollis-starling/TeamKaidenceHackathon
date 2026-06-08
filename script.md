# Proximity — Demo Script (~2 min)

---

**[SCENE: AI video — person arrives at the office, desk chaos, megaphones everywhere, shouting across the floor to find teammates]**

Every day, thousands of people book a desk.
And every day, they end up sitting nowhere near the people they actually need.

---

**[CUT TO: clean title card — "Proximity"]**

Proximity fixes that.

Every morning, before work starts, Proximity runs and assigns every person who's booked to a desk — making sure you sit next to the people you work with.

---

**[CUT TO: simple animation — two overlapping matrices, an arrow, a cost dropping]**

Under the hood, seating people optimally is actually a hard mathematics problem. You've got two matrices — people and desks — and you're trying to map them together in a way that minimises a cost function. This is the Quadratic Assignment Problem, and it's NP-hard.

We use Simulated Annealing to find a great solution fast: 400 parallel runs, 200,000 iterations each, and Proximity picks the best.

---

**[CUT TO: ScoreDashboard UI — metrics lighting up]**

We measure success across four metrics:

- Team Cohesion — are teammates sitting near each other?
- Manager Proximity — are reports close to their manager?
- QAP Cost — the raw optimisation score. Lower is better.

---

**[CUT TO: floor map — desks lighting up, clusters forming]**

Here's what it looks like in practice.

[INSERT: 119 random people from Technology booked in. Show floor map before SA runs — scattered. sThen run assignment — clusters appear around team neighbourhoods.]

Now let's take an example closer to home — the Money Engineering division books in together.

[INSERT: money-eng booking, run SA, show tight cluster on floor map, read out scores.]

---

**[CUT TO: side-by-side comparison — Kadence vs Proximity scores]**

How does this compare to Kadence?

We took [INSERT: last Wednesday's real bookings] and simulated the same people through Proximity.

[INSERT: side-by-side score bars — Kadence vs Proximity. Highlight Xx improvement on Team Cohesion and Manager Proximity.]

---

**[CUT TO: title card]**

Proximity.
Keep your teammates close — and your managers closer.

---

### Speaker / editing notes

- Scene 1 AI video: aim for 10–15 seconds, no dialogue, let the chaos speak.
- Metrics section: animate each bar filling in as the voiceover names it.
- Floor map demo: pause ~2s after SA runs so the cluster formation lands visually.
- Final tagline: hold for 3 seconds before fade.
