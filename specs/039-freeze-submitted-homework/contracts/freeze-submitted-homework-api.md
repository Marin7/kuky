# Contract: Freeze Submitted Homework API

**Feature**: `039-freeze-submitted-homework` | **Date**: 2026-08-15

No new routes. Extends existing homework admin + learning payloads. Auth unchanged. Activities and placement: **no change**.

Errors stay `{"error":"ERROR_CODE","message":"..."}`.

## New error

| Code | HTTP | When |
|------|------|------|
| `HOMEWORK_UPDATED` | 409 | Student submit (`PUT /api/v1/learning/homework/{id}` or `.../answers`) whose `contentRevisedAt` is missing or not equal to the live assignment’s `content_revised_at`. No grade, no snapshot, status stays unsubmitted. |

Do not reuse `SUBMISSION_NOT_ALLOWED` (already used for re-submit after lock).

## Teacher authoring

`PUT /api/v1/admin/homework/{id}` — unchanged body.

Server:

- Persists live assignment + questions as today.
- Bumps `content_revised_at` if title, instructions, homeworkType, level, media, or questions changed.
- Does **not** bump for due-date-only.
- Retires referenced questions/options instead of deleting them.
- Does **not** rewrite `assignment_snapshot`, scores, or teacher review on existing submissions.

`PUT /api/v1/admin/homework/{id}/assignees` — unchanged; does not bump `content_revised_at`.

Editor GET still returns **live** non-retired questions (current homework), never a student’s snapshot.

## Student take (not submitted)

`GET /api/v1/learning/homework/{assignmentId}` while PENDING:

Add:

```json
{
  "contentRevisedAt": "2026-08-15T09:12:00Z",
  "title": "<live title>",
  "questions": ["<live non-retired>"]
}
```

`contentRevisedAt` is omitted (or ignored) once the homework is submitted.

### Submit

`PUT /api/v1/learning/homework/{assignmentId}` (WRITE / manual)  
`PUT /api/v1/learning/homework/{assignmentId}/answers` (exercise / mixed)

Body adds:

```json
{
  "contentRevisedAt": "2026-08-15T09:12:00Z"
}
```

| Case | Result |
|------|--------|
| Token matches, first submit | Grade/store as today; write `assignment_snapshot` from live homework; lock submit |
| Token mismatch or omitted | `409 HOMEWORK_UPDATED` |
| Already submitted | existing `SUBMISSION_NOT_ALLOWED` / conflict |

## Student result / teacher review

After submit, `GET /api/v1/learning/homework/{assignmentId}` and admin:

- `GET /api/v1/admin/homework/submissions/{submissionId}`
- `GET /api/v1/admin/homework/submissions/{submissionId}/exercise-result`

return **snapshot** title, instructions, media, questions (including `correct` / expected answers as today’s post-submit payloads already do). `dueOn` and assignment id stay live operational fields.

`scorePercent`, per-answer scores, `unitResults`, and teacher percents come from stored submission/answers, not from re-grading the live key.

List endpoints (`GET /api/v1/learning`, admin homework list) may keep the **live** title so the teacher can find the homework they just edited.

## Activities

`/api/v1/learning/activities/**` and `/api/v1/admin/activities/**` unchanged (no `contentRevisedAt`, no snapshot).
