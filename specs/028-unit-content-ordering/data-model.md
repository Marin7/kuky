# Data Model: Unit Content Ordering

**Branch**: `028-unit-content-ordering` | **Date**: 2026-08-07

## Schema changes (Flyway `V10__unit_content_position.sql`)

### `presentations`

| Column | Type | Notes |
|--------|------|--------|
| `unit_position` | `INT NOT NULL DEFAULT 0` | Rank within owning unit’s mixed sequence; ignored when `unit_id` is NULL |

### `homework_assignments`

| Column | Type | Notes |
|--------|------|--------|
| `unit_position` | `INT NOT NULL DEFAULT 0` | Same semantics as presentations |

No new tables. Do **not** alter or reuse legacy `homework_assignments.sort_order`
(still used only by older learning ordering paths; remains independent).

Index (optional, nice-to-have for list queries):
`(unit_id, unit_position)` on each table where `unit_id IS NOT NULL` is not
required for correctness at current scale; skip unless query plans need it.

## Seed (same migration)

For each distinct non-null `unit_id`:

1. Assign presentations `unit_position = 0..p-1` ordered by `updated_at DESC`
   (matches current `UnitRepository.findPresentations` order).
2. Assign homeworks `unit_position = p..(p+h-1)` ordered by `created_at DESC`
   (matches current `UnitRepository.findHomeworks` order).

Result: contiguous `0..n-1` per unit; immediately visible to students.

## Logical entities

### Unit content item

A presentation or homework belonging to a unit, with a shared rank.

| Attribute | Description |
|-----------|-------------|
| `type` | `PRESENTATION` \| `HOMEWORK` |
| `id` | Content UUID |
| `unitId` | Owning unit |
| `unitPosition` | Zero-based index in the unit’s mixed sequence |

**Invariants**:
- At most one unit per content item (existing `unit_id` FK).
- Within a unit, `(type, id)` pairs are unique; positions are a permutation of
  `0..n-1` after every successful reorder or membership mutation that rewrites
  ranks.
- Detach → `unit_id = NULL`, `unit_position = 0`.
- Attach / move-in → append at `max(unit_position)+1` (then compact only if
  reorder is called; append need not leave gaps if using max+1 on a contiguous
  list — prefer rewrite retained positions + append so the sequence stays
  contiguous after detach).

### Membership mutation rules

When `setPresentations` / `setHomeworks` runs for a unit:

1. Items removed from the unit: clear `unit_id`, `unit_position = 0`.
2. Items retained: keep relative order among the **full mixed sequence** (do not
   reshuffle presentations ahead of homeworks).
3. Items newly added (including moved from another unit): append after current
   max position on the **destination** unit; if moved from a source unit, remove
   from source and compact/preserve remaining source relative order.

Practical implementation: after membership updates for one type, rebuild
positions for the unit’s full content set by sorting current rows by
`unit_position`, then appending newcomers — or: load current ordered mixed list,
apply membership diff, assign `0..n-1`. Prefer the latter for clarity.

### Unchanged

- `units.position` (order of units within a level)
- `unit_assignments` / presentation access / homework assignees
- Content authoring tables (files, questions, etc.)

## Validation rules

- Reorder request items MUST be exactly the set of current unit members
  (same ids + types, no extras, no missing) → else `400` (e.g.
  `INVALID_CONTENT_ORDER`).
- Unknown `type` → `400`.
- Unit missing → `404 UNIT_NOT_FOUND` (existing).
