# Data Model: Unit as a PDF with Page Activities

One Flyway migration: `V28__unit_owned_pdf_activities.sql`. See [research.md](./research.md)
D1–D3 and D10 for the reasoning.

## Entities

### Unit (`units`, unchanged columns)

| Field | Type | Notes |
|---|---|---|
| id | UUID PK | |
| level | VARCHAR(5) | A1…C2 |
| subject | VARCHAR(200) | also written to the owned presentation's `title` |
| position | INT | order within its level |

Relationships:
- 0..1 **Unit PDF**: the presentation with `unit_id = units.id`
- 0..N homeworks: `homework_assignments.unit_id`, unchanged
- 0..N assignees: `unit_assignments`, unchanged

The content sequence is the PDF (if any) plus the homeworks, ordered by `unit_position`.

### Unit PDF = unit-owned presentation (`presentations` + one `presentation_files` row)

| Field | Change | Notes |
|---|---|---|
| presentations.unit_id | **meaning changes**: FK `ON DELETE SET NULL` → **`ON DELETE CASCADE`** | NULL = standalone (Presentations tab); non-NULL = owned by that unit |
| — | **NEW** partial unique index `presentations_unit_owned_unique ON presentations (unit_id) WHERE unit_id IS NOT NULL` | at most one PDF per unit |
| presentations.unit_position | unchanged | the PDF's slot in the unit's mixed sequence |
| presentations.title / level | kept in sync with the unit | set when the PDF is first uploaded and on unit update |
| presentation_files.page_count | **NEW** `INT NULL CHECK (page_count IS NULL OR page_count BETWEEN 1 AND 500)` | required (service-enforced) for files of unit-owned presentations; NULL for legacy standalone files |

Rules:
- A unit-owned presentation has exactly one file, and it is a PDF (`application/pdf`, `%PDF-` header), max 50 MB (the existing `MAX_FILE_BYTES`).
- Unit-owned presentations never have `presentation_shares` rows. Access comes only from `unit_assignments`, which the existing `isSharedWith` query already handles.
- The admin Presentations APIs ignore unit-owned rows (lists filter `unit_id IS NULL`; by-id operations return 404).

### Activity (`activities`, reshaped)

| Field | Type | Change |
|---|---|---|
| id | UUID PK | — |
| presentation_id | UUID FK → presentations ON DELETE CASCADE | now always a unit-owned presentation |
| page | INT NOT NULL CHECK (page ≥ 1) | **NEW**, with `UNIQUE (presentation_id, page)` |
| title | VARCHAR(200) **NULL** | was NOT NULL. NULL or blank → client shows "Actividad – página N" |
| format | VARCHAR(10) MANUAL/EXERCISE/MIXED | unchanged (derived from the questions) |
| created_at / updated_at | TIMESTAMPTZ | unchanged |
| trigger_file_id, trigger_page, position, level, homework_type, instructions_text, youtube_url, image_id | — | **DROPPED** |

Indexes: drop `activities_presentation_position_idx`, `activities_trigger_file_idx`,
`activities_image_idx`. The new unique constraint covers lookups by `(presentation_id, page)`.

Validation (service):
- `1 ≤ page ≤ unitPdf.page_count`; the unit must have a PDF.
- At least one question, using the same per-kind validation as today (`HomeworkAdminService.validateStructure` and friends).
- Title is trimmed, blank becomes NULL, max 200 characters.

Unchanged child tables: `activity_questions`, `activity_question_options`, `activity_answers`,
`activity_answer_options`.

**Dropped table**: `activity_instructions_files`.

### Activity Submission (`activity_submissions`)

| Field | Change |
|---|---|
| teacher_seen_at | **NEW** `TIMESTAMPTZ NULL`. Set to NULL on every student submit (manual or auto) and set to NOW() when the teacher opens the submission |
| — | **NEW** partial index `activity_submissions_teacher_unseen_idx ON activity_submissions (activity_id) WHERE status IN ('SUBMITTED','REVIEWED','GRADED') AND teacher_seen_at IS NULL` |

The status lifecycle is unchanged: `PENDING → SUBMITTED → GRADED` (or `REVIEWED`, legacy).
All-auto activities go straight to `GRADED` on submit; anything containing FREE_TEXT
stays `SUBMITTED` until the teacher finalizes it.

## Migration outline (`V28__unit_owned_pdf_activities.sql`)

1. `DELETE FROM activities;` (cascades questions, options, submissions and answers)
2. `DROP TABLE activity_instructions_files;`
3. `UPDATE presentations SET unit_id = NULL, unit_position = 0 WHERE unit_id IS NOT NULL;` (legacy links become standalone decks)
4. Re-create the `presentations.unit_id` FK with `ON DELETE CASCADE`, then add `presentations_unit_owned_unique`.
5. `ALTER TABLE presentation_files ADD COLUMN page_count INT …CHECK…;`
6. Reshape `activities`:
   - drop the indexes and the `activities_trigger_pair_check` / `activities_trigger_page_check` constraints
   - drop the listed columns
   - `ADD COLUMN page INT NOT NULL` (the table is empty) plus the CHECK
   - `ALTER title DROP NOT NULL`
   - add `UNIQUE (presentation_id, page)`
7. `ALTER TABLE activity_submissions ADD COLUMN teacher_seen_at TIMESTAMPTZ;` plus the partial index.

Dev seed (`db/dev/full_seed.sql`): change the two `INSERT INTO presentations (…, unit_id)`
rows to standalone (`unit_id` NULL), and drop any seeded activities.

## State transitions

**Unit PDF**

`none` → (upload) → `present(pageCount = n)` → (replace with m pages):
- `m ≥ max(activity.page)` → replaced; all activities are kept.
- otherwise, without confirmation → `409 ACTIVITIES_OUT_OF_RANGE`, nothing changes.
- otherwise, with `removeOutOfRangeActivities=true` → replaced, and activities with `page > m` are deleted along with their submissions.

The unit itself being deleted cascades: the presentation, its file row (and the disk file, deleted after commit), its activities and their submissions.

**Activity (per page)**

`unmarked` → (create) → `marked` → (update title or questions) → `marked`
`marked` → (delete):
- no submissions → `unmarked`
- has submissions, without `deleteSubmissions=true` → `409 ACTIVITY_HAS_SUBMISSIONS`
- has submissions, with the flag → `unmarked`, and its submissions are deleted
