# Data Model: New Exercise Types

**Feature**: `024-new-exercise-types` | **Date**: 2026-08-03

Extends the homework exercise model from `007-homework-exercises`. Placement-test tables are out of scope.

## Schema changes (Flyway `V6__new_exercise_types.sql`)

### `homework_questions`

| Change | Detail |
|--------|--------|
| `kind` CHECK | Add `'MULTI_BLANK'`, `'DRAG_DROP'`, `'TABLE_FILL'`, `'MATCHING'` alongside existing three |
| `structure_json` | `JSONB NOT NULL DEFAULT '{}'` — kind-specific authoring payload (answer key included for teacher persistence; stripped for student DTOs) |

`prompt` remains required TEXT:
- `MULTI_BLANK` / `DRAG_DROP`: passage containing `___` tokens
- `TABLE_FILL` / `MATCHING`: optional short instruction / title line (may be empty string only if product validation allows — prefer non-empty prompt for consistency with other kinds; if empty needed, allow blank prompt for these two only)
- Choice / FILL_BLANK: unchanged

`homework_question_options` rows: **only** for SINGLE_CHOICE, MULTI_CHOICE, FILL_BLANK (unchanged). New kinds MUST persist with zero options rows.

### `homework_answers`

| Change | Detail |
|--------|--------|
| `answer_json` | `JSONB NULL` — student payload for new kinds; null for original kinds |

`answer_text` / `homework_answer_options` remain for FILL_BLANK / choice only.

## `QuestionKind`

`SINGLE_CHOICE` | `MULTI_CHOICE` | `FILL_BLANK` | `MULTI_BLANK` | `DRAG_DROP` | `TABLE_FILL` | `MATCHING`

## Structure JSON by kind (teacher / stored)

IDs inside structure are UUIDs assigned at authoring save (client may send temporary ids; server may reissue on replace — same full-replace semantics as today).

### MULTI_BLANK

```json
{
  "blanks": [
    { "acceptedAnswers": ["el", "El"] },
    { "acceptedAnswers": ["va"] }
  ]
}
```

- Length of `blanks` MUST equal count of `___` in `prompt` (exact-three token parse).
- Each blank: ≥1 accepted answer, non-blank after trim.
- 2–20 blanks.

### DRAG_DROP

```json
{
  "bank": [
    { "id": "uuid-1", "label": "manzana" },
    { "id": "uuid-2", "label": "pera" }
  ]
}
```

- `bank.length` MUST equal blank count in `prompt` (2–20).
- Bank item *i* is the correct placement for blank *i*.
- Labels non-empty after trim; ids unique within the question.

### TABLE_FILL

```json
{
  "rowHeaders": ["yo", "tú", "él"],
  "colHeaders": ["Presente"],
  "cells": [
    { "r": 0, "c": 0, "type": "blank", "acceptedAnswers": ["hablo"] },
    { "r": 1, "c": 0, "type": "blank", "acceptedAnswers": ["hablas"] },
    { "r": 2, "c": 0, "type": "fixed", "text": "habla" }
  ]
}
```

- Rectangular grid: `rowHeaders.length` = rows, `colHeaders.length` = cols (allow empty header strings).
- Every `(r,c)` in range appears once in `cells`.
- `type: "blank"` → ≥1 acceptedAnswers; `type: "fixed"` → `text` (may be empty for spacer).
- 1–12 rows/cols; 1–50 blanks.

### MATCHING

```json
{
  "left": [{ "id": "L1", "label": "dog" }, { "id": "L2", "label": "cat" }],
  "right": [{ "id": "R1", "label": "perro" }, { "id": "R2", "label": "gato" }, { "id": "R3", "label": "casa" }],
  "pairs": [{ "leftId": "L1", "rightId": "R1" }, { "leftId": "L2", "rightId": "R2" }]
}
```

- 1–20 items per side; labels non-empty; ids unique within side.
- `pairs`: ≥1; each `leftId`/`rightId` must exist; each left id at most one pair; each right id at most one pair.
- Unpaired items are distractors (either side).

## Student answer JSON (`answer_json`)

### MULTI_BLANK

```json
{ "blanks": ["el", "va"] }
```

Array length SHOULD equal blank count; missing/short treated as empty → unit score 0.

### DRAG_DROP

```json
{ "placements": ["uuid-1", null, "uuid-2"] }
```

Per-blank bank item id or null; unknown id → 0.

### TABLE_FILL

```json
{ "cells": { "0,0": "hablo", "1,0": "hablas" } }
```

Keys `"r,c"` for blank cells only; omitted → 0.

### MATCHING

```json
{ "pairs": [{ "leftId": "L1", "rightId": "R1" }] }
```

Student may omit pairs; each expected pair is a unit.

## Grading

| Kind | Unit | Correct when |
|------|------|----------------|
| MULTI_BLANK | each blank | normalize(student) ∈ normalize(acceptedAnswers) |
| DRAG_DROP | each blank | placement id === bank[i].id |
| TABLE_FILL | each blank cell | same normalize rule as FILL_BLANK |
| MATCHING | each expected pair | student has that leftId↔rightId |

`normalize` = trim + lower case (`Locale.ROOT`); accents preserved.

Question `score` = mean(unit scores) in `[0,1]`. Overall percent / fully-correct count unchanged at exercise level.

## Student-facing structure (answer key stripped)

| Kind | Student sees |
|------|----------------|
| MULTI_BLANK | `prompt` only (render `___` → inputs); no `blanks` acceptedAnswers |
| DRAG_DROP | `prompt` + `bank` `{id,label}[]` (client shuffles); no correctness metadata |
| TABLE_FILL | headers + cells with `type`/`text` for fixed; blanks without acceptedAnswers |
| MATCHING | `left` + `right` labels/ids (client shuffles each); no `pairs` |

## Validation (authoring)

Reject save (`VALIDATION_ERROR`) when:
- New kind has options rows or empty/invalid structure
- MULTI_BLANK / DRAG_DROP: blank count ∉ [2,20] or blanks≠structure
- DRAG_DROP: bank length ≠ blanks
- TABLE_FILL: no blank cells, bad coords, limits exceeded
- MATCHING: no pairs, dangling ids, duplicate pair endpoints
- Any accepted answer / label empty after trim

## State / lifecycle

Unchanged: EXERCISE homework, single GRADED submission, full question replace on PUT does not re-grade past submissions (`question_id` may null out; stored scores remain).

## Relationships (unchanged)

`HomeworkAssignment` 1—* `HomeworkQuestion` 1—* `QuestionOption` (legacy kinds only)  
`HomeworkSubmission` 1—* `HomeworkAnswer` (*—* options for choice only)
