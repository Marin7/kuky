# API Contracts: Rich Text Formatting for Writing Homework Feedback

Error codes follow the existing convention (`{"error":"ERROR_CODE","message":"..."}`, see `contracts/api.md` at the repo root / `GlobalExceptionHandler`).

## Shared type: `FormattedText`

```json
[
  { "text": "string, required, non-empty", "color": "red|green|blue|neutral", "highlight": "yellow|green|pink", "strike": true }
]
```

`color`, `highlight`, `strike` are all optional and independent. Total concatenated `text` length ≤ 2000 chars.

---

## Student-facing (existing endpoint, changed request/response shape)

### `PUT /api/v1/learning/homework/{assignmentId}`

Requires role `STUDENT` or `ADMIN` (unchanged).

**Request** (`SubmitHomeworkRequest`):
```json
{ "response": FormattedText }
```
(was: `{ "response": "plain string" }`)

**Response** (`HomeworkItemResponse`, relevant fields):
```json
{
  "status": "SUBMITTED",
  "response": FormattedText,
  "feedback": null
}
```

Errors (existing + new):
- `400 VALIDATION_ERROR` — empty segment array, empty `text` in a segment, length > 2000, or unknown `color`/`highlight` value.
- `409 ALREADY_REVIEWED` — attempting to resubmit a `REVIEWED` submission (existing rule, unchanged).

### `GET /api/v1/learning` (overview) — unchanged endpoint, response addition

Each MANUAL homework item now includes:
```json
{
  "response": FormattedText | null,
  "feedback": FormattedText | null,
  "status": "PENDING|SUBMITTED|REVIEWED"
}
```

---

## Teacher-facing (new endpoints, under existing `/api/v1/admin` surface, `ADMIN` role required)

### `GET /api/v1/admin/homework/submissions?status=SUBMITTED`

Cross-student review queue (FR-010). Lists all MANUAL-format submissions in the given status (default/only supported value: `SUBMITTED`).

**Response**:
```json
[
  {
    "submissionId": "uuid",
    "studentId": "uuid",
    "studentName": "string",
    "assignmentTitle": "string",
    "submittedAt": "2026-07-02T10:00:00Z"
  }
]
```

### `GET /api/v1/admin/homework/submissions/{submissionId}`

Full detail for the review screen.

**Response**:
```json
{
  "submissionId": "uuid",
  "studentId": "uuid",
  "studentName": "string",
  "assignmentTitle": "string",
  "status": "SUBMITTED|REVIEWED",
  "response": FormattedText,
  "feedback": FormattedText | null,
  "submittedAt": "2026-07-02T10:00:00Z",
  "reviewedAt": "2026-07-02T10:00:00Z" 
}
```

Errors: `404 NOT_FOUND` if no such submission.

### `PUT /api/v1/admin/homework/submissions/{submissionId}/feedback`

Teacher saves feedback and marks the submission reviewed (FR-002).

**Request**:
```json
{ "feedback": FormattedText }
```

**Response**: same shape as the `GET .../{submissionId}` detail response above, with `status: "REVIEWED"`, `reviewedAt` set.

Errors:
- `400 VALIDATION_ERROR` — empty feedback (edge case: teacher cannot mark reviewed with no feedback content), or invalid segment values.
- `409 ALREADY_REVIEWED` — submission is already `REVIEWED` (feedback is terminal, FR-007).
- `409 NOT_SUBMITTED` — submission is still `PENDING` (nothing to review yet).

---

## Student-profile indicator (existing endpoint, response addition)

### `GET /api/v1/admin/students/{id}/profile`

Each entry in `homeworks[]` (`StudentProfileHomeworkDto`) gains:
```json
{ "needsReview": true }
```
`true` when that assignment is MANUAL-format and currently `SUBMITTED` (FR-010's per-student indicator).
