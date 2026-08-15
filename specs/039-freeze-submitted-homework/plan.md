# Implementation Plan: Freeze Submitted Homework

**Branch**: `039-freeze-submitted-homework` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/039-freeze-submitted-homework/spec.md`

## Summary

When the teacher edits a homework, **already-submitted** work stays exactly as it was at submit (questions, options, keys, media, instructions, answers, scores, item-level right/wrong, existing teacher review). Unsubmitted students take the live homework. A student with the take still open is told on next reload/return/submit (`409 HOMEWORK_UPDATED`), in-progress answers are discarded, and they start fresh. Implementation: JSONB `assignment_snapshot` on the submission, `content_revised_at` token, retire referenced questions/options instead of deleting them. **Homework only** — activities and placement unchanged.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate, Jackson JSONB, Flyway. **No new libraries.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn. **No new libraries.** Reuse existing take/result/review components; add i18n copy + form reset on `HOMEWORK_UPDATED`.

**Storage**: PostgreSQL 18 — Flyway `V20`: `homework_submissions.assignment_snapshot JSONB`, `homework_assignments.content_revised_at`, `retired` on questions/options; backfill snapshots for existing submitted rows.

**Testing**: JUnit — snapshot written on submit; live edit does not change stored snapshot/scores; retire instead of delete when referenced; stale token → 409; due-date-only does not bump token; backfill leaves scores; activity paths untouched. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Snapshot size is one homework (small). Submit copies live questions once; result GET parses JSONB instead of joining live questions.

**Constraints**: Freeze at submit only; no retake; no activity freeze; no version-history UI; list title may stay live; due date / assignees do not discard in-progress takes; i18n es/en/ro for the update message.

**Scale/Scope**: Single-teacher site; homework authoring, take, result, and review. Activities out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. One JSONB column + a revision timestamp + a retire flag. No version table, no activity parity, no live push.
- **II. Component-Driven UI** — PASS. Existing take/result/review components; named alert/banner for `HOMEWORK_UPDATED`; no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Snapshot read isolated in submission/grading mapping; live editor path unchanged.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `039-freeze-submitted-homework`.

**Post-design re-check**: PASS — contracts add one error code and `contentRevisedAt`; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/039-freeze-submitted-homework/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── freeze-submitted-homework-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V20__homework_submission_snapshot.sql          # NEW

back-end/src/main/java/com/kuky/backend/
├── learning/model/HomeworkAssignment.java         # EDIT: contentRevisedAt
├── learning/model/HomeworkQuestion.java           # EDIT: retired
├── learning/model/QuestionOption.java             # EDIT: retired
├── learning/model/HomeworkSubmission.java         # EDIT: assignmentSnapshot
├── learning/repository/HomeworkQuestionRepository.java  # EDIT: retire vs delete; live filter
├── learning/repository/ContentRepository.java     # EDIT: persist content_revised_at
├── learning/repository/HomeworkSubmissionRepository.java
├── learning/service/HomeworkSubmissionService.java # EDIT: token check, write snapshot, read snapshot
├── learning/service/ExerciseGradingService.java    # EDIT: storedResultFor from snapshot
├── learning/service/LearningService.java           # EDIT: take GET token; result from snapshot
├── admin/service/HomeworkAdminService.java         # EDIT: bump token; review/result from snapshot
├── config/GlobalExceptionHandler.java              # EDIT: HOMEWORK_UPDATED
└── (tests under src/test/java/…)

front-end/src/
├── lib/learning.ts                                 # EDIT: contentRevisedAt; 409 handling
├── lib/admin.ts                                    # EDIT: review DTOs if snapshot fields surface
├── components/learning/ExerciseForm.tsx            # EDIT: token + reset
├── components/learning/MixedHomeworkForm.tsx
├── components/learning/ManualAnswerForm.tsx        # or WRITE form equivalent
├── components/learning/ExerciseResult.tsx          # uses GET payload (snapshot-backed)
├── components/admin/homework/HomeworkReviewDialog.tsx
├── components/admin/homework/ExerciseResultDialog.tsx
└── i18n/locales/{en,es,ro}.ts                      # EDIT: homework-updated copy
```

**Structure Decision**: Existing full-stack layout. Homework learning + admin only. Activity packages not touched.

## Complexity Tracking

> No constitution violations requiring justification.
