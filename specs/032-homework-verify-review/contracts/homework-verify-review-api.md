# Contract: Homework Verify Review API

**Feature**: `032-homework-verify-review` | **Date**: 2026-08-10

Extends MANUAL homework + MANUAL activity admin review and student learning views. Auth: `ADMIN` for review; `STUDENT`/`ADMIN` for learning reads. Errors: `{"error":"ERROR_CODE","message":"..."}`.

## Admin — submission detail

`GET /api/v1/admin/homework/submissions/{submissionId}`  
`GET /api/v1/admin/activities/submissions/{submissionId}`

```json
{
  "submissionId": "…",
  "assignmentTitle": "…",
  "status": "SUBMITTED",
  "reviewModel": null,
  "response": [ { "text": "…", "color": "red", "highlight": null, "strike": false } ],
  "answers": [
    {
      "questionId": "q1",
      "promptSnapshot": "¿Qué…?",
      "text": "democracia",
      "formatted": null
    }
  ],
  "feedback": null,
  "feedbackText": null,
  "submittedAt": "…",
  "reviewedAt": null
}
```

| Field | Notes |
|-------|--------|
| `reviewModel` | `null` \| `"LEGACY_RICH"` \| `"ANNOTATED"` |
| `response` | WRITE FormattedText; `null` when multi answers present |
| `answers[].text` | Always plain wording (derived if stored as FormattedText) |
| `answers[].formatted` | FormattedText when annotated; else `null` (UI may treat plain `text` as single segment) |
| `feedback` | FormattedText for **LEGACY_RICH** only; `null` for ANNOTATED / unreviewed |
| `feedbackText` | Plain string for **ANNOTATED**; `null` when empty or not ANNOTATED |

`editableReview`: clients may treat as editable when `status === "SUBMITTED"` or (`status === "REVIEWED"` && `reviewModel === "ANNOTATED"`). Legacy: read-only.

## Admin — save / update review (new-model)

`PUT /api/v1/admin/homework/submissions/{submissionId}/feedback`  
`PUT /api/v1/admin/activities/submissions/{submissionId}/feedback`

**Request (new shape):**

```json
{
  "feedbackText": "Bien — revisa el género.",
  "response": [ { "text": "Hola", "color": "red", "strike": true } ],
  "answers": [
    {
      "questionId": "q1",
      "formatted": [ { "text": "democracia", "highlight": "yellow" } ]
    }
  ]
}
```

| Field | Rules |
|-------|--------|
| `feedbackText` | Optional; max **500** chars after strip; omit/blank clears |
| `response` | Required for WRITE submissions; omit/null for multi |
| `answers` | Required for multi FREE_TEXT; each row’s `plainText(formatted)` must match stored wording; omit/null for WRITE |

**Behavior**:
- `SUBMITTED` → set `REVIEWED`, `review_model = ANNOTATED`, set `reviewed_at`, store annotations + plain feedback.
- `REVIEWED` + `ANNOTATED` → update annotations + plain feedback; keep `reviewed_at`.
- `REVIEWED` + `LEGACY_RICH` → **`ALREADY_REVIEWED`** (frozen).
- Other statuses → `NOT_SUBMITTED` / not found as today.

**Response**: same shape as GET detail (with `reviewModel: "ANNOTATED"`, `feedbackText`, updated `formatted` / `response`).

**Validation**:
- `VALIDATION_ERROR` — feedback > 500; plainText mismatch; malformed FormattedText; wrong payload shape for WRITE vs multi.
- `ALREADY_REVIEWED` — legacy frozen.
- Empty `feedbackText` allowed; annotations optional (identity FormattedText / plain wrap OK).

Backward note: old clients sending only `{ "feedback": FormattedText }` are **not** supported for new reviews; frontend ships with the new body. No dual-protocol required (single-teacher admin UI).

## Student learning views

`GET /api/v1/learning` item / homework detail paths and activity embeds:

For MANUAL after ANNOTATED review:

```json
{
  "status": "REVIEWED",
  "reviewModel": "ANNOTATED",
  "response": [ /* WRITE annotated */ ],
  "answers": [
    {
      "questionId": "q1",
      "promptSnapshot": "…",
      "text": "democracia",
      "formatted": [ { "text": "democracia", "highlight": "yellow" } ]
    }
  ],
  "feedback": null,
  "feedbackText": "Bien — revisa el género."
}
```

For `LEGACY_RICH`: keep today’s `feedback: FormattedText` and un-annotated answers/`response`; `feedbackText` null; `reviewModel: "LEGACY_RICH"`.

Student cannot PUT annotations; answers remain read-only when `REVIEWED` (existing rules).

## Unchanged

- `PUT …/exercise-feedback` (homework + activity) — still plain ≤2000, `GRADED` only.
- EXERCISE take/grade APIs.
- Student MANUAL submit bodies (plain FREE_TEXT / WRITE FormattedText).
- Review queue list endpoints (optional later: indicator that item is legacy vs editable — not required by spec).
