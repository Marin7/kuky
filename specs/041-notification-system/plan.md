# Implementation Plan: In-App Activity Notifications

**Branch**: `041-notification-system` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/041-notification-system/spec.md`

## Summary

Small in-site **unseen icons** (not email, not an inbox). The teacher sees a mark on **Panel**, then **Tareas** or **Pruebas de evaluación**, plus the homework/test and each student row, when a student **submits** homework or a quiz. A student sees a mark on **Mi aprendizaje** and on the new unit or quiz when the teacher **assigns a unit or a quiz**. Icons clear only when the recipient opens that specific work (teacher: that student’s submitted review/result; student: the unit page or the quiz page). Existing events at ship are treated as already seen. No websockets, no counts, no presentation-activity or grade-ready events.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate, Flyway. **No new libraries.** New `notification` package for badge summary + mark-seen; hooks in existing submit/assign/get services.
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn. **No new libraries.** Named `NotificationDot` on nav, tabs, and list rows; `GET /notifications/badges` from `SiteHeader` / `AdminPanel` on navigation.

**Storage**: PostgreSQL 18 — Flyway `V22`: `teacher_seen_at` on `homework_submissions` and `quiz_attempts`; `student_seen_at` on `unit_assignments`, `quiz_assignees`, and `homework_targets`. Backfill existing rows to `NOW()` (FR-016). No new notifications table.

**Testing**: JUnit — submit creates unseen; assign unit/quiz creates unseen; opening submission/attempt/unit/quiz marks seen; editor/list GET does not; unassign/delete removes; existing rows not unseen; `USER` has no student badges. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Badge summary cheap enough for every authenticated page load/navigation (SC-001/SC-002). Partial indexes on unseen (`seen_at IS NULL`) rows.

**Constraints**: In-site icons only. No email/push/inbox/mark-all-read. No live pop-in without navigation. No student notify for homework added to an already-assigned unit, presentation activities, or teacher grading. i18n es/en/ro for the icon’s accessible name.

**Scale/Scope**: Single teacher (shared `teacher_seen_at`); tens of students. Four event kinds only.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Unseen flags on the four existing tables instead of a generic notifications table, websocket, or inbox. Presence-only dots, not counts. Mark-seen is a side effect of the GET/POST the user already makes to open the work.
- **II. Component-Driven UI** — PASS. One named `NotificationDot` reused on header, tabs, and rows; no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Badge fetch in `lib/notifications.ts`; list flags on existing DTOs; `NotificationService` owns queries and mark-seen, called from submit/assign/get — not inlined in views.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `041-notification-system`.

**Post-design re-check**: PASS — contracts add one badge GET plus `unseen` flags and implicit mark-seen; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/041-notification-system/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── notifications-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V22__activity_unseen_flags.sql              # NEW

back-end/src/main/java/com/kuky/backend/
├── notification/                               # NEW: model/dto, repo, service, controller
├── learning/service/HomeworkSubmissionService.java  # EDIT: unseen after submit
├── quiz/service/QuizService.java               # EDIT: unseen after submit; mark student seen on GET
├── quiz/service/QuizAdminService.java          # EDIT: new assignees unseen; mark teacher seen on attempt GET
├── units/service/UnitService.java              # EDIT: new unit assignees unseen; preserve seen on remaining
├── admin/service/HomeworkAdminService.java     # EDIT: mark teacher seen on submission GET; unseen on DTOs
├── admin/service/StudentProfileAdminService.java # EDIT: unseen on profile homework/quiz rows
├── learning/service/LearningService.java       # EDIT: unseen on unit refs
├── learning/controller/LearningController.java # EDIT: POST /units/{id}/seen
├── config/SecurityConfig.java                  # EDIT: /api/v1/notifications/** authenticated
└── (tests: Notification*; submit/assign/seen hooks)

front-end/src/
├── components/NotificationDot.tsx              # NEW
├── lib/notifications.ts                        # NEW: getBadges, markUnitSeen
├── components/SiteHeader.tsx                   # EDIT: dots on Panel / Mi aprendizaje
├── components/admin/AdminPanel.tsx             # EDIT: dots on Tareas / Pruebas tabs
├── components/admin/homework/*                 # EDIT: list, queue, assignee rows
├── components/quiz/admin/*                     # EDIT: list, queue, attempt rows
├── routes/panel_.alumnos.$studentId.tsx        # EDIT: homework/quiz row dots
├── components/learning/LearningContent.tsx     # EDIT: unit card dot
├── components/learning/AssignedQuizList.tsx    # EDIT: quiz row dot
├── components/learning/UnitLearningView.tsx    # EDIT: POST unit seen on mount
└── i18n/locales/{en,es,ro}.ts                  # EDIT: notification aria labels
```

**Structure Decision**: Existing full-stack layout. Unseen flags live on current tables; a small `notification` package exposes the badge summary and mark-seen so header/tabs stay off list payloads.

## Complexity Tracking

> No constitution violations requiring justification.
