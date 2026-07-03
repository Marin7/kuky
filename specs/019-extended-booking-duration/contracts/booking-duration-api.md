# Contract: Extended Booking Duration

Base path `/api/v1`. Conventions match the existing API: JSON, HTTP-only JWT cookie auth (`credentials: 'include'`), current user via `@AuthenticationPrincipal String email`, error bodies `{"error":"CODE","message":"..."}`.

Two durations exist system-wide, sourced from `SchedulingProperties`: **60** (standard, `classDurationMinutes`) and **90** (extended, `extendedClassDurationMinutes`). No other value is ever valid.

---

## GET `/schedule` *(existing, public — unchanged access)*

New optional query parameter: `durationMinutes` (`60` default if omitted, `90` the only other accepted value).

`GET /schedule?durationMinutes=90`

`200 OK` → same `ScheduleResponse` shape as today; `slots` are generated at the requested grid size, with `status: BOOKED` now determined by real interval overlap against every confirmed booking in the horizon (any duration), not just exact `slot_start` equality.

Errors: `422 INVALID_DURATION` if `durationMinutes` is present and not `60` or `90`.

No eligibility check here — browsing which 90-minute windows are theoretically open is not gated (equivalent information is already derivable from the 60-minute grid's consecutive-OPEN pattern). The frontend only *offers* the 90-minute toggle to eligible students; enforcement lives at booking-creation time.

## POST `/bookings` *(existing — `hasAnyRole("STUDENT","ADMIN")`, unchanged)*

`CreateBookingRequest` gains a required field:

```json
{ "slotStart": "2026-07-10T09:00:00Z", "durationMinutes": 90 }
```

`201 Created` → `BookingResponse` (unchanged shape — `durationMinutes` and `slotEnd` already exist on it).

New errors:
- `422 INVALID_DURATION` — `durationMinutes` isn't `60` or `90`.
- `403 EXTENDED_CLASS_NOT_ELIGIBLE` — `durationMinutes: 90` requested by a student whose `extendedClassEligible` is `false`. Enforced server-side regardless of what the UI showed (FR-012) — this is the authoritative gate, not just a UI hide.

Existing errors (`SLOT_UNAVAILABLE`, `SLOT_OUT_OF_RANGE`, `BOOKING_TOO_SOON`, `MEETING_PROVISIONING_FAILED`) are unchanged in meaning; `SLOT_UNAVAILABLE` now also covers "this 90-minute window overlaps an existing booking that doesn't share its exact start time" (previously only exact-start conflicts could occur).

## GET `/bookings`, DELETE `/bookings/{id}` *(existing — unchanged)*

`BookingSummary` already includes `slotEnd`; no contract change. (Frontend rendering of that field is a display-only change, not a contract change.)

---

## Admin endpoints (existing `/api/v1/admin/**` matcher — `ADMIN` role; non-admin → `403 ACCESS_DENIED`)

### POST `/admin/users/{id}/extended-class`

Grants extended-class eligibility: `extended_class_eligible` `false` → `true`. Idempotent — calling it on an already-eligible user succeeds without change. Sends a best-effort "you can now book 1.5-hour classes" email to the user (failure is logged, not surfaced — mirrors `sendStudentGrantedEmail`).

`200 OK` → `{ "id": "uuid", "extendedClassEligible": true }`

Errors: `404 USER_NOT_FOUND` (no such id, or the id belongs to an `ADMIN` account — same guard as the `/student` endpoint).

### DELETE `/admin/users/{id}/extended-class`

Revokes extended-class eligibility: `true` → `false`. Idempotent. Does **not** affect the duration of bookings the student already made while eligible (FR-013). Sends a best-effort "extended classes no longer available" email.

`200 OK` → `{ "id": "uuid", "extendedClassEligible": false }`

Errors: `404 USER_NOT_FOUND` (same guard).

### GET `/admin/students` *(existing, unchanged)*

`StudentResponse` may gain `extendedClassEligible` if the admin Students-tab UI needs to render current status inline without a second lookup — implementation detail decided in `tasks.md`, not a breaking contract change either way (additive field).

---

## Error code additions (`contracts/api.md`-style summary)

| Code | HTTP status | When |
|------|-------------|------|
| `INVALID_DURATION` | 422 | `durationMinutes` (query param or request body) isn't `60` or `90`. |
| `EXTENDED_CLASS_NOT_ELIGIBLE` | 403 | A 90-minute booking was requested by a student without `extendedClassEligible`. |

## Frontend contract notes

- `UserResponse.extendedClassEligible` (front-end/src/lib/auth.ts) — new boolean field, mirrors `role`'s existing role-gate pattern (`ScheduleView.tsx` already computes `canBook` from `user?.role`; it gains a sibling `canBookExtended` from this field).
- `Slot` (front-end/src/lib/scheduling.ts) shape is unchanged; which grid it represents is now a property of the `getSchedule(durationMinutes)` call that produced it, not of the slot itself.
- `getErrorMessage` maps in `BookingDialog.tsx` gain two entries: `EXTENDED_CLASS_NOT_ELIGIBLE` and `INVALID_DURATION`.
