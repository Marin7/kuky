# Contract: Per-Question Homework Grading API

**Feature**: `033-per-question-grading` | **Date**: 2026-08-10

Extends learning + admin homework/activity endpoints. Auth: `STUDENT`/`ADMIN` for learning; `ADMIN` for authoring/review. Errors: `{"error":"ERROR_CODE","message":"..."}`.

## Composition helper (response field)

All homework/activity item payloads gain:

| Field | Values | Meaning |
|-------|--------|---------|
| `composition` | `WRITE` \| `ALL_MANUAL` \| `ALL_AUTO` \| `MIXED` | Derived; preferred UI discriminator |
| `format` | `MANUAL` \| `EXERCISE` \| `MIXED` | Derived cache (compat); **omit or ignore on write** |

## Authoring (admin)

### Create / update homework

`POST /api/v1/admin/homework` · `PUT /api/v1/admin/homework/{id}`

`format` is **not required** (ignored if present). Server derives `format` / `composition` from `homeworkType` + `questions`.

```json
{
  "title": "Escucha mixta",
  "instructions": "Escucha y responde.",
  "homeworkType": "AUDIO",
  "audioFileId": "…",
  "questions": [
    {
      "kind": "SINGLE_CHOICE",
      "prompt": "¿Cuál es el tema?",
      "options": [
        { "text": "Deportes", "correct": false },
        { "text": "Política", "correct": true }
      ],
      "structure": {}
    },
    {
      "kind": "FREE_TEXT",
      "prompt": "Resume la idea principal en una frase.",
      "options": [],
      "structure": {}
    }
  ],
  "assigneeIds": ["…"]
}
```

| Case | `questions` rules |
|------|-------------------|
| `homeworkType = WRITE` | `[]` required; composition `WRITE` |
| Non-WRITE | ≥1 question; any mix of `FREE_TEXT` + structured kinds |
| Structured kind | Existing option/structure validation |
| `FREE_TEXT` | Prompt required; no answer key / options |

`VALIDATION_ERROR` if rules fail.

### Create / update activity

`POST /api/v1/admin/activities` · `PUT /api/v1/admin/activities/{id}` — same question rules as non-WRITE homework (no WRITE). May yield `MIXED`.

### Admin detail

`GET /api/v1/admin/homework/{id}` (activity mirror) returns `composition`, derived `format`, full questions (keys included for admin).

## Student learning

### Item payload (pre-submit MIXED)

```json
{
  "id": "…",
  "composition": "MIXED",
  "format": "MIXED",
  "homeworkType": "AUDIO",
  "status": "PENDING",
  "questions": [
    {
      "id": "q1",
      "kind": "SINGLE_CHOICE",
      "prompt": "¿Cuál es el tema?",
      "options": [{ "id": "o1", "text": "Deportes" }, { "id": "o2", "text": "Política" }],
      "structure": {}
    },
    { "id": "q2", "kind": "FREE_TEXT", "prompt": "Resume…", "position": 1 }
  ],
  "answers": null,
  "scorePercent": null,
  "provisionalScorePercent": null
}
```

Structured student questions **omit** correct flags / answer-bearing structure fields (same stripping as today).

### Submit non-WRITE (unified)

`PUT /api/v1/learning/homework/{assignmentId}/answers`  
`PUT /api/v1/learning/activities/{activityId}/answers`

```json
{
  "answers": [
    { "questionId": "q1", "selectedOptionIds": ["o2"] },
    { "questionId": "q2", "text": "La política europea." }
  ]
}
```

Structured answer shapes remain as in exercise-types contracts (`answer` / `answer_json` fields per kind). `FREE_TEXT` uses `{ questionId, text }` (non-empty, max length as today).

| Composition | Result status | Scoring |
|-------------|---------------|---------|
| `ALL_AUTO` | `GRADED` | Final `scorePercent` |
| `ALL_MANUAL` | `SUBMITTED` | No score |
| `MIXED` | `SUBMITTED` | Auto answers scored; keys revealed for auto; `scorePercent` null; optional `provisionalScorePercent` (auto-only mean) |

