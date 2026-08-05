# Implementation Plan: Teacher Feedback on Homework Submissions

**Branch**: `025-homework-feedback` | **Date**: 2026-08-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/025-homework-feedback/spec.md`

## Summary

Let the teacher leave optional **plain-text feedback** on **graded exercise** submissions (Writing/manual feedback unchanged). Reuse `homework_submissions.feedback` for storage (single unformatted FormattedText segment under the hood; exercise APIs expose a plain string). Add save/update/clear without status change, show feedback on teacher and student exercise result views, and add a **feedback-present indicator** on student learning homework cards and admin assignee/student-profile homework lists.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway, PostgreSQL. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new dependencies** — plain `<textarea>` / existing Input primitives.

**Storage**: PostgreSQL 18 — **no new migration**. Reuse existing `homework_submissions.feedback` (`TEXT`, already nullable). Writing continues to store rich FormattedText JSON and transition `SUBMITTED → REVIEWED`. Exercises store the same JSON shape as a single plain segment (or `NULL` when cleared); status stays `GRADED`.

**Testing**: Backend JUnit 5 — `HomeworkAdminService` save/update/clear on GRADED; reject MANUAL/non-GRADED; length > 2000; Writing `saveFeedback` regression. Frontend: browser verify teacher save + indicators + student result.

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: N/A — single-row update; list indicator is a boolean derived from existing feedback column.

**Constraints**: Exercise/`GRADED` only; ≤ 2000 chars; plain text UX (no rich editor); editable after save; empty save clears; no email; Writing path untouched.

**Scale/Scope**: Single-teacher site; one admin PUT endpoint; DTO field additions; `ExerciseResultDialog` + student `ExerciseResult` / homework cards; i18n es/en/ro.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Reuse `feedback` column and existing exercise result dialog; no new queue, table, or rich-text stack for exercises. Separate admin endpoint keeps Writing’s `SUBMITTED→REVIEWED` semantics intact.
- **II. Component-Driven UI** — PASS. Extend `ExerciseResultDialog`, `ExerciseResult`, `HomeworkItemCard`, admin assignee row / student-profile homework list with named UI pieces (textarea + indicator), not raw DOM.
- **III. Evolution-Ready Architecture** — PASS. API clients stay in `front-end/src/lib/admin.ts` and `learning.ts`.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart includes browser checks; branch `025-homework-feedback`.

**Post-design re-check**: PASS — contracts extend existing admin/learning surfaces; no schema migration; Complexity Tracking empty.

**Result: PASS — no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/025-homework-feedback/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── exercise-feedback-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/
├── admin/
│   ├── controller/HomeworkAdminController.java     # EDIT: PUT …/exercise-feedback
│   ├── service/HomeworkAdminService.java           # EDIT: saveExerciseFeedback; include text on getExerciseResult
│   ├── service/StudentProfileAdminService.java     # EDIT: hasTeacherFeedback on homework DTO
│   └── dto/
│       ├── ExerciseSubmissionResultAdminDto.java   # EDIT: teacherFeedback
│       ├── SaveExerciseFeedbackRequest.java        # NEW: plain string body
│       ├── AssigneeDto.java                        # EDIT: hasTeacherFeedback
│       └── StudentProfileHomeworkDto.java          # EDIT: hasTeacherFeedback
├── learning/
│   ├── controller/LearningController.java          # (unchanged routes; DTOs gain fields)
│   ├── repository/HomeworkSubmissionRepository.java # EDIT: updateExerciseFeedback (no status change)
│   ├── service/HomeworkItems.java                  # EDIT: hasTeacherFeedback on overview items
│   ├── service/ExerciseGradingService.java         # EDIT: expose teacherFeedback on student exercise view
│   └── dto/
│       ├── HomeworkItemResponse.java               # EDIT: hasTeacherFeedback
│       ├── ExerciseResponse.java                   # EDIT: teacherFeedback
│       └── ExerciseResultResponse.java             # (optional: teacherFeedback here vs on ExerciseResponse)

front-end/src/
├── lib/admin.ts                                    # EDIT: types + saveExerciseFeedback
├── lib/learning.ts                                 # EDIT: hasTeacherFeedback, teacherFeedback
├── components/admin/homework/
│   ├── ExerciseResultDialog.tsx                    # EDIT: textarea + save/clear
│   ├── HomeworkAdminList.tsx                       # EDIT: feedback indicator on GRADED assignees
│   └── (student profile homework list if separate) # EDIT: indicator
├── components/learning/
│   ├── ExerciseResult.tsx                          # EDIT: show teacherFeedback when present
│   ├── ExerciseForm.tsx                            # EDIT: pass teacherFeedback through
│   └── HomeworkItemCard.tsx                        # EDIT: feedback-present indicator
└── i18n/locales/{es,en,ro}.ts                      # EDIT: labels, errors, indicator copy
```

**Structure Decision**: Stay inside existing admin homework + learning exercise surfaces. Do not touch Writing review queue/dialog or `PUT …/feedback`.

## Complexity Tracking

> No constitution violations requiring justification.
