# Contract: Reusable Word Bank

**Feature**: `042-reusable-word-bank` | **Date**: 2026-08-15

No new HTTP endpoints. Extends the existing student take `structure` object for `kind: "DRAG_DROP"`. Admin authoring and submit `answerJson` shapes are unchanged. Auth and error envelope unchanged.

## Admin authoring

Unchanged from [034 multi-correct-blanks](../../034-multi-correct-blanks/contracts/multi-correct-blanks-api.md). Same bank id on multiple blanks remains **allowed**.

## Student take (pre-submit)

Homework `GET` exercise, activity take, and quiz take question DTOs — `structure` for DRAG_DROP:

```json
{
  "bank": [
    { "id": "a", "label": "el" },
    { "id": "b", "label": "la" }
  ],
  "bankReusable": true
}
```

| Case | `bankReusable` |
|------|----------------|
| Any bank id listed in ≥2 blanks’ `correctBankIds` | `true` |
| Otherwise, including legacy positional bank | `false` |

**Must not** include `blanks` or `correctBankIds`.

Clients that ignore unknown fields keep exclusive behaviour (`bankReusable` missing → exclusive).

## Submit answer (unchanged)

```json
{ "questionId": "…", "answerJson": { "placements": ["a", "a"] } }
```

Server accepts duplicate ids. Grading remains per-blank any-of against `correctBankIds`.

## Results / review

Unchanged `unitResults` shape. Repeated `studentDisplay` values across units are valid.

## Out of scope

- Authoring editor and validation messages
- New i18n strings
- TABLE_FILL, MULTI_BLANK, MATCHING, choice, TRUE_FALSE
- Schema / Flyway
