# Phase 1 Data Model: Placement Test

New migration: `back-end/src/main/resources/db/migration/V19__create_placement_test.sql`. All identifiers are `UUID DEFAULT gen_random_uuid()`, timestamps `TIMESTAMPTZ`, matching existing tables. Reuses `audio_files`, `bookings`, `users` unchanged.

Enumerations (stored as `VARCHAR` with `CHECK`, mirroring existing tables):
- **Skill**: `READING`, `LISTENING`, `GRAMMAR` (auto-graded sections). Writing and Speaking are not skills in this enum — Writing is a separate submission table, Speaking is a normal booking.
- **QuestionKind**: `SINGLE_CHOICE`, `MULTI_CHOICE`, `FILL_BLANK` (same semantics as `homework_questions.kind`).
- **CefrLevel**: `A1`, `A2`, `B1`, `B2`, `C1`, `C2` (item tags). Reported per-skill level may also be `A0` ("below A1") — `A0` is a computed result value, never stored on a question.
- **AttemptStatus**: `IN_PROGRESS`, `COMPLETED`.

---

## 1. `placement_config` (singleton)

Teacher-editable global config. Exactly one row (id fixed/asserted by service).

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | singleton |
| `reading_time_seconds` | INT NOT NULL DEFAULT 600 | Reading limit |
| `listening_time_seconds` | INT NOT NULL DEFAULT 480 | Listening limit |
| `grammar_time_seconds` | INT NOT NULL DEFAULT 420 | Grammar limit |
| `pass_threshold_percent` | INT NOT NULL DEFAULT 60 | CEFR banding threshold (research §2) |
| `writing_prompt` | TEXT NOT NULL DEFAULT '' | shown on the Writing form |
| `payment_instructions` | TEXT NOT NULL DEFAULT '' | static bank-transfer text (FR-009) |
| `updated_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |

Seeded with one row (default limits + empty texts) in the migration.

## 2. `placement_questions`

Global authored items for the three auto-graded skills.

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | |
| `skill` | VARCHAR(10) NOT NULL | CHECK in (`READING`,`LISTENING`,`GRAMMAR`) |
| `position` | INT NOT NULL | order within the skill |
| `kind` | VARCHAR(20) NOT NULL | CHECK in (`SINGLE_CHOICE`,`MULTI_CHOICE`,`FILL_BLANK`) |
| `prompt` | TEXT NOT NULL | |
| `cefr_level` | VARCHAR(2) NOT NULL | CHECK in (`A1`..`C2`) — the level this item tests |
| `audio_url` | TEXT NULL | Listening only; external clip |
| `audio_file_id` | UUID NULL REFERENCES audio_files(id) ON DELETE SET NULL | Listening only; uploaded clip |
| `active` | BOOLEAN NOT NULL DEFAULT true | inactive items excluded from new attempts |

Index: `(skill, position)`. Validation (service): choice questions need ≥2 options with the correct count (single = exactly 1 correct, multi = ≥1 correct); fill-blank needs ≥1 accepted answer; Listening questions may set at most one of `audio_url` / `audio_file_id`.

## 3. `placement_question_options`

Choice options and fill-blank accepted answers (same dual use as `homework_question_options`).

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | |
| `question_id` | UUID NOT NULL REFERENCES placement_questions(id) ON DELETE CASCADE | |
| `position` | INT NOT NULL | |
| `label` | TEXT NOT NULL | option text, or an accepted fill-blank answer |
| `is_correct` | BOOLEAN NOT NULL DEFAULT false | fill-blank rows are all `true` |

Index: `(question_id, position)`.

## 4. `placement_attempts`

One run-through by a logged-in user. Multiple attempts per user allowed (no limit, edge case).

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | |
| `user_id` | UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE | |
| `status` | VARCHAR(12) NOT NULL DEFAULT 'IN_PROGRESS' | CHECK in (`IN_PROGRESS`,`COMPLETED`) |
| `started_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |
| `completed_at` | TIMESTAMPTZ NULL | set when all three sections submitted |
| `overall_cefr` | VARCHAR(2) NULL | computed (`A0`/`A1`..`C2`), weakest skill |

Index: `(user_id, started_at DESC)`.

## 5. `placement_attempt_sections`

