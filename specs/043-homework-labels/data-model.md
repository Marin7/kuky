# Data Model: Homework Labels

**Feature**: `043-homework-labels` | **Date**: 2026-08-16

## Migration

`back-end/src/main/resources/db/migration/V24__homework_label.sql`

```sql
ALTER TABLE homework_assignments
    ADD COLUMN label VARCHAR(40);

COMMENT ON COLUMN homework_assignments.label IS
    'Optional teacher-only organization label; NULL means unlabeled.';
```

Existing rows remain `NULL` (unlabeled). No index required at current list size. No CHECK beyond `VARCHAR(40)` (application enforces trim/empty→null).

Next Flyway version after `V23__homework_target_student_seen.sql`.

## Entity: Homework assignment (extended)

| Field | Type | Rules |
|-------|------|--------|
| `label` | `String` or `null` | Optional. After trim: empty → `null`. Max **40** characters after trim. Any characters (Unicode) otherwise. Not unique. Not part of freeze/`assignment_snapshot`. |

A homework has **either one label or none**. There is no separate Label entity.

## Grouping (derived, not stored)

| Concept | Rule |
|---------|------|
| Group key | `trim(label).toLowerCase` with a case fold that **does not** strip accents (`Locale.ROOT` / `toLocaleLowerCase('es')`) |
| Same group | Non-null labels whose keys are equal |
| Distinct group | Different letters or accents (`Gramática` ≠ `Gramatica`) |
| Filter / reuse list | One option per group among **currently stored non-null** labels |
| Display of option | First spelling seen when scanning the admin list |
| Card badge | Exact stored `label` for that homework |
| Unused label | When no row has that group key, the option disappears |

## Validation

| Input | Stored |
|-------|--------|
| omitted / `null` | `null` |
| `""` or whitespace only | `null` |
| `"  Subjuntivo  "` | `"Subjuntivo"` |
| 41+ characters after trim | reject (`VALIDATION_ERROR`); do not truncate |
| `"subjuntivo"` while another row has `"Subjuntivo"` | store `"subjuntivo"`; both share one filter group |

## Relationships

- Teacher admin create/update/list/get: read and write `label`.
- Student take, submit, result, snapshots, review queue, units, quizzes, presentations: **no** `label` field.
- Changing `label` does not change assignees, submissions, scores, or `content_revised_at`.

## Lifecycle

1. Create with optional label → persist normalized value.
2. Edit: change, clear (→ `null`), or leave as-is.
3. Delete homework → that row’s label vanishes with it; groups with no remaining rows drop from filter/reuse.
4. No rename-everywhere operation.
