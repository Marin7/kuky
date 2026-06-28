# Phase 1 Data Model: Class Units (Packages)

Migration: `back-end/src/main/resources/db/migration/V18__create_units.sql`.
All identifiers are UUID `gen_random_uuid()` defaults, consistent with existing tables.

## New entity: `units`

A class package grouping presentations and homeworks, ordered within its level.

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | `DEFAULT gen_random_uuid()` |
| `level` | VARCHAR(5) NOT NULL | One of `A1,A2,B1,B2,C1,C2` (CHECK constraint) |
| `subject` | VARCHAR(200) NOT NULL | Free text, e.g. "Family" |
| `position` | INT NOT NULL DEFAULT 0 | Ordering within the level (FR-002a) |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |
| `updated_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |

Index: `units_level_position_idx (level, position)` for the grouped/ordered list (FR-007).

**Validation rules**:
- `level` ∈ the six CEFR levels (DB CHECK + service validation, reusing the existing
  `VALID_LEVELS` set pattern from `PresentationService`).
- `subject` non-blank, ≤ 200 chars.
- `position` ≥ 0; duplicate level+subject is allowed (edge case "Duplicate level+subject").

## Modified entity: `presentations`

Add membership FK.

| Column | Type | Notes |
|--------|------|-------|
| `unit_id` | UUID NULL | `REFERENCES units(id) ON DELETE SET NULL` |

- NULL = unattached (legacy content or not-yet-filed). New presentations created
  through the Units UI are created with a `unit_id` (FR-005b enforced in the service,
  not the DB, so legacy NULLs remain valid).
- At most one unit per presentation is guaranteed by the single-column FK (FR-005a).
- Index: `presentations_unit_idx (unit_id)`.

## Modified entity: `homework_assignments`

Add membership FK (organizational only — does **not** affect access).

| Column | Type | Notes |
|--------|------|-------|
| `unit_id` | UUID NULL | `REFERENCES units(id) ON DELETE SET NULL` |

- Index: `homework_assignments_unit_idx (unit_id)`.

## New entity: `unit_assignments`

Grants a student access to a unit's presentations.

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | `DEFAULT gen_random_uuid()` |
| `unit_id` | UUID NOT NULL | `REFERENCES units(id) ON DELETE CASCADE` |
| `user_id` | UUID NOT NULL | `REFERENCES users(id) ON DELETE CASCADE` |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |

- Unique: `(unit_id, user_id)`.
- Index: `unit_assignments_user_idx (user_id)`.
- `user_id` must reference a `STUDENT` (validated in service, like `validateStudents`).

## Relationships

```text
units 1 ──── 0..* presentations          (presentations.unit_id, SET NULL)
units 1 ──── 0..* homework_assignments    (homework_assignments.unit_id, SET NULL)
units 1 ──── 0..* unit_assignments ──── 1 users   (CASCADE both sides)
presentations 1 ──── 0..* presentation_shares ──── 1 users   (LEGACY, unchanged)
homework_assignments 1 ──── 0..* homework_targets ──── 1 users (unchanged)
```

## Derived access rules (query-time, no new storage)

**Presentation visible to student `u`** ⇔
```sql
EXISTS (SELECT 1 FROM presentation_shares s
        WHERE s.presentation_id = p.id AND s.user_id = :u)      -- legacy
OR
EXISTS (SELECT 1 FROM unit_assignments ua
        WHERE ua.unit_id = p.unit_id AND ua.user_id = :u)       -- unit-derived
```
This replaces the body of `isSharedWith` and `findSharedSummariesForUser`
(FR-009/FR-010/FR-011/FR-017). Because it is evaluated per request, presentations
added to an assigned unit appear automatically (FR-010) and removals/unassignments
revoke immediately (FR-011).

**Homework visible to student `u`** ⇔ unchanged: a row in `homework_targets`
for the assignment and user. `unit_id` is never consulted (FR-012/FR-016).

## State & lifecycle notes

- **Delete unit** → `unit_assignments` rows CASCADE-deleted (students lose unit-derived
  access); `presentations.unit_id` / `homework_assignments.unit_id` SET NULL (content
  preserved, becomes unattached). Existing legacy `presentation_shares` rows are
  untouched, so any directly-shared presentation stays visible.
- **Remove presentation from unit** → service sets `presentations.unit_id = NULL`;
  unit-derived access drops on next query (legacy share, if any, still applies).
- **Unassign unit from student** → delete the `unit_assignments` row; in-progress
  homework (assigned separately) is unaffected (edge case).

## Front-end types (in `lib/admin.ts` / `lib/learning.ts`)

- `Unit` summary: `{ id, level, subject, position, presentationCount, homeworkCount, assignedStudentIds }`.
- `UnitDetail`: adds `presentations: PresentationSummary[]`, `homeworks: HomeworkAdminItem[]`, `assignedStudents: Student[]`.
- Learning overview `SharedPresentationSummary` gains optional `unit?: { level, subject, position }` so the student list can group by unit (FR-015); legacy unattached shares have `unit` undefined and render under an "Otros" group.
