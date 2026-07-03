# Contract: Companion Student on a Shared Booking

Base path `/api/v1`. Conventions match the existing API: JSON, HTTP-only JWT cookie auth (`credentials: 'include'`), current user via `@AuthenticationPrincipal String email`, error bodies `{"error":"CODE","message":"..."}`.

A booking has at most one "companion student" in addition to its booking student (`user_id`). Attach/detach is teacher-only; cancellation is open to either student (unchanged access otherwise).

---

## POST `/admin/bookings/{id}/companion-student` *(new — `ADMIN` only)*

Attaches a companion student to an existing, upcoming, confirmed booking.

```json
{ "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6" }
```

`200 OK` → `AdminBookingDto`, now including the companion student's fields:

```json
{
  "id": "…", "studentId": "…", "studentEmail": "ana@example.com", "studentFirstName": "Ana", "studentLastName": "Popescu", "studentUsername": "ana",
  "slotStart": "2026-07-10T09:00:00Z", "slotEnd": "2026-07-10T10:00:00Z", "zoomJoinUrl": "…",
  "companionStudentId": "…", "companionStudentEmail": "mihai@example.com", "companionStudentFirstName": "Mihai", "companionStudentLastName": "Ionescu", "companionStudentUsername": "mihai", "companionStudentNoShow": null
}
```

Sends the companion student a class-details email (date, time, join link — same information the booking student received at booking time), independent of whether the booking student is notified (FR-015: the booking student is **not** actively notified).

Errors:
- `404 BOOKING_NOT_FOUND` — no such booking.
- `404 USER_NOT_FOUND` — no such student.
- `422 COMPANION_NOT_STUDENT` — target user doesn't hold the `STUDENT` role.
- `409 COMPANION_SAME_AS_BOOKING_STUDENT` — target is already the booking student on this booking.
- `409 COMPANION_ALREADY_ATTACHED` — this booking already has a companion student; detach first.
- `409 BOOKING_NOT_ATTACHABLE` — booking is `CANCELLED` or its `slotStart` has already passed. A dedicated reason (not a reuse of the cancellation-cutoff `ALREADY_CANCELLED` code) since "this class already happened" and "this class was cancelled" are different facts a caller shouldn't have to disambiguate from one error code.
- `403 EXTENDED_CLASS_NOT_ELIGIBLE` — booking is the 90-minute duration and the target student lacks `extendedClassEligible` (reuses the existing reason/code from the extended-duration feature; message text distinguishes "the companion student" from "you").

## DELETE `/admin/bookings/{id}/companion-student` *(new — `ADMIN` only)*

Detaches the companion student, reverting the booking to a normal single-student booking. Does not affect `status`, `user_id`, or the booking student's booking history (FR-006, FR-012).

`204 No Content`

Errors:
- `404 BOOKING_NOT_FOUND` — no such booking.
- `404 COMPANION_NOT_ATTACHED` — this booking has no companion student to remove.

## GET `/admin/bookings` *(existing — unchanged shape aside from the new fields above)*

`AdminBookingDto` entries now include the nullable `companionStudent*` fields shown above (`null` when no companion student is attached), so the teacher can see at a glance which classes are shared (SC-003).

## PUT `/admin/bookings/{id}/no-show` *(existing — request body extended)*

```json
{ "noShow": true, "studentRole": "COMPANION" }
```

`studentRole` is optional, one of `"BOOKING_STUDENT"` (default, backward-compatible with the current single-field body) or `"COMPANION"`. Sets `no_show` or `second_student_no_show` accordingly (FR-010).

`204 No Content`

New error:
- `404 COMPANION_NOT_ATTACHED` — `studentRole: "COMPANION"` but the booking has no companion student (same code/status as the detach endpoint's "nothing to remove" case).

## GET `/bookings`, DELETE `/bookings/{id}` *(existing — access widened, shape extended)*

`GET /bookings` now also returns bookings where the caller is the companion student, not only the booking student. Each `BookingSummary` gains a boolean `isCompanionStudent` so the student's own "My Bookings" view can distinguish "your booking" from "you're joining someone else's class."

`DELETE /bookings/{id}` now succeeds for either the booking student or the companion student on a shared booking (FR-013) — both have equal standing to cancel it. Cancelling removes the whole class for both students — unchanged cancellation semantics, just two eligible callers. Both students (and the teacher) receive a cancellation email regardless of who initiated it (FR-011). Existing errors (`CANCELLATION_TOO_LATE`, `ALREADY_CANCELLED`, `BOOKING_NOT_FOUND`) are unchanged in meaning.
