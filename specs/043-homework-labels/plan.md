# Implementation Plan: Homework Labels

**Branch**: `043-homework-labels` | **Date**: 2026-08-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/043-homework-labels/spec.md`

## Summary

The admin Homework tab (Tareas) is too large to scan. Add one optional short **label** (`VARCHAR(40)`, nullable) on `homework_assignments`. The teacher sets it when creating or editing a homework (type a new value or pick one already in use). The tab lists a badge per labeled item and adds a label filter beside the existing type and level filters. Filtering and reuse grouping ignore capitalization; each row still displays the saved text. No unlabeled filter. Student learning APIs, the review queue, unit picker, and quizzes/presentations stay unchanged. Label-only edits must not bump `content_revised_at` (freeze).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, `NamedParameterJdbcTemplate`, Flyway 11, existing `HomeworkAdminController` / `HomeworkAdminService`. **No new libraries.**
- Frontend: React 19, Shadcn `Select` + `Command`/`Popover` (same pattern as `AddContentCombobox`). **No new libraries.**

**Storage**: PostgreSQL 18 — Flyway `V24__homework_label.sql`: nullable `homework_assignments.label VARCHAR(40)`. Existing rows stay unlabeled (`NULL`).

**Testing**: JUnit (`HomeworkAdminServiceTest`, `HomeworkAdminControllerIntegrationTest`); label-only update must not change `content_revised_at`. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Client-side filter over the existing admin list (dozens of items today; same as type/level). No extra round-trip.

**Constraints**: One optional label per homework; max 40 characters after trim; no labels catalog; teacher-only; student payloads omit `label`; unused labels disappear because they are not stored separately; `VALIDATION_ERROR` on too-long (no silent truncate).

**Scale/Scope**: Single-teacher admin Homework tab; other surfaces ignore the new field.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. One nullable column on the existing assignment row. No `labels` table, no many-to-many, no dedicated list endpoint, no colors, no unlabeled filter. Filter/reuse derived from `GET /api/v1/admin/homework` the same way type/level already are.
- **II. Component-Driven UI** — PASS. Named pieces: label field on `HomeworkEditorPage` (Command combobox), label `Select` + badge on `HomeworkAdminList`. No raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Persist/read in `ContentRepository` + admin DTOs; UI reads `HomeworkAdminItem.label`. Student `lib/learning.ts` unchanged.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `043-homework-labels`.

**Post-design re-check**: PASS — contract extends existing admin homework JSON only; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/043-homework-labels/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── homework-labels-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V24__homework_label.sql                    # NEW

back-end/src/main/java/com/kuky/backend/
├── learning/model/HomeworkAssignment.java     # EDIT: label
├── learning/repository/ContentRepository.java # EDIT: INSERT/UPDATE/SELECT
├── admin/dto/HomeworkAdminItem.java           # EDIT: label
├── admin/dto/CreateHomeworkRequest.java       # EDIT: label
├── admin/dto/UpdateHomeworkRequest.java       # EDIT: label
└── admin/service/HomeworkAdminService.java    # EDIT: normalize, persist, omit from freeze bump

back-end/src/test/java/com/kuky/backend/admin/
├── HomeworkAdminServiceTest.java
└── HomeworkAdminControllerIntegrationTest.java

front-end/src/
├── lib/admin.ts                               # EDIT: label on item + create/update bodies
├── i18n/locales/{es,en,ro}.ts                 # EDIT: editor + filter copy
└── components/admin/homework/
    ├── HomeworkEditorPage.tsx                 # EDIT: combobox field
    └── HomeworkAdminList.tsx                  # EDIT: filter + badge; reset stale filter
```

**Structure Decision**: Existing full-stack layout. Admin-only field; student learning DTOs and unit/quiz/presentation code are out of scope unless a mapper would otherwise fail to compile (it should not).

## Complexity Tracking

> No constitution violations requiring justification.
