# Data Model: Quiz Terminology

**Feature**: `040-quiz-terminology` | **Date**: 2026-08-15

Flyway **`V21__quizzes_replace_placement.sql`**: `DROP` all `placement_*` tables (CASCADE), then create the quiz tables below. No backfill.

## Entities

### Quiz (live)

`quizzes`

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID PK | |
| `title` | VARCHAR(200) NOT NULL | |
| `description` | TEXT | Optional |
| `created_at` | timestamptz NOT NULL | `NOW()` |
| `updated_at` | timestamptz NOT NULL | bumped on title/description/question edits, not on assignee-only changes |

Not a homework. No `unit_id`, due date, or publish flag — availability is assignee rows plus ≥1 live question.

### Quiz question (live)

`quiz_questions`

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID PK | |
| `quiz_id` | UUID FK → quizzes ON DELETE CASCADE | |
| `position` | INT NOT NULL | 0-based order |
| `skill` | VARCHAR(10) NOT NULL | `READING` \| `WRITING` \| `GRAMMAR` \| `LISTENING` |
| `kind` | VARCHAR(20) NOT NULL | Same CHECK as homework: `SINGLE_CHOICE`, `MULTI_CHOICE`, `MULTI_BLANK`, `DRAG_DROP`, `TABLE_FILL`, `MATCHING`, `TRUE_FALSE`, `FREE_TEXT` |
| `prompt` | TEXT NOT NULL | |
| `structure_json` | JSONB NOT NULL DEFAULT `{}` | Same shapes as homework |
| `media_source_kind` | VARCHAR(20) | `AUDIO_URL` \| `UPLOADED_FILE` \| `VIDEO_PAGE` \| `YOUTUBE`; required when skill is `LISTENING` |
| `audio_url` | TEXT | URL or YouTube/video-page link |
| `audio_file_id` | UUID FK → audio_files ON DELETE SET NULL | |
| `retired` | boolean NOT NULL DEFAULT false | True if removed from live quiz but an attempt snapshot still references the id |

Live take (not started) and editor load `retired = false` ordered by `position`.

`quiz_question_options` — same as `homework_question_options` (`label`, `is_correct`, `position`, `retired`).

**Replace rules** on teacher save (quiz id `Q`, incoming ids `I`): same as homework V20 — update / insert / retire-if-referenced-else-delete. Attempts already have JSONB snapshots, so retire is only needed if answers still FK to live question ids.

### Quiz assignee

`quiz_assignees`

| Field | Type | Notes |
|-------|------|--------|
| `quiz_id` | UUID FK CASCADE | |
| `user_id` | UUID FK → users CASCADE | Must hold `STUDENT` at assign time |
| UNIQUE (`quiz_id`, `user_id`) | | |

Assigning requires the quiz to have ≥1 non-retired question.

### Quiz attempt

`quiz_attempts`

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID PK | |
| `quiz_id` | UUID FK CASCADE | |
| `user_id` | UUID FK CASCADE | |
| `status` | VARCHAR(12) NOT NULL | `IN_PROGRESS` \| `SUBMITTED` \| `GRADED` |
| `started_at` | timestamptz NOT NULL | Snapshot moment |
| `submitted_at` | timestamptz | |
| `score_percent` | INT | Overall 0–100; set on auto submit and/or teacher finalize |
| `fully_correct_count` | INT | Questions (and numbered SINGLE_CHOICE items) at 100% |
| `question_unit_count` | INT | Denominator for fully-correct |
| `quiz_snapshot` | JSONB NOT NULL | Captured at start; immutable after start |
| `feedback` | TEXT | Optional teacher note (≤500 chars, homework parity) |
| UNIQUE (`quiz_id`, `user_id`) | | One attempt per student per quiz |

**Snapshot JSON** (logical):

```text
{
  title, description,
  questions: [
    {
      id, position, skill, kind, prompt, structure,
      mediaSourceKind, audioUrl, audioFileId,
      options: [{ id, position, label, correct }]
    }
  ]
}
```

### Quiz answer

`quiz_answers`

| Field | Type | Notes |
|-------|------|--------|
| `attempt_id` | UUID FK CASCADE | |
| `question_id` | UUID | Id from snapshot (and live row if not deleted) |
| `answer_json` | JSONB | Same payload as homework answers |
| `score` | NUMERIC(4,3) | Auto 0–1; FREE_TEXT null until teacher % |
| `teacher_percent` | INT | 0–100 for FREE_TEXT; null until scored |
| `annotations` | JSONB | Optional, homework-shaped |
| UNIQUE (`attempt_id`, `question_id`) | | |

## Relationships

```text
quizzes 1──* quiz_questions 1──* quiz_question_options
quizzes 1──* quiz_assignees *──1 users
quizzes 1──* quiz_attempts *──1 users
quiz_attempts 1──* quiz_answers
```

## State

```text
(no attempt)
        │  assigned student opens take (first GET)
        ▼
IN_PROGRESS   snapshot set, answers empty
        │  submit (one shot)
        ▼
GRADED          all questions auto-gradable
SUBMITTED       any FREE_TEXT — awaiting teacher %
        │  teacher sets % on every FREE_TEXT and finalizes
        ▼
GRADED          overall + per-skill from snapshot + answers
```

- Unassign while `IN_PROGRESS`: delete attempt (student never submitted).
- Unassign after `SUBMITTED`/`GRADED`: keep attempt; student can still GET result; they cannot start again.
- Live quiz edits never rewrite `quiz_snapshot`.
- Per-skill % is **derived** on read from snapshot questions grouped by `skill` + stored answer scores (no skill-score table). Skills with zero questions in **that snapshot** are omitted. A skill whose FREE_TEXT still lacks `teacher_percent` is returned as awaiting.

## Validation

- Skill required on every question.
- `LISTENING` → media source required (same four kinds; YouTube/Enlace auto-switch parity with homework).
- Auto kinds need a complete key (homework rules, including numbered SINGLE_CHOICE).
- `FREE_TEXT` needs a prompt, no key.
- Cannot assign with zero live questions.
- Cannot assign a non-`STUDENT` user.
- Second submit → `QUIZ_ALREADY_SUBMITTED`.
- Take/submit without assignee row → `QUIZ_NOT_ASSIGNED`.
- Missing quiz → `QUIZ_NOT_FOUND`.
