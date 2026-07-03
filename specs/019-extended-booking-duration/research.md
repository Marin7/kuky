# Phase 0 Research: Extended Booking Duration

No `NEEDS CLARIFICATION` markers remain in the Technical Context — this feature reuses the existing stack end-to-end. The decisions below resolve the open *design* questions (not stack unknowns) surfaced while reading the current scheduling implementation.

## Decision 1: How does a client select 1-hour vs. 1.5-hour?

**Decision**: `GET /api/v1/schedule` gains an optional `durationMinutes` query parameter (`60` default, `90` the only other accepted value); `POST /api/v1/bookings` gains a required `durationMinutes` field on `CreateBookingRequest`. Both are validated against the same fixed set sourced from `SchedulingProperties` (`classDurationMinutes` = 60, new `extendedClassDurationMinutes` = 90).

**Rationale**: The existing `/schedule` endpoint already returns a flat list of `Slot`s generated at one fixed grid size (`AvailabilityService.generateSchedule()`); duration is a property of *which grid* to generate, not a property of an individual slot in a mixed response. Asking for one duration at a time keeps `AvailabilityService`'s existing single-duration chunking loop intact (Simplicity First) — no need to compute and merge two interleaved grids server-side per request.

**Alternatives considered**:
- *Return both grids in one `/schedule` response* (e.g. `slots60`/`slots90`). Rejected: doubles the payload and the horizon-materialization work on every page load, for a value (the 90-minute grid) most anonymous/non-eligible visitors can never book anyway.
- *A `durationMinutes` field per `Slot` computed server-side by scanning for the longest fit*. Rejected: overengineered for two fixed values; the UI need is binary (show 1h list or show 1.5h list), not "what's the max possible duration here."

## Decision 2: How is "extended class eligible" modeled and granted?

**Decision**: A single `extended_class_eligible BOOLEAN NOT NULL DEFAULT false` column on `users`, granted/revoked one student at a time via `POST`/`DELETE /api/v1/admin/users/{id}/extended-class` — copying `StudentAdminController`'s existing `POST`/`DELETE /api/v1/admin/users/{id}/student` (`promoteToStudentById`/`revokeStudentById` in `UserRepository`) field-for-field: same idempotent conditional-`UPDATE` pattern, same "404 if missing or ADMIN" guard, same best-effort notification email pattern.

**Rationale**: The `STUDENT`/`USER` role toggle (feature 012) is the exact same shape of decision — a teacher-controlled boolean-ish flag on a user, changed one at a time from the admin panel, non-blocking email notification. Reusing the pattern verbatim means no new architectural concept enters the codebase for what is, functionally, a second independent flag on the same row.

**Alternatives considered**:
- *A generic `user_permissions` join table* for future-proofing against more flags later. Rejected by Simplicity First / YAGNI — no second permission flag exists today to justify the abstraction; `timezone_is_manual` (V27) already established the precedent of adding single-purpose boolean columns directly to `users` when needed.
- *Self-service student toggle*. Rejected per spec clarification — the teacher grants it after an off-platform agreement; a self-service toggle would let any student flip it regardless of that agreement.

## Decision 3: How is overlap correctness maintained once durations differ?

