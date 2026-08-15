# Research: In-App Activity Notifications

**Feature**: `041-notification-system` | **Date**: 2026-08-15

## 1. Storage: columns vs notifications table

**Decision**: Add `teacher_seen_at TIMESTAMPTZ NULL` on `homework_submissions` and `quiz_attempts`, and `student_seen_at TIMESTAMPTZ NULL` on `unit_assignments` and `quiz_assignees`. Unseen = submitted (or newly assigned) **and** `*_seen_at IS NULL`. No `activity_notifications` table.

**Rationale**: Four event kinds already have a unique row (one submission, one attempt, one unit assignment, one quiz assignee). A generic inbox table would duplicate those keys and add lifecycle the spec forbids (list, mark-all-read, body text). YAGNI; delete/unassign already cascades the row so the icon goes away (FR-014).

**Alternatives considered**:
- `notifications` table (recipient, kind, target_id, seen_at) — easier to add grade-ready later, extra writes and cleanup on unassign/delete.
- Boolean `unseen` — cannot tell “never submitted” from “submitted and unseen” as cleanly; timestamptz matches other audit columns.

## 2. Existing rows at ship (FR-016)

**Decision**: Flyway `UPDATE` every existing homework submission with status `SUBMITTED`/`REVIEWED`/`GRADED`, every quiz attempt with status `SUBMITTED`/`GRADED`, and every existing `unit_assignments` / `quiz_assignees` row, setting `*_seen_at = NOW()`. New submits and new assignee inserts leave the column NULL.

**Rationale**: Spec: do not backfill historical news as unseen. Treating old rows as already seen is one UPDATE, not a “feature start” timestamp check in every query.

**Alternatives considered**: Compare `created_at` to a shipped-at constant — fragile across environments.

## 3. Who is “the teacher”

**Decision**: `teacher_seen_at` is shared (no recipient user id). Any `ADMIN` session sees the same badges. Student flags are per `user_id` on the assignment row.

**Rationale**: Spec assumes a single teacher. Storing per-admin seen state is unused complexity.

**Alternatives considered**: Notify `app.scheduling.teacher-email` only — same one person; extra lookup on every submit.

## 4. Mark-seen mechanism

**Decision**:
- Teacher: side effect of **GET** that already loads that student’s submitted work — `GET /admin/homework/submissions/{id}` and `.../exercise-result`, `GET /admin/quizzes/{quizId}/attempts/{attemptId}` (and the review dialogs that call those). Opening the homework/quiz editor, Tareas/Pruebas tabs, assignee lists, or the student profile page does **not** mark seen.
- Student unit: **POST** `/api/v1/learning/units/{unitId}/seen` when `/aprendizaje/unidad/{unitId}` mounts (`UnitLearningView`). Opening Mi aprendizaje, “Otros”, or a homework inside the unit does not.
- Student quiz: side effect of **GET** `/quizzes/{quizId}` (the take/result page). List GET `/quizzes` does not.

Mark-seen is idempotent: `SET seen_at = NOW() WHERE … AND seen_at IS NULL`.

**Rationale**: Spec: only opening that work counts. Teacher review already GETs the submission. Students have no unit GET today (`UnitLearningView` reuses `GET /learning`), so a tiny POST is smaller than inventing a unit resource. Quiz GET is already “open the test.”

**Alternatives considered**:
- Mark seen on GET `/learning` — would clear all units when they only open Mi aprendizaje (violates FR-010 / FR-018).
- Explicit “dismiss” control — spec forbids an inbox / mark-all-read.

## 5. Badge summary vs list flags

**Decision**: Both.
- `GET /api/v1/notifications/badges` — booleans for **Panel**, **Tareas**, **Pruebas de evaluación**, **Mi aprendizaje**. Used by `SiteHeader` and `AdminPanel` on login and pathname change (same pattern as `getMe`).
- `unseen` (or `hasUnseenSubmissions` / `unseenAttempt`) on existing list DTOs so rows can render a dot without a second fan-out.

**Rationale**: Nav icons must work on every page without loading all homework. Row dots need ids the list already has.

**Alternatives considered**: Put badge booleans on `GET /auth/me` — couples auth to learning/admin queries.

## 6. Real-time

**Decision**: Refresh badges on next load or client navigation. No websocket, SSE, or polling loop.

**Rationale**: Spec: live pop-in while staring at an open page is not required.

## 7. Assignee replace must preserve seen

**Decision**: `UnitService.setAssignees` and `QuizAdminService.setAssignees` must **insert only newly added** students (NULL `student_seen_at`) and **delete only removed** ones. Remaining rows keep `student_seen_at`. If today’s `replaceAssignees` deletes-all-then-inserts, change it so remaining students are not re-notified.

**Rationale**: Spec: no second notification for work they already had. Wiping the row would reset seen.

**Alternatives considered**: Copy seen_at through a full replace — extra code vs insert/delete diff (the services already compute `added` / `removed` for homework targets).

## 8. What does not notify

**Decision**: No hooks on presentation-activity submit/assign, homework `setAssignees` from Tareas (direct homework assign), adding homework to a unit (`setHomeworks`), or teacher feedback/grade PUTs.

**Rationale**: Clarifications and FR-015 / FR-017.

## 9. UI marker

**Decision**: Presence-only `NotificationDot` (small attention mark + i18n `aria-label`). No numeric count. Place on: header nav, admin tabs, homework/quiz cards, review-queue rows, assignee/attempt rows, student-profile homework/quiz rows, student unit cards, assigned-quiz rows.

**Rationale**: Spec: compact marker, not a message. Auto-graded work never appears in the writing/quiz **review queue**, so list + row dots are required for those submits.

**Alternatives considered**: Count badges — extra UI and copy; spec does not require them.
