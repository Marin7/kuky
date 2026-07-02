# Contract: Student Progress API

Base path `/api/v1`. Conventions match the existing API: JSON, HTTP-only JWT cookie auth (`credentials: 'include'`), error bodies `{"error":"CODE","message":"..."}`. Both endpoints below fall under the existing `/api/v1/admin/**` matcher (`hasRole("ADMIN")` — no new `SecurityConfig` rule needed).

---

## GET `/admin/students/{id}/profile` (existing endpoint, extended response)

No change to the request. The response gains a `progress` field.

`200 OK`
```json
{
  "id": "uuid",
  "email": "student@example.com",
  "firstName": "Ana",
  "lastName": "Popescu",
  "username": "ana.p",
  "avatarImageId": null,
  "createdAt": "2026-05-01T10:00:00Z",
  "bookings": [
    {
      "id": "uuid",
      "slotStart": "2026-06-20T17:00:00Z",
      "slotEnd": "2026-06-20T17:50:00Z",
      "status": "CONFIRMED",
      "zoomJoinUrl": "https://...",
      "noShow": false
    }
  ],
  "homeworks": [
    { "id": "uuid", "title": "Ejercicio de subjuntivo", "status": "GRADED", "submittedAt": "2026-06-18T09:00:00Z" }
  ],
  "presentations": [
    { "id": "uuid", "title": "Verbos irregulares", "level": "B1" }
  ],
  "progress": {
    "units": [
      {
        "unitId": "uuid",
        "subject": "Gramática B1",
        "level": "B1",
        "totalHomeworks": 3,
        "completedHomeworks": 2,
        "complete": false
      }
    ],
    "homeworkBreakdown": { "pending": 1, "submitted": 1, "completed": 3 },
    "attendedClasses": 6
  }
}
```

Errors: unchanged from the existing endpoint (`404` if the student doesn't exist).

`noShow` on each booking is new; `progress` is new. Every other field is unchanged.

---

## PUT `/admin/bookings/{id}/no-show`

Marks (or unmarks) a specific past confirmed booking as a no-show, excluding (or re-including) it from the student's `attendedClasses` count (FR-010, FR-011).

Request:
```json
{ "noShow": true }
```

`204 No Content` — no response body; the frontend re-fetches the student profile to reflect the updated count and per-booking flag.

Errors:
- `404 BOOKING_NOT_FOUND` — no booking with that id.
- `422 BOOKING_NOT_ELIGIBLE_FOR_NO_SHOW` — the booking is not `CONFIRMED`, or its `slotStart` is not in the past (FR-012). Applies both when marking (`noShow: true`) and unmarking (`noShow: false`) — a booking has to be past-and-confirmed either way for the flag to be meaningful.
- `403 ACCESS_DENIED` — caller is not `ADMIN` (existing `/api/v1/admin/**` matcher).

---

## Security matcher additions (`SecurityConfig`)

None. Both endpoints are already covered by the existing `/api/v1/admin/**` → `hasRole("ADMIN")` matcher.

## Frontend contract notes

- `front-end/src/lib/admin.ts` (extended):
  - `StudentProfileBooking` gains `noShow: boolean`.
  - New `StudentProgress`, `UnitProgress`, `HomeworkBreakdown` interfaces; `StudentProfile` gains `progress: StudentProgress`.
  - New `setBookingNoShow(bookingId: string, noShow: boolean): Promise<void>` — `PUT /admin/bookings/${bookingId}/no-show`, same `apiCall<T>` wrapper shape as `cancelBooking`.
- On a `422 BOOKING_NOT_ELIGIBLE_FOR_NO_SHOW` (e.g., a race where the class hasn't started yet by server clock), the toggle surfaces the existing generic admin error-toast pattern — no new UI copy beyond the server's `message`.
