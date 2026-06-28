# Phase 0 Research: Class Units (Packages)

All Technical Context items were resolvable from the existing codebase; no
NEEDS CLARIFICATION markers remained after `/speckit-clarify`. This document records
the key design decisions and the alternatives considered.

## Decision 1 — Unit membership as one-to-many (nullable FK), not a join table

**Decision**: Add a nullable `unit_id UUID` column to both `presentations` and
`homework_assignments`, referencing `units(id) ON DELETE SET NULL`.

**Rationale**: The clarification locked membership to "at most one unit per item".
A direct nullable FK is the simplest representation of one-to-many, matches the
Simplicity-First principle, and makes "move item to another unit" a single UPDATE.
`ON DELETE SET NULL` means deleting a unit leaves its content intact and unattached,
satisfying the "Deleting a unit" edge case (content not deleted).

**Alternatives considered**:
- *Join tables (`unit_presentations`, `unit_homeworks`)* — supports many-to-many but
  was explicitly rejected by the clarification; adds tables and uniqueness
  constraints for no current benefit.
- *Reusing the existing `presentation_shares` semantics* — conflates per-student
  sharing with content grouping; rejected.

## Decision 2 — Presentation access = UNION of legacy shares + unit assignments

**Decision**: A student may access a presentation if EITHER a row exists in
`presentation_shares(presentation_id, user_id)` (legacy) OR the presentation's
`unit_id` is assigned to the student in `unit_assignments(unit_id, user_id)`. The
share gate (`isSharedWith`) and the learning overview query
(`findSharedSummariesForUser`) are rewritten to UNION both sources.

**Rationale**: Preserves FR-017 (legacy direct shares keep working) while making unit
assignment the new primary grant path. Dynamic-by-construction: adding a presentation
to an assigned unit immediately makes it visible (FR-010) because access is computed
at query time, not snapshotted. No data migration of existing shares required.

**Alternatives considered**:
- *Migrate all `presentation_shares` into units and drop the table* — risky, lossy
  (legacy items have no natural unit), and violates "migration optional". Rejected.
- *Materialize unit grants into `presentation_shares` rows on assignment* — breaks
  the dynamic requirement (FR-010) and needs reconciliation whenever unit contents
  change. Rejected in favor of query-time UNION.

## Decision 3 — Homework access stays unchanged (targets only)

**Decision**: Do not touch homework access resolution. `unit_id` on
`homework_assignments` is organizational only; student visibility remains driven
solely by `homework_targets`.

**Rationale**: Directly enforces FR-012/FR-013/FR-016 ("assigning a unit MUST NOT
grant homework") with zero risk — the access path simply never reads `unit_id`. The
teacher's existing per-student assignment flow (`setAssignees` →
`SetAssigneesRequest`) is reused as-is.

## Decision 4 — Unit ordering via integer `position` within level

**Decision**: `units` carries an integer `position`. Ordering is scoped per level
(A1 unit 1, 2, 3 …). The list query sorts by `(level, position)`. Reordering is a
bulk endpoint that rewrites positions for a level, mirroring the existing slide
reorder pattern (`updateSortOrders`).

**Rationale**: Matches the spec's "unit 1, 2, 3" phrasing (FR-002a) and the
established reorder idiom already in `PresentationRepository`. Integer positions are
simple and human-meaningful.

**Alternatives considered**:
- *Fractional/lexicographic ranks* — avoids rewrites on reorder but adds complexity
  unjustified at this scale (tens of units). Rejected.
- *Sort by subject name only* — rejected by clarification (explicit ordering wanted).

## Decision 5 — New `units` bounded context mirroring `presentations`

**Decision**: Create `com.kuky.backend.units` with controller/service/repository/
model/dto/exception, following the existing `presentations` package shape, plain JDBC
via `NamedParameterJdbcTemplate`. Admin endpoints under `/api/v1/admin/units` (covered
by the existing `/api/v1/admin/**` security matcher → ADMIN-only).

**Rationale**: Consistency with established conventions; ADMIN gating is automatic via
the existing matcher (same as `/api/v1/admin/presentations`). No security config change
needed.

**Alternatives considered**:
- *Add unit logic into the existing `presentations`/`admin` packages* — would couple
  unrelated concerns and bloat those services. A dedicated context is cleaner.

## Decision 6 — Front-end tab consolidation

**Decision**: In `AdminPanel.tsx`, remove the `homework` and `presentations` tabs from
`VALID_TABS`/`TabsList` and add a single `units` tab rendering `<UnitsTab />`. The
existing homework editor routes (`/panel/tareas/...`) are retained and reached from
inside a unit; the presentation authoring controls (upload/replace/level) move into
the unit's content view.

**Rationale**: Satisfies FR-001/SC-005 (single tab) with minimal disruption — the
existing homework editor pages and presentation file endpoints are reused; only the
list shells are replaced by the unit-grouped view.

**Alternatives considered**:
- *Keep three tabs and just add Units* — fails FR-001 (must replace the two tabs).
- *Rewrite the homework editor inline* — unnecessary; the existing routes work.

## Open considerations deferred to implementation

- Whether the legacy per-presentation "Share" dialog UI is removed entirely or kept
  read-only for legacy rows. The data model supports both; default is to drop the
  per-presentation share control in the new UI and rely on unit assignment, while the
  `presentation_shares` table and its query path remain for existing rows.
- i18n keys for the new Units tab (`admin.tabs.units`, `admin.units.*`) follow the
  existing `react-i18next` resource structure.
