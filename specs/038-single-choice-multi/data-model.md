# Data Model: Multi-Item Opción Única

**Feature**: `038-single-choice-multi` | **Date**: 2026-08-15

No new tables. Dual representation on existing question rows.

## Entities

### Opción única question (existing row)

| Field | Classic (no markers) | Numbered (`(1)`…`(N)` in `prompt`) |
|-------|----------------------|-------------------------------------|
| `kind` | `SINGLE_CHOICE` | `SINGLE_CHOICE` |
| `prompt` | Free text, no `(digits)` tokens | Text containing consecutive `(1)`…`(N)` |
| `structure_json` | `{}` | `{ "items": [ … ] }` length N |
| Options table | ≥2 rows, exactly one `is_correct` | **Zero** rows |

Same for `activity_questions` / `activity_question_options`.

### Number-in-brackets marker

Parsed from `prompt` only (not title). Pattern: `(` + digits + `)`. Integer value identifies the item. `(01)` ≡ `(1)`.

Validation (save):

| Rule | Error if violated |
|------|-------------------|
| Distinct numbers = `{1…N}` consecutive, N ≥ 1 | Sequence invalid |
| N ≤ 20 | Too many items |
| Each item: ≥2 options, exactly one `correct` | Same messages as classic opción única, per number |
| Non-numeric `(ser)` | Ignored (not an item) |
| Duplicate `(1)` | Same item (one option list) |

### Pick-one item (`structure_json.items[]`)

Ordered by `number` 1…N.

| Field | Type | Rules |
|-------|------|-------|
| `number` | int | 1…N matching markers |
| `options` | array | ≥2; each `{ id, label, correct }` |
| `options[].id` | string (UUID) | Stable for grading; generated on author if missing |
| `options[].label` | string | Non-blank after trim |
| `options[].correct` | boolean | Exactly one true per item |

### Student item answer (`answer_json` on the single answer row)

Numbered:

```json
{ "selections": { "1": "<optionId>", "2": "<optionId>" } }
```

Classic: `selected_option_ids` as today; `answer_json` null.

`homework_answers.score` / `activity_answers.score`: mean of item 0/1 scores (row snapshot). **Overall assignment average must not use this mean as one contribution** — expand to N item scores (see Scoring).

### Scoring contributions

| Kind | Contributions to overall `%` / fully-correct |
|------|-----------------------------------------------|
| Numbered SINGLE_CHOICE | N values, each 0 or 1 |
| Classic SINGLE_CHOICE, TRUE_FALSE, other auto | 1 value = existing 0–1 `score` |
| FREE_TEXT | 1 value = teacher % / 100 |
| WRITE | 1 value = submission teacher % |

Fully-correct: count of contributions equal to 1.0.

Example: homework with one `(1)(2)(3)` entry + one FREE_TEXT → **4** questions.

## Relationships

```text
HomeworkQuestion 1──* QuestionOption     (classic SINGLE_CHOICE only)
HomeworkQuestion 1──  structure.items[]  (numbered SINGLE_CHOICE only)
HomeworkAnswer   1──  one row per question (UNIQUE submission + question)
                 └──  selections map when numbered
```

Identical for activities.

## State / mode transitions (authoring)

```text
classic ──(prompt gains first (1))──► numbered
         copy options table → items[0] (number 1);
         extra numbers start with empty options

numbered ──(all markers removed)──► classic
         items number=1 options → options table;
         structure_json → {}

numbered ──(add (N+1) as consecutive)──► numbered N+1 (empty options)
numbered ──(remove last N)──► numbered N-1
numbered ──(gap, e.g. drop (2) keep (3))──► invalid (cannot save)
```

Existing published questions without markers never enter numbered mode unless the teacher edits the prompt.

## Snapshot on key change

Unchanged 014/033 rule: already-graded submissions keep stored `score` / `answer_json` / revealed keys from submit time. Review may re-derive `unitResults` from stored JSON vs **current** structure only for display of units when the code already does that for MULTI_BLANK — prefer showing stored unit snapshot if present; do not change `score_percent` of old submissions.

## Caps

| Limit | Value |
|-------|-------|
| Items per numbered question | 1–20 |
| Options per item | ≥2 (no extra cap; same as classic) |