`VALIDATION_ERROR` if any question missing/blank. `SUBMISSION_NOT_ALLOWED` if already submitted/graded (no retake).

WRITE remains:

`PUT /api/v1/learning/homework/{assignmentId}` with `{ "response": FormattedText }` → `SUBMITTED`.

### Post-submit MIXED (awaiting teacher)

```json
{
  "composition": "MIXED",
  "status": "SUBMITTED",
  "scorePercent": null,
  "provisionalScorePercent": 100,
  "questions": [ "/* auto questions include key reveal for wrong items */" ],
  "answers": [
    {
      "questionId": "q1",
      "kind": "SINGLE_CHOICE",
      "score": 1.0,
      "correct": true
    },
    {
      "questionId": "q2",
      "kind": "FREE_TEXT",
      "promptSnapshot": "Resume…",
      "text": "La política europea.",
      "teacherValidation": null
    }
  ]
}
```

Exact auto result field names follow existing `ExerciseResult` / per-kind result DTOs embedded or paralleled in the item response.

### Post-finalize MIXED

```json
{
  "status": "GRADED",
  "scorePercent": 50,
  "fullyCorrectCount": 1,
  "questionCount": 2,
  "provisionalScorePercent": null,
  "reviewModel": "ANNOTATED",
  "feedbackText": "Buen resumen, pero incompleto.",
  "answers": [
    { "questionId": "q1", "score": 1.0, "correct": true },
    {
      "questionId": "q2",
      "promptSnapshot": "Resume…",
      "formatted": [ { "text": "La política europea.", "strike": true } ],
      "teacherValidation": "INVALIDATED",
      "score": 0.0
    }
  ]
}
```

## Teacher review (admin)

### Review queue

`GET /api/v1/admin/homework/submissions` — includes `SUBMITTED` rows for `ALL_MANUAL`, `WRITE`, and **`MIXED`** (same awaiting bucket). Optionally return `composition` on each row.

### Finalize MIXED (extend feedback PUT)

`PUT /api/v1/admin/homework/submissions/{id}/feedback`  
`PUT /api/v1/admin/activities/submissions/{id}/feedback`

```json
{
  "feedbackText": "Opcional ≤500",
  "answers": [
    {
      "questionId": "q2",
      "teacherValidation": "INVALIDATED",
      "formatted": [ { "text": "La política europea.", "color": null, "highlight": null, "strike": true } ]
    }
  ]
}
```

| Rule | Behavior |
|------|----------|
| MIXED | Every FREE_TEXT answer must include `teacherValidation` `VALIDATED` or `INVALIDATED` |
| MIXED success | Status → `GRADED`; set combined `scorePercent`; store annotations/note; `review_model` per 032 |
| Missing validation | `VALIDATION_ERROR` — do not finalize |
| ALL_MANUAL / WRITE | Existing 032 review (`REVIEWED`); `teacherValidation` not required |
| Re-edit MIXED after `GRADED` | Allowed; recalculate `scorePercent` |
| LEGACY_RICH pure-manual | Remains frozen (032) |

### ALL_AUTO teacher view

Unchanged: `GET .../submissions/{id}/exercise-result`, `PUT .../exercise-feedback`.

## Error codes

| Code | When |
|------|------|
| `VALIDATION_ERROR` | Authoring/submit/finalize rules fail |
| `SUBMISSION_NOT_ALLOWED` | Retake / wrong endpoint for WRITE vs questions |
| `NOT_FOUND` | Unknown id |
| `FORBIDDEN` | Authz |

## Compatibility notes

- Clients must stop sending `format` as an authoring control; old clients sending `MANUAL`/`EXERCISE` are ignored in favor of derived value.
- Prefer `composition` for UI branching; `format === "MIXED"` is also reliable after migration.
- Manual-only `PUT /learning/homework/{id}` with `answers` (031) may remain accepted briefly for `ALL_MANUAL` then redirected to `/answers` — implementation may accept both during one release.
