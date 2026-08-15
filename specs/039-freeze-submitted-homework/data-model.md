# Data Model: Freeze Submitted Homework

**Feature**: `039-freeze-submitted-homework` | **Date**: 2026-08-15

## Entities

### Homework assignment (live)

Existing `homework_assignments`. Adds:

| Field | Type | Notes |
|-------|------|--------|
| `content_revised_at` | timestamptz NOT NULL | Default `NOW()` on create. Bumped when title, instructions, type, level, media, or questions change. Not bumped for due-date-only or assignee-only updates. |

Still the row the teacher edits. Unsubmitted students take this row + non-retired questions.

### Homework question / option (live)

Existing tables. Add:

| Field | Type | Notes |
|-------|------|--------|
| `homework_questions.retired` | boolean NOT NULL DEFAULT false | True when the teacher removed the question but some submission still references it |
| `homework_question_options.retired` | boolean NOT NULL DEFAULT false | Same for options |

Live take and editor load `retired = false` only, ordered by `position`.

**Replace rules** (assignment id `A`, incoming ids `I`):

1. Update rows in `I` that already belong to `A` (prompt, kind, structure, labels, `is_correct`, position). Keep `retired = false`.
2. Insert ids not in `A`.
3. For leftover ids on `A`: if referenced by `homework_answers` / `homework_answer_options` → set `retired = true`; else DELETE (cascade as today).

Unsubmitted students never see retired rows. Submitted views use the snapshot, not these rows.

### Homework submission

Existing `homework_submissions`. Adds:

| Field | Type | Notes |
|-------|------|--------|
| `assignment_snapshot` | jsonb NULL | Null while PENDING / no row. Set at submit; immutable afterward (teacher review does not rewrite it). |

**Snapshot JSON** (logical shape):

```text
{
  title, instructions, homeworkType, level, format, composition,
  audioUrl, audioFileId, mediaSourceKind,
  questions: [
    { id, position, kind, prompt, structure, options: [{ id, position, label, correct }] }
  ]
}
```

WRITE: `questions` is `[]`.

### Student answers / teacher review

Unchanged tables: `homework_answers`, `homework_answer_options`, submission `feedback` / percents / annotations. Freeze does not copy these into JSON; they remain rows. Matching key: `homework_answers.question_id` = snapshot `questions[].id`.

## State

```text
(no row or PENDING, snapshot null)
        │  student submit (token matches content_revised_at)
        ▼
SUBMITTED | GRADED   snapshot set, frozen
        │  teacher review (FR-009)
        ▼
GRADED / REVIEWED    snapshot unchanged
```

- Teacher `PUT` homework: live assignment only; existing snapshots untouched.
- Stale submit (`contentRevisedAt` ≠ `content_revised_at`): stay PENDING, no snapshot.
- Concurrent submit vs teacher save: assignment row locked; first commit wins (spec edge case).

## Validation

- Snapshot written only on successful submit (status becomes SUBMITTED or GRADED).
- Submit without `contentRevisedAt` or with a mismatch → `409 HOMEWORK_UPDATED`.
- Retired questions/options never appear on take GET.
- Backfill: every existing SUBMITTED/GRADED/REVIEWED homework submission gets a snapshot from live homework at migration; scores unchanged.

## Relationships

```text
homework_assignments 1 ─── * homework_questions (live, may be retired)
homework_questions 1 ─── * homework_question_options (live, may be retired)
homework_assignments 1 ─── * homework_submissions
homework_submissions 1 ─── 0..1 assignment_snapshot (JSONB on the same row)
homework_submissions 1 ─── * homework_answers  (question_id → live question, preserved via retire)
```

Activities, placement: no new fields.
