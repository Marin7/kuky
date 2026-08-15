# Contract: Notifications API

**Feature**: `041-notification-system` | **Date**: 2026-08-15

Base `/api/v1`. JSON, HTTP-only JWT cookie (`credentials: 'include'`). Errors `{"error":"CODE","message":"..."}`.

No new error codes beyond existing `UNAUTHENTICATED`, `ACCESS_DENIED`, `UNIT_NOT_FOUND`, `QUIZ_NOT_FOUND` / `QUIZ_NOT_ASSIGNED`.

---

## GET `/notifications/badges`

Authenticated. Cheap summary for nav/tabs. Refresh on login and client navigation.

`200 OK`

```json
{
  "panel": false,
  "homework": false,
  "quiz": false,
  "learning": false
}
```

| Field | Who | True when |
|-------|-----|-----------|
| `panel` | ADMIN | `homework` or `quiz` |
| `homework` | ADMIN | Any unseen homework submission |
| `quiz` | ADMIN | Any unseen quiz attempt |
| `learning` | STUDENT | Any unseen unit or quiz assignment for this user |

Non-admin: `panel`/`homework`/`quiz` always false. Non-student: `learning` always false. `USER`: all false.

---

## POST `/learning/units/{unitId}/seen`

`STUDENT` or `ADMIN`. Marks **this user’s** unit assignment seen. Idempotent.

- Assigned to that unit: `200 OK` `{ "unseen": false }`
- Not assigned / unknown unit: `404 UNIT_NOT_FOUND` (do not leak other units)

Does not mark homeworks or quizzes.

---

## Implicit mark-seen (existing endpoints)

| Request | Effect |
|---------|--------|
| `GET /admin/homework/submissions/{id}` | `teacher_seen_at` on that submission |
| `GET /admin/homework/submissions/{id}/exercise-result` | same |
| `GET /admin/quizzes/{quizId}/attempts/{attemptId}` | `teacher_seen_at` on that attempt |
| `GET /quizzes/{quizId}` | `student_seen_at` on this user’s `quiz_assignees` row |

Not marked: `GET /admin/homework`, `GET /admin/homework/{id}`, `GET /admin/quizzes`, `GET /learning`, `GET /quizzes` (list), student profile GET, assignee list inside the editor.

---

## Create unseen (existing write endpoints)

| Event | Write |
|-------|--------|
| Student `PUT /learning/homework/{id}` or `.../answers` that becomes SUBMITTED/GRADED | `teacher_seen_at = NULL` on that submission |
| Student `PUT /quizzes/{id}/answers` (submit) | `teacher_seen_at = NULL` on that attempt |
| Admin `PUT /admin/units/{id}/assignees` — **added** students only | those `unit_assignments.student_seen_at = NULL` |
| Admin `PUT /admin/quizzes/{id}/assignees` — **added** students only | those `quiz_assignees.student_seen_at = NULL` |

No unseen write: activity submit, `PUT /admin/homework/{id}/assignees`, `PUT /admin/units/{id}/homeworks`, feedback/grade PUTs.

---

## List flags (additive)

Existing 200 bodies gain booleans as in [data-model.md](../data-model.md). Absent/`false` means no icon. Clients must tolerate the new fields; old clients ignore them.
