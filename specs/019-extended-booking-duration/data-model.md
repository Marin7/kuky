# Phase 1 Data Model: Extended Booking Duration

New migration: `back-end/src/main/resources/db/migration/V29__extended_booking_duration.sql`.

```sql
-- Per-student permission to book the 1.5-hour class option, granted/revoked by the teacher.
ALTER TABLE users
    ADD COLUMN extended_class_eligible BOOLEAN NOT NULL DEFAULT false;

-- The existing unique index only prevented two bookings sharing the exact same slot_start,
-- which was sufficient while every booking was a fixed 60 minutes on one shared grid. With a
-- 90-minute booking able to start at a time that only partially overlaps a 60-minute booking,
-- slot_start equality is no longer sufficient — replace it with a true overlap-exclusion
-- constraint over each booking's actual [slot_start, slot_start + duration) window.
--
-- A GiST exclusion constraint's index expression must be IMMUTABLE, but timestamptz + interval
-- is only STABLE in PostgreSQL — so slot_end must be a real stored column (computed once here,
-- and by the application on every future insert), not computed inline in the constraint.
ALTER TABLE bookings
    ADD COLUMN slot_end TIMESTAMPTZ;

UPDATE bookings
    SET slot_end = slot_start + (duration_minutes * INTERVAL '1 minute');

ALTER TABLE bookings
    ALTER COLUMN slot_end SET NOT NULL;

DROP INDEX bookings_active_slot_uniq;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlap
    EXCLUDE USING gist (
        tstzrange(slot_start, slot_end, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED');
```

`extended_class_eligible` defaults `false` for all existing rows (correct — nobody has been granted it yet), no cleanup needed. `slot_end` **does** need the one-time backfill `UPDATE` above (existing rows have no value to derive it from otherwise); going forward, `BookingRepository.insert()` sets it explicitly from `Booking.getSlotEnd()`, so it never depends on the STABLE `+` operator at write time either.

---

## Entity: `User` (existing, `users` table — one new column)

| Column | Type | Notes |
|--------|------|-------|
| `extended_class_eligible` | BOOLEAN NOT NULL DEFAULT `false` | Teacher-granted permission to book a 90-minute class. Independent of `role` — a `STUDENT` can hold `role = 'STUDENT'` without this flag (the default and common case); a `USER` could technically hold it too, but it has no effect until they're also promoted to `STUDENT` (booking creation already requires `hasAnyRole("STUDENT","ADMIN")`). |

**State transitions**:

```
false --(teacher grants extended-class eligibility)--> true
true  --(teacher revokes extended-class eligibility)--> false
```

- Transitions are idempotent at the repository layer, mirroring `promoteToStudentById`/`revokeStudentById`: granting to an already-eligible student, or revoking an already-ineligible one, is a no-op that still returns success.
- Revoking does **not** retroactively affect bookings already created while eligible (FR-013) — `bookings.duration_minutes` on existing rows is untouched by this flag; only future `POST /bookings` calls are gated by its current value.

**Java model** (`back-end/src/main/java/com/kuky/backend/auth/model/User.java`): add one field + accessor pair, following the existing `timezoneIsManual` boolean pattern.

```java
private boolean extendedClassEligible;
// ...
public boolean isExtendedClassEligible() { return extendedClassEligible; }
public void setExtendedClassEligible(boolean extendedClassEligible) { this.extendedClassEligible = extendedClassEligible; }
```

`UserRepository`'s `USER_MAPPER` needs `.setExtendedClassEligible(rs.getBoolean("extended_class_eligible"))` alongside its existing column mappings.

## New repository methods (`UserRepository`, alongside `promoteToStudentById`/`revokeStudentById`)

