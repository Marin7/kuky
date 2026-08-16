# Implementation Plan: Student Dots for Homework Corrections and Feedback

**Branch**: `047-homework-grade-dots` | **Date**: 2026-08-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/047-homework-grade-dots/spec.md`

## Summary

Extend the existing in-site **unseen icons** so a student sees a mark on **Mi aprendizaje**, the **unit**, and the **homework** when the teacher **finalizes** a homework correction or saves **student-visible** comments/annotations on already-visible homework. Icons clear only when that student opens the **GRADED result**. Assignment dots (unit/quiz/homework-target) stay independent. No email, inbox, websockets, activity/quiz review dots, or live pop-in.

Technical approach: two null-timestamp columns on `homework_submissions` (grade vs feedback) so FR-015 can dismiss comment news without dismissing an unseen finalize; hooks in `HomeworkAdminService` only; mark-seen as a `GRADED`-gated side effect of the student homework GET/seen POST already used to open work; `learning` badge and homework `unseen` OR in review unseen.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate, Flyway. **No new libraries.** Extend `notification` package + `HomeworkAdminService` / student GET-seen paths.
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn. **No new libraries.** Reuse `NotificationDot`; homework `unseen` already drives the row/unit-group dots.

**Storage**: PostgreSQL 18 — Flyway `V25`: `student_grade_seen_at` and `student_feedback_seen_at` on `homework_submissions`. Backfill existing rows to `NOW()`. Submit writes `NOW()`; teacher visible review writes NULL on the matching column(s).

**Testing**: JUnit — finalize creates student unseen; progress save does not; auto-submit does not; exercise feedback / annotations on GRADED do; awaiting comments wait until finalize; opening GRADED result marks seen; unit seen POST does not; activity/quiz saves do not; existing rows not unseen; other student has no badge. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Badge summary remains cheap on every authenticated navigation (SC-001/SC-002). Partial index on review-unseen submissions.

**Constraints**: In-site icons only. No email/push/inbox. No live pop-in. Homework only (not presentation activities or pruebas). i18n: reuse existing notification aria labels unless a distinct review label is needed.

**Scale/Scope**: Single teacher; tens of students. One extra event family (homework review) on the existing badge channel.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Flags on `homework_submissions` instead of a notifications table or new badge field. Two columns only because FR-015 cannot be correct with one. Reuse `NotificationDot` and existing student open endpoints.
- **II. Component-Driven UI** — PASS. No new visual primitive; named `NotificationDot` already on Mi aprendizaje, unit groups, and homework rows.
- **III. Evolution-Ready Architecture** — PASS. `NotificationService` owns NULL/NOW writes; admin/student services call it; list `unseen` stays on learning DTOs.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `047-homework-grade-dots`.

**Post-design re-check**: PASS — contract extends badge meaning and implicit seen/unseen on existing endpoints; no new public path required; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/047-homework-grade-dots/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── homework-grade-dots-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V25__homework_student_review_unseen.sql     # NEW

back-end/src/main/java/com/kuky/backend/
├── notification/repository/NotificationRepository.java   # EDIT: learning badge + mark/create review unseen
├── notification/service/NotificationService.java         # EDIT: helpers for grade/feedback unseen + GRADED seen
├── admin/service/HomeworkAdminService.java               # EDIT: after saveFeedback / saveExerciseFeedback
├── learning/service/ExerciseGradingService.java          # EDIT: mark review seen on GRADED GET
├── learning/service/LearningService.java                 # EDIT: unseen OR review; GRADED seen POST
├── learning/service/HomeworkSubmissionService.java       # EDIT: submit sets review columns NOW()
└── (tests: Notification*; HomeworkAdminService*; submission)

front-end/src/
├── lib/learning.ts                    # EDIT: comment on unseen (assignment or review)
├── components/learning/unitGroups.ts  # unchanged behavior if homework.unseen ORs review
├── components/learning/HomeworkInlinePanel.tsx  # already dots + seen POST / getExercise
└── (SiteHeader learning badge already GET /notifications/badges)
```

**Structure Decision**: Existing full-stack layout. Review unseen lives on submissions next to `teacher_seen_at`; the `notification` package remains the only place that queries badge aggregates.

## Complexity Tracking

> No constitution violations requiring justification.
