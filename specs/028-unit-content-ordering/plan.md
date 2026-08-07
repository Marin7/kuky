# Implementation Plan: Unit Content Ordering

**Branch**: `028-unit-content-ordering` | **Date**: 2026-08-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/028-unit-content-ordering/spec.md`
plus planning note: prefer drag-and-drop for teacher-panel ordering.

## Summary

Let the teacher define one mixed sequence of presentations and homeworks inside a
unit, and show students that sequence as a single interleaved list on the unit
page (accessible items only). Persist a shared `unit_position` on both content
tables; expose admin reorder + ordered `UnitDetail.contents`; seed existing units
as presentations-then-homeworks. Teacher Units UI uses `@dnd-kit` sortable
drag-and-drop (with keyboard support).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway,
  PostgreSQL. **No new backend dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI,
  react-i18next, **plus `@dnd-kit/core`, `@dnd-kit/sortable`, `@dnd-kit/utilities`**
  for teacher content reorder.

**Storage**: PostgreSQL 18 — Flyway `V10__unit_content_position.sql`: add
`unit_position` to `presentations` and `homework_assignments`; seed per-unit
sequences (presentations by `updated_at DESC`, then homeworks by `created_at DESC`).

**Testing**: Backend JUnit — seed positions, reorder permutation validation,
attach appends / detach clears, move-to-other-unit appends. Frontend: browser
verification of DnD reorder, reload persistence, student interleaved unit list
and hidden unassigned homework.

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: N/A — units hold small content sets; one bulk rewrite per
reorder.

**Constraints**: Access rules unchanged; no locked placeholders for unassigned
homeworks; unit-level reorder unchanged; legacy `homework_assignments.sort_order`
not reused; drag-and-drop is the preferred teacher reorder affordance.

**Scale/Scope**: Single-teacher site; one migration; units admin + learning unit
detail; i18n for content-type labels / reorder errors (es/en/ro).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS with noted dependency. Shared
  `unit_position` columns (no join table); reorder mirrors existing unit reorder.
  Adding `@dnd-kit` is justified by explicit product preference for drag-and-drop
  (see Complexity Tracking).
- **II. Component-Driven UI** — PASS. Sortable content list component in admin
  units; student unit page remains composed React components with one interleaved
  list.
- **III. Evolution-Ready Architecture** — PASS. Types/API helpers stay in
  `front-end/src/lib/admin.ts` and `learning.ts`.
- **Technology Stack** — PASS. Existing stack + one well-scoped DnD library.
- **Development Workflow** — PASS. Quickstart lists browser checks; branch
  `028-unit-content-ordering`.

**Post-design re-check**: PASS — contracts extend units admin + learning DTOs;
data model is two columns + seed; Complexity Tracking documents `@dnd-kit` only.

**Result: PASS — justified dependency noted below.**

## Project Structure

### Documentation (this feature)

```text
specs/028-unit-content-ordering/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── unit-content-ordering-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V10__unit_content_position.sql

back-end/src/main/java/com/kuky/backend/units/
├── dto/
│   ├── UnitDetail.java                 # EDIT: contents[] instead of dual lists
│   ├── UnitContentItem.java            # NEW
│   ├── ReorderUnitContentsRequest.java # NEW
│   └── UnitContentRef.java             # NEW (type + id)
├── repository/UnitRepository.java      # EDIT: ordered contents; reorderContents;
│                                       #       setPresentations/Homeworks append positions
├── service/UnitService.java            # EDIT: reorderContents + mapping
└── controller/UnitAdminController.java # EDIT: PUT /{id}/contents/reorder

back-end/.../learning/
├── dto/SharedPresentationSummary.java  # EDIT: + unitPosition
├── dto/HomeworkItemResponse.java       # EDIT: + unitPosition
├── repository/ContentRepository.java   # EDIT: select unit_position
├── repository/PresentationRepository   # EDIT: include unit_position in shared rows
└── service/LearningService.java        # EDIT: map unitPosition

front-end/
├── package.json                        # EDIT: add @dnd-kit/*
├── src/lib/admin.ts                    # EDIT: UnitDetail contents; reorderUnitContents
├── src/lib/learning.ts                 # EDIT: unitPosition on items
├── src/components/admin/units/
│   ├── UnitContentPicker.tsx           # EDIT: single sortable list + DnD
│   └── UnitContentSortableList.tsx     # NEW (optional extract)
├── src/components/learning/
│   ├── UnitDetailContent.tsx           # EDIT: interleaved by unitPosition
│   └── unitGroups.ts                   # EDIT if needed for position passthrough
└── src/i18n/locales/{es,en,ro}.ts      # EDIT: content type / reorder strings
```

**Structure Decision**: Extend the existing `units` package and learning overview
DTOs. Teacher DnD lives only under admin units. Student change is localized to the
unit detail view.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| New frontend deps `@dnd-kit/*` | Product preference: drag-and-drop for teacher content ordering | ▲/▼-only matches other admin lists but was explicitly rejected for this feature |
