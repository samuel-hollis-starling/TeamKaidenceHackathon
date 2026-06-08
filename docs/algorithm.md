# Assignment Algorithm & Scoring

## Problem formulation

This is a **Quadratic Assignment Problem (QAP)**:

> Assign N employees to N desks to minimise total weighted distance between all pairs.

```
minimise  Σ_{i≠j} weight(i, j) × distance(desk(i), desk(j))
```

Where:
- `weight(i, j)` = how strongly employees i and j should sit near each other
- `distance(desk_a, desk_b)` = Euclidean distance between desk coordinates
- Negative weight = the pair should be pushed *apart*

## Weight function

```
weight(i, j) = team_weight(i, j) + social_modifier(i, j)
```

### `team_weight`
```
1.0 / tree_distance(i, j)
```
See [org-chart.md](org-chart.md) for tree distance calculation. Employees with no meaningful org relationship (tree_distance very large) contribute negligible weight.

### `social_modifier`
| i preference | j preference | modifier |
|---|---|---|
| TALK_TO_ME | TALK_TO_ME | +bonus (e.g. +0.5) |
| DONT_TALK_TO_ME | any | −penalty (e.g. −1.0, push apart) |
| any | DONT_TALK_TO_ME | −penalty |
| NONE | any | 0 |

The penalty for `DONT_TALK_TO_ME` effectively inverts the pair's pull — they should be far from others.

### `feeling_lucky`
**Stubbed** — interface is defined but the modifier is always 0. When implemented: add strong positive weight between the lucky employee and the highest-ranking person present.

## Algorithm implementations

### `AssignmentServiceImpl` (current: stub)
Random shuffle — assigns desks to employees in random order. Used as the baseline for score comparison.

### Future: local search / simulated annealing
Standard approach for QAP:
1. Start from random assignment
2. Repeatedly try swapping two employees' desks
3. Accept swap if it reduces total cost (or with probability e^(-Δ/T) for SA)
4. Return best assignment found

The interface (`AssignmentService`) is fixed — only the implementation changes.

## Scoring heuristics

All metrics are 0–100. Returned as `AssignmentScore`.

### `totalQapCost`
The raw QAP objective normalised to 0–100. Compute actual cost, then normalise against a worst-case baseline (e.g. cost of random assignment). Lower = better.

### `teamCohesion`
For each employee, find their nearest teammate (same manager). Average those minimum distances. Normalise: 0 = everyone right next to a teammate, 100 = max possible separation. Invert so higher = better.

### `managerProximity`
For each employee whose manager is also booked in: compute distance to manager. Average across all such pairs. Normalise and invert so higher = better.

### `socialSatisfaction`
Count pairs where the social preference was correctly honoured:
- TALK_TO_ME employee seated within N desks of another TALK_TO_ME employee ✓
- DONT_TALK_TO_ME employee has no immediate neighbours ✓
Return as a percentage.

## Current stub

`ScoringServiceImpl` returns hardcoded values: `{72, 58, 61, 80, 65}`. Replace with real computation when implementing Dev 3's work.
