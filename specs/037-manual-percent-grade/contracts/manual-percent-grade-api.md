# Contract: Manual Percent Grade API

**Feature**: `037-manual-percent-grade` | **Date**: 2026-08-11

Extends admin feedback + learning result payloads from `033-per-question-grading`. Auth unchanged (`ADMIN` review; `STUDENT`/`ADMIN` learning). Errors: `{"error":"ERROR_CODE","message":"..."}`.

## Breaking field change

| Removed | Replacement |
|---------|-------------|
| `teacherValidation`: `VALIDATED` \| `INVALIDATED` | `teacherScorePercent`: integer `0`–`100` or `null` |

Applies to admin feedback request/response answer objects, WRITE top-level field, and student graded result answers.

## Teacher review

### Save progress / finalize

`PUT /api/v1/admin/homework/submissions/{id}/feedback`  
`PUT /api/v1/admin/activities/submissions/{id}/feedback`

```json
{
  "finalize": false,
  "feedbackText": "Opcional ≤500",
  "teacherScorePercent": 85,
  "response": [ { "text": "…", "color": null, "highlight": null, "strike": false } ],
  "answers": [
    {
      "questionId": "q2",
      "teacherScorePercent": 70,
      "formatted": [ { "text": "La política europea.", "strike": false } ]
    }
  ]
}
```

| Field | When |
|-------|------|
| `finalize` | `true` = require all percents and set `GRADED`; `false`/omit = progress save, stay `SUBMITTED` if not yet graded |
| `teacherScorePercent` (top-level) | WRITE only |
| `answers[].teacherScorePercent` | Each FREE_TEXT answer (ALL_MANUAL / MIXED / activities) |
| `feedbackText` / `formatted` / `response` | Optional annotations/note as today |

### Rules

| Case | Behavior |
|------|----------|
| Progress (`finalize` false), still awaiting | Persist any provided percents (subset OK); status stays `SUBMITTED`; do not publish final `scorePercent` |
| Finalize (`finalize` true) | Every FREE_TEXT (or WRITE top-level) must have non-null `teacherScorePercent` ∈ 0–100; else `VALIDATION_ERROR` |
| Finalize success | Status → `GRADED`; `scorePercent` = equal average (auto scores + manual percent/100), half-up; `fullyCorrectCount` = count of 100% contributions; `reviewModel` as today |
| Re-edit `GRADED` | `finalize: true` with all percents; recalc `scorePercent` |
| Out of range / non-integer percent | `VALIDATION_ERROR` |
| LEGACY_RICH frozen | Unchanged (032) — still not editable |

### Admin GET submission

`GET /api/v1/admin/homework/submissions/{id}` (activity mirror)

Returns `teacherScorePercent` on FREE_TEXT answers (and WRITE top-level) even while `SUBMITTED`, so the teacher can resume partial review. Includes `composition`, `status`, `scorePercent` (null until graded), annotations.

## Student learning

### While `SUBMITTED` (awaiting)

```json
{
  "status": "SUBMITTED",
  "scorePercent": null,
  "provisionalScorePercent": 100,
  "answers": [
    { "questionId": "q1", "kind": "SINGLE_CHOICE", "score": 1.0, "correct": true },
    {
      "questionId": "q2",
      "kind": "FREE_TEXT",
      "promptSnapshot": "Resume…",
      "text": "La política europea."
    }
  ]
}
```

- **Must omit** `teacherScorePercent` on manual answers (even if teacher saved drafts).
- **Must not** expose a final overall that includes teacher manual scores.
- MIXED: auto feedback / keys / `provisionalScorePercent` unchanged.

### After `GRADED`

```json
{
  "status": "GRADED",
  "scorePercent": 85,
  "fullyCorrectCount": 1,
  "questionCount": 2,
  "feedbackText": "Buen resumen.",
  "answers": [
    { "questionId": "q1", "score": 1.0, "correct": true },
    {
      "questionId": "q2",
      "formatted": [ { "text": "La política europea." } ],
      "teacherScorePercent": 70,
      "score": 0.7
    }
  ]
}
```

WRITE graded example: top-level `teacherScorePercent` equals `scorePercent`.

## Error codes

| Code | When |
|------|------|
| `VALIDATION_ERROR` | Missing percents on finalize; percent out of 0–100; non-integer; wording changed illegally (existing annotate rules) |
| `NOT_FOUND` | Unknown submission |
| `FORBIDDEN` | Authz |
| `CONFLICT` / existing already-reviewed codes | LEGACY_RICH frozen / illegal re-edit (keep current codes) |

## Compatibility

- Clients must stop sending `teacherValidation`.
- Prefer `teacherScorePercent` for UI; treat absence/`null` as unscored.
- Historical rows migrated to 100/0; overall `%` for already-graded binary reviews unchanged.
