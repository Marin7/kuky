# Data Model: University Student Portal

## Entity relationship (logical)

```text
users
  ├── role ∈ {USER, STUDENT, UNIVERSITY_STUDENT, ADMIN}
  └── university_level ∈ {BEGINNER, INTERMEDIATE} | null
        │
        ├── (when UNIVERSITY_STUDENT) sees schedule sessions/exceptions for level
        ├── sees public exams + news (all readers)
        └── via availability joins → homework_assignments / presentations
              └── homework_submissions (existing) per (user_id, assignment_id)

university_schedule_sessions (weekly template)
university_schedule_exceptions (dated CANCEL | EXTRA)
university_exam_dates
university_news_items
university_homework_availability (assignment_id, level)
university_presentation_availability (presentation_id, level)
```

## `users` (extended)

| Field | Type | Notes |
|-------|------|--------|
| `role` | `VARCHAR(20)` | CHECK `IN ('USER','STUDENT','UNIVERSITY_STUDENT','ADMIN')`. Default `'USER'`. |
| `university_level` | `VARCHAR(20)` NULL | CHECK null or `IN ('BEGINNER','INTERMEDIATE')`. **Required** when role is `UNIVERSITY_STUDENT`; **must be null** otherwise (DB CHECK). |

### Validation / transitions

- Register → `USER`, `university_level = null`.
- Grant university: target must be `USER` (not `STUDENT`, not `ADMIN`); set `UNIVERSITY_STUDENT` + level; **no email**.
- Revoke university: `UNIVERSITY_STUDENT` → `USER`, clear level; **no email**; submissions retained.
- Grant private `STUDENT`: reject if role is `UNIVERSITY_STUDENT` (must revoke university first).
- Grant university: reject if role is `STUDENT` (must revoke student first).
- Change level: only while `UNIVERSITY_STUDENT`; updates which schedule/materials/homework apply.

## `university_schedule_sessions`

Weekly template row (one meeting slot).

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID PK | |
| `level` | `VARCHAR(20)` | `BEGINNER` \| `INTERMEDIATE` |
| `day_of_week` | `SMALLINT` | ISO 1–7 (Mon–Sun) or project-consistent convention — document in migration comment |
| `start_time` | `TIME` | Local wall time in teacher’s course timezone (assume Europe/Madrid or existing app default; document in quickstart) |
| `end_time` | `TIME` | Must be after `start_time` |
| `title` | `VARCHAR(200)` NULL | Optional label |
| `created_at` / `updated_at` | timestamptz | |

**Rules**: Product targets 5 beginner + 2 intermediate rows; DB does not hard-cap counts (teacher may temporarily publish fewer). Unique optional: `(level, day_of_week, start_time)` to prevent duplicates.

## `university_schedule_exceptions`

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID PK | |
| `level` | `VARCHAR(20)` | |
| `exception_date` | `DATE` | Calendar date of the override |
| `kind` | `VARCHAR(20)` | `CANCEL` \| `EXTRA` |
| `session_id` | UUID NULL | FK to template session when `CANCEL` (which occurrence to remove) |
| `start_time` / `end_time` | `TIME` NULL | Required for `EXTRA` |
| `title` | `VARCHAR(200)` NULL | |
| `created_at` / `updated_at` | timestamptz | |

**Rules**: For a given date+level, `CANCEL` removes the matching template occurrence; `EXTRA` adds a one-off session. Exceptions win over template for that date (FR-008a).

## `university_exam_dates`

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID PK | |
| `title` | `VARCHAR(200)` | |
| `exam_at` | timestamptz | |
| `description` | `TEXT` NULL | |
| `published` | `BOOLEAN` | Default true for v1 simplicity, or false until publish — prefer `published` flag; public list returns `published = true` only |
| `created_at` / `updated_at` | timestamptz | |

Not filtered by level (cohort-wide, public).

## `university_news_items`

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID PK | |
| `title` | `VARCHAR(200)` | |
| `body` | `TEXT` | Plain or existing rich-text JSON if project already standardizes — prefer plain/`TEXT` for v1 unless news reuses homework rich text (keep plain markdown/plain text for simplicity) |
| `published` | `BOOLEAN` | |
| `published_at` | timestamptz NULL | Set when published |
| `created_at` / `updated_at` | timestamptz | |

Public list: `published = true`, newest first.

## `university_homework_availability`

| Field | Type | Notes |
|-------|------|--------|
| `assignment_id` | UUID FK → `homework_assignments(id)` ON DELETE CASCADE | |
| `level` | `VARCHAR(20)` | |
| PK | `(assignment_id, level)` | |

Teacher makes an existing catalog homework visible to that university level. Does **not** create `homework_targets` rows.

## `university_presentation_availability`

| Field | Type | Notes |
|-------|------|--------|
| `presentation_id` | UUID FK → `presentations(id)` ON DELETE CASCADE | |
| `level` | `VARCHAR(20)` | |
| PK | `(presentation_id, level)` | |

Same pattern for materials/presentations.

## Existing entities (reuse, no schema fork)

- **`homework_assignments` / questions / options**: unchanged catalog.
- **`homework_submissions` / answers**: unchanged; university students write rows when completing available homework. Access check: university availability for `users.university_level` (not only `homework_targets`).
- **`presentations` / slides / files**: unchanged; university read path checks presentation availability for level (not only shares/units).

## State transitions

### University enrollment

```text
USER --grant(level)--> UNIVERSITY_STUDENT
UNIVERSITY_STUDENT --revoke--> USER
UNIVERSITY_STUDENT --changeLevel--> UNIVERSITY_STUDENT (new level)
STUDENT --grant university--> REJECT (revoke STUDENT first)
UNIVERSITY_STUDENT --grant STUDENT--> REJECT (revoke university first)
```

### Homework (university)

Same as private: MANUAL `PENDING → SUBMITTED → REVIEWED`; EXERCISE → terminal `GRADED`. Single submission per `(user_id, assignment_id)`.

### News / exams

Draft (`published=false`) → Published (`published=true`); unpublish optional via setting `published=false`.
