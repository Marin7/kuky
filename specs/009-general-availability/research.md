# Phase 0 Research: General Availability Template

All Technical Context fields are known (existing stack). No `NEEDS CLARIFICATION` remained after `/speckit-clarify`. Research below records the three design decisions that carry real risk, each tied to a clarification.

## Decision 1 — Snapshot storage shape

**Decision**: Store per-week snapshots as **absolute per-date windows** in a new `week_availability(slot_date, start_time, end_time)` table, with a `materialized_weeks(week_start)` marker table to represent a materialized-but-empty (fully unavailable) week.

**Rationale**:
- The "snapshot per week" clarification means a week's availability must be persisted independently, so editing the general template later cannot mutate it. A delta-on-template representation (today's `availability_exceptions`) cannot express "frozen against future template edits" without a second source of truth — exactly the ambiguity we are removing.
- Absolute windows make per-day editing a simple replace (delete rows for the date, insert the new windows), eliminating the OPEN/BLOCK delta arithmetic currently in the front-end and `availableIntervals`.
- A marker table is required because zero windows for a date is a legitimate state ("that day is off this week") and must be distinguished from "not yet materialized" (which should fall back to the template).

**Alternatives considered**:
- *Reuse `availability_exceptions` as the snapshot* (write OPEN rows per date + suppress rules for materialized dates): rejected — overloads delta semantics with absolute semantics, requires a per-date "materialized" flag anyway, and keeps the very complexity we want to drop.
- *Single high-water-mark date instead of a marker table* (a week is materialized iff its Monday ≤ stored max): elegant but couples correctness to a monotonic-contiguous-materialization invariant; an explicit marker table is clearer for maintainers and trivially idempotent with `ON CONFLICT DO NOTHING`.

## Decision 2 — Materialization trigger

**Decision**: **Materialize-on-read, lazily.** A single `ensureWeeksMaterialized()` runs at the top of `generateSchedule()`, `validateBookable()`, and the admin `getAvailability()`. For each Monday in the current horizon not present in `materialized_weeks`, in one transaction it inserts the marker and copies the general template's windows into `week_availability` for that week's 7 dates.

**Rationale**:
- No scheduler/cron exists in the app today; slots are already generated synchronously per request. Lazy materialization needs no new infrastructure (YAGNI) and is naturally triggered as the rolling Monday advances.
- Idempotent and concurrency-safe via the marker primary key: the window copy is performed only when the marker insert is genuinely new; concurrent requests collide on the PK and the loser does nothing.

**Alternatives considered**:
- *Scheduled weekly job*: rejected — adds scheduling infrastructure for a single-teacher app; lazy read covers it.
- *Materialize on template save only*: rejected — would not seed weeks that roll into the horizon purely due to passage of time.

## Decision 3 — Behavior-preserving migration (FR-011)

**Decision**: A one-time `WeekAvailabilityBootstrap` (`CommandLineRunner`, mirroring the existing `AdminBootstrap`) materializes the **current horizon from today's effective availability** (`rules ∪ OPEN − BLOCK`, computed with the existing interval logic) when no weeks are yet materialized. Runtime materialization thereafter copies template windows only; the legacy `availability_exceptions` table is read solely by this bootstrap and is then dead, retained one release for rollback safety.

**Rationale**:
- Guarantees students and the teacher see exactly today's schedule at upgrade — both weeks currently in the window (including any future-dated exceptions the teacher already set) are snapshotted verbatim.
- Keeps the runtime path clean (template-only snapshots); all legacy delta handling is isolated in a single, removable bootstrap.
- Follows the project's established startup-bootstrap pattern (`AdminBootstrap`), so no new pattern is introduced.

**Alternatives considered**:
- *Pure SQL Flyway data migration*: rejected — interval merge/subtract is awkward in SQL and would duplicate logic already in `AvailabilityService`.
- *Fold legacy exceptions into runtime `ensureWeeksMaterialized` forever*: rejected — keeps dead-on-arrival branches in the hot path; the bootstrap confines it to startup.

## Supporting facts (from code review)

- The general weekly pattern (`availability_rules`) currently has **no editing UI**; the admin grid only writes per-date `availability_exceptions`. This feature delivers the first real template editor and re-targets the existing `PUT /availability/weekly` endpoint (already implemented but unused by the UI).
- Horizon = current Monday + 2 weeks, hard-coded in `AvailabilityService` (`plusWeeks(2)`); the "Keep current window" clarification means this stays.
- `findConfirmedBookingsOutsideAvailability` (FR-010 conflict surfacing) must be re-pointed at `week_availability`.
