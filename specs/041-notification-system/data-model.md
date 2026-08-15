# Data Model: In-App Activity Notifications

**Feature**: `041-notification-system` | **Date**: 2026-08-15

Flyway **`V22__activity_unseen_flags.sql`**. No new tables.

## Entities

### Homework submission (existing)

`homework_submissions` adds:

| Field | Type | Notes |
|-------|------|--------|
| `teacher_seen_at` | timestamptz NULL | NULL after a successful submit until the teacher opens that submission’s review/result GET. Backfill `NOW()` for rows already `SUBMITTED`/`REVIEWED`/`GRADED`. PENDING stays NULL and is **not** unseen. |

Unseen teacher homework item: `status IN ('SUBMITTED','REVIEWED','GRADED') AND teacher_seen_at IS NULL`.

Partial index: `(assignment_id)` WHERE unseen (for Tareas card + assignee rows).

### Quiz attempt (existing)

`quiz_attempts` adds:

| Field | Type | Notes |
|-------|------|--------|
| `teacher_seen_at` | timestamptz NULL | NULL after submit until the teacher opens that attempt GET. Backfill `NOW()` for existing `SUBMITTED`/`GRADED`. `IN_PROGRESS` is not unseen. |

Unseen: `status IN ('SUBMITTED','GRADED') AND teacher_seen_at IS NULL`.

### Unit assignment (existing)

`unit_assignments` adds:

| Field | Type | Notes |
|-------|------|--------|
| `student_seen_at` | timestamptz NULL | NULL when the student is **newly** assigned. Set on `POST /learning/units/{unitId}/seen`. Backfill `NOW()` for existing rows. |

Unseen student unit: `student_seen_at IS NULL`. Unique `(unit_id, user_id)` unchanged. `ON DELETE CASCADE` from unit or user removes the icon (FR-014).

### Quiz assignee (existing)

`quiz_assignees` adds:

| Field | Type | Notes |
|-------|------|--------|
| `student_seen_at` | timestamptz NULL | NULL when newly assigned. Set on student `GET /quizzes/{quizId}`. Backfill `NOW()` for existing rows. |

Unseen student quiz: `student_seen_at IS NULL`. PK `(quiz_id, user_id)`. Cascade on quiz/user delete.

### Homework target (existing)

`homework_targets` adds:

| Field | Type | Notes |
|-------|------|--------|
| `student_seen_at` | timestamptz NULL | NULL when newly assigned from Tareas. Set on `POST /learning/homework/{id}/seen` or exercise GET. Backfill `NOW()` for existing rows. Unit-synced targets are inserted already seen. |

Unseen student homework: `student_seen_at IS NULL`. Unique `(assignment_id, user_id)` unchanged.

## State

```text
Homework / quiz submit
  teacher_seen_at NULL  →  teacher opens submission/attempt GET  →  teacher_seen_at NOW()

Unit / quiz / homework assign (new student only)
  student_seen_at NULL  →  student opens unit/homework seen POST or quiz GET  →  student_seen_at NOW()

Unassign or delete target
  row gone  →  no icon
```

- Opening editor, tabs, lists, or Mi aprendizaje does not transition seen.
- Opening a homework inside a unit marks that homework target seen; it does not clear the unit assignment.
- Re-opening an already-seen item leaves `seen_at` unchanged (idempotent).
- Remaining assignees on a replace keep their `student_seen_at`.

## Validation

- Teacher unseen only after **submit** (not PENDING / IN_PROGRESS).
- Student unseen for **new** unit, quiz, or direct homework assignment (not presentation activities; unit-synced homework targets stay seen).
- Badge `panel` = any teacher homework unseen OR any teacher quiz unseen.
- Badge `homework` / `quiz` = that kind only.
- Badge `learning` = any of that student’s unit, quiz, or homework target rows with NULL `student_seen_at`.
- `USER` role: all badge flags false; no student assignment rows.

## DTO flags (existing payloads)

| DTO | Field | True when |
|-----|--------|-----------|
| `HomeworkAdminItem` | `hasUnseenSubmissions` | Any unseen submission on that assignment |
| `AssigneeDto` | `unseen` | That assignee’s submission is unseen |
| Review-queue homework/quiz item | `unseen` | That submission/attempt is unseen |
| `QuizAdminListItem` | `hasUnseenAttempts` | Any unseen attempt on that quiz |
| `QuizAttemptListItem` | `unseen` | That attempt is unseen |
| `StudentProfileHomeworkDto` | `unseen` | That submission is unseen |
| `StudentQuizSummary` | `unseen` | That student’s attempt is unseen (teacher view) |
| `UnitRef` (student learning) | `unseen` | That student’s `unit_assignments.student_seen_at` is NULL |
| Student quiz list item | `unseen` | That student’s `quiz_assignees.student_seen_at` is NULL |
