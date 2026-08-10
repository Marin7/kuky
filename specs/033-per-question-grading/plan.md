# Implementation Plan: Per-Question Homework Grading

**Branch**: `033-per-question-grading` | **Date**: 2026-08-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/033-per-question-grading/spec.md`

## Summary

Move auto vs manual grading from homework/activity-level `format` (`MANUAL` | `EXERCISE`) to **per question**: `FREE_TEXT` = manual; structured kinds = auto-correctible. Non-Writing authoring drops the Manual/Exercise radio — one question list may mix kinds. Writing stays a distinct single large manual answer. Submit auto-grades structured answers immediately (keys revealed); if any manual answers exist alongside auto, status stays `SUBMITTED` (same awaiting-teacher queue as pure-manual) until the teacher validates/invalidates every manual response (optional annotate + ≤500-char note). Finalize computes one **combined** `score_percent` (each manual = full question: validated=1, invalidated=0) and moves to `GRADED`. Pure-manual/Writing keep `SUBMITTED` → `REVIEWED`. Pure-auto stays immediate `GRADED`. Activities get full parity. Flyway `V17`.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway, PostgreSQL. **No new libraries.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, existing question editors / review dialogs. **No new libraries.**

**Storage**: PostgreSQL 18 — Flyway `V17__per_question_grading.sql`: expand `format` CHECK with `MIXED` (derived on save); add `teacher_validation` on `homework_answers` / `activity_answers`; backfill derived `format` from question kinds.

**Testing**: Backend JUnit — authoring mix validation, submit paths (all-auto / all-manual / mixed / WRITE), combined score math, finalize requires all validations, key reveal on mixed submit, activity parity, migration of legacy MANUAL/EXERCISE. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Single-request submit and finalize; score over typical homework size (≤50 questions).

**Constraints**: No retake after submit; answer keys hidden pre-submit; Writing unchanged as single-answer form; pure-manual review path unchanged (no required validate/invalidate); mixed uses same `SUBMITTED` awaiting status as pure-manual; activities full parity; i18n es/en/ro.

**Scale/Scope**: Single-teacher site; one migration; unify authoring + student take + admin review for homework and activities; deprecate request-body `format` (server derives).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Grading kind = existing `QuestionKind` (`FREE_TEXT` vs structured); no new question-type system. Derive assignment `format`/`composition` from questions rather than a parallel taxonomy. Reuse annotate + short-note review from 032; add only `teacher_validation` + combined score.
- **II. Component-Driven UI** — PASS. Extend `HomeworkEditorPage` / `ActivityEditorPage` with a unified question list; student take merges `ExerciseForm` + free-text fields; review extends `HomeworkReviewDialog` with validate/invalidate controls — named components, no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Types/API in `admin.ts` / `learning.ts`; backend under existing `admin` + `learning` packages; shared validation stays on `HomeworkAdminService.validateAndMapQuestions`.
- **Technology Stack** — PASS. Unchanged stack.
- **Development Workflow** — PASS. Quickstart browser checks; branch `033-per-question-grading`.

**Post-design re-check**: PASS — contracts extend existing endpoints; data model is one migration + derived `MIXED`; Complexity Tracking empty (no new abstraction layer beyond composition helpers).

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/033-per-question-grading/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── per-question-grading-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V17__per_question_grading.sql

back-end/src/main/java/com/kuky/backend/
├── learning/
│   ├── model/HomeworkFormat.java              # EDIT: + MIXED
│   ├── model/HomeworkComposition.java         # NEW: WRITE | ALL_MANUAL | ALL_AUTO | MIXED (derived)
│   ├── model/TeacherValidation.java           # NEW: VALIDATED | INVALIDATED
│   ├── model/HomeworkAnswer.java              # EDIT: teacherValidation
│   ├── model/ActivityAnswer.java              # EDIT: same
│   ├── dto/*                                  # EDIT: composition, provisionalScore, validations
│   ├── service/HomeworkComposition.java       # NEW or helper: derive from type+questions
│   ├── service/HomeworkSubmissionService.java # EDIT: accept mixed submit → SUBMITTED + auto scores
│   ├── service/ExerciseGradingService.java    # EDIT: grade structured subset; combined score helper
│   ├── service/ActivityExerciseGradingService.java
│   ├── service/ActivityStudentService.java
│   ├── service/HomeworkItems.java             # EDIT: mixed student view
│   └── repository/*                           # EDIT: validation + score updates
├── admin/
│   ├── service/HomeworkAdminService.java      # EDIT: drop format-gated question rules; derive format
│   ├── service/ActivityAdminService.java      # EDIT: parity
│   ├── dto/SaveHomeworkFeedbackRequest.java   # EDIT: per-answer teacherValidation
│   └── controller/*                           # EDIT: review finalize for mixed → GRADED
└── (tests under src/test/java/…)

front-end/src/
├── lib/admin.ts                               # EDIT: format optional; composition; validation fields
├── lib/learning.ts                            # EDIT: mixed item + submit payloads
├── components/admin/homework/
│   ├── HomeworkEditorPage.tsx                 # EDIT: remove Manual/Exercise radio; unified question list
│   ├── UnifiedQuestionListEditor.tsx          # NEW or evolve Manual+Exercise editors
│   └── HomeworkReviewDialog.tsx               # EDIT: validate/invalidate + score finalize
├── components/admin/activities/               # EDIT: parity
└── components/learning/
    ├── HomeworkInlinePanel.tsx                # EDIT: composition-based dispatch
    ├── MixedHomeworkForm.tsx                  # NEW or merge ExerciseForm + free-text
    └── ExerciseResult.tsx / panels            # EDIT: provisional vs final combined score
```

**Structure Decision**: Existing full-stack layout (`front-end/` + `back-end/`). No new packages; extend learning/admin homework and activity paths in parallel (same pattern as 031/032).

## Complexity Tracking

> No constitution violations requiring justification.
