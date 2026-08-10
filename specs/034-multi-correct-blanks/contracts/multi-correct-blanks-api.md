# Contract: Multiple Correct Blank Answers

**Feature**: `034-multi-correct-blanks` | **Date**: 2026-08-10

No new HTTP endpoints. Extends existing admin create/update and learning take/result payloads via `structure` / `unitResults` for `MULTI_BLANK` and `DRAG_DROP`. Auth and error envelope unchanged (`VALIDATION_ERROR` on bad structure).

## Admin authoring

`POST`/`PUT` `/api/v1/admin/homework` (and `/api/v1/admin/activities`) — question `structure` rules:

### MULTI_BLANK

```json
{
  "kind": "MULTI_BLANK",
  "prompt": "___ va a ___ casa.",
  "structure": {
    "blanks": [
      { "acceptedAnswers": ["Él", "Ella"] },
      { "acceptedAnswers": ["la"] }
    ]
  }
}
```

| Rule | Error if violated |
|------|-------------------|
| `acceptedAnswers` length 1–10 per blank | `VALIDATION_ERROR` |
| Empty/blank strings after trim | `VALIDATION_ERROR` |
| `blanks.length` ≠ `___` count | `VALIDATION_ERROR` |

### DRAG_DROP (canonical)

```json
{
  "kind": "DRAG_DROP",
  "prompt": "Como ___ y ___.",
  "structure": {
    "bank": [
      { "id": "a", "label": "manzana" },
      { "id": "b", "label": "pera" },
      { "id": "c", "label": "casa" }
    ],
    "blanks": [
      { "correctBankIds": ["a", "b"] },
      { "correctBankIds": ["c"] }
    ]
  }
}
```

| Rule | Error if violated |
|------|-------------------|
| Bank size ∉ [blankCount, 30] | `VALIDATION_ERROR` |
| Missing/empty `correctBankIds`, or >10 | `VALIDATION_ERROR` |
| Unknown id / empty correct set | `VALIDATION_ERROR` |
| Same bank id on multiple blanks | Allowed |
| Legacy body `{ "bank": [...] }` only with `bank.length === blankCount` | Accepted; server normalizes to canonical on persist |

Admin GET detail returns **canonical** structure after save (with `blanks`).

## Student learning

### Pre-submit question structure

`DRAG_DROP`: `{ "bank": [ { "id", "label" }, ... ] }` only (no `blanks`, no correctness). Client may shuffle bank order.

`MULTI_BLANK`: prompt with blanks; no `acceptedAnswers` in student payload (unchanged).

### Submit answer (unchanged)

```json
{ "questionId": "…", "answerJson": { "placements": ["a", "c"] } }
```

```json
{ "questionId": "…", "answerJson": { "blanks": ["Él", "la"] } }
```

### Result `unitResults`

For MULTI_BLANK / DRAG_DROP units with **multiple** accepted answers/correct ids:

```json
{
  "index": 0,
  "score": 1.0,
  "correct": true,
  "studentDisplay": "pera",
  "expectedDisplay": ["manzana", "pera"]
}
```

`expectedDisplay` is populated even when `correct: true`. Single-accepted units may omit `expectedDisplay` when correct (today).

## Out of scope

- TABLE_FILL structure/grading/feedback contracts unchanged.
- No changes to choice / TRUE_FALSE / MATCHING / FREE_TEXT.
