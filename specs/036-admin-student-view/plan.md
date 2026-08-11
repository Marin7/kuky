# Implementation Plan: Admin Student View Overhaul

**Branch**: `036-admin-student-view` | **Date**: 2026-08-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/036-admin-student-view/spec.md`

## Summary

Overhaul the admin student profile (`/panel/alumnos/:id`): remove the incomplete **Progreso** section; make the top **Tareas** summary box the single homework surface — collapsed state shows total count plus pending/submitted/completed breakdown; expanded state shows a full-width homework list (status, score, open responses) directly under the stats row; delete the duplicate lower **Tareas** section. Slim the profile API by dropping the unused `progress` aggregate (units, activity breakdown, attended classes); derive the three-bucket homework breakdown on the client from the existing `homeworks` list.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate. **No new libraries.** Optional cleanup only — remove progress computation from `StudentProfileAdminService`.
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, existing `StudentHomeworkBreakdown`, `HomeworkReviewDialog`, `ExerciseResultDialog`. **No new libraries.**

**Storage**: N/A — no schema or Flyway changes. Profile already returns `homeworks[]` with status/score/submission fields.

**Testing**: Backend JUnit — update/remove `StudentProfileAdminServiceTest` cases that assert `progress` (units, attended, activity). Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Negligible — less work on profile fetch (no unit/activity/attended aggregation); client breakdown is O(n) over homeworks already loaded.

**Constraints**: Admin-only student profile UI; preserve upcoming/past classes (incl. no-show), presentations, interests, placement evaluation; do not relocate attended-count or CEFR chips; i18n es/en/ro; reuse existing review/result dialogs and homework row actions.

**Scale/Scope**: Single admin page route; one API response shape change (`progress` removed).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. UI consolidation + delete unused progress aggregation; no new progress product, no new tables, no new libraries. Client derives breakdown from `homeworks`.
- **II. Component-Driven UI** — PASS. Evolve existing profile route + reuse `StudentHomeworkBreakdown` and review/result dialogs; expandable Tareas as a named control (inline or small extracted component), no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Types stay in `admin.ts`; API DTO change is additive-removal only on admin profile response.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `036-admin-student-view`.

**Post-design re-check**: PASS — contracts document `progress` removal; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/036-admin-student-view/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── admin-student-profile-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/admin/
├── dto/StudentProfileResponse.java              # EDIT: remove progress field
├── dto/StudentProgressDto.java                  # DELETE if unused elsewhere (or leave orphan → delete)
├── dto/UnitProgressDto.java                     # DELETE if only used by progress
├── dto/HomeworkBreakdownDto.java                # DELETE if only used by progress
├── dto/ActivityBreakdownDto.java                # DELETE if only used by progress
├── service/StudentProfileAdminService.java      # EDIT: stop computing/returning progress; drop unused deps
└── (tests) StudentProfileAdminServiceTest.java  # EDIT: drop progress assertions; keep profile/homework coverage

front-end/src/
├── routes/panel_.alumnos.$studentId.tsx         # EDIT: expandable Tareas; remove Progreso + lower Tareas
├── components/admin/students/
│   ├── StudentHomeworkBreakdown.tsx             # EDIT: reuse in collapsed Tareas; i18n keys may move off progress.*
│   └── (optional) ExpandableTareasSummary.tsx   # NEW only if page stays clearer with extraction
├── lib/admin.ts                                 # EDIT: remove StudentProgress / related types from StudentProfile
└── i18n/locales/{en,es,ro}.ts                   # EDIT: Tareas expand/collapse + breakdown labels; drop unused progress copy
```

**Structure Decision**: Existing full-stack layout. Primary work is frontend UX on the student profile route; backend only removes the obsolete `progress` payload and its aggregation paths (YAGNI). No DB migration.

## Complexity Tracking

> No constitution violations requiring justification.
