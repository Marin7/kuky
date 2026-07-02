# Phase 1 Data Model: Calendar (.ics) Attachment for Bookings

No new persisted tables or columns. This feature reads the existing `Booking` entity and derives a transient, in-memory value object at email-send time.

## Booking (existing — `com.kuky.backend.scheduling.model.Booking`)

Fields already present and relevant to this feature (read-only for this feature; no changes):

| Field | Type | Used for |
|---|---|---|
| `id` | `UUID` | Stable per-booking iCalendar `UID` (`booking-<id>@kuky.es`) |
| `userId` | `UUID` | Resolves the student's email (via `UserRepository`) for the calendar attachment's recipient/description |
| `slotStart` | `Instant` | `DTSTART` (UTC) |
| `slotEnd` (derived from `slotStart` + `durationMinutes`) | `Instant` | `DTEND` (UTC) |
| `durationMinutes` | `int` | Used to compute `slotEnd` where not already available |
| `zoomJoinUrl` | `String` | `LOCATION`/`URL` and included in `DESCRIPTION` |
| `status` | `String` (`CONFIRMED`/`CANCELLED`) | Determines whether the generated event is a `REQUEST` (confirmation) or `CANCEL` (cancellation) |

## CalendarEvent (new, transient — not persisted)

Produced by `IcsEventFactory` from a `Booking` (plus the student/teacher email needed for the `ATTENDEE`/`ORGANIZER` lines) at the moment an email is sent. Never stored; regenerated on every send per FR-008.

| Field | Type | Notes |
|---|---|---|
| `uid` | `String` | `booking-<bookingId>@kuky.es` — stable across confirmation and cancellation for the same booking |
| `method` | enum: `REQUEST` \| `CANCEL` | `REQUEST` for booking confirmation / teacher notification; `CANCEL` for cancellation emails |
| `sequence` | `int` | `0` for the original `REQUEST`; `1` for a subsequent `CANCEL` of the same booking |
| `dtStart` | `Instant` | Booking's `slotStart`, emitted in UTC (`Z` suffix) |
| `dtEnd` | `Instant` | Booking's `slotStart` + `durationMinutes`, emitted in UTC |
| `summary` | `String` | e.g. `"Clase de español con Paula"` |
| `description` | `String` | Includes the Zoom join link and student identifier (for the teacher's copy) |
| `location` | `String` | Zoom join URL |
| `organizerEmail` | `String` | Teacher email (`app.scheduling.teacher-email`) |
| `attendeeEmail` | `String` | Student email |

### State transitions

```
(booking created)  → CalendarEvent{method=REQUEST, sequence=0}   attached to confirmation + teacher notification emails
(booking cancelled) → CalendarEvent{method=CANCEL,  sequence=1}   attached to cancellation emails (student + teacher), same uid
```

There is no "reschedule" transition — per the spec's resolved edge case, a time change is always a cancellation (sequence 1, `CANCEL`) followed by a brand-new booking (new `id`, therefore a new `uid`, `sequence=0`, `REQUEST`).

## Validation rules

- `dtEnd` MUST be strictly after `dtStart` (guaranteed by existing `durationMinutes > 0` invariant on `Booking`).
- `uid` MUST be identical between a booking's `REQUEST` and its later `CANCEL` (derived deterministically from `Booking.id`, so this holds by construction).
- ICS generation MUST NOT throw past its own boundary — any failure is caught within `IcsEventFactory`/`BookingEmailService` and results in the email being sent without an attachment (FR-007).
