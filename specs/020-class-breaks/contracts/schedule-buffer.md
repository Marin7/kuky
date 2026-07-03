# Contract: Class Booking Buffer Time

Base path `/api/v1`. No new endpoints, no changed request/response shapes — this feature only changes which existing status/outcome two already-documented endpoints return for a given input. Conventions match the existing API (`contracts/api.md`-style): JSON, HTTP-only JWT cookie auth (`credentials: 'include'`), error bodies `{"error":"CODE","message":"..."}`.

A fixed 15-minute buffer (`SchedulingProperties.Scheduling.bufferMinutes`) now applies between every pair of confirmed bookings, regardless of duration (60/90 min) or which student holds either booking.

---

## GET `/schedule` *(existing, public — unchanged request shape)*

`GET /schedule?durationMinutes=60` (or `90`)

`200 OK` → same `ScheduleResponse` shape as today (`{ slots: Slot[] }`, `Slot = { start, end, status }`). **Behavior change only**: a slot within 15 minutes of a confirmed booking's start or end, but not itself overlapping that booking, now returns `status: "UNAVAILABLE"` instead of `"OPEN"`. A slot that genuinely overlaps a confirmed booking still returns `status: "BOOKED"`, unchanged.

No new status value, no new query parameter, no new error.

## POST `/bookings` *(existing — `hasAnyRole("STUDENT","ADMIN")`, unchanged request/response shape)*

`{ "slotStart": "2026-07-10T09:00:00Z", "durationMinutes": 60 }` → `201 Created` → `BookingResponse` (unchanged shape), **or**:

`409 SLOT_UNAVAILABLE` — now also returned when the requested `[slotStart, slotStart + durationMinutes)` window is within 15 minutes of an existing confirmed booking's window, even if the two windows don't literally overlap. This is the same error code already returned for a true overlap; the client-side handling in `BookingDialog.tsx` (`SLOT_UNAVAILABLE: t("schedule.booking.slotUnavailableError")`) needs no change.

No new error code.

## GET `/bookings`, DELETE `/bookings/{id}` *(existing — unchanged)*

No contract change. Cancelling a booking (`DELETE /bookings/{id}`) has no special buffer-release step — the buffer is recomputed fresh from `bookings` on every subsequent `GET /schedule` / `POST /bookings` call, so a cancelled booking simply stops contributing to anyone's buffer check on the very next request (FR-006), with no separate "release" operation to invoke.

---

## Frontend contract notes

- `Slot` (`front-end/src/lib/scheduling.ts`) shape and `SlotStatus` union (`"OPEN" | "BOOKED" | "UNAVAILABLE"`) are unchanged — no new value.
- `SlotGrid.tsx` needs no change: it already renders any non-`OPEN`, non-`BOOKED` slot (i.e. `UNAVAILABLE`) as a plain disabled slot with no label, which is exactly the correct appearance for a buffer-blocked slot.
- `getErrorMessage` in `BookingDialog.tsx` needs no new entry — `SLOT_UNAVAILABLE` already has a copy string covering this case.
