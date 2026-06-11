# Phase 1 — Data Model: Teacher Backoffice — Control Panel

Feature: `006-teacher-backoffice` | Date: 2026-06-10 | Spec: [spec.md](spec.md) · Research: [research.md](research.md)

All schema is plain PostgreSQL via Flyway, following the existing `V1`–`V5` style
(UUID `gen_random_uuid()` PKs, `TIMESTAMPTZ ... DEFAULT NOW()`, `CHECK` constraints,
explicit indexes). Times of day are stored as `TIME` in the teacher timezone
(`app.scheduling.teacher-timezone`); calendar instants remain `TIMESTAMPTZ`.

Migrations (one per cohesive concern):

- `V6__add_user_roles.sql`
- `V7__create_availability.sql`
- `V8__create_homework_targets.sql`
- `V9__create_presentations.sql`

---

## 1. Users — role (V6)

Add an authorization designation to the existing `users` table.

| Column | Type | Notes |
|--------|------|-------|
| `role` | `VARCHAR(20) NOT NULL DEFAULT 'STUDENT'` | `CHECK (role IN ('STUDENT','ADMIN'))` |

- No data destruction; existing rows default to `STUDENT`.
- Promotion to `ADMIN` is **not** done in the migration. A startup `AdminBootstrap`
  (`CommandLineRunner`) runs `UPDATE users SET role='ADMIN' WHERE LOWER(email)=LOWER(:adminEmail)`
  using `app.scheduling.teacher-email`; idempotent.
- `User` model gains a `String role`; `USER_MAPPER` and `UserRepository.save` read/write it.
- `UserResponse` record gains `String role`.

**Validation / rules**: role is server-authoritative; never accepted from a client payload.

---

## 2. Availability (V7)

### `availability_rules` — recurring weekly pattern

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK` | |
| `day_of_week` | `SMALLINT NOT NULL` | ISO 1=Monday … 7=Sunday; `CHECK (day_of_week BETWEEN 1 AND 7)` |
| `start_time` | `TIME NOT NULL` | teacher-local |
| `end_time` | `TIME NOT NULL` | teacher-local; `CHECK (end_time > start_time)` |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | |

- A weekday with **no rows** ⇒ fully unavailable (weekends seeded with none).
- Multiple rows per weekday model split windows (e.g. morning + afternoon).
- Index: `availability_rules_day_idx (day_of_week)`.

### `availability_exceptions` — date-specific overrides

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK` | |
| `exception_date` | `DATE NOT NULL` | teacher-local date |
| `kind` | `VARCHAR(10) NOT NULL` | `CHECK (kind IN ('BLOCK','OPEN'))` |
| `start_time` | `TIME NOT NULL` | window start |
| `end_time` | `TIME NOT NULL` | `CHECK (end_time > start_time)` |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | |

- `BLOCK` removes time on `exception_date` that the weekly pattern would otherwise offer.
- `OPEN` adds time on `exception_date` (may be a weekday with no weekly rule).
- A full-day block is expressed as a window spanning the working day (e.g. `00:00`–`23:59`)
  or the day's rule windows; the generator subtracts it.
- Index: `availability_exceptions_date_idx (exception_date)`.

**Derivation (no stored slots)** — for each day `D` in the 2-week horizon, in `AvailabilityService`:

```
windows(D) = union(rules where day_of_week = isoWeekday(D))
windows(D) = windows(D) ∪ union(OPEN exceptions where exception_date = D)
windows(D) = windows(D) − union(BLOCK exceptions where exception_date = D)
for each class-duration step inside windows(D): emit a slot
  status = BOOKED              if a confirmed booking starts there
         = UNAVAILABLE         if in the past or inside the min-lead window
         = OPEN                otherwise
```

