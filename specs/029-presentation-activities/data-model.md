# Data Model: Presentation Activities

**Feature**: `029-presentation-activities` | **Date**: 2026-08-07

## Entities

### Activity

Teacher-authored work item owned by exactly one presentation.

| Field | Type | Notes |
|-------|------|--------|
| id | UUID PK | |
| presentation_id | UUID FK → presentations | `ON DELETE CASCADE`, NOT NULL |
| title | VARCHAR(200) | NOT NULL |
| format | VARCHAR(10) | `MANUAL` \| `EXERCISE` (same as homework) |
| level | VARCHAR(5) | nullable; optional CEFR-style label (parity with homework authoring UI) |
| homework_type | VARCHAR(20) | nullable; optional skill label when useful for exercises (AUDIO/WRITE/…) |
| position | INT | NOT NULL; order within presentation (teacher reorder) |
| trigger_file_id | UUID FK → presentation_files | nullable; `ON DELETE SET NULL` |
| trigger_page | INT | nullable; must be ≥ 1 when set |
| created_at / updated_at | TIMESTAMPTZ | |

**Rules**:
- Student-visible iff complete: title + instructions PDF row present + presentation_id set.
- `trigger_file_id` and `trigger_page` both null OR both non-null.
- When both set: `trigger_file_id` must belong to `presentation_id`.
- No `due_on`, no assignee table, no `unit_id`, no `unit_position`.
- No rich-text `instructions` column — instructions are the PDF file only.

### ActivityInstructionsFile (1:1)

| Field | Type | Notes |
|-------|------|--------|
| id | UUID PK | disk key |
| activity_id | UUID FK → activities | UNIQUE, `ON DELETE CASCADE` |
| original_name | TEXT | |
| content_type | TEXT | must be `application/pdf` |
| byte_size | BIGINT | |
| created_at | TIMESTAMPTZ | |

Bytes on disk via `ActivityInstructionsFileStore`.

### ActivityQuestion / ActivityQuestionOption

Mirror `homework_questions` / `homework_question_options` (kind, prompt, position, structure_json, options with is_correct). FK `activity_id` instead of `assignment_id`.

### ActivitySubmission

| Field | Type | Notes |
|-------|------|--------|
| id | UUID PK | |
| activity_id | UUID FK | `ON DELETE CASCADE` |
| user_id | UUID FK → users | `ON DELETE CASCADE` |
| status | VARCHAR | `PENDING` \| `SUBMITTED` \| `REVIEWED` \| `GRADED` |
| response_text | TEXT | MANUAL rich-text JSON (same encoding as homework) |
| score_percent | NUMERIC | EXERCISE |
| feedback | TEXT | teacher feedback |
| submitted_at / reviewed_at | TIMESTAMPTZ | |
| UNIQUE(activity_id, user_id) | | |

### ActivityAnswer / ActivityAnswerOption

Mirror homework answer tables; FK to `activity_submissions` / `activity_questions`.

## Relationships

```text
presentations 1──* activities
presentation_files 1──* activities (optional trigger_file_id)
activities 1──1 activity_instructions_files
activities 1──* activity_questions 1──* activity_question_options
activities 1──* activity_submissions 1──* activity_answers
users 1──* activity_submissions
```

## Access

No activity-specific share table. A student may see/fulfill an activity iff they can access its presentation (`presentation_shares` OR unit assignment via presentation’s `unit_id`) — same as presentation file download.

## State transitions (submission)

Identical to homework:

- MANUAL: `PENDING` → `SUBMITTED` → `REVIEWED`
- EXERCISE: `PENDING` → `GRADED` (auto); optional teacher feedback afterward

**Fulfilled** for prompts/progress = status ∈ {`SUBMITTED`, `REVIEWED`, `GRADED`} (not `PENDING`).

## Ordering

`position` is unique per presentation among activities (enforce in service on reorder: rewrite 0..n-1). New activities append at max+1.

## Delete behavior

| Action | Effect |
|--------|--------|
| DELETE presentation | CASCADE activities → questions/submissions/answers/instructions rows; app deletes instruction PDFs from disk |
| DELETE presentation file | `trigger_file_id` SET NULL (and clear `trigger_page` in service); activity remains |
| DELETE activity | CASCADE children; delete instructions PDF from disk |
| Change activity.presentation_id | Clear trigger if old file not in new presentation; re-append position on new presentation |

## Progress aggregation

For a student profile: count activities accessible via their presentations; completed = submissions in fulfilled statuses. Expose alongside existing homework breakdown (see contracts).
