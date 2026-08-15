# Data Model: Reusable Word Bank

**Feature**: `042-reusable-word-bank` | **Date**: 2026-08-15

No new tables or columns. Stored `homework_questions.structure_json` / activity / quiz question JSON for `DRAG_DROP` is unchanged (canonical `bank` + `blanks[].correctBankIds`, plus legacy positional `bank` only).

## Placement mode (derived, not stored)

| Mode | When | Student occupancy |
|------|------|-------------------|
| Exclusive | No bank id appears in more than one blank’s `correctBankIds` | Each bank id in at most one blank; chip disabled while placed |
| Reusable | At least one bank id appears in two or more blanks’ `correctBankIds` | Same bank id may occupy several blanks; chips stay available |

Detection uses **bank item id**, not label. Two chips with the same text remain exclusive unless it is the same id.

Legacy positional keys are exclusive (each blank owns `bank[i].id` once).

## Student-facing structure (pre-submit)

```json
{
  "bank": [
    { "id": "uuid-1", "label": "el" },
    { "id": "uuid-2", "label": "la" }
  ],
  "bankReusable": true
}
```

| Field | Rule |
|-------|------|
| `bank` | Unchanged; ids + labels; no correctness |
| `bankReusable` | `true` or `false`; omit treated as `false` by the client |
| `blanks` / `correctBankIds` | **Must not** appear on student take payloads |

## Student answer (unchanged shape)

```json
{ "placements": ["uuid-1", "uuid-1"] }
```

| Rule | Constraint |
|------|------------|
| Length | Matches blank count (nulls allowed; empty scores incorrect) |
| Values | Bank ids or null |
| Duplicates | Allowed in reusable mode; exclusive take UI prevents them; grader does not reject duplicates |

## Grading (unchanged rule, duplicate occupancy now reachable)

Blank `i` is correct iff `placements[i]` is non-null and ∈ that blank’s `correctBankIds`. Units average as today. Result `unitResults[].studentDisplay` is the placed label per blank (may repeat).

## Validation (authoring — no change)

Existing 034 rules stand: bank size ∈ [blankCount, 30]; 1–10 correct ids per blank; same id **may** appear on multiple blanks.
