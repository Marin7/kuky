# Implementation Plan: Student-Based Homework Deadlines

**Branch**: `044-student-homework-deadlines` | **Date**: 2026-08-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/044-student-homework-deadlines/spec.md`

## Summary

Replace the homework-wide optional due date (`homework_assignments.due_on`) with an optional per-student date on `homework_targets.due_on`. Drop the assignment column with no data copy (unused in practice). Students see only their own date; overdue stays derived (`due_on < today` in the teacher zone and status `PENDING`). Teacher sets a convenience date when assigning (new assignees only), then edits or clears dates per row on the homework assignee list. Student profile and Mi aprendizaje are view surfaces. Unit assign never sets dates. Due-date writes must not bump `content_revised_at`.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, `NamedParameterJdbcTemplate`, Flyway 11, existing `HomeworkAdminService` / `HomeworkTargetRepository` / `HomeworkItems`. **No new libraries.**
- Frontend: React 19, existing date input on the editor, `HomeworkAssigneeList`, student profile Tareas list. **No new libraries.**

**Storage**: PostgreSQL 18 — Flyway `V24__homework_target_due_on.sql`: add nullable `homework_targets.due_on DATE`; drop `homework_assignments.due_on`. Existing targets start with `NULL`.

**Testing**: JUnit (`LearningServiceTest` overdue per student; `HomeworkAdminServiceTest` / controller integration for assign-time date vs existing rows and per-row update; freeze: due-date writes do not bump `content_revised_at`). Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Same homework list/assignee payloads as today; one extra nullable date per target row. No extra list round-trip.

**Constraints**: One optional calendar date per student–homework target; assign-time `dueOn` applies only to newly inserted targets (`ON CONFLICT DO NOTHING` keeps existing `due_on`); unit assign leaves `NULL`; no bulk overwrite of existing dates; teacher-only writes; overdue derived, never stored; submit after due still allowed; drop assignment-level `dueOn` from create/update content and admin list cards.

**Scale/Scope**: Single-teacher homework assign + student learning + student profile Tareas. Quizzes, activities, notifications out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. One nullable column on the existing target row. No new table, no overall+override hybrid, no unit-assign date, no second editor on the student profile, no bulk “set all existing” action. Reuse `PUT .../assignees` plus one small per-row due-date update.
- **II. Component-Driven UI** — PASS. Named pieces: assign-time date on `HomeworkEditorPage` (with `StudentMultiSelect`), per-row date on `HomeworkAssigneeList`, view-only date/overdue on student profile homework rows and existing student `HomeworkItemCard` / unit homework rows. No raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Persist in `HomeworkTargetRepository`; student `dueOn`/`overdue` still on `HomeworkItemResponse` (source is the target). Admin list item loses homework-level `dueOn`; assignees gain it.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `044-student-homework-deadlines`.

**Post-design re-check**: PASS — contract moves `dueOn` from assignment JSON to target/assignee JSON; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/044-student-homework-deadlines/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── student-homework-deadlines-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V24__homework_target_due_on.sql            # NEW: targets.due_on; DROP assignments.due_on

back-end/src/main/java/com/kuky/backend/
├── learning/model/HomeworkAssignment.java     # EDIT: remove dueOn
├── learning/repository/ContentRepository.java # EDIT: stop read/write assignments.due_on
├── learning/repository/HomeworkTargetRepository.java  # EDIT: persist/read due_on; AssigneeView + StudentAssignmentView
├── learning/service/HomeworkItems.java        # EDIT: overdue/dueOn from the student's target date
├── admin/dto/HomeworkAdminItem.java           # EDIT: remove dueOn
├── admin/dto/CreateHomeworkRequest.java       # EDIT: dueOn = date for initial assignees only
├── admin/dto/UpdateHomeworkRequest.java       # EDIT: remove dueOn
├── admin/dto/SetAssigneesRequest.java         # EDIT: optional dueOn for newly added
├── admin/dto/AssigneeDto.java                 # EDIT: dueOn + overdue
├── admin/dto/StudentProfileHomeworkDto.java   # EDIT: dueOn + overdue
├── admin/dto/UpdateAssigneeDueOnRequest.java  # NEW
├── admin/controller/HomeworkAdminController.java  # EDIT: assignees body; NEW per-row due-on
├── admin/service/HomeworkAdminService.java    # EDIT: create/setAssignees/updateDueOn; map overdue
├── admin/service/StudentProfileAdminService.java  # EDIT: pass dueOn/overdue
└── units/service/UnitService.java             # EDIT: HomeworkAdminItem mapping without dueOn; addTargets stay NULL

back-end/src/test/java/com/kuky/backend/
├── learning/LearningServiceTest.java
├── learning/HomeworkSubmissionServiceTest.java
├── learning/HomeworkFreezeSubmittedIntegrationTest.java
├── admin/HomeworkAdminServiceTest.java
├── admin/HomeworkAdminControllerIntegrationTest.java
└── admin/StudentProfileAdminServiceTest.java

front-end/src/
├── lib/admin.ts                               # EDIT: dueOn off homework item; on Assignee + setAssignees + create; profile fields; updateAssigneeDueOn
├── lib/learning.ts                            # unchanged shape (dueOn/overdue already per item)
├── i18n/locales/{es,en,ro}.ts                 # EDIT: assign-time + assignee-row copy if needed
└── components/admin/homework/
    ├── HomeworkEditorPage.tsx                 # EDIT: date with assignees, not content
    ├── HomeworkAssigneeList.tsx               # EDIT: per-row date + overdue
    └── HomeworkAdminCard.tsx                  # EDIT: drop overall due date
└── routes/panel_.alumnos.$studentId.tsx       # EDIT: view-only dueOn/overdue on Tareas rows
```

**Structure Decision**: Existing full-stack layout. Student learning JSON keeps `dueOn`/`overdue` so `HomeworkItemCard` / `UnitDetailContent` stay; only the source of the date changes. Unit assign and quizzes/activities untouched except compile-safe DTO mapping.

## Complexity Tracking

> No constitution violations requiring justification.
