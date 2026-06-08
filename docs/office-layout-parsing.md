# Office Layout Parsing

Describes the pipeline that converts a Kadence browser HAR capture into a typed `FloorMap` Java object, and how to run it against any office floor.

## Overview

Kadence's desk booking UI loads two things when you open a floor view: an SVG floor plan and a JSON desk metadata API. Both are captured in a single HAR file. The pipeline extracts and merges them.

```
.har file
  ├── SVG (static.onkadence.co/…/floor-plan/….svg)
  │     └── desk positions + wall polygons + landmarks
  └── Desk API (api.onkadence.co/v1/floors/{id}/past-future-spaces)
        └── desk names + neighborhoods
              ↓ merged by ULID
          FloorMap (Java object / JSON output)
```

---

## How to capture a HAR

1. Open Chrome DevTools → Network tab
2. Navigate to the Kadence floor view for the target office/floor
3. Export as HAR (right-click in Network → "Save all as HAR")
4. Drop the file in `input-data/`

One HAR = one floor. Each floor requires a separate capture.

---

## Pipeline classes

### `HarParser` (`parser/HarParser.java`)

Reads the HAR (which is just JSON) with Jackson and does two passes over `.log.entries[]`.

**Pass 1 — collect raw data:**

| What | URL pattern | Field extracted |
|---|---|---|
| SVG text | url contains `.svg` | `response.content.text` |
| Desk API JSON | url contains `past-future-spaces` | `response.content.text` (first non-empty) |
| Floor ID | url contains `past-future-spaces` | regex `/floors/([A-Z0-9]+)/past-future-spaces` |
| Building ID | url contains `v1/floors?` | query param `building.id=` |
| Floors list | url contains `v1/floors?` | full response body |
| Buildings list | url contains `v1/buildings?` | full response body |

Some HAR entries for `past-future-spaces` are empty (size 0 — preflight or cached). The parser skips these and takes the first entry with non-empty content.

**Pass 2 — resolve names:**

Uses the floor ID (from pass 1) to find the matching floor name in the floors list response, and the building ID to find the building name in the buildings list response. This two-pass approach is necessary because the floors list entry appears before `past-future-spaces` in HAR entry order.

Output: `HarExtract { svgText, apiJsonText, floorId, floorName, buildingName }`

---

### `SvgParser` (`parser/SvgParser.java`)

Parses the SVG string using JAXP `DocumentBuilder` (no external dependencies). Builds a parent map (child Element → parent Element) before walking, needed for landmark extraction.

**Desk / pod elements:**

```xml
<g id="space::desk::01G6X1AXT33V1KY943F8CXP442"
   transform="translate(453.16, 2883.90) rotate(90) translate(-453.16, -2883.90) translate(424.16, 2839.90)">
```

- ID prefix `space::desk::` → desk; `space::pod::` → pod
- ULID = everything after the second `::`
- Position = **first** `translate(x, y)` in the transform string (desk centre)
- Rotation = **first** `rotate(angle)` value

**Walls:**

```xml
<g id="walls" transform="translate(342, 337)">
  <polygon id="Path-36" points="487.43 0 3093.9 0 …"/>
```

The walls group has a translate offset that is applied to all child polygon coordinates. Points are space-separated alternating x/y numbers (scientific notation is handled by `Double.parseDouble`).

**Landmarks:**

Landmark type IDs (`KITCHEN`, `ELEVATOR`, `RECEPTION`, `RESTROOM`, `STAIRS`, `WORKPLACETECHBAR`) appear on `<path>` child elements. The position is taken from the **parent `<g>`'s** first translate.

```xml
<g id="layer2" transform="translate(1633.16, 746.90)">
  <path … id="KITCHEN" …/>
</g>
```

Multiple instances of the same landmark type are all captured (e.g. multiple kitchens on one floor).

**Unavailable spaces:**

`<g>` elements whose `id` starts with `unavailable` (case-insensitive). Same transform parsing as desks.

**ViewBox:**

Extracted from the root `<svg viewBox="0 0 W H">` attribute.

---

### `FloorMapParser` (`parser/FloorMapParser.java`)

Orchestrates the two parsers and merges by ULID:

```
SVG desk  {id, x, y, rotation}
API desk  {id, name, neighborhood, type}
    ↓ joined on id (ULID)
Desk      {id, name, neighborhood, x, y, rotation}
```

Desks present in the SVG but absent from the API are dropped (typically 10–20 per floor — spaces that exist visually but are not bookable). Floor metadata (id, name, building, viewBox) comes entirely from `HarParser`.

---

## Output: `FloorMap`

```json
{
  "floor": {
    "id": "01G6X15J9J81YY4RJRDCHWR27V",
    "name": "5th Floor",
    "building": "London Fruit & Wool Exchange",
    "viewBox": { "width": 6736.0, "height": 4290.0 }
  },
  "neighborhoods": ["Banking", "Tech South C", …],
  "spaces": {
    "desks": [{ "id": "…", "name": "UK-LDN-LFW-5-100A", "neighborhood": "Tech South C", "x": 453.16, "y": 2883.9, "rotation": 90.0 }],
    "pods":  [{ … }]
  },
  "walls": [{ "id": "Path-36", "points": [[x,y], …] }],
  "landmarks": [{ "type": "KITCHEN", "x": 1633.16, "y": 746.90 }],
  "unavailableSpaces": [{ "x": …, "y": …, "rotation": … }]
}
```

Coordinate system: raw SVG pixels from the viewBox. Origin is top-left. No further transformation is applied.

---

## Coordinate system

| Field | Notes |
|---|---|
| Origin | Top-left |
| Units | SVG pixels (no real-world scale) |
| x range | Varies by floor (London 5th: 0–6736) |
| y range | Varies by floor (London 5th: 0–4290) |
| rotation | Degrees; 0 = up, 90 = right |

Use Euclidean distance for desk-to-desk proximity — see [algorithm.md](algorithm.md).

---

## Export endpoint

`POST /api/floor-map/export?har=<filename>`

Parses the named HAR from `input-data/` and writes a timestamped JSON to `input-data/floor-map-<yyyy-MM-dd'T'HH-mm-ss>.json`. Returns a summary:

```json
{ "file": "input-data/floor-map-2026-06-08T11-36-36.json", "floor": "5th Floor",
  "building": "London Fruit & Wool Exchange", "desks": 191, "pods": 8, "walls": 16, "landmarks": 9 }
```

If `?har=` is omitted, uses the HAR loaded at startup (`kadence-london.har`).

---

## Known floors

| File | Building | Floor | Desks |
|---|---|---|---|
| `kadence-london.har` | London Fruit & Wool Exchange | 5th Floor | 191 |
| `manchester-floor-5.har` | Manchester Landmark | 5th Floor | 160 |
| `cardiff-14.har` | Cardiff Brunel House | 14th Floor | 142 |