| Method | Query | Purpose |
|--------|-------|---------|
| `grantExtendedClassById(UUID id)` | `UPDATE users SET extended_class_eligible = true, updated_at = NOW() WHERE id = :id AND extended_class_eligible = false` | Rows-affected return value; `0` means already eligible — caller treats as idempotent success. |
| `revokeExtendedClassById(UUID id)` | `UPDATE users SET extended_class_eligible = false, updated_at = NOW() WHERE id = :id AND extended_class_eligible = true` | Same idempotent handling, mirrors `revokeStudentById`. |

Both follow the exact shape of `promoteToStudentById`/`revokeStudentById` (`back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java:73-84`).

## New DTO: `ExtendedClassEligibilityResponse` (new, `admin/dto`)

Mirrors `UserRoleResponse`'s shape.

```java
public record ExtendedClassEligibilityResponse(UUID id, boolean extendedClassEligible) {}
```

## Changed DTO: `UserResponse` (`auth/dto`)

Add `extendedClassEligible` (boolean) so the frontend can decide whether to offer the 90-minute option without a separate lookup — mirrors how `role` is already exposed for the `STUDENT`/`ADMIN` booking gate.

## Entity: `Booking` (existing, `bookings` table — one new column, changed invariants)

| Column | Type | Notes |
|--------|------|-------|
| `duration_minutes` | INT NOT NULL DEFAULT 60 | **Unchanged column, changed meaning**: previously always `60` in practice (fed from the hardcoded `SchedulingProperties.classDurationMinutes`); now carries whichever of the two configured durations (`60` or `90`) the student requested and was eligible/available for. `Booking.getSlotEnd()` (`slotStart.plusSeconds(durationMinutes * 60)`) still derives correctly from it — the in-memory Java getter is unchanged. |
| `slot_end` | TIMESTAMPTZ NOT NULL (new) | Persisted copy of `slot_start + duration_minutes`, set by `BookingRepository.insert()` from `Booking.getSlotEnd()`. Exists purely so `bookings_no_overlap` has two plain stored columns to build an IMMUTABLE `tstzrange()` from — not read anywhere else in the app (in-memory code keeps using the `getSlotEnd()` getter). |

**New invariant**: no two rows with `status = 'CONFIRMED'` may have overlapping `[slot_start, slot_start + duration_minutes)` ranges — enforced by `bookings_no_overlap` (see migration above), backed by an equivalent application-level check in `AvailabilityService` before a candidate start time is ever offered or accepted (defense in depth against the race the old unique index existed to close, per research.md Decision 3).

**Exception handling note**: `BookingService.createBooking`'s existing `catch (DuplicateKeyException e)` must widen to `catch (DataIntegrityViolationException e)` — the exclusion constraint's SQLState (`23P01`) is not a `DuplicateKeyException` (`23505`), but both are `DataIntegrityViolationException` subtypes, and both should still surface as `409 SLOT_UNAVAILABLE` to the caller.

## New/changed request-response shapes (`scheduling` package)

| Type | Change |
|------|--------|
| `CreateBookingRequest` | ADD `@NotNull Integer durationMinutes` field, alongside existing `slotStart`. |
| `GET /schedule` query params | ADD optional `durationMinutes` (default `60`). |
| `BookingNotAllowedException.Reason` | ADD `INVALID_DURATION` (requested duration isn't one of the two configured values), `NOT_ELIGIBLE_FOR_EXTENDED` (90 requested by a student without `extendedClassEligible`). |

See [contracts/booking-duration-api.md](contracts/booking-duration-api.md) for the full request/response shapes and error-code mapping.

## No changes to

- `Booking` table schema beyond the constraint swap above — `duration_minutes` already existed and was already persisted correctly; this feature only stops overwriting it with a hardcoded `60`.
- `AdminBookingDto` / `BookingsTab.tsx` — already carry and render `slotStart`/`slotEnd`; the teacher's schedule view is duration-aware today (research.md Decision 5).
- `bookings`, `purchases`, `homework_targets` and every other `user_id`-keyed table — unaffected by the new `extended_class_eligible` column, exactly as the `STUDENT`/`USER` role transition left them unaffected (feature 012 precedent).
