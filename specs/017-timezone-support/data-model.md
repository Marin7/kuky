# Phase 1 Data Model: Timezone Support

No new tables. One additive column pair on the existing `users` table; every other entity in the spec already exists in the current schema and is unaffected.

## `users` (extended)

| Column | Type | Notes |
|---|---|---|
| `timezone` | `VARCHAR(64)` NULL | IANA zone id (e.g. `Europe/Bucharest`). `NULL` until the front-end syncs a detected value at least once. Corresponds to spec's **Student Time Zone Preference**. |
| `timezone_is_manual` | `BOOLEAN NOT NULL DEFAULT false` | `false` = auto-detected value, resynced by the front-end every session (per Clarification Session 2026-07-02, Q2). `true` = student explicitly chose an override; the front-end must stop silently resyncing until the student clears it. |

Migration: `back-end/src/main/resources/db/migration/V27__add_user_timezone.sql`:

```sql
ALTER TABLE users
    ADD COLUMN timezone VARCHAR(64),
    ADD COLUMN timezone_is_manual BOOLEAN NOT NULL DEFAULT false;
```

No backfill needed — `NULL` is a valid, meaningful state (no preference synced yet; callers fall back to the teacher's configured zone).

## Entity mapping to spec's Key Entities

- **Student Time Zone Preference** → `users.timezone` + `users.timezone_is_manual`, as above.
- **Teacher Working Time Zone** → unchanged; remains the existing `app.scheduling.teacher-timezone` config property (`Europe/Madrid`), already consumed via `AvailabilityService.zone()` and now also read consistently by front-end admin views (see [contracts/timezone.md](./contracts/timezone.md)).
- **Scheduled Class Instant** → unchanged; remains `bookings.slot_start` (`TIMESTAMPTZ`) / `Booking.slotStart` (`Instant`), the existing zone-independent source of truth.

## Validation rules

- `timezone`, when non-null, MUST be a valid IANA zone id resolvable by `ZoneId.of(...)` (back-end) — reject with the existing validation-error contract (`{"error":"...", "message":"..."}`) on an invalid value rather than persisting garbage.
- `timezone_is_manual` MUST only be settable to `true` together with a non-null `timezone` (an override without a value is meaningless) — enforced in `AuthService.updateTimezone`, not at the DB layer (no CHECK constraint needed; keeps the migration minimal per Simplicity First).

## State transitions (`timezone_is_manual`)

```text
[no preference: timezone=NULL, manual=false]
        │  auto-sync (session load, browser-detected zone)
        ▼
[auto: timezone=<detected>, manual=false] ──resync each session──┐
        │  student explicitly picks a zone in account settings   │
        ▼                                                        │
[manual: timezone=<chosen>, manual=true] ◄────────────────────────┘
        │  student clears override
        ▼
[auto: timezone=<next detected>, manual=false]
```
