# Research: Unit Content Ordering

**Branch**: `028-unit-content-ordering` | **Date**: 2026-08-07

## Decision 1 — Shared `unit_position` on both content tables

**Decision**: Add `unit_position INT NOT NULL DEFAULT 0` to `presentations` and
`homework_assignments`. Within a unit, positions form one contiguous sequence
`0..n-1` across both types (a homework at position 1 sits between presentations at
0 and 2). Items with `unit_id IS NULL` keep `unit_position = 0` (meaningless until
attached).

**Rationale**: Membership is already a nullable FK on each table. A sibling integer
matches the established `units.position` / testimonials `display_order` idiom and
avoids a new join table. Positions are comparable across types without a union view
for writes.

**Alternatives considered**:
- *Join table `unit_contents(unit_id, item_type, item_id, position)`* — cleaner
  polymorphism, but duplicates membership already stored on `unit_id` and adds sync
  risk (YAGNI).
- *Reuse `homework_assignments.sort_order`* — legacy/dead for admin authoring; does
  not exist on presentations; renaming would confuse learning queries that still
  `ORDER BY sort_order`. Rejected.
- *Lexical / fractional ranks* — unnecessary at single-teacher unit sizes.

## Decision 2 — Reorder API mirrors unit reorder; membership endpoints append

**Decision**:
- New `PUT /api/v1/admin/units/{id}/contents/reorder` with body
  `{ "items": [ { "type": "PRESENTATION"|"HOMEWORK", "id": "uuid" }, ... ] }` —
  must be an exact permutation of the unit’s current members; rewrites
  `unit_position` to `0..n-1`.
- Existing `PUT .../presentations` and `PUT .../homeworks` keep membership
  semantics: retained items keep their relative positions; newly attached items
  append at `max(unit_position)+1` (and onward); detached items clear `unit_id`
  and reset `unit_position` to 0. Moving into another unit appends on the
  destination (FR-011).

**Rationale**: Matches `PUT /admin/units/reorder` and testimonials reorder.
Separating membership from order keeps attach/detach UX simple while giving drag-
and-drop a single bulk write.

**Alternatives considered**:
- *Single `PUT .../contents` that sets membership + order* — one call, but forces
  the UI to rebuild the full mixed list on every attach; easier to get wrong.
- *Per-item PATCH position* — more chatty; harder to keep contiguous indices.

## Decision 3 — `UnitDetail.contents` replaces dual arrays for display

**Decision**: Change `UnitDetail` so the teacher UI consumes a single ordered
`contents` array of discriminated items (`type` + nested presentation or homework
summary + `unitPosition`). Drop the separate top-level `presentations` /
`homeworks` arrays from `UnitDetail` (counts remain on `UnitSummary`).

**Rationale**: Spec FR-001 requires one combined list; dual arrays invite the old
type-segregated UI to persist. Frontend already loads detail only for the expanded
unit.

**Alternatives considered**: Keep both shapes for compatibility — rejected as
duplicate surface area for a teacher-only admin DTO with no external consumers.

## Decision 4 — Student overview carries `unitPosition`; unit page interleaves

**Decision**: Add optional `unitPosition` (integer, present when the item has a
unit) to learning `SharedPresentationSummary` and `HomeworkItemResponse`.
`UnitDetailContent` merges accessible presentations + homeworks for that unit,
sorts by `unitPosition`, and renders one list. Remove the client-side status-based
sort on the unit page (status remains a badge/label only). Non-unit (“Other”)
buckets and surfaces outside the unit detail view stay as today.

**Rationale**: Spec US2 / FR-006–007; overview already returns both lists with
`unit` refs — only the shared rank is missing. Status sort currently fights the
teacher sequence.

**Alternatives considered**: Dedicated `GET /learning/units/{id}` — unnecessary
while overview payload is small for this site.

## Decision 5 — Migration seed = presentations then homeworks (current list order)

**Decision**: Flyway `V10__unit_content_position.sql` adds columns, then for each
unit assigns positions: all presentations ordered as currently returned
(`updated_at DESC`), then all homeworks (`created_at DESC`), zero-based contiguous.

**Rationale**: Matches clarification FR-013 and what teachers see today in the
type-segregated lists. Seed is immediately active for students (FR-014).

## Decision 6 — Teacher reorder UX: `@dnd-kit` sortable list

**Decision**: Prefer **drag-and-drop** on the Units tab content list using
`@dnd-kit/core` + `@dnd-kit/sortable` (+ `@dnd-kit/utilities`). On drag end, call
`reorderUnitContents` with the new item order. Keep a compact keyboard-accessible
fallback (▲/▼ or dnd-kit keyboard sensors) so reorder is not mouse-only.

**Rationale**: Explicit product preference for this feature. No existing sortable
DnD library in the repo (only exercise `DragDropQuestion` HTML5 DnD, unsuitable as
list infrastructure). `@dnd-kit` is the current React-friendly accessible choice
and stays localized to the admin Units content list.

**Alternatives considered**:
- *▲/▼ only (existing unit/testimonial pattern)* — simpler dependency-wise but
  rejected by the planning input for content ordering.
- *Native HTML5 drag on rows* — brittle with SSR/React 19; poor a11y; more custom
  code than adopting `@dnd-kit`.

## Decision 7 — Do not change access rules or unit-level ordering

**Decision**: Reorder never grants/revokes access. `units.position` and
`PUT /admin/units/reorder` unchanged. Homework assignment and unit assignment
flows unchanged.

**Rationale**: FR-005, FR-012, SC-004.
