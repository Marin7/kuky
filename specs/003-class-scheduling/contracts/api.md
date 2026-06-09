# API Contract: 1-on-1 Class Scheduling

Base URL (local): `http://localhost:8081/api/v1`
All responses are JSON. Auth uses the existing `auth-token` HTTP-only cookie (sent with `credentials: "include"`). Errors follow the established envelope: `{"error":"ERROR_CODE","message":"..."}` (see `GlobalExceptionHandler`).

Times are UTC ISO-8601 instants (e.g. `2026-06-15T08:00:00Z`). The frontend localizes them for display.

---

## Security (SecurityConfig change)

- `GET /api/v1/schedule` → **public** (add to `permitAll`, alongside `/api/v1/auth/**`).
- `POST/GET/DELETE /api/v1/bookings/**` → **authenticated** (covered by the existing `anyRequest().authenticated()`); unauthenticated calls receive `401`.

---

## 1. Get schedule (public) — FR-001, FR-002, FR-020

`GET /api/v1/schedule`

Returns all slots for the current + next calendar week in Paula's timezone, with status. No auth required.

**200 OK**
```json
{
  "teacherTimezone": "Europe/Madrid",
  "horizonStart": "2026-06-08T00:00:00Z",
  "horizonEnd": "2026-06-22T00:00:00Z",
  "slots": [
    { "start": "2026-06-15T07:00:00Z", "end": "2026-06-15T08:00:00Z", "status": "OPEN" },
    { "start": "2026-06-15T08:00:00Z", "end": "2026-06-15T09:00:00Z", "status": "BOOKED" },
    { "start": "2026-06-09T07:00:00Z", "end": "2026-06-09T08:00:00Z", "status": "UNAVAILABLE" }
  ]
}
```
- `status`: `OPEN` (bookable), `BOOKED` (taken), `UNAVAILABLE` (past or within the 24 h lead window).
- The response never reveals who booked a slot (privacy).

---

## 2. Create booking (auth) — FR-004..FR-011

`POST /api/v1/bookings`

**Request**
```json
{ "slotStart": "2026-06-15T07:00:00Z" }
```

**201 Created**
```json
{
  "id": "5f1c…",
  "slotStart": "2026-06-15T07:00:00Z",
  "slotEnd": "2026-06-15T08:00:00Z",
  "durationMinutes": 60,
  "status": "CONFIRMED",
  "zoomJoinUrl": "https://us05web.zoom.us/j/123456789",
  "createdAt": "2026-06-09T10:12:00Z"
}
```

**Error responses**
| HTTP | error | When |
|------|-------|------|
| 401 | (filter) | Not signed in |
| 409 | `SLOT_UNAVAILABLE` | Slot already booked (incl. concurrent race) or not a valid/open slot |
| 422 | `BOOKING_TOO_SOON` | Slot start is within the 24 h lead window (FR-021) |
| 422 | `SLOT_OUT_OF_RANGE` | Slot start is outside the bookable horizon or misaligned |
| 502 | `MEETING_PROVISIONING_FAILED` | Zoom meeting could not be created; booking not finalized, slot remains open (FR-011) |

Side effects on success: slot reserved exclusively (FR-006/007); Zoom meeting created (FR-008/009); confirmation emails to student + Paula (FR-010).

---

## 3. List my bookings (auth) — FR-012

`GET /api/v1/bookings`

Returns the authenticated student's bookings, split for convenience; "past" is derived from slot end vs now (no stored COMPLETED state).

**200 OK**
```json
{
  "upcoming": [
    {
      "id": "5f1c…",
      "slotStart": "2026-06-15T07:00:00Z",
      "slotEnd": "2026-06-15T08:00:00Z",
      "status": "CONFIRMED",
      "zoomJoinUrl": "https://us05web.zoom.us/j/123456789",
      "cancellable": true
    }
  ],
  "past": [
    {
      "id": "9a2b…",
      "slotStart": "2026-06-02T07:00:00Z",
      "slotEnd": "2026-06-02T08:00:00Z",
      "status": "CONFIRMED",
      "zoomJoinUrl": "https://us05web.zoom.us/j/987654321",
      "cancellable": false
    }
  ]
}
```
- `cancellable`: `true` only when `status = CONFIRMED` and start is ≥ 24 h away.
- Cancelled bookings MAY be included in `past` with `status: "CANCELLED"` or omitted; implementation choice (omitting is acceptable for MVP).

---

## 4. Cancel booking (auth) — FR-013, FR-014

`DELETE /api/v1/bookings/{id}`

**204 No Content** on success. Side effects: status → `CANCELLED`, `cancelled_at` set, slot reopens (FR-014), Zoom meeting deleted, Paula notified.

**Error responses**
| HTTP | error | When |
|------|-------|------|
| 401 | (filter) | Not signed in |
| 404 | `BOOKING_NOT_FOUND` | No such booking, or it does not belong to the caller |
| 409 | `ALREADY_CANCELLED` | Booking is already cancelled |
| 422 | `CANCELLATION_TOO_LATE` | Within the 24 h cancellation cutoff (FR-013) |

---

## Error code summary (added to GlobalExceptionHandler)

| Code | HTTP | Exception |
|------|------|-----------|
| `SLOT_UNAVAILABLE` | 409 | `SlotUnavailableException` (also thrown on partial-unique-index `DuplicateKeyException`) |
| `SLOT_OUT_OF_RANGE` | 422 | `BookingNotAllowedException` (reason: range/alignment) |
| `BOOKING_TOO_SOON` | 422 | `BookingNotAllowedException` (reason: lead time) |
| `CANCELLATION_TOO_LATE` | 422 | `BookingNotAllowedException` (reason: cutoff) |
| `MEETING_PROVISIONING_FAILED` | 502 | `MeetingProvisioningException` |
| `BOOKING_NOT_FOUND` | 404 | `BookingNotFoundException` |
| `ALREADY_CANCELLED` | 409 | `BookingNotAllowedException` (reason: state) |

Validation errors on the request body reuse the existing `VALIDATION_ERROR` (400) handler.
