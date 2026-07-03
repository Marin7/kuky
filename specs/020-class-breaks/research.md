# Phase 0 Research: Class Booking Buffer Time

No `NEEDS CLARIFICATION` markers remain in the Technical Context — this feature reuses the existing stack end-to-end. The decisions below resolve the open *design* questions surfaced while reading the current scheduling implementation (`AvailabilityService.java`, `BookingService.java`, `BookingRepository.java`).

## Decision 1: Where does the buffer live — new column, new table, or a runtime constant?

**Decision**: A single `bufferMinutes = 15` field on `SchedulingProperties.Scheduling`, exactly like `classDurationMinutes`, `extendedClassDurationMinutes`, `minLeadHours`, and `cancelCutoffHours` already are. No database column, no migration.

**Rationale**: The spec's own Assumptions rule out per-teacher/per-student/per-class-type configurability, so there is nothing to persist per row — the buffer is a single global constant, the same shape as the other four scheduling constants already sourced this way. Every existing constant of this shape lives in `SchedulingProperties`, not the database, so this is the established pattern, not a new one.

**Alternatives considered**:
- *A `buffer_minutes` column on `bookings`* (per-booking override). Rejected: nothing in the spec asks for per-booking variation, and it would need a migration + backfill for zero present benefit (YAGNI).
- *A hardcoded literal `15` inline in `AvailabilityService`*. Rejected: every other scheduling constant in this codebase is externalized to `SchedulingProperties` (even ones that, like this one, aren't exposed in any admin UI), so a bare literal would be the inconsistent choice, not the simpler one.

## Decision 2: How does the buffer plug into the existing overlap check?

**Decision**: `AvailabilityService`'s private `overlapsAny(start, end, intervals)` gains a fourth parameter, `bufferMinutes`, and widens the comparison window from `[start, end)` to `[start - bufferMinutes, end + bufferMinutes)` before testing for intersection against each booked interval's real `[slotStart, slotStart + duration)`. Two call sites, two buffer values:

1. **`generateSchedule()`** — keeps its existing call with `bufferMinutes = 0` to decide `Slot.Status.BOOKED` (a slot the student is looking at genuinely is that class), then adds a **second** call with `bufferMinutes = props.getScheduling().getBufferMinutes()`; if that one matches but the first didn't, the slot is `Slot.Status.UNAVAILABLE` instead of `OPEN` — reusing the status the frontend already renders as a plain disabled slot with no "booked" label (`SlotGrid.tsx:64-66`), which is the correct appearance since nothing is actually booked at that time.
2. **`validateBookable()`** — its single overlap check is replaced outright with the buffer-widened version (`bufferMinutes = props.getScheduling().getBufferMinutes()`). Because the buffered window strictly contains the exact window, this one check catches both a true overlap and a too-close neighbor, so the existing `SlotUnavailableException("Esta hora ya ha sido reservada.")` / `409 SLOT_UNAVAILABLE` path already covers both cases — no new exception reason, no new error code.

**Rationale**: `overlapsAny` is a pure interval-intersection test; buffering is just widening one side's bounds before the same `<`/`>` comparison — the smallest possible change to an already-correct function, and it keeps the "genuinely booked" vs. "just needs spacing" distinction visible in the two different call sites rather than conflating them into one meaning.

**Alternatives considered**:
- *Introduce a third `Slot.Status` value (e.g. `BUFFERED`)* so the frontend could style it differently from a past/lead-time-blocked slot. Rejected: the spec doesn't ask for the buffer to be visually distinguished from any other reason a slot isn't offered (Assumptions: "not a purchasable or separately visible line item"), and it would require a frontend change this feature otherwise doesn't need — reintroducing complexity the constitution's Simplicity First principle forbids without a concrete present need.
- *Pre-filter booked intervals once, expanding each by the buffer, and reuse a single buffered list everywhere.* Rejected: `generateSchedule()` needs both the exact and the buffered classification in the same loop to distinguish `BOOKED` from `UNAVAILABLE` — a single pre-expanded list would lose the exact-match information the first branch needs.

## Decision 3: Does the database-level `bookings_no_overlap` exclusion constraint need widening to the buffer too?

**Decision**: No — leave `bookings_no_overlap` (added in `V29__extended_booking_duration.sql`) exactly as it is: an exact-overlap-only `EXCLUDE USING gist (tstzrange(slot_start, slot_end, '[)') WITH &&)`. The buffer is enforced only at the application layer (`validateBookable()`, called from `BookingService.createBooking()` before every insert).

**Rationale**: The existing DB constraint exists as a defense-in-depth backstop specifically against a *true* double-booking race (two requests for the same or overlapping instant landing concurrently) — a scenario realistic enough on this single-teacher site to have already justified a schema change once (feature 019). A race that produces two *non-overlapping* bookings merely closer than 15 minutes apart is a strictly narrower, lower-consequence failure mode (the teacher gets a shorter break, not a double-booked class), and widening the constraint would require two new stored "buffered" range columns (the same `timestamptz ± interval` IMMUTABLE problem that forced `slot_end` to become a real column in V29 applies here too — `slot_start - INTERVAL '15 minutes'` is no more IMMUTABLE than `slot_start + duration_minutes` was). That's real schema complexity for a race window that is vanishingly unlikely at this site's booking volume (low tens of students, one teacher, no bulk/programmatic booking client).

**Alternatives considered**:
- *Add `buffered_start`/`buffered_end` stored columns and widen the GiST exclusion to them.* Rejected for now under Simplicity First — no evidence of the race actually occurring, and it can be added later without touching any other part of this feature if it ever does (the application-layer check would be unaffected).
- *Wrap `createBooking()` in a serializable transaction / advisory lock instead of relying on the constraint.* Rejected: more moving parts than the problem currently warrants; the existing `DataIntegrityViolationException` catch already exists for the true-overlap case and needs no change.

## Decision 4: How is FR-008 (no buffer at availability-window boundaries) satisfied?

**Decision**: No special-case code. The buffer check only ever compares a candidate slot against *other confirmed bookings*; a day's opening/closing time is never represented as a `BookedInterval`, so it can never trigger the buffer. The first slot of a window can already start exactly when the window opens today (unchanged), and will continue to do so unless another booking's buffer happens to fall there.

**Rationale**: Confirmed by reading `generateSchedule()` — window boundaries come from `week_availability` (via `DayWindow`/`Interval`), a completely separate data path from `bookedIntervals`. There is nothing to turn "off" for FR-008; it was already the natural behavior of not modeling the window edge as a booking.

## Decision 5: How is FR-009 (no retroactive effect on pre-existing close bookings) satisfied?

**Decision**: No special-case code, no "rollout date" flag, no migration touching existing rows. The buffer check runs only when (a) generating the schedule shown to students and (b) validating a *new* booking attempt — it never scans existing `bookings` rows to flag, cancel, or modify them. Two old bookings that already sit 5 minutes apart simply remain in the table unchanged; the only thing the buffer prevents is a *third*, newly requested booking from packing into that already-tight gap or either old booking's outer edge.

**Rationale**: Confirmed by reading `BookingService` — nothing in the create/cancel/list flow ever re-validates a *previously confirmed* booking against current rules (the closest analogue, `findConfirmedBookingsOutsideAvailability()`, exists for a different, already-shipped concern — availability template edits shrinking a window — and this feature does not need an equivalent, since the buffer rule was never true "availability" in that sense). Grandfathering falls out of the design for free.
