# Phase 1 Data Model: Mi aprendizaje — Classes & Homework Tab

Persisted in PostgreSQL 18 via Flyway migration `V5__create_learning.sql`. Style mirrors the existing `auth`, `scheduling`, and `resources` schemas: `UUID` PKs via `gen_random_uuid()`, `TIMESTAMPTZ` timestamps, `CHECK` constraints for enums, plain JDBC access (no JPA). Reuses the existing `users` table.

Three tables hold **shared seeded definitions** shown to every authenticated student (`learning_presentation`, `past_classes`, `homework_assignments`); one table holds **per-student state** (`homework_submissions`). The future teacher backoffice will write to the same tables.

---

## Tables

### `learning_presentation`

An ordered block of teacher presentation copy shown at the top of the section (read-only to students).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | `DEFAULT gen_random_uuid()` |
| `heading` | `VARCHAR(200)` | NOT NULL — block heading |
| `body` | `TEXT` | NOT NULL — block paragraph(s) |
| `published` | `BOOLEAN` | NOT NULL `DEFAULT true` — unpublished blocks excluded |
| `sort_order` | `INT` | NOT NULL `DEFAULT 0` — display ordering |
| `created_at` | `TIMESTAMPTZ` | NOT NULL `DEFAULT NOW()` |

### `past_classes`

A completed lesson shown to students as seeded sample data (shared across students this iteration).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | `DEFAULT gen_random_uuid()` |
| `title` | `VARCHAR(200)` | NOT NULL — topic/title |
| `held_on` | `DATE` | NOT NULL — the day the class took place |
| `teacher_note` | `TEXT` | NOT NULL — short summary/note from the teacher |
| `published` | `BOOLEAN` | NOT NULL `DEFAULT true` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL `DEFAULT NOW()` |

Ordering: the overview returns rows `WHERE published = true ORDER BY held_on DESC` (most recent first, FR-006).

### `homework_assignments`

A homework task **definition** shown to students (seeded sample, shared across students this iteration). Per-student progress lives in `homework_submissions`.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | `DEFAULT gen_random_uuid()` |
| `title` | `VARCHAR(200)` | NOT NULL |
| `instructions` | `TEXT` | NOT NULL — what the student must do |
| `due_on` | `DATE` | **nullable** — optional due date (FR-007); overdue derived from it |
| `published` | `BOOLEAN` | NOT NULL `DEFAULT true` |
| `sort_order` | `INT` | NOT NULL `DEFAULT 0` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL `DEFAULT NOW()` |

### `homework_submissions`

An individual student's state for a given assignment. Created lazily on first submit; absence ⇒ the assignment is `PENDING` for that student.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | `DEFAULT gen_random_uuid()` |
| `user_id` | `UUID` | NOT NULL FK → `users(id)` `ON DELETE CASCADE` |
| `assignment_id` | `UUID` | NOT NULL FK → `homework_assignments(id)` `ON DELETE CASCADE` |
| `status` | `VARCHAR(10)` | NOT NULL `DEFAULT 'PENDING'`, `CHECK (status IN ('PENDING','SUBMITTED','REVIEWED'))` |
| `response_text` | `TEXT` | nullable — student's short written response (length-capped in the service layer) |
| `submitted_at` | `TIMESTAMPTZ` | nullable — set when status becomes `SUBMITTED` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL `DEFAULT NOW()` — last change |

Constraints:
- **`UNIQUE (user_id, assignment_id)`** — at most one submission row per student per assignment; upsert via `ON CONFLICT (user_id, assignment_id) DO UPDATE`.
- Index `homework_submissions_user_id_idx` on `(user_id)`.

---

## Relationships

```text
users 1───∞ homework_submissions ∞───1 homework_assignments   (UNIQUE per user+assignment)
learning_presentation   (standalone, shared)
past_classes            (standalone, shared)
homework_assignments    (shared definitions) 1───∞ homework_submissions (per-user state)
```

---

## Derived / virtual concepts (not stored)

- **Effective status**: for the calling student, an assignment's status is its `homework_submissions.status` if a row exists, otherwise **`PENDING`** (LEFT JOIN default).
- **`overdue` flag**: `due_on IS NOT NULL AND due_on < today(teacherZone) AND effective status = PENDING`. `teacherZone` = `app.scheduling.teacher-timezone` (default `Europe/Madrid`). Never stored; computed per request (FR-009).
- **Empty states**: `pastClasses == []` and `homework == []` drive the friendly empty-state copy in the UI (FR-010); seeded data makes these non-empty in dev.

---

## Validation & rules (from requirements)

| Rule | Source | Enforcement |
|------|--------|-------------|
| Entire section requires auth | FR-002, FR-003 | `/api/v1/learning/**` under `anyRequest().authenticated()` → `401` for guests |
| Past classes shown most-recent-first | FR-006 | `ORDER BY held_on DESC` in `ContentRepository` |
| Homework shows title/instructions/due/status | FR-007 | `HomeworkItemResponse` assembled by `LearningService` |
| Student drives PENDING → SUBMITTED only | FR-014 | `HomeworkSubmissionService` upsert sets `SUBMITTED`, `submitted_at = NOW()` |
| `REVIEWED` is read-only to students | FR-014 | Service rejects submit when current status is `REVIEWED` → `409 SUBMISSION_NOT_ALLOWED` |
| Submission persists per student across sessions/devices | FR-008, FR-015 | Row keyed by `(user_id, assignment_id)` in PostgreSQL |
| Overdue derived, not stored | FR-009 | Computed in `LearningService` from `due_on` + effective status |
| Response is a short text | Assumptions | `@Size(max = 2000)` on `SubmitHomeworkRequest.response` → `400` if exceeded |
| Unknown assignment id | Edge case | `AssignmentNotFoundException` → `404 ASSIGNMENT_NOT_FOUND` |
| Empty/"nothing yet" state | FR-010 | Empty arrays in the overview; frontend renders empty state |

---

## Seed data (placeholder, in `V5`)

Clearly-labelled Spanish placeholder content so every user story is demonstrable in dev (removable without breaking the empty state). No `homework_submissions` rows are seeded, so every student starts with all homework `PENDING`:

- **`learning_presentation`** — 2–3 blocks introducing Paula's classes (e.g. "Cómo son mis clases", "Qué aprenderás", "Cómo funciona la tarea").
- **`past_classes`** — 3–4 past lessons with `held_on` dates in the recent past and a teacher note each (e.g. "El pretérito indefinido", "Vocabulario de viajes", "Conversación: pedir en un restaurante").
- **`homework_assignments`** — 3 assignments covering the demonstrable states:
  - one with `due_on` in the **past** (renders **overdue** while `PENDING`),
  - one with `due_on` in the **future**,
  - one with **no** `due_on`.