**Decision**: Two layers, both required:
1. **Application layer** — `AvailabilityService` stops treating "is this start time free" as "is this exact instant in the set of booked start-instants" (`bookedStarts: Set<Instant>`, today's `generateSchedule()`). It instead fetches confirmed bookings as `(slotStart, durationMinutes)` intervals for the horizon and marks a candidate `[start, start+durationMinutes)` window BOOKED if it overlaps *any* existing interval, not just an exact-start match. `validateBookable(slotStart, durationMinutes)` performs the same interval-overlap check before allowing a booking to be created.
2. **Database layer** — replace `bookings_active_slot_uniq` (`UNIQUE INDEX ON bookings (slot_start) WHERE status = 'CONFIRMED'`) with `EXCLUDE USING gist (tstzrange(slot_start, slot_start + (duration_minutes || ' minutes')::interval, '[)') WITH &&) WHERE (status = 'CONFIRMED')`.

**Rationale**: This is the one place the existing implementation silently assumed a single global duration. With every booking at 60 minutes, "same `slot_start`" and "overlaps" are equivalent, so the unique index alone was sufficient and cheap. Once a 90-minute booking can start 30 minutes before or after a 60-minute booking's start, two *different* `slot_start` values can still describe overlapping time — the unique index would let that through. `tstzrange` + `EXCLUDE USING gist` is PostgreSQL's standard, well-supported mechanism for exactly this "no two rows may occupy overlapping ranges" constraint, and needs no extension (`gist` supports `tstzrange`'s `&&` operator natively — `btree_gist` is only needed when an exclusion constraint mixes a range column with a *scalar equality* column, which this one doesn't).

**Alternatives considered**:
- *Rely on the application-layer check alone.* Rejected: the existing code already shows why this codebase wants a DB-level backstop — the current `DuplicateKeyException` catch in `BookingService.createBooking` exists specifically to convert a race-condition double-insert into a clean `409 SLOT_UNAVAILABLE` rather than a 500. Removing that backstop for the overlap case would reintroduce exactly the race the unique index was preventing.
- *Row-level locking (`SELECT ... FOR UPDATE` over a computed range) instead of a constraint.* Rejected: more code, and Postgres's native exclusion constraint already gives the same guarantee declaratively.

**Implementation note carried into data-model.md**: `BookingService.createBooking`'s existing `catch (DuplicateKeyException e)` must be broadened to `DataIntegrityViolationException` (the common superclass covering both unique-violation and exclusion-violation SQLStates), so a database-level overlap rejection still surfaces as the same `409 SLOT_UNAVAILABLE` the frontend already handles — not an unhandled 500.

**Correction found during implementation**: the originally planned expression `tstzrange(slot_start, slot_start + (duration_minutes * INTERVAL '1 minute'), '[)')` inline inside the `EXCLUDE` constraint does **not** work — PostgreSQL rejected it with `functions in index expression must be marked IMMUTABLE`, because `timestamptz + interval` is only STABLE (interval arithmetic can be timezone-dependent), regardless of how the interval value itself is constructed. GiST exclusion constraints require a provably IMMUTABLE index expression. The fix: `bookings` gains a real stored `slot_end TIMESTAMPTZ NOT NULL` column, computed once by the application (`BookingRepository.insert`) or by the migration's one-time backfill `UPDATE`, and the constraint references the two plain columns directly — `tstzrange(slot_start, slot_end, '[)')`, which is immutable since it performs no arithmetic. See the updated `data-model.md` and `V29__extended_booking_duration.sql`.

## Decision 4: Does the Zoom meeting / confirmation email need separate work?

**Decision**: No. `ZoomMeetingProvider.create(Instant start, int durationMinutes, String topic)` already accepts and forwards a duration to the Zoom API; `BookingService.createBooking` already reads `duration` into one local variable used for the Zoom call, the confirmation email, and the persisted row. Passing the *requested* duration into that same variable (instead of always reading it from `props.getScheduling().getClassDurationMinutes()`) is sufficient — no signature or provider change needed.

**Rationale**: Confirmed by reading `ZoomMeetingProvider.java` and `BookingService.java` directly — this was speculative risk going in, resolved by inspection rather than assumption.

## Decision 5: Do "My Bookings" and the admin schedule need new fields to show duration?

**Decision**: No new fields, and the admin side needs no frontend change at all. `BookingResponse` and `BookingSummary` already carry `slotEnd` (`Booking.getSlotEnd()` is already computed as `slotStart.plusSeconds(durationMinutes * 60)`, and has been since the `duration_minutes` column was added in `V3__create_bookings.sql`). `AdminBookingDto` already carries `slotEnd` too, and `BookingsTab.tsx:100` already renders `formatSlot(b.slotStart, b.slotEnd, teacherTimezone)` — **User Story 4 (teacher sees class length in her schedule) is already fully satisfied today**, as soon as bookings start carrying a real (non-hardcoded) duration. Only the student-facing side needs a display change: `MyBookings.tsx`'s `formatSlot` currently formats `booking.slotStart` alone and needs to render the `slotStart`–`slotEnd` range instead; `BookingDialog.tsx`'s confirmation copy needs the same treatment.

**Rationale**: Confirmed via `Booking.java:getSlotEnd()`, `AdminBookingDto.java`, `BookingsTab.tsx:100`, and the front-end `BookingResponse`/`BookingSummary` TypeScript interfaces in `front-end/src/lib/scheduling.ts` — the data has been flowing through since the booking table was created; only the 60-minutes-everywhere assumption prevented it from mattering, and the teacher's own view was already written duration-aware.
