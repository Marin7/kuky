# Implementation Plan: Presentation Activities

**Branch**: `029-presentation-activities` | **Date**: 2026-08-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/029-presentation-activities/spec.md`

## Summary

Introduce **Activities** — homework-parity student work (MANUAL + all EXERCISE kinds) that belong to a presentation, use a single PDF as instructions (no rich-text instructions, no due dates, no assignees), optionally prompt when the student lands on a chosen presentation PDF page, and appear nested under the expanded presentation in the unit view. Reuse the existing exercise grading/validation engine via parallel activity tables; nest student discovery under presentations (not unit-sequence peers); open from the viewer as an overlay and from the unit list as a full screen; cascade-delete with the presentation; surface fulfillment in the student progress overview.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway, PostgreSQL. **No new backend libraries.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next, pdfjs-dist (existing viewer), `@dnd-kit/*` (existing — teacher activity reorder under a presentation).

**Storage**: PostgreSQL 18 — Flyway `V11__presentation_activities.sql` (activities + instructions file metadata + questions/options/submissions/answers mirroring homework shapes). Instructions PDF bytes on disk via a new `ActivityInstructionsFileStore` (`app.activity-instructions.storage-dir`). Page-trigger FK to `presentation_files`.

**Testing**: Backend JUnit — activity CRUD/reorder, access inheritance from presentation, page-trigger validation, cascade delete, submit/grade MANUAL+EXERCISE, progress DTO includes activities. Frontend browser verification — admin authoring, unit nested list, viewer page prompt + overlay, progress overview.

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Prompt within ~2s of page visibility (client IntersectionObserver); activity lists small (per-presentation).

**Constraints**: No due dates / assignees; not in unit mixed sequence; PDF instructions only in v1; page trigger = presentation file id + page; non-blocking prompts; no re-prompt after fulfillment; cascade delete with presentation; homework/presentation flows unchanged when unused.

**Scale/Scope**: Single-teacher site; one migration; admin Activities (or Presentations-adjacent) authoring; student unit expansion + viewer overlay; student profile progress extension; i18n es/en/ro.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Parallel activity tables (not conflating with `homework_assignments`) keep unit sequencing and assignee/due-date logic untouched. Shared grading/validation extracted or called with activity IDs — no second exercise engine. No new frameworks.
- **II. Component-Driven UI** — PASS. Nested activity list under presentation accordion; overlay Dialog for viewer; reuse homework question editors and exercise forms behind activity-specific containers.
- **III. Evolution-Ready Architecture** — PASS. Types/API helpers in `front-end/src/lib/admin.ts` and `learning.ts`; backend under `learning` + `admin` packages consistent with homework.
- **Technology Stack** — PASS. Existing stack; reuse `@dnd-kit` already introduced for unit content ordering.
- **Development Workflow** — PASS. Quickstart lists browser checks; branch `029-presentation-activities`.

**Post-design re-check**: PASS — contracts, data model, and research keep activities separate from homework rows while reusing grading; Complexity Tracking empty (no new unjustified deps).

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/029-presentation-activities/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── activities-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V11__presentation_activities.sql

back-end/src/main/java/com/kuky/backend/
├── learning/
│   ├── model/          # Activity*, ActivitySubmission, ActivityQuestion, …
│   ├── repository/     # ActivityRepository, ActivitySubmissionRepository, …
│   ├── service/        # ActivityService (student), reuse ExerciseGradingService patterns
│   ├── dto/            # ActivityItemResponse, embed on SharedPresentationSummary
│   └── controller/LearningController.java   # EDIT: activity endpoints + overview embed
├── admin/
│   ├── service/ActivityAdminService.java    # NEW
│   ├── controller/ActivityAdminController.java  # NEW
│   └── service/StudentProfileAdminService.java  # EDIT: activity progress
├── presentations/
│   └── service/PresentationService.java     # EDIT: clear page triggers when file deleted
└── config/             # ActivityInstructionsProperties + FileStore bean

front-end/
├── src/lib/admin.ts                    # EDIT: activity admin API
├── src/lib/learning.ts                 # EDIT: activity student API + types
├── src/components/admin/activities/    # NEW: tab/list/editor/reorder
├── src/components/admin/AdminPanel.tsx # EDIT: Activities tab
├── src/components/learning/
│   ├── UnitDetailContent.tsx           # EDIT: nest activities under presentation
│   ├── PresentationExpandBody.tsx      # EDIT: activity list
│   ├── PresentationPdfViewer.tsx       # EDIT: page-visible callback / prompts
│   ├── ActivityOverlay.tsx             # NEW: viewer modal
│   └── ActivityPanel.tsx               # NEW: full fulfill UI (reuse exercise/manual forms)
├── src/components/admin/students/      # EDIT: progress breakdown includes activities
└── src/routes/
    ├── panel_.actividades*.tsx         # NEW: admin authoring routes (mirror tareas)
    └── aprendizaje_.actividad.$activityId.tsx  # NEW: full-page fulfill from unit list
```

**Structure Decision**: Full-stack `back-end/` + `front-end/`. Activities live as a first-class learning content type parallel to homework, owned by presentations, with admin under `/api/v1/admin/activities` and student under `/api/v1/learning/activities` (plus overview embed).

## Complexity Tracking

> No constitution violations requiring justification.
