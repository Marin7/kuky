# Data Model: True/False Homework Exercises

**Feature**: `027-true-false-homework` | **Date**: 2026-08-06

Extends the homework exercise model from `007-homework-exercises` / `024-new-exercise-types`. Placement-test tables are out of scope.

## Schema changes (Flyway `V9__true_false_homework.sql`)

### `homework_questions`

| Change | Detail |
|--------|--------|
| `kind` CHECK | Add `'TRUE_FALSE'` alongside existing: `SINGLE_CHOICE`, `MULTI_CHOICE`, `MULTI_BLANK`, `DRAG_DROP`, `TABLE_FILL`, `MATCHING` |

No new columns. `structure_json` remains `'{}'` for TRUE_FALSE. `prompt` is required non-empty TEXT (plain statement; same as other exercise kinds).

### `homework_question_options`

Used for TRUE_FALSE (same as SINGLE_CHOICE / MULTI_CHOICE):

| Rule | Detail |
|------|--------|
| Row count | Exactly **2** |
| Positions | `0` = true, `1` = false (fixed; never shuffled) |
| Labels | Canonical stored values `true` and `false` (not teacher-editable free text) |
| Correctness | Exactly **one** row with `is_correct = true` |

### `homework_answers`

Unchanged. TRUE_FALSE submissions use `selected_option_ids` / `homework_answer_options` (single selected option id). `answer_json` stays null.

## `QuestionKind`

`SINGLE_CHOICE` | `MULTI_CHOICE` | `MULTI_BLANK` | `DRAG_DROP` | `TABLE_FILL` | `MATCHING` | **`TRUE_FALSE`**

`TRUE_FALSE.isStructured()` = **false** (options path, not structure_json).

## Entities

### True/False Item (logical)

| Attribute | Notes |
|-----------|--------|
| `prompt` | Non-empty plain text statement |
| `options[0]` | Canonical label `true`, may be correct |
| `options[1]` | Canonical label `false`, may be correct |
| Correct answer | Exactly one of the two options |

### Student answer

| Attribute | Notes |
|-----------|--------|
| `selectedOptionIds` | Empty (unanswered → score 0) or one option id |

## Validation rules

- `kind = TRUE_FALSE` ⇒ `structure` empty / `{}`; no structured payload.
- Prompt non-empty after trim.
- Exactly 2 options; positions 0 and 1; labels exactly `true` / `false` (case-sensitive canonical).
- Exactly one `correct: true`.
- Reject add/remove beyond the fixed pair (admin service).

## State / lifecycle

Same as other exercise questions: author → assign → student take → single submit → GRADED → read-only review with `correctOptionIds` revealed for wrong (and available in result DTO as today). Teacher edits after submissions do not rewrite past scores.
