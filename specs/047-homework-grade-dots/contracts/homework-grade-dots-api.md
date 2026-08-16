# Contract: Homework Grade / Feedback Student Dots

**Feature**: `047-homework-grade-dots` | **Date**: 2026-08-16

Base `/api/v1`. JSON, HTTP-only JWT cookie (`credentials: 'include'`). Errors unchanged.

Extends [041 notifications-api.md](../../041-notification-system/contracts/notifications-api.md). No new error codes. No new paths required.

---

## GET `/notifications/badges`

Authenticated. Same body:

```json
{
  "panel": false,
  "homework": false,
  "quiz": false,
  "learning": false
}
```

| Field | Change |
|-------|--------|
| `panel` / `homework` / `quiz` | Unchanged (teacher submission/attempt unseen). |
| `learning` | **Also** true when this student has any `homework_submissions` row with `student_grade_seen_at IS NULL` or `student_feedback_seen_at IS NULL`. Still true for unseen unit / quiz / homework-target assignment. |

Non-student: `learning` still always false.

---

## Implicit create-unseen (existing admin writes)

| Request | Effect |
|---------|--------|
| `PUT /admin/homework/submissions/{id}/feedback` with **finalize** (first visible teacher percents) | `student_grade_seen_at = NULL`; `student_feedback_seen_at = NULL` if that save has a written comment and/or annotations |
| Same PUT, progress only (still awaiting) | No review-column writes |
| Same PUT on already-`GRADED` (re-edit) | NULL `student_grade_seen_at` if percents changed; NULL `student_feedback_seen_at` if comment and/or annotations changed; no-op if neither changed |
| Same PUT that **clears** all comments and annotations | `student_feedback_seen_at = NOW()`; grade column unchanged |
| `PUT /admin/homework/submissions/{id}/exercise-feedback` | If homework is already visible (`GRADED`): NULL `student_feedback_seen_at` when feedback text **changes** to non-empty; `NOW()` when cleared to empty and there is no other student-visible comment/annotation left. No write if text unchanged. Awaiting/non-graded: no write. |

No review-column writes: `PUT /admin/activities/submissions/{id}/feedback`, activity exercise-feedback, quiz grade PUTs, student submit `PUT /learning/homework/{id}` or `.../answers` (those set both columns to `NOW()`).

---

## Implicit mark-seen (existing student endpoints)

| Request | Effect |
|---------|--------|
| `GET /learning/homework/{assignmentId}` | Existing: marks homework-**target** assignment seen. **New:** if this user’s submission is `GRADED`, also set both review columns to `NOW()` where NULL. |
| `POST /learning/homework/{assignmentId}/seen` | Existing: marks homework-target assignment seen. **New:** if this user’s submission is `GRADED`, also mark both review columns seen. |
| `POST /learning/units/{unitId}/seen` | Unchanged — unit assignment only. Does **not** mark homework review seen. |
| `GET /learning` | Unchanged — does **not** mark review seen. |

Idempotent: `SET … = NOW() WHERE … IS NULL`.

---

## List flags (additive meaning)

`GET /learning` homework items: `unseen` is true if the assignment target is unseen **or** the student’s submission is review-unseen. Absent/`false` means no student icon on that row. Clients already render a single `NotificationDot` from `unseen`.

---

## Out of scope for this contract

- New badge fields, websocket, email.
- Teacher DTOs.
- Presentation activity and quiz student-review flags.