Times outside any window are simply not emitted as OPEN (matching today's UNAVAILABLE).

**Seed (behavior-preserving)**: `availability_rules` seeded Mon–Fri (1–5) with two windows
each — `09:00–12:00` and `14:00–18:00` — reproducing the current day window minus the
12:00–14:00 lunch break. No exceptions seeded.

**Validation rules** (service + DB `CHECK`):
- `end_time > start_time` (per window).
- Exceptions cannot be in the past relative to the teacher-local today (service-level; rejected with `VALIDATION_ERROR`).
- Overlapping windows on the same weekday/date are normalized (merged) by the generator; the
  editor may also coalesce before save.

---

## 3. Homework targeting (V8)

### `homework_targets` — which students an assignment is assigned to

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK` | |
| `assignment_id` | `UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE` | |
| `user_id` | `UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE` | |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | |
| | | `UNIQUE (assignment_id, user_id)` |

- Index: `homework_targets_user_idx (user_id)`, `homework_targets_assignment_idx (assignment_id)`.

**Existing tables unchanged**: `homework_assignments`, `homework_submissions` keep their `V5`
shape (status `PENDING/SUBMITTED/REVIEWED`, `UNIQUE(user_id, assignment_id)`).

**Visibility rule change (FR-018)**: A student sees an assignment **iff** a
`homework_targets` row links them. The three `V5` placeholder assignments have no targets ⇒
invisible to all students until Paula assigns them (they are drafts), so no shared-to-all
items remain.

**State transitions** (unchanged from 005, student-driven):

```
(no submission row) ─submit─▶ SUBMITTED ─(re-submit while editable)─▶ SUBMITTED
        │  default view = PENDING
        └─ overdue = (due_on < today AND status != SUBMITTED)   [derived, never stored]
REVIEWED: reserved; set only by a future teacher-review feature; read-only to students here.
```

**Teacher-side read (FR-015)**: per assignment, join `homework_targets → users` and
LEFT-JOIN `homework_submissions` to list each assignee with status + `response_text`.

---

## 4. Presentations (V9)

### `images` — uploaded binary content

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK` | |
| `content_type` | `VARCHAR(50) NOT NULL` | `CHECK (content_type IN ('image/jpeg','image/png','image/webp'))` |
| `byte_size` | `INT NOT NULL` | `CHECK (byte_size > 0 AND byte_size <= 2097152)` (2 MB) |
| `data` | `BYTEA NOT NULL` | raw bytes |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | |

Served via `GET /api/v1/images/{id}` with the stored `content_type`.

### `presentations` — a deck

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK` | |
| `title` | `VARCHAR(200) NOT NULL` | |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | |
| `updated_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | bumped on edit |

### `presentation_slides` — ordered slides

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK` | |
| `presentation_id` | `UUID NOT NULL REFERENCES presentations(id) ON DELETE CASCADE` | |
| `heading` | `VARCHAR(200) NOT NULL` | |
| `body` | `TEXT NOT NULL DEFAULT ''` | slide text |
| `image_id` | `UUID REFERENCES images(id) ON DELETE SET NULL` | optional single image |
| `sort_order` | `INT NOT NULL DEFAULT 0` | order within the deck |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | |

- Index: `presentation_slides_presentation_idx (presentation_id, sort_order)`.

### `presentation_shares` — which students a deck is shared with

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID PK` | |
| `presentation_id` | `UUID NOT NULL REFERENCES presentations(id) ON DELETE CASCADE` | |
| `user_id` | `UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE` | |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | |
| | | `UNIQUE (presentation_id, user_id)` |

- Index: `presentation_shares_user_idx (user_id)`.

**Visibility rule**: a student may list/open a deck **iff** a `presentation_shares` row links
them. The deck-detail endpoint returns `PRESENTATION_NOT_FOUND` (404) when the caller is not
an admin and not shared, so non-targeted decks are indistinguishable from non-existent ones.

**Validation rules**:
- `title`, slide `heading` ≤ 200 chars; slide `body` length-capped (e.g. ≤ 5 000 chars).
- A deck may have up to a bounded number of slides (e.g. ≤ 100) — `VALIDATION_ERROR` beyond.
- Image upload restricted to allowed content types and ≤ 2 MB (`INVALID_IMAGE` otherwise).
- Reorder payload must be a permutation of the deck's existing slide ids.

---

## Entity relationships (new + touched)

```
users (1) ───< homework_targets >─── (1) homework_assignments
  │  role: STUDENT|ADMIN                         │
  │                                              └──< homework_submissions (per user, status)
  ├───< presentation_shares >─── presentations ──< presentation_slides >── images (optional)
  │
availability_rules (weekly, standalone)     availability_exceptions (per date, standalone)
        └──────────── consumed by AvailabilityService (virtual slots) ─────────────┘
```

## Mapping to functional requirements

| Requirement | Backing data |
|-------------|--------------|
| FR-001/002/003 (teacher-only) | `users.role` + JWT claim + `hasRole` matcher |
| FR-005/006/007 (availability) | `availability_rules`, `availability_exceptions` → `AvailabilityService` |
| FR-009 (safeguards retained) | booked overlay from `bookings`; lead/past checks unchanged |
| FR-010 (booking preserved + warn) | bookings untouched; save response lists out-of-availability confirmed bookings |
| FR-012–017 (homework author/assign/review/edit/delete) | `homework_assignments` + `homework_targets` (+ `homework_submissions` read) |
| FR-014/018 (per-student visibility, migration) | visibility via `homework_targets` join |
| FR-019–025 (presentations) | `presentations`, `presentation_slides`, `presentation_shares`, `images` |
| FR-026 (scope limit) | schema has only heading/body/one image/order — no extra fields |
| FR-028 (bounds) | `CHECK` constraints + service validation |
| FR-030 (persistence) | all server-side tables; no client-only state |
