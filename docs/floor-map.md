# Floor Map

## Source

Floor maps are parsed at runtime from `.har` files in `input-data/` via the HAR parser pipeline.
See [office-layout-parsing.md](office-layout-parsing.md) for how the pipeline works and how to add a new floor.

Pre-exported snapshots (for reference): `floor-map-2026-06-08T11-36-36.json` (London), `floor-map-2026-06-08T11-36-38.json` (Manchester), `floor-map-2026-06-08T11-36-40.json` (Cardiff).

Top-level keys: `floor`, `landmarks`, `neighborhoods`, `spaces`, `unavailableSpaces`, `walls`

Desks are at `.spaces.desks` — an array of objects:
```json
{
  "id": "01G6X1AXT33V1KY943F8CXP442",
  "name": "UK-LDN-LFW-5-100A",
  "neighborhood": "Tech South C",
  "x": 453.16,
  "y": 2883.9,
  "rotation": 90.0
}
```

`neighborhood` is nullable — some desks have `null`.

## Stats

- **191 desks** total
- **15 neighborhoods** (plus null)

Neighborhoods: `Banking`, `Desk Pods`, `EMBER`, `Facilities`, `Red Team`, `Tech Central`, `Tech North A`, `Tech North B`, `Tech North C`, `Tech South A`, `Tech South B`, `Tech South B2`, `Tech South C`, `Technology - Priority`, `Workplace Technology`

## Coordinate system

- Origin is top-left
- x range: approximately 0–3500
- y range: approximately 0–4000
- Units are arbitrary (SVG coordinate space from the original floor plan)
- `rotation` is in degrees — relevant for rendering desk orientation, not for distance calculations

## Window / perimeter desk detection

The office is described as a square with windows all around. Perimeter desks = desks near the min/max bounds of the x and y coordinate ranges.

Detection approach: compute bounding box of all desk coordinates, apply a margin (e.g. 10% of range), flag any desk within that margin as a window desk. Tune the margin until the set looks right visually.

## Distance between desks

Use Euclidean distance on (x, y) coordinates:
```
distance(a, b) = sqrt((a.x - b.x)² + (a.y - b.y)²)
```

This is the distance metric used in the QAP objective. See [algorithm.md](algorithm.md).

## Floor map rendering (frontend)

The frontend renders desks as positioned elements using x/y from this JSON — it does **not** use the SVG file. The coordinate range requires zoom and pan. See [frontend.md](frontend.md) for renderer details.
