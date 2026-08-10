# Data Model: Multi-Question Manual Homework

**Feature**: `031-manual-multi-questions` | **Date**: 2026-08-10

Extends homework/activity models from `007-homework-exercises` / `029-presentation-activities`. Placement-test tables are out of scope.

## Schema changes (Flyway `V15__manual_free_text_questions.sql`)

### `homework_questions` / `activity_questions`

| Change | Detail |
|--------|--------|
| `kind` CHECK | Add `'FREE_TEXT'` alongside existing EXERCISE kinds |

For `FREE_TEXT`: `prompt` required non-empty; `structure_json = '{}'`; **no** option rows.

### `homework_answers` / `activity_answers`

| Column | Detail |
|--------|--------|
| `answer_text` | `TEXT NULL` — plain student answer for FREE_TEXT; null for EXERCISE |
| `prompt_snapshot` | `TEXT NULL` — copy of question prompt at submit time for FREE_TEXT; null for EXERCISE |

Existing: `question_id` remains `ON DELETE SET NULL`; `score` stays `NOT NULL` (use `0` for FREE_TEXT); `answer_json` / option joins unused for FREE_TEXT.

### Unchanged

- `homework_submissions.response_text` / `activity_submissions.response_text` — still used **only** for WRITE (and any pre-migration dual-read edge); multi MANUAL leaves it null after migration.
- Submission status lifecycle: `PENDING → SUBMITTED → REVIEWED` for MANUAL; EXERCISE `GRADED` path untouched.
- Whole-submission `feedback` / `reviewed_at` unchanged.

## `QuestionKind`

Existing EXERCISE kinds **+ `FREE_TEXT`**.

- `FREE_TEXT.isStructured()` = **false**
- `FREE_TEXT` is **not** graded by `ExerciseGradingService` / `ActivityExerciseGradingService` (those services only run for `format = EXERCISE`, which must not contain FREE_TEXT).

## Entities

### Manual free-text question

| Attribute | Notes |
|-----------|--------|
| Owned by | MANUAL non-WRITE homework assignment, or MANUAL activity |
| `kind` | `FREE_TEXT` |
| `prompt` | Non-empty plain text |
| `position` | Order in list |
| Options / answer key | None |

### Per-question free-text answer

| Attribute | Notes |
|-----------|--------|
| Belongs to | One MANUAL submission |
| `questionId` | Live FK when question still exists; may become null if teacher deletes question |
| `promptSnapshot` | Prompt as of submit (always set for FREE_TEXT answers) |
| `answerText` | Non-empty plain text (validated on submit) |
| `score` | `0` (not auto-graded) |

### Manual submission (logical)

| Attribute | Notes |
|-----------|--------|
| WRITE | Single `response` rich-text blob; no answer rows |
| Multi MANUAL | Zero `response_text`; N FREE_TEXT answer rows matching questions at submit (plus retained rows for later-removed questions via snapshot) |
| Feedback | One rich-text feedback on the submission |

## Validation rules

### Authoring

- MANUAL + WRITE → questions empty.
- MANUAL + non-WRITE homework → ≥1 question, all `FREE_TEXT`, non-blank prompts, no options.
- MANUAL activity → same as non-WRITE MANUAL.
- EXERCISE → existing rules; reject `FREE_TEXT`.

### Student submit (multi MANUAL)

- Payload `answers` must include every **current** question id exactly once.
- Each `text` trimmed non-empty (max length: align with existing plain-text limits, e.g. 2000 chars visible — same cap spirit as FormattedText without formatting).
- Reject if already `REVIEWED`.
- On success: replace answer rows for current questions; **do not delete** orphan answer rows whose `question_id` is null or whose question was removed earlier in the same replace cycle — prefer: delete only answers whose `question_id` is still in the current question set or that are being rewritten; retain answers for question ids no longer in the assignment (orphans with snapshot). Practical approach: delete answers for this submission where `question_id` IS NOT NULL AND `question_id` IN (current ids), then insert fresh rows for current ids; leave rows with `question_id` null or not in current set untouched… Simpler approach used in plan: **on resubmit before review**, replace all FREE_TEXT answers for the submission with the new current set only (orphans from previously removed questions are dropped on resubmit — acceptable because student is rewriting the attempt). **Once SUBMITTED and while editable until REVIEWED**, same replace-with-current-set. Snapshots matter for **teacher/student review of a given submitted payload** and for questions removed **after** submit without a further student edit. If student re-submits after a question was removed, new payload follows current list only (spec: unfinished/new work uses current list).

### Teacher review

- Multi MANUAL DTO returns ordered pairs: `promptSnapshot` + `answerText` (prefer snapshot over live prompt when both exist).
- WRITE DTO unchanged (`response` rich text).

## State transitions

Unchanged MANUAL lifecycle. Multi-question content is nested under the submission; feedback still transitions `SUBMITTED → REVIEWED`.

## Data migration (V15)

See [research.md](./research.md) §6: seed one `FREE_TEXT` (“Tu respuesta”) per legacy non-WRITE MANUAL homework/activity without questions; move plain text from `response_text` into `answer_text` + snapshot; null `response_text`; leave WRITE alone.
