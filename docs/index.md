# Docs Index

This folder is a living wiki for the desk-booking hackathon project. The LLM maintains it; devs read it. When you finish a piece of work, update the relevant page.

## Navigation for Claude

Before starting any task, read the pages marked for that area. Do not rely on memory from prior sessions — read the page.

| Task | Read first |
|---|---|
| Adding or changing a model class | [models.md](models.md) |
| Adding or changing an API endpoint | [api.md](api.md), [backend.md](backend.md) |
| Working on the assignment algorithm | [algorithm.md](algorithm.md), [org-chart.md](org-chart.md), [floor-map.md](floor-map.md) |
| Working on scoring heuristics | [algorithm.md](algorithm.md) |
| Working on the floor map renderer | [frontend.md](frontend.md), [floor-map.md](floor-map.md) |
| Working on the booking form or score dashboard | [frontend.md](frontend.md), [api.md](api.md) |
| Loading or parsing input data | [floor-map.md](floor-map.md), [org-chart.md](org-chart.md) |
| Setting up Guice / HK2 wiring | [backend.md](backend.md) |
| Regenerating the TypeScript client | [backend.md](backend.md), [frontend.md](frontend.md) |
| Understanding how the pieces fit together | [architecture.md](architecture.md) |

## Pages

| Page | Summary |
|---|---|
| [architecture.md](architecture.md) | System overview — 3-dev split, data flow, tech stack |
| [models.md](models.md) | All Java model classes with fields and purpose |
| [api.md](api.md) | All REST endpoints, request/response shapes, current stub status |
| [floor-map.md](floor-map.md) | Floor map JSON structure, coordinate system, desk layout, window detection |
| [org-chart.md](org-chart.md) | Org chart JSON structure, tree traversal, relationship weight function |
| [algorithm.md](algorithm.md) | QAP formulation, weight function, assignment algorithm, scoring heuristics |
| [frontend.md](frontend.md) | React app structure, floor map renderer, TypeScript generation workflow |
| [backend.md](backend.md) | Jersey/Guice/Grizzly setup, how to add endpoints, DI wiring |

## How to update

When you make a meaningful change to an aspect of the project, update the corresponding page. Keep pages accurate and concise — the goal is a fast read before starting a task, not comprehensive documentation.
