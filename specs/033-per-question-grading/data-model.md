# Data Model: Per-Question Homework Grading

**Feature**: `033-per-question-grading` | **Date**: 2026-08-10

## Overview

Grading mode is derived from each question’s `kind`. Assignment/activity `format` becomes a **derived** cache (`MANUAL` | `EXERCISE` | `MIXED`). Mixed submissions stay `SUBMITTED` until every manual answer has `teacher_validation`, then become `GRADED` with a combined `score_percent`.

```text
homework_assignments / activities
        │ 1
        │
        ├──── homework_questions / activity_questions  (kind ⇒ auto | manual)
        │
        └──── *_submissions
                 │ 1
                 └──── *_answers  (+ teacher_validation for FREE_TEXT on MIXED)
```

## Entities

### HomeworkAssignment / Activity (unchanged columns except `format` semantics)

| Field | Notes |
|-------|--------|
| `format` | Derived on save: `MANUAL` \| `EXERCISE` \| `MIXED`. Not chosen in authoring UI. WRITE → always `MANUAL`. |
| `homework_type` | Still AUDIO/READ/WRITE/GRAMMAR on homework; WRITE ⇒ no questions, single `response_text` path. Activities: no WRITE. |

**Derived composition** (API-only, not stored): `WRITE` | `ALL_MANUAL` | `ALL_AUTO` | `MIXED` from `homework_type` + question kinds.

### Question (`homework_questions` / `activity_questions`)

| Field | Notes |
|-------|--------|
| `kind` | `FREE_TEXT` = manual; structured kinds = auto-correctible |
| `prompt` | Required |
| `structure_json` / options | Required completeness rules for structured kinds only; empty for `FREE_TEXT` |
| `position` | Order |

**Validation**: Non-WRITE homeworks and all activities require ≥1 question; kinds may mix. WRITE: zero questions.

### Answer (`homework_answers` / `activity_answers`)

| Field | Notes |
|-------|--------|
| `answer_json` | Structured student response |
| `answer_text` | FREE_TEXT plain or FormattedText JSON after annotation |
| `prompt_snapshot` | FREE_TEXT snapshot at submit |
| `score` | `0..1` — auto on submit for structured; `0` until validated for FREE_TEXT; then `1`/`0` |
| **`teacher_validation`** | **NEW** `NULL` \| `VALIDATED` \| `INVALIDATED`. Required non-null for every FREE_TEXT answer before MIXED finalize. NULL for structured answers. Ignored for pure-manual finalize (`REVIEWED`). |

### Submission (`homework_submissions` / `activity_submissions`)

| Field | Notes |
|-------|--------|
| `status` | See lifecycle |
| `score_percent` | ALL_AUTO: set on submit. MIXED: **null** until finalize, then combined. ALL_MANUAL/WRITE: null |
| `response_text` | WRITE only |
| `feedback` / `review_model` | 032 semantics; MIXED finalize may set `ANNOTATED` |
| `reviewed_at` | Set on REVIEWED or MIXED→GRADED finalize |

## Lifecycles

### ALL_AUTO

```text
PENDING → GRADED (submit)
```

### ALL_MANUAL / WRITE

```text
PENDING → SUBMITTED (submit) → REVIEWED (teacher feedback/annotate)
```

### MIXED

```text
PENDING → SUBMITTED (submit: auto scored + keys revealed; manuals pending)
       → GRADED (teacher: all FREE_TEXT teacher_validation set; combined score)
```

Re-edit after MIXED `GRADED`: teacher may update validations / annotations / short note; recalculate `score_percent`.

## Combined score

```text
questionScore(q) =
  structured: existing graded score ∈ [0,1]
  FREE_TEXT:  VALIDATED → 1.0 ; INVALIDATED → 0.0

score_percent = round( mean(questionScore) * 100 )
fully_correct = count( questionScore == 1.0 )
```

Provisional (pre-finalize MIXED): mean over structured questions only → `provisionalScorePercent` (API); do not treat as final `scorePercent`.

## Migration `V17__per_question_grading.sql`

1. `ALTER` answers: add `teacher_validation VARCHAR(16) NULL` with CHECK (`teacher_validation IS NULL OR teacher_validation IN ('VALIDATED','INVALIDATED')`).
2. Replace `format` CHECK on `homework_assignments` and `activities` to `IN ('MANUAL','EXERCISE','MIXED')`.
3. Backfill: set `format = 'MIXED'` where both a `FREE_TEXT` and a structured question exist (expect 0 rows today).
4. Optional: CHECK that structured answers keep `teacher_validation IS NULL` (app-enforced is enough for v1).

## Validation rules (app)

| Rule | Enforcement |
|------|-------------|
| Non-WRITE ≥1 question | Admin save |
| FREE_TEXT: no options/structure key | Admin save |
| Structured: existing kind validators | Admin save |
| Submit: every question answered | Student submit |
| MIXED finalize: every FREE_TEXT has validation | Admin feedback PUT |
| Keys hidden until submit | Student GET pre-submit strip |

## Relationships to prior features

- **031**: FREE_TEXT multi-answers + `prompt_snapshot` unchanged.
- **032**: annotate + plain note reused on MIXED finalize; pure-manual path unchanged.
- **007/024/027**: structured grading reused for the auto subset only.
