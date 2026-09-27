# Implementation Plan: Homework Admin Search, Sort & Pagination

**Branch**: `051-homework-admin-search-pagination` | **Date**: 2026-09-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/051-homework-admin-search-pagination/spec.md`

## Summary

The admin panel's Homework tab (`HomeworkAdminList.tsx`) already fetches every homework in one request and filters/sorts it entirely in the browser (by type, level, label, then alphabetically by title) with no pagination. This feature adds a title search box, a full sort control (creation date asc/desc, title asc/desc — defaulting to creation date desc), and 20-per-page pagination, all combined with the existing type/level/label filters. Per the clarified scale (low hundreds of homeworks total) and the existing all-client-side precedent, this stays a front-end-only change: no backend endpoint, DTO, or database changes are needed — the full `HomeworkAdminItem` list already returned by `GET /api/v1/admin/homework` is filtered, sorted, and sliced into pages client-side.

## Technical Context

**Language/Version**: TypeScript 5 (strict mode) — front-end only; no back-end changes in this feature

**Primary Dependencies**: React 19, TanStack Router (file-based), TailwindCSS 4, Shadcn UI (existing `components/ui/pagination.tsx`, `select.tsx`, `input.tsx`)

**Storage**: N/A — reuses the existing `homework_assignments` table and `GET /api/v1/admin/homework` response unchanged; no migrations

**Testing**: No front-end test framework is configured in this repo (no Vitest/Testing Library, no `test` script in `front-end/package.json`) — verification is manual, in a running browser, per the project's Development Workflow constitution rule. Back-end is untouched, so no new integration tests are needed.

**Target Platform**: Web (desktop browser, admin-only route `/panel`)

**Project Type**: Web application (existing `front-end/` + `back-end/` split); this feature only touches `front-end/`

**Performance Goals**: List interactions (search keystroke, sort change, page change) reflect in the UI with no perceptible delay at the clarified scale (low hundreds of homeworks) — see spec SC-004/SC-005

**Constraints**: 20 items per page, fixed (not teacher-configurable, per spec Assumptions); search/sort/pagination must not alter what students see on `/aprendizaje` (FR-007)

**Scale/Scope**: Single teacher's admin panel, expected total homework count in the low hundreds (per Clarifications) — not designed for indefinite growth beyond that

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First**: Pass. No new dependency, backend endpoint, or data layer is introduced. The feature reuses the existing fetch-everything-once pattern and adds filtering/sorting/pagination as pure client-side computation — the smallest change that satisfies every functional requirement at the clarified scale.
- **II. Component-Driven UI**: Pass. Search input, sort select, and pagination controls are built from existing Shadcn primitives (`Input`, `Select`, `Pagination`) as named sub-components, consistent with the rest of `HomeworkAdminList.tsx`.
- **III. Evolution-Ready Architecture**: Pass. Data-fetching stays isolated in `front-end/src/lib/admin.ts`'s `getHomework()`; if the homework count later outgrows client-side handling, only that function and the list's query-building logic need to change to move filtering/sorting/pagination server-side — the component's rendering logic (cards, empty state, actions) is unaffected.

No violations. Complexity Tracking is not needed.

## Project Structure

### Documentation (this feature)

```text
specs/051-homework-admin-search-pagination/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command) — no API changes; documents that fact
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
front-end/
├── src/
│   ├── components/admin/homework/
│   │   ├── HomeworkAdminList.tsx      # MODIFIED: add search/sort/pagination state + rendering
│   │   ├── HomeworkAdminCard.tsx      # UNCHANGED: per-item rendering, reused as-is
│   │   └── HomeworkTab.tsx            # UNCHANGED: container, still renders HomeworkAdminList
│   ├── components/ui/
│   │   ├── pagination.tsx             # UNCHANGED (existing, first real usage)
│   │   ├── input.tsx / select.tsx     # UNCHANGED (existing, reused)
│   ├── hooks/
│   │   └── use-debounced-value.ts     # NEW: small reusable debounce hook for the search input
│   └── lib/
│       └── admin.ts                   # UNCHANGED: getHomework() signature/response stay the same

back-end/
└── (no changes)
```

**Structure Decision**: Front-end-only change inside the existing `front-end/src/components/admin/homework/` module, plus one new small shared hook under `front-end/src/hooks/`. No back-end, database, or API contract changes.

## Complexity Tracking

*No Constitution Check violations — this section is intentionally empty.*
