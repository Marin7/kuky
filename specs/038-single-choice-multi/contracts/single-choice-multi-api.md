# Contract: Multi-Item Opción Única API

**Feature**: `038-single-choice-multi` | **Date**: 2026-08-15

No new endpoints. Extends existing homework/activity question DTOs, student take payloads, and result `unitResults`. Auth unchanged. Errors: `{"error":"ERROR_CODE","message":"..."}` (`VALIDATION_ERROR` for authoring/submit completeness).

Placement test: **no change**.

## Authoring (admin)

`POST`/`PUT` `/api/v1/admin/homework` (and activity question payloads) — `questions[]` as today.

### Classic (no `(N)` in `prompt`)

Unchanged:

```json
{
  "kind": "SINGLE_CHOICE",
  "prompt": "El plural de lápiz",
  "options": [
    { "label": "lápizes", "correct": false },
    { "label": "lápices", "correct": true }
  ],
  "structure": {}
}
```

### Numbered

```json
{
  "kind": "SINGLE_CHOICE",
  "prompt": "Elige: (1) ser / estar  (2) por / para  (3) muy / mucho",
  "options": [],
  "structure": {
    "items": [
      {
        "number": 1,
        "options": [
          { "id": "opt-1a", "label": "ser", "correct": true },
          { "id": "opt-1b", "label": "estar", "correct": false }
        ]
      },
      {
        "number": 2,
        "options": [
          { "id": "opt-2a", "label": "por", "correct": false },
          { "id": "opt-2b", "label": "para", "correct": true }
        ]
      },
      {
        "number": 3,
        "options": [
          { "id": "opt-3a", "label": "muy", "correct": true },
          { "id": "opt-3b", "label": "mucho", "correct": false }
        ]
      }
    ]
  }
}
```

| Save rule | Result |
|-----------|--------|
| Markers present, `options` non-empty | Server ignores options-table payload; persists `structure.items` only |
| Markers present, items missing / not 1…N | `VALIDATION_ERROR` |
| Item with fewer than 2 options or not exactly one correct | `VALIDATION_ERROR` |
| N &gt; 20 | `VALIDATION_ERROR` |
| No markers, `structure.items` sent | Treat as classic; ignore `items` (or `VALIDATION_ERROR` if items non-empty — prefer ignore + persist classic options) |

**Prefer**: if prompt has no markers, persist classic options and `structure_json = {}` even if the client left stale `items` (mode follows the prompt).

## Student take

GET exercise / mixed homework questions: numbered SINGLE_CHOICE includes stripped structure:

```json
{
  "id": "q1",
  "kind": "SINGLE_CHOICE",
  "prompt": "Elige: (1) ser / estar  (2) por / para  (3) muy / mucho",
  "options": [],
  "structure": {
    "items": [
      {
        "number": 1,
        "options": [
          { "id": "opt-1a", "label": "ser" },
          { "id": "opt-1b", "label": "estar" }
        ]
      }
    ]
  }
}
```

No `correct` flags. Classic still uses top-level `options`.

### Submit

Classic:

```json
{ "questionId": "q1", "selectedOptionIds": ["opt-correct"] }
```

Numbered:

```json
{
  "questionId": "q1",
  "selectedOptionIds": [],
  "answerJson": {
    "selections": {
      "1": "opt-1a",
      "2": "opt-2b",
      "3": "opt-3a"
    }
  }
}
```

| Submit rule | Result |
|-------------|--------|
| Any number 1…N missing or blank | `VALIDATION_ERROR` — do not grade |
| Unknown option id for that item | Incorrect (score 0) for that item |
| Classic unanswered | Existing mixed/all-auto completeness rules unchanged |

## Results

`ExerciseResultResponse` / mixed auto subset:

| Field | Numbered SINGLE_CHOICE |
|-------|------------------------|
| `totalQuestions` / denominator | **Expanded**: this entry adds N, not 1 |
| `fullyCorrectCount` | Count of items at 100%, not “whole entry correct” |
| `questions[].score` | Mean of that entry’s items (0–1), for the result card |
| `questions[].correct` | `true` iff **every** item correct |
| `questions[].unitResults` | One unit per item (`index` = number − 1) |
| `questions[].unitResults[].expectedDisplay` | Correct option label(s) when the item is wrong (same reveal as classic) |
| `questions[].correctOptionIds` | Empty (keys live on units) or union of correct ids — prefer **empty + units** to avoid implying one list |

Example: only that 3-item entry, 2 correct → `scorePercent` 67, `fullyCorrectCount` 2, `totalQuestions` 3.

Mixed finalize overall `%` uses the same expanded contributions (each item 0/100 alongside other questions and teacher percents).

## Activities

Same DTO shapes on activity admin + `ActivityStudentService` take/submit/result.
