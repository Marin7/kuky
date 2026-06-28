# Implementation Plan: Class Units (Packages)

**Branch**: `010-class-units` | **Date**: 2026-06-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/010-class-units/spec.md`

## Summary

Replace the teacher Panel's separate **Homework** and **Presentations** tabs with a
single **Units** tab. A *unit* is a class package identified by a CEFR level (A1–C2),
a free-text subject ("Family", "Food"…), and an explicit position within its level.
Each presentation and homework belongs to at most one unit. Assigning a unit to a
student grants access to **all** the unit's presentations (dynamically, including
later additions) but **never** its homeworks — homeworks remain individually
assigned per student, exactly as today.

Technical approach: add a `units` table plus a nullable `unit_id` foreign key on the
existing `presentations` and `homework_assignments` tables (one-to-many, since an
item lives in one unit). Add a `unit_assignments` join table. Student presentation
access becomes the UNION of legacy per-student `presentation_shares` rows and rows
derived from `unit_assignments`. Homework access is unchanged (`homework_targets`
only). The front-end consolidates the two admin lists into a unit-grouped view and
groups student presentations by unit in the learning area.

## Technical Context

**Language/Version**: TypeScript 5 (strict), Java 21

**Primary Dependencies**: Front-end — React 19, TanStack Start/Router, TailwindCSS 4,
Shadcn UI, Vite 7. Back-end — Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate
(plain JDBC), Flyway 11.

**Storage**: PostgreSQL 18. New tables `units`, `unit_assignments`; new nullable
`unit_id` columns on `presentations` and `homework_assignments`.

**Testing**: Back-end `./gradlew test` (JUnit). Front-end `npm run lint` + manual
browser verification per the constitution.

**Target Platform**: SSR web app (front-end on :8080, REST API on :8081).

**Project Type**: Web application (separate `front-end/` and `back-end/`).

**Performance Goals**: Interactive admin CRUD; no special throughput targets. Unit
list and student learning overview must load within standard web expectations
(< 1s perceived).

**Constraints**: Reuse existing auth model (`auth-token` cookie, ADMIN/STUDENT roles).
All API calls use `credentials: 'include'`. Error responses follow the
`{"error":"CODE","message":"..."}` shape mapped in `GlobalExceptionHandler`.

**Scale/Scope**: Single teacher, tens of students, low hundreds of presentations and
homeworks. One new admin tab, one migration, ~3 new endpoint groups.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First** — PASS. Reuses existing `presentations`, `homework_assignments`,
  `presentation_shares`, and `homework_targets`. Adds the minimum: one entity table,
  one join table, two FK columns. No new abstraction layers; access stays in plain
  JDBC repositories. The presentation-access change is a single SQL UNION, not a new
  permission framework.
- **II. Component-Driven UI** — PASS. The Units tab is built from named React
  components (`UnitsTab`, `UnitCard`, `UnitContentPicker`, `UnitAssignDialog`),
  reusing existing Shadcn primitives. No raw DOM work.
- **III. Evolution-Ready Architecture** — PASS. All data fetching lives in
  `front-end/src/lib/admin.ts` and `learning.ts` service modules, never inlined in
  components, consistent with the existing pattern.

No violations → Complexity Tracking table omitted.

## Project Structure

### Documentation (this feature)

```text
specs/010-class-units/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── api.md           # Phase 1 output — REST contract for units
├── checklists/
│   └── requirements.md  # Spec quality checklist (from /speckit-specify)
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/
├── units/                              # NEW bounded context for units
│   ├── controller/UnitAdminController.java
│   ├── service/UnitService.java
│   ├── repository/UnitRepository.java
│   ├── model/Unit.java
│   ├── dto/ (UnitSummary, UnitDetail, CreateUnitRequest, UpdateUnitRequest,
│   │         ReorderUnitsRequest, SetUnitContentRequest, SetUnitAssigneesRequest)
│   └── exception/UnitNotFoundException.java
├── presentations/repository/PresentationRepository.java   # access UNION + unit_id
├── learning/service/LearningService.java                  # group by unit
├── learning/repository/                                   # unit-derived access
└── admin/...                                              # existing homework/student

back-end/src/main/resources/db/migration/
└── V18__create_units.sql               # NEW: units, unit_assignments, unit_id FKs

front-end/src/
├── components/admin/
│   ├── AdminPanel.tsx                  # tabs: replace homework+presentations → units
│   └── units/                          # NEW
│       ├── UnitsTab.tsx
│       ├── UnitCard.tsx
│       ├── UnitContentPicker.tsx       # attach/detach presentations & homeworks
│       └── UnitAssignDialog.tsx        # assign unit to students
├── components/learning/
│   └── SharedPresentationsList.tsx     # group presentations by unit
└── lib/
    ├── admin.ts                        # NEW unit API functions
    └── learning.ts                     # unit grouping in overview type
```

**Structure Decision**: Web application with the existing two-package layout. A new
`units` bounded context is added on the back-end mirroring the existing
`presentations` package structure (controller/service/repository/model/dto). The
front-end adds a `components/admin/units/` folder and folds the unit API into the
existing `lib/admin.ts` service module. Existing homework and presentation
controllers/services are reused; only access-resolution queries and the admin UI
shell change.
