# API Contracts: Exercise Teacher Feedback

Error codes follow the existing convention (`{"error":"ERROR_CODE","message":"..."}`).

## Admin — save / update / clear

### `PUT /api/v1/admin/homework/submissions/{submissionId}/exercise-feedback`

Requires role `ADMIN`.

**Request**:
```json
{ "feedback": "string" }
```

- Non-blank string → save/replace plain-text feedback (≤ 2000 chars).
- Empty string or whitespace-only → clear feedback (`NULL`).

**Response** (`ExerciseSubmissionResultAdminDto` — same shape as GET exercise-result, including updated feedback):
```json
{
  "submissionId": "uuid",
  "assignmentId": "uuid",
  "assignmentTitle": "string",
  "studentId": "uuid",
  "studentEmail": "string",
  "studentFirstName": "string|null",
  "studentLastName": "string|null",
  "studentUsername": "string|null",
  "questions": [ /* unchanged */ ],
  "result": { /* unchanged ExerciseResultResponse */ },
  "teacherFeedback": "string|null"
}
```

**Errors**:
- `404 SUBMISSION_NOT_FOUND` — unknown id
- `404` / assignment mismatch style — not an EXERCISE assignment (reuse existing assignment/not-found patterns)
- `409 NOT_SUBMITTED` (or equivalent) — status is not `GRADED`
- `400 VALIDATION_ERROR` — length > 2000

Does **not** change `status` or `reviewed_at`. Does **not** send email.

---

## Admin — graded exercise detail (extended)

### `GET /api/v1/admin/homework/submissions/{submissionId}/exercise-result`

Unchanged path; response adds:

```json
{ "teacherFeedback": "string|null" }
```

---

## Admin — list indicators

### Homework assignees (existing homework admin item / assignee payload)

Each assignee gains:

```json
{ "hasTeacherFeedback": false }
```

`true` only when that student’s submission is `GRADED` (or otherwise has stored feedback) with non-empty teacher feedback. Writing `REVIEWED` items may also be `true` if feedback exists; UI indicator for this feature targets graded exercises primarily.

### `GET /api/v1/admin/users/{id}` student profile homework entries

Each homework row gains:

```json
{ "hasTeacherFeedback": false }
```

---

## Student — learning overview (extended)

### `GET /api/v1/learning`

Each `HomeworkItemResponse` gains:

```json
{ "hasTeacherFeedback": false }
```

`feedback` (FormattedText) remains as today for MANUAL/`REVIEWED`. Exercise items may leave `feedback` populated consistently with storage encoding or null at the API layer; clients should use `hasTeacherFeedback` for the list indicator and `teacherFeedback` on the exercise payload for the comment body.

---

## Student — exercise take/result (extended)

### `GET /api/v1/learning/homework/{assignmentId}/exercise` (existing get-exercise)

Response (`ExerciseResponse`) adds:

```json
{ "teacherFeedback": "string|null" }
```

Present when `status === "GRADED"` and the teacher has saved non-empty feedback; otherwise `null`. Shown on the locked result view.

Submit exercise endpoint unchanged (no teacher feedback on submit response required beyond subsequent GET).

---

## Out of scope (unchanged)

- `PUT /api/v1/admin/homework/submissions/{id}/feedback` — Writing only
- Review queue endpoints — Writing only
- Email / push notifications
