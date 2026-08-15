# Contract: Quiz API

**Feature**: `040-quiz-terminology` | **Date**: 2026-08-15

Base `/api/v1`. JSON, HTTP-only JWT cookie (`credentials: 'include'`). Errors `{"error":"CODE","message":"..."}`.

Student: `/quizzes/**` — `STUDENT` or `ADMIN`, plus assignee (or existing submitted attempt for result GET).  
Admin: `/admin/quizzes/**` — `ADMIN`.

Placement `/placement/**` and `/admin/placement/**` are **removed**.

---

## Errors

| Code | HTTP | When |
|------|------|------|
| `QUIZ_NOT_FOUND` | 404 | Unknown quiz/attempt, or not owned |
| `QUIZ_NOT_ASSIGNED` | 403 | Student is not an assignee (and has no submitted attempt) |
| `QUIZ_ALREADY_SUBMITTED` | 409 | Second submit |
| `VALIDATION_ERROR` | 400 | Authoring/submit completeness, assign with no questions, listening without media |
| `UNAUTHENTICATED` | 401 | Anonymous |
| `ACCESS_DENIED` | 403 | Non-admin on admin routes; `USER` role on `/quizzes/**` |

Removed: `PLACEMENT_NOT_FOUND`, `SECTION_ALREADY_SUBMITTED`, `SECTION_NOT_STARTED`.

---

## Student

### GET `/quizzes`

Assigned quizzes for the current student (not a catalogue). Include status from attempt if any.

`200 OK`

```json
{
  "quizzes": [
    {
      "id": "uuid",
      "title": "…",
      "description": "…",
      "status": "AVAILABLE|IN_PROGRESS|SUBMITTED|GRADED"
    }
  ]
}
```

`AVAILABLE` = assigned, no attempt. Empty list if none.

### GET `/quizzes/{quizId}`

- No attempt yet: live non-retired questions (keys stripped), **and** creating `IN_PROGRESS` + snapshot is allowed on this GET (idempotent start). Response `status: IN_PROGRESS` after start.
- `IN_PROGRESS`: snapshot questions, keys stripped.
- `SUBMITTED` / `GRADED`: snapshot + scores + per-skill; keys revealed for auto questions as homework.

Student question DTO matches homework take (`kind`, `prompt`, `structure` stripped, `options` without `correct`) plus `skill` and listening media fields.

`200` / `404 QUIZ_NOT_FOUND` / `403 QUIZ_NOT_ASSIGNED`

### PUT `/quizzes/{quizId}/answers`

Submit once. Body same shape as homework mixed submit (`answers[]` with `questionId` + kind payload). Graded against **snapshot**.

`200 OK`

```json
{
  "status": "SUBMITTED|GRADED",
  "scorePercent": 80,
  "fullyCorrectCount": 8,
  "questionUnitCount": 10,
  "skills": [
    { "skill": "READING", "scorePercent": 100, "fullyCorrectCount": 3, "questionUnitCount": 3, "awaitingTeacher": false },
    { "skill": "WRITING", "scorePercent": null, "fullyCorrectCount": null, "questionUnitCount": 1, "awaitingTeacher": true }
  ],
  "questions": ["<result items, homework-shaped, plus skill>"]
}
```

All-auto → `GRADED` and numeric overall/skills immediately. Any FREE_TEXT → `SUBMITTED`; overall `scorePercent` may be omitted or provisional auto-only; skills with only auto are filled; writing skill `awaitingTeacher: true`.

`409 QUIZ_ALREADY_SUBMITTED` if already submitted.

---

## Admin authoring

### GET `/admin/quizzes`

List: id, title, question count, assignee count, submission counts.

### POST `/admin/quizzes`

Create `{ "title", "description?" }` → `201` with id. Questions optional later.

### GET `/admin/quizzes/{id}`

Live quiz + non-retired questions **with** keys + assignees `{ id, name, email }`.

### PUT `/admin/quizzes/{id}`

Replace title, description, questions (homework authoring payload + `skill` + per-question media). Does not change assignees. Does not rewrite existing snapshots.

`VALIDATION_ERROR` if a `LISTENING` question lacks media or an auto kind lacks a key.

### DELETE `/admin/quizzes/{id}`

`204`. Cascades assignees, attempts, questions.

### PUT `/admin/quizzes/{id}/assignees`

`{ "studentIds": ["uuid"] }` replace set. Each id must be `STUDENT`. Quiz must have ≥1 live question.

`VALIDATION_ERROR` otherwise.

---

## Admin results / review

### GET `/admin/quizzes/{id}/attempts`

List attempts: student name, status, overall %, submittedAt.

### GET `/admin/quizzes/{id}/attempts/{attemptId}`

Snapshot questions + answers + overall + skills (same result shape as student). Teacher reviews FREE_TEXT against **snapshot**, not live quiz.

### PUT `/admin/quizzes/{id}/attempts/{attemptId}/review`

Homework-parity: `teacherPercent` per FREE_TEXT question id, optional annotations, optional note. Finalize when every FREE_TEXT has a percent → `GRADED` and combined overall/skills.

`VALIDATION_ERROR` if finalize with any FREE_TEXT still null.

### GET `/admin/students/{studentId}/quizzes`

Attempts for that student (replaces placement evaluation GET). No CEFR.

---

## Removed endpoints

All `/api/v1/placement/**` and `/api/v1/admin/placement/**` (test, attempts, sections, writing, config, levels, questions, student evaluation).

`GET /api/v1/audio/{id}` unchanged (shared files).
