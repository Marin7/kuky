# Phase 1 Data Model: Homework Exercises & Self-Correction

Migration: `V15__create_homework_exercises.sql`. Extends existing tables and adds four new ones.
All new code uses plain JDBC (`NamedParameterJdbcTemplate` + `RowMapper`), mirroring the `learning`
domain.

## Enumerations

- **HomeworkFormat**: `MANUAL` | `EXERCISE`. (Java enum; stored as `VARCHAR`.)
- **QuestionKind**: `SINGLE_CHOICE` | `MULTI_CHOICE` | `FILL_BLANK`. (Java enum; stored as `VARCHAR`.)
- **HomeworkStatus** (extended): `PENDING` | `SUBMITTED` | `REVIEWED` | `GRADED`. `GRADED` is the
  terminal auto-graded state for exercises.

## Altered tables

### `homework_assignments` (existing — V5/V13)

| Column | Type | Notes |
|--------|------|-------|
| `format` | `VARCHAR(10) NOT NULL DEFAULT 'MANUAL'` | NEW. `MANUAL` or `EXERCISE`. Existing rows → `MANUAL`. CHECK in (`MANUAL`,`EXERCISE`). |

All other columns unchanged (`id, title, instructions, due_on, published, sort_order, created_at,
homework_type, level`).

### `homework_submissions` (existing — V5)

| Column | Type | Notes |
|--------|------|-------|
| `score_percent` | `INT` | NEW. Nullable; 0–100; set only when `status = 'GRADED'`. |
| `status` CHECK | — | REPLACED: now `IN ('PENDING','SUBMITTED','REVIEWED','GRADED')`. |

`response_text` stays for manual homework. Exercise submissions leave `response_text` null and use
`homework_answers` instead.

## New tables

### `homework_questions`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK DEFAULT gen_random_uuid()` | |
| `assignment_id` | `UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE` | |
| `position` | `INT NOT NULL` | 0-based display order within the exercise. |
| `kind` | `VARCHAR(20) NOT NULL` | CHECK in (`SINGLE_CHOICE`,`MULTI_CHOICE`,`FILL_BLANK`). |
| `prompt` | `TEXT NOT NULL` | The question text. |

Index: `(assignment_id, position)`.

### `homework_question_options`

Serves both choice options and fill-blank accepted answers (see research §2).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK DEFAULT gen_random_uuid()` | |
| `question_id` | `UUID NOT NULL REFERENCES homework_questions(id) ON DELETE CASCADE` | |
| `position` | `INT NOT NULL` | Display order (choice). Sequential for fill-blank answers. |
| `label` | `TEXT NOT NULL` | Choice: option text. Fill-blank: an accepted answer. |
| `is_correct` | `BOOLEAN NOT NULL DEFAULT false` | Choice: marks correct option(s). Fill-blank: always `true`. |

Index: `(question_id, position)`.

### `homework_answers`

One row per question a student answered in a graded submission.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK DEFAULT gen_random_uuid()` | |
| `submission_id` | `UUID NOT NULL REFERENCES homework_submissions(id) ON DELETE CASCADE` | |
| `question_id` | `UUID REFERENCES homework_questions(id) ON DELETE SET NULL` | Nullable so later question edits don't break history (research §8). |
| `answer_text` | `TEXT` | Fill-blank response (raw, as typed). Null for choice. |
| `score` | `NUMERIC(4,3) NOT NULL` | Per-question score in `[0,1]`. |

Index: `(submission_id)`. Unique: `(submission_id, question_id)`.

### `homework_answer_options`

Which options a student selected for a choice question.

| Column | Type | Notes |
|--------|------|-------|
| `answer_id` | `UUID NOT NULL REFERENCES homework_answers(id) ON DELETE CASCADE` | |
| `option_id` | `UUID NOT NULL REFERENCES homework_question_options(id) ON DELETE CASCADE` | |
| | | PRIMARY KEY `(answer_id, option_id)`. |

## Relationships

```
homework_assignments 1───* homework_questions 1───* homework_question_options
        │                                                      ▲
        │                                                      │ (selected)
        *                                                      │
homework_submissions 1───* homework_answers *───* homework_answer_options
   (per student)                  │
                                  └─ question_id ─▶ homework_questions (SET NULL on delete)
```

- An `EXERCISE` assignment has ≥ 1 `homework_questions`; a `MANUAL` assignment has none.
- A `SINGLE_CHOICE`/`MULTI_CHOICE` question has ≥ 2 options (≥ 1 marked correct; single-choice
  exactly 1). A `FILL_BLANK` question has ≥ 1 option (accepted answer, `is_correct = true`).
- A `GRADED` submission has one `homework_answers` row per answered question and a `score_percent`.

## Validation rules (enforced in `HomeworkAdminService` on create/update)

| Rule | Source |
|------|--------|
| `EXERCISE` must have ≥ 1 question | FR-010 |
| Every question has a non-blank prompt | FR-010 |
| `SINGLE_CHOICE`: ≥ 2 options, **exactly one** `is_correct` | FR-008 |
| `MULTI_CHOICE`: ≥ 2 options, **≥ one** `is_correct` | FR-008 |
| `FILL_BLANK`: ≥ 1 accepted answer (non-blank) | FR-009 |
| `MANUAL` must have **zero** questions | FR-017 (binary format) |
| Title + instructions required | existing FR (unchanged) |

Validation failure → `400 VALIDATION_ERROR` (existing `GlobalExceptionHandler` convention).

## Grading (computed in `ExerciseGradingService`, persisted on submit)

See research §5 for the formulas. On submit:
1. Reject if assignment `format != EXERCISE` → `400`/`SUBMISSION_NOT_ALLOWED`.
2. Reject if an existing submission for `(user, assignment)` is already `GRADED` → single-submission
   lock, `409 SUBMISSION_NOT_ALLOWED` (FR-020).
3. Grade each question, write `homework_answers` (+ `homework_answer_options`), upsert the submission
   to `status = GRADED`, `score_percent = round(avg*100)`, `submitted_at = now`.
4. Return `ExerciseResultResponse` (per-question score + correctness + correct answers; overall
   percent + fully-correct count + total).

## State transitions

```
MANUAL  homework:   PENDING ──submit──▶ SUBMITTED ──teacher──▶ REVIEWED   (unchanged)
EXERCISE homework:  PENDING ──submit (auto-grade)──▶ GRADED  (terminal, locked; no retake)
```

## Seed data

None required. Existing `V5` seeded assignments remain `MANUAL` (default) with no questions, so they
keep their current behavior. (Optional: a `/speckit-tasks` step may add one seeded sample `EXERCISE`
for manual QA — not required by the spec.)
