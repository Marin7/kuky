# Contract: Student-Based Homework Deadlines API

**Feature**: `044-student-homework-deadlines` | **Date**: 2026-08-16

Auth unchanged (`ADMIN` on `/api/v1/admin/homework/**`; `STUDENT`/`ADMIN` on `/api/v1/learning/**`). Errors stay `{"error":"ERROR_CODE","message":"..."}`.

`dueOn` is always an ISO calendar date (`YYYY-MM-DD`) or `null`.

## Student learning (shape unchanged, source changed)

`GET /api/v1/learning` homework items and `GET /api/v1/learning/homework/{id}` keep:

| JSON | Type | Meaning |
|------|------|---------|
| `dueOn` | `string` \| `null` | **This student's** due date for this homework |
| `overdue` | `boolean` | Derived for this student (pending + date before today) |

Two students on the same homework may receive different `dueOn` / `overdue`. Submit after due remains allowed (existing submit rules).

## Admin homework item

`HomeworkAdminItem` **drops** `dueOn`.

`AssigneeDto` **gains**:

| JSON | Type | Meaning |
|------|------|---------|
| `dueOn` | `string` \| `null` | That assignee's date |
| `overdue` | `boolean` | Derived for that assignee |

## `POST /api/v1/admin/homework`

Remove homework-level due date as content. Optional `dueOn` in the body means **initial assignees only** (same as assign-time date).

```json
{
  "title": "El subjuntivo",
  "instructions": "...",
  "homeworkType": "GRAMMAR",
  "level": "B1",
  "assigneeIds": ["…-student-a", "…-student-b"],
  "dueOn": "2026-09-01"
}
```

Both new targets get `2026-09-01`. Omit `dueOn` or send `null` → both `null`. Empty `assigneeIds` → no targets; `dueOn` has no effect. Response item has no `dueOn`; assignees include `dueOn` / `overdue`.

## `PUT /api/v1/admin/homework/{id}`

Body **must not** include a homework due date (drop `dueOn` from `UpdateHomeworkRequest`). Content-only. Does not change any target `due_on`.

## `PUT /api/v1/admin/homework/{id}/assignees`

```json
{
  "assigneeIds": ["…-a", "…-b"],
  "dueOn": "2026-09-08"
}
```

| Student | Effect |
|---------|--------|
| Already assigned and still in the list | Keep existing `due_on` (`dueOn` in this body is ignored for them) |
| Newly added | INSERT with `dueOn` (or null if omitted) |
| Dropped from the list | Target deleted |

`dueOn` omitted/`null` → new rows have no date. Does **not** bump `content_revised_at`.

## `PUT /api/v1/admin/homework/{id}/assignees/{userId}/due-on`

**New.** Body:

```json
{ "dueOn": "2026-09-15" }
```

or `{ "dueOn": null }` to clear.

Response: full `HomeworkAdminItem` (same as other homework admin writes) so the assignee list can refresh.

| HTTP | Code | When |
|------|------|------|
| 404 | `NOT_FOUND` (existing assignment/student not-found style) | Unknown homework, or that user is not assigned |
| 400 | `VALIDATION_ERROR` | Malformed date |
| 200 | — | That target updated; others unchanged |

Does **not** bump `content_revised_at`. Students in progress must not get `HOMEWORK_UPDATED` from this call.

## Student profile

`GET /api/v1/admin/students/{id}` (existing profile) homework entries gain `dueOn` and `overdue` for **that** student. No write on this resource for dates.

## Unchanged / out of scope

- `PUT /api/v1/admin/units/{id}/assignees` — still no due date; new homework targets stay `due_on` null
- Quiz assign, activities, review-queue payloads (no homework due date today)
- Labels endpoint

## Unit admin homework embed

`HomeworkAdminItem` inside unit detail loses `dueOn` along with the list item. Assignees on that embed, if present, follow `AssigneeDto` (per-student dates).
