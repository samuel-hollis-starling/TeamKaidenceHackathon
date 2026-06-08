# Org Chart

## Source file

`input-data/orgchart.json`

A flat JSON object. Each key is an employee ID; each value is:
```json
{
  "id": "0_David_Sproul",
  "parent": null,
  "children": ["1_Darren_Pope", "2_Raman_Bhatia", ...],
  "depth": 0,
  "orgPath": ["0_David_Sproul"],
  "name": "David Sproul",
  "role": "Board",
  "location": "Remote - United Kingdom",
  "org": ""
}
```

- `parent` is `null` for the root node (CEO)
- `children` is an empty list for leaf nodes
- `depth` is 0 at root, increasing downward
- `orgPath` lists all ancestor IDs from root → this node (inclusive)
- 1000+ employees total

## Parsing into models

When loading, split each JSON entry into two model objects:

`Employee`: `id`, `name`, `role`, `location`

`OrgNode`: `employeeId = id`, `parentId = parent`, `childrenIds = children`, `depth`, `orgPath`

The `org` field in the JSON is not currently used.

## Relationship weight between two employees

Used by the assignment algorithm to determine how strongly two people should be seated near each other. Higher weight = stronger pull to be close.

**Tree distance** between nodes A and B:
1. Find their LCA (Lowest Common Ancestor) — the deepest node that appears in both `orgPath` lists
2. `tree_distance = (depth_A - depth_LCA) + (depth_B - depth_LCA)`

**Weight formula**:
```
team_weight(A, B) = 1.0 / tree_distance(A, B)
```

- Direct siblings (same manager): `tree_distance = 2`, weight = `0.5`
- Manager → direct report: `tree_distance = 1`, weight = `1.0`
- Cousins one level up: `tree_distance = 4`, weight = `0.25`

Weights decay quickly with distance, which is the intended behaviour — immediate team matters most.

## LCA algorithm

```
orgPath_A = ["root", "A1", "A2", "A"]
orgPath_B = ["root", "A1", "B1", "B"]

LCA = last common element by walking both paths simultaneously from index 0
    → "A1" (paths diverge after index 1)

depth_LCA = 1
tree_distance = (3 - 1) + (3 - 1) = 4
```

Because `orgPath` is already stored on each node (root → self), LCA lookup is O(depth) with no tree traversal needed.

## Social preference modifiers

Applied on top of `team_weight` in the QAP weight function. See [algorithm.md](algorithm.md).
