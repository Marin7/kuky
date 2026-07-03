# Phase 1 Data Model: Class Booking Buffer Time

**No schema changes.** This feature introduces no new table, column, or migration — see research.md Decisions 1 and 3 for why the buffer is a runtime constant and comparison widening, not persisted state.

---

## Changed configuration: `SchedulingProperties.Scheduling`

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `bufferMinutes` | `int` | `15` | New. Minimum gap, in minutes, required between the end of one confirmed booking and the start of another (in either direction). Sourced and overridden the same way as `classDurationMinutes`/`minLeadHours` (`application.yml` / `app.scheduling.buffer-minutes`), though no admin UI exposes it — matches the spec's Assumption that it isn't configurable per teacher/student in this feature. |

```java
private int bufferMinutes = 15;

public int getBufferMinutes() { return bufferMinutes; }
public void setBufferMinutes(int bufferMinutes) { this.bufferMinutes = bufferMinutes; }
```

## Changed behavior: `AvailabilityService`

No new fields or types — the existing private `overlapsAny(Instant start, Instant end, List<BookedInterval> intervals)` gains a fourth parameter:

```java
private static boolean overlapsAny(Instant start, Instant end,
                                    List<BookingRepository.BookedInterval> intervals,
                                    int bufferMinutes) {
    long bufferSeconds = bufferMinutes * 60L;
    Instant bStart = start.minusSeconds(bufferSeconds);
    Instant bEnd = end.plusSeconds(bufferSeconds);
    for (BookingRepository.BookedInterval interval : intervals) {
        Instant bookedEnd = interval.slotStart().plusSeconds((long) interval.durationMinutes() * 60);
        if (bStart.isBefore(bookedEnd) && interval.slotStart().isBefore(bEnd)) {
            return true;
        }
    }
    return false;
}
```

Existing call sites become:

| Call site | Before | After |
|-----------|--------|-------|
| `generateSchedule()` — decide `Slot.Status.BOOKED` | `overlapsAny(start, end, bookedIntervals)` | `overlapsAny(start, end, bookedIntervals, 0)` (unchanged meaning, exact overlap only) |
| `generateSchedule()` — decide `Slot.Status.UNAVAILABLE` (new branch) | *(none)* | `overlapsAny(start, end, bookedIntervals, props.getScheduling().getBufferMinutes())` — only reached when the exact-overlap check above was `false`; slot becomes `UNAVAILABLE` instead of falling through to the lead-time check |
| `validateBookable()` — reject a booking attempt | `overlapsAny(slotStart, slotEnd, nearby)` (queried via `findConfirmedBookingIntervalsBetween(slotStart, slotEnd)`) | `overlapsAny(slotStart, slotEnd, nearby, props.getScheduling().getBufferMinutes())`, with `nearby` now queried via `findConfirmedBookingIntervalsBetween(slotStart.minusSeconds(bufferSeconds), slotEnd.plusSeconds(bufferSeconds))` so bookings just outside `[slotStart, slotEnd)` but inside the buffer zone are actually fetched from the database |

**State/entity note**: `BookingRepository.BookedInterval` (`record BookedInterval(Instant slotStart, int durationMinutes)`) is unchanged — it already carries everything the buffer math needs.

## No changes to

- `bookings` table schema — `slot_start`, `slot_end`, `duration_minutes` already fully describe each booking's real (unbuffered) window.
- `bookings_no_overlap` exclusion constraint — remains exact-overlap only (research.md Decision 3).
- `Slot` model / `Slot.Status` enum — reuses the existing `UNAVAILABLE` value; no new status.
- `BookingNotAllowedException.Reason` / error codes — the buffer rejection at booking time surfaces through the existing `SlotUnavailableException` → `409 SLOT_UNAVAILABLE`, identical to today's overlap rejection.
- Any frontend type, component, or service module — `Slot`'s shape and the set of statuses it can carry are unchanged; `SlotGrid.tsx` already renders `UNAVAILABLE` correctly.
