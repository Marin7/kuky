# Data Model: Multiple Correct Blank Answers

**Feature**: `034-multi-correct-blanks` | **Date**: 2026-08-10

No new tables or columns. Changes are confined to `homework_questions.structure_json` / `activity_questions.structure_json` (and equivalent) for `MULTI_BLANK` and `DRAG_DROP`.

## MULTI_BLANK (typed gaps)

Unchanged shape:

```json
{
  "blanks": [
    { "acceptedAnswers": ["el", "la"] },
    { "acceptedAnswers": ["va"] }
  ]
}
```

### Validation

| Rule | Constraint |
|------|------------|
| Blank count | Equals `___` tokens in prompt (1–20) |
| Per blank acceptedAnswers | After trim: ≥1, ≤10; non-empty strings |
| Duplicates | Drop or reject normalized duplicates (trim + casefold for comparison; store stripped originals) — prefer reject on save with clear message, or silent dedupe; **implement: silent dedupe by normalize key, keep first** |
| Matching | Existing: trim, case-insensitive, accent-exact |

### Grading / feedback

- Unit correct iff student matches any accepted (`matchesAny`).
- If `acceptedAnswers.size() > 1` → `expectedDisplay` = all accepted (correct or not).
- If size == 1 → `expectedDisplay` = that answer only when incorrect (today).

## DRAG_DROP (word bank) — canonical

```json
{
  "bank": [
    { "id": "uuid-1", "label": "manzana" },
    { "id": "uuid-2", "label": "pera" },
    { "id": "uuid-3", "label": "uva" }
  ],
  "blanks": [
    { "correctBankIds": ["uuid-1", "uuid-2"] },
    { "correctBankIds": ["uuid-3"] }
  ]
}
```

Example: 2 blanks, 3 bank items — blank 0 accepts manzana **or** pera; uva is correct only for blank 1 (or could be a distractor if not listed).

### Validation

| Rule | Constraint |
|------|------------|
| Blank tokens in prompt | 2–20 |
| `blanks.length` | Equals blank token count |
| `bank.length` | ≥ blank count, ≤ 30; ids unique; labels non-empty after trim |
| Per blank `correctBankIds` | 1–10; each id ∈ bank; ids unique within blank |
| Across blanks | Same bank id **MAY** appear on multiple blanks (order-independent sets) |
| Coverage | Every blank has ≥1 correct id (implied) |

### Legacy shape (read-only compatibility)

```json
{
  "bank": [
    { "id": "uuid-1", "label": "manzana" },
    { "id": "uuid-2", "label": "pera" }
  ]
}
```

Interpreted as: `blanks[i].correctBankIds = [bank[i].id]`, requiring `bank.length === blankCount`.

### Student answer (unchanged)

```json
{ "placements": ["uuid-1", null] }
```

### Grading

| Case | Blank correct when |
|------|-------------------|
| Canonical | `placements[i]` ∈ `blanks[i].correctBankIds` |
| Legacy | `placements[i] === bank[i].id` |

`expectedDisplay`: labels for all correct bank ids when `correctBankIds.size() > 1` (always); when size == 1, label only if incorrect (same MAY as typed).

Question score = mean of blank 0/1 scores.

### Student-facing structure (pre-submit)

Strip answer key: send `bank` (shuffled client-side as today) **without** `blanks` / without revealing which ids are correct. Same as today for legacy (bank only).

## TABLE_FILL / others

No model changes. Do not alter accepted-answer caps or unit reveal behaviour.

## Entities (logical)

| Entity | Notes |
|--------|-------|
| Blank answer key | Typed strings or bank ids; max 10 |
| Word-bank item | `{ id, label }` in `bank[]` |
| Word-bank blank mapping | `correctBankIds[]` on `blanks[i]` |
| Graded unit result | Existing `UnitResultDto`; `expectedDisplay` semantics extended |

## Migration

None. Dual-read in services. Optional future cleanup: rewrite legacy JSON on read-modify-write only.
