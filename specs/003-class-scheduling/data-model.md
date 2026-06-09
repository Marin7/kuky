# Phase 1 Data Model: 1-on-1 Class Scheduling

Derived from the Key Entities in `spec.md`. Storage is PostgreSQL 18 via plain JDBC + Flyway (no JPA), matching the existing `auth` package conventions.

## Overview

Only **one new table** is introduced: `bookings`. Availability **Slots** are virtual value objects computed at request time (see `research.md` §1) and are not persisted. **Student** and **Teacher** are not new tables: a student is an existing `users` row referenced by FK; the teacher (Paula) is a single configured identity, not a table.

---

## Entity: Booking (persisted — table `bookings`)

A confirmed reservation of a slot by a student, owning one Zoom meeting.

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID (PK) | `gen_random_uuid()` default, matches `users` convention |
| `user_id` | UUID (FK → `users.id`) | The student who booked. `ON DELETE CASCADE` |
| `slot_start` | TIMESTAMPTZ | UTC instant of the class start. Identifies the virtual slot |
| `duration_minutes` | INT | Class length captured at booking time (default 60, configurable). Stored so a later duration change does not retro-alter past bookings |
| `status` | VARCHAR(20) | `CONFIRMED` or `CANCELLED`. No `COMPLETED` — "past" is derived from `slot_start + duration` vs now (clarified) |
| `zoom_meeting_id` | VARCHAR(64) | Provider meeting id (nullable until provisioned; in practice set before commit) |
| `zoom_join_url` | TEXT | Join link shown to student and emailed |
| `created_at` | TIMESTAMPTZ | `NOW()` default |
| `cancelled_at` | TIMESTAMPTZ | Null unless cancelled |

### Constraints & indexes

- **PK**: `id`.
- **FK**: `user_id` → `users(id)` `ON DELETE CASCADE`.
- **Partial unique index** (concurrency / FR-007):
  `CREATE UNIQUE INDEX bookings_active_slot_uniq ON bookings (slot_start) WHERE status = 'CONFIRMED';`
  Guarantees at most one active booking per slot while allowing re-booking after cancellation.
- **Index** on `user_id` for the "my bookings" query.
- **CHECK**: `status IN ('CONFIRMED','CANCELLED')`.

### Validation rules (enforced in `BookingService`, not just DB)

- `slot_start` MUST align to a valid generated slot (on the hour within 09:00–18:00 in the teacher timezone, within the horizon). Reject misaligned/out-of-window starts → `SLOT_UNAVAILABLE`.
- `slot_start` MUST be ≥ `min-lead-hours` (24 h) in the future → else `BOOKING_TOO_SOON`.
- `slot_start` MUST NOT be in the past → covered by the lead-time rule.
- The slot MUST currently be open (no active booking) → else `SLOT_UNAVAILABLE`.
- Cancellation allowed only while `slot_start - now ≥ cancel-cutoff-hours` (24 h) → else `CANCELLATION_TOO_LATE`.
- A user may cancel only their own booking → else `403`/`404`.

### State transitions

```
            book (reserve + Zoom OK)
   (none) ─────────────────────────▶ CONFIRMED
                                        │
                                        │ cancel (≥24h before start)
                                        ▼
                                     CANCELLED   (slot reopens; Zoom meeting deleted)

   CONFIRMED ──(slot_start+duration < now)──▶ still CONFIRMED, rendered as "past"
   reserve then Zoom fails ─▶ row deleted (compensating); never reaches a usable CONFIRMED
```

---

## Value Object: Slot (virtual — not persisted)

Computed by `AvailabilityService` for the requested horizon.

| Field | Type | Notes |
|-------|------|-------|
| `start` | Instant (UTC) | Slot start |
| `end` | Instant (UTC) | `start + duration` |
| `status` | enum | `OPEN`, `BOOKED`, or `UNAVAILABLE` (past or inside the 24 h lead window) |

Generation algorithm (per request):
1. Determine horizon = current ISO week + next ISO week (Mon–Sun) in `teacher-timezone`.
2. For each date in the horizon, emit slots at `class-duration-minutes` steps from `day-start` (09:00) up to `day-end` (18:00), in the teacher timezone, converted to UTC instants.
3. Mark a slot `BOOKED` if a `CONFIRMED` booking exists for that `slot_start`.
4. Mark a slot `UNAVAILABLE` if it is in the past or within the 24 h lead window.
5. Otherwise `OPEN`.

---

## Reused entity: Student → existing `users` table

No schema change. Booking references `users.id`; the student's email (for notifications) comes from `users.email`. The authenticated principal is the email (per `JwtCookieAuthenticationFilter`); `BookingService` resolves it to the `users` row via the existing `UserRepository`.

## Configured identity: Teacher (Paula)

Not a table. Represented by configuration:
- `app.scheduling.teacher-timezone` (default `Europe/Madrid`)
- `app.scheduling.teacher-email` (notification recipient)
- `app.zoom.user-id` (Zoom account hosting the meetings; default `me`)

---

## Flyway migration

`back-end/src/main/resources/db/migration/V3__create_bookings.sql` creates the `bookings` table, the FK, the partial unique index, the `user_id` index, and the status CHECK. It follows the column-style and `TIMESTAMPTZ`/`gen_random_uuid()` conventions of `V1__create_users.sql`.

## Future-phase readiness (informative)

- **Paid booking** (FR-018): add `payment_status` / `payment_ref` columns and a pre-confirmation payment step without altering slot or Zoom logic.
- **Teacher-managed availability** (FR-016): introduce an `availability_slots` (or rules) table consumed by `AvailabilityService`; the booking table and constraints are unaffected.
