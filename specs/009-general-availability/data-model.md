# Phase 1 Data Model: General Availability Template

## Entities

### General Availability Template
The teacher's default weekly pattern; the seed for future weeks. **Backed by the existing `availability_rules` table — unchanged schema, new role.**

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | PK |
| day_of_week | smallint | ISO 1=Mon … 7=Sun |
| start_time | time | |
| end_time | time | `end_time > start_time` |

- A weekday with no rows ⇒ unavailable by default.
- Edited via `PUT /api/v1/admin/availability/weekly` (`replaceWeekly`, delete-all + insert).
- Editing it affects **only weeks not yet materialized** (FR-007) — it never touches `week_availability`.

### Week Availability (concrete, source of truth) — NEW `week_availability`
Absolute available windows for a specific date within a materialized week.

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | PK, `gen_random_uuid()` |
| slot_date | date | the concrete calendar date |
| start_time | time | |
| end_time | time | `end_time > start_time` |
| created_at | timestamptz | default `now()` |

- One row per available window per date; multiple rows per date allowed (merged on read).
- Zero rows for a date in a materialized week ⇒ that date is fully off (distinct from "not materialized").
- Index on `slot_date`.

### Materialized Weeks marker — NEW `materialized_weeks`
Records that a week has been snapshotted (needed to represent materialized-but-empty).

| Field | Type | Notes |
|-------|------|-------|
| week_start | date | PK, the Monday of the week (ISO) |
| created_at | timestamptz | default `now()` |

### Booking (existing)
Confirmed reservations. Relevant only for conflict surfacing (FR-010); seeding/edits must not auto-cancel.

### Availability Exception (existing `availability_exceptions`) — DEPRECATED
Retained read-only; consumed exclusively by `WeekAvailabilityBootstrap` for behavior-preserving migration, then dead. Write endpoints removed.

## Resolution algorithm (replaces `rules ∪ OPEN − BLOCK`)

```
ensureWeeksMaterialized(horizonStart, horizonEnd):
  for each Monday w in [horizonStart, horizonEnd):
    if INSERT INTO materialized_weeks(w) ON CONFLICT DO NOTHING affected a row:
      for each date d in [w, w+7):
        for each template window in availability_rules where day_of_week = ISO(d):
          INSERT INTO week_availability(d, window.start, window.end)
  # transactional per week; idempotent via PK

availableIntervals(date):
  return merge( week_availability rows where slot_date = date )   # absolute, no BLOCK subtraction
```

- `generateSchedule()`, `validateBookable()`, and admin `getAvailability()` all call `ensureWeeksMaterialized` first, then read exclusively from `week_availability`.
- BOOKED / UNAVAILABLE (past / within lead) layering is unchanged.

## State & lifecycle

- **Not materialized** → (week enters horizon) → **Materialized (= template snapshot)** → (teacher edits a day) → **Customized**. There is no transition back to template (no reset — FR-013); customization is overwritten only by further manual edits.
- Template edits create no transition for already-materialized weeks.

## Validation rules

- Every window: `end_time > start_time` (DB CHECK + service validation), times parse as `HH:mm`.
- Per-day edit target date must be within the current horizon and not in the past.
- Per-day edit replaces all rows for that date atomically (delete + insert).
- After any template or per-day change, recompute and surface confirmed bookings now outside availability (FR-010), without cancelling them.

## Migration (`V17__week_availability.sql` + `WeekAvailabilityBootstrap`)

1. `V17` creates `week_availability` and `materialized_weeks` with constraints/indexes.
2. On startup, if `materialized_weeks` is empty, the bootstrap materializes the current horizon from today's effective availability (`rules ∪ OPEN − BLOCK`) so the launch schedule is identical to pre-feature behavior (FR-011).
