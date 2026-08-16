# Data Model: Student-Based Homework Deadlines

**Feature**: `044-student-homework-deadlines` | **Date**: 2026-08-16

## Migration

`back-end/src/main/resources/db/migration/V24__homework_target_due_on.sql`

```sql
ALTER TABLE homework_targets
    ADD COLUMN due_on DATE;

COMMENT ON COLUMN homework_targets.due_on IS
    'Optional per-student due date for this homework; NULL means no deadline.';

ALTER TABLE homework_assignments
    DROP COLUMN due_on;
```

Existing targets remain `NULL` (no deadline). No backfill. No index required at current assignee counts.

If V24 is already taken on the branch, use the next free Flyway version; keep this filename intent.

## Entity: Homework assignment (narrowed)

| Field | Change |
|-------|--------|
| `dueOn` / `due_on` | **Removed.** The homework definition has no due date. |

Content freeze / `assignment_snapshot` still exclude due dates (they never belonged in the snapshot).

## Entity: Student–homework assignment (`homework_targets`)

| Field | Type | Rules |
|-------|------|--------|
| `due_on` | `DATE` or `null` | Optional calendar day for **this** student on **this** homework. Empty / omitted → `null`. Clearing sets `null`. |

Identity stays `UNIQUE (assignment_id, user_id)`. Deleting the target deletes the date.

## Derived: Overdue

Not stored.

| Condition | Overdue |
|-----------|---------|
| `due_on` is null | no |
| `due_on >= today` (teacher working calendar) | no |
| status is not `PENDING` (submitted / reviewed / graded) | no |
| `due_on < today` and status `PENDING` | yes |

`today` is the same teacher-zone date already used in `HomeworkItems`.

## Validation

| Input | Stored |
|-------|--------|
| omitted / `null` on assign or per-row update | `null` |
| valid ISO calendar date | that date (past allowed) |
| malformed date | reject (`VALIDATION_ERROR` / existing Jackson 400) |

No silent coerce to the homework-wide field (that field is gone).

## Write paths

| Action | Effect on `due_on` |
|--------|--------------------|
| `POST /homework` with `assigneeIds` + optional `dueOn` | INSERT targets with that date (or null) |
| `POST /homework` with `dueOn` but no assignees | Date discarded; nothing to attach to |
| `PUT .../assignees` `{ assigneeIds, dueOn? }` | Removed students: row gone. Remaining: **unchanged** `due_on`. Newly inserted: `dueOn` from the body (or null) |
| `PUT .../assignees/{userId}/due-on` | That row only |
| Unit assign / unit homework sync `addTargets` | INSERT with `due_on` null |
| Unassign / unit unassign `removeTargets` | Row (and date) deleted |
| Re-assign later | New INSERT; previous date is not restored |

None of these bump `content_revised_at`.

## Read paths

| Surface | Date shown |
|---------|------------|
| Student learning / take / unit homework item | That student's `homework_targets.due_on` + derived overdue |
| Admin homework item | **No** `dueOn` on the homework |
| Admin assignee row | That assignee's `due_on` + overdue |
| Student profile Tareas row | That student's `due_on` + overdue (view only) |

## Relationships

- Homework 1—* targets; each target 0..1 due date.
- Submission status (on `homework_submissions`) combines with the target date to derive overdue; submissions do not store the date.
- Quizzes, presentation activities: no homework target; unchanged.