Per-section timing + result. Created when the user **starts** a section (server stamps the deadline).

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | |
| `attempt_id` | UUID NOT NULL REFERENCES placement_attempts(id) ON DELETE CASCADE | |
| `skill` | VARCHAR(10) NOT NULL | CHECK in (`READING`,`LISTENING`,`GRAMMAR`) |
| `started_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |
| `deadline_at` | TIMESTAMPTZ NOT NULL | `started_at + config limit` (server-authoritative) |
| `submitted_at` | TIMESTAMPTZ NULL | set on submit/auto-submit |
| `score_percent` | INT NULL | 0–100, set on submit |
| `cefr_level` | VARCHAR(2) NULL | per-skill computed level (`A0`/`A1`..`C2`) |

Unique: `(attempt_id, skill)` — a skill is taken once per attempt. Index: `(attempt_id)`.

## 6. `placement_answers`

Per-question graded answer within a section (mirrors `homework_answers`).

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | |
| `attempt_section_id` | UUID NOT NULL REFERENCES placement_attempt_sections(id) ON DELETE CASCADE | |
| `question_id` | UUID NULL REFERENCES placement_questions(id) ON DELETE SET NULL | nullable to preserve history after item edits |
| `answer_text` | TEXT NULL | fill-blank raw input |
| `score` | NUMERIC(4,3) NOT NULL | 0.000–1.000 (per-question, supports multi-choice partials) |

Unique: `(attempt_section_id, question_id)`. Index: `(attempt_section_id)`.

## 7. `placement_answer_options`

Options a student selected for a choice answer (mirrors `homework_answer_options`).

| Column | Type | Notes |
|--------|------|-------|
| `answer_id` | UUID NOT NULL REFERENCES placement_answers(id) ON DELETE CASCADE | |
| `option_id` | UUID NOT NULL REFERENCES placement_question_options(id) ON DELETE CASCADE | |
| | | PRIMARY KEY (`answer_id`, `option_id`) |

## 8. `placement_writing_submissions`

Free-text Writing responses. Trust-based: any logged-in user may submit; no payment gate, no review-status machinery (research / FR-010).

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | |
| `user_id` | UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE | |
| `body` | TEXT NOT NULL | the written response |
| `prompt_snapshot` | TEXT NOT NULL | the `writing_prompt` text at submit time (so later prompt edits don't rewrite history) |
| `submitted_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |

Index: `(user_id, submitted_at DESC)`. The teacher reads the latest (and full history) per student. No explicit cap; the UI submits one at a time.

---

## Relationships (text ERD)

```text
users ─1──*─ placement_attempts ─1──*─ placement_attempt_sections ─1──*─ placement_answers ─*──*─ placement_question_options
users ─1──*─ placement_writing_submissions
placement_questions ─1──*─ placement_question_options
placement_questions ─0..1─ audio_files            (Listening clip, reused table)
placement_attempt_sections.question refs ─*─ placement_questions (via answers, ON DELETE SET NULL)
placement_config (singleton)
bookings (reused, no FK to placement — the evaluation appointment is a normal booking)
```

## State transitions

**Attempt**: `IN_PROGRESS` → `COMPLETED` (when the third section is submitted; `completed_at` + `overall_cefr` set).

**Section** (no explicit status column; derived from timestamps):
- *not started* → row absent.
- *in progress* → row exists, `submitted_at IS NULL`, `now() < deadline_at`.
- *expired* → row exists, `submitted_at IS NULL`, `now() >= deadline_at` → next interaction finalizes it (grades sent answers, blanks incorrect).
- *submitted* → `submitted_at`, `score_percent`, `cefr_level` set; immutable (single submission per skill, like exercises).

## Validation rules (enforced in services)

- **Login**: every `/placement/**` call requires an authenticated user (FR-001) — enforced by `SecurityConfig`, not data.
- **Single section submission**: a second submit for an already-submitted `(attempt, skill)` → `409` (mirrors the exercise single-submission lock).
- **Server-authoritative timing**: `deadline_at` is computed server-side from `placement_config`; submit grades only what was sent, with unanswered items scored 0 (FR-006, FR-007).
- **Answer key hidden**: the student GET (`getTest`) returns options without `is_correct` and without `cefr_level`/accepted-answer text; correctness is only revealed in the result after submission (mirrors `ExerciseGradingService.buildStudentQuestions`).
- **No payment data**: no table stores any order/status/reference/amount (FR-012).
- **Authoring validation**: per question-kind option rules (see table 2); a skill must have ≥1 active question before it can be taken (otherwise the section is empty and skipped/blocked with a clear admin warning).
