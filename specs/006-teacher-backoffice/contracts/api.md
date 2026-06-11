# Phase 1 — API Contract: Teacher Backoffice — Control Panel

Feature: `006-teacher-backoffice` | Date: 2026-06-10 | Spec: [spec.md](../spec.md)

Conventions (existing): JSON over HTTP on `:8081`; auth via the `auth-token` HTTP-only
cookie (`credentials: include`); errors are `{ "error": "CODE", "message": "..." }`;
Spanish user-facing messages. All `/api/v1/admin/**` endpoints require an authenticated user
whose `role = ADMIN` — guests get **401**, authenticated non-admins get **403 ACCESS_DENIED**.

Dates of day are `HH:mm` strings (teacher timezone); calendar dates are `YYYY-MM-DD`;
instants are ISO-8601 UTC.

---

## Auth (modified)

### `GET /api/v1/auth/me` — now returns role
```json
200 → { "id": "uuid", "email": "paula@kuky.es", "role": "ADMIN" }
```
`role` is `"STUDENT"` or `"ADMIN"`. (login/register/reset responses likewise gain `role`.)

---

## Availability (admin)

### `GET /api/v1/admin/availability`
Returns the full weekly pattern and upcoming exceptions.
```json
200 → {
  "weekly": [
    { "id": "uuid", "dayOfWeek": 1, "startTime": "09:00", "endTime": "12:00" },
    { "id": "uuid", "dayOfWeek": 1, "startTime": "14:00", "endTime": "18:00" }
  ],
  "exceptions": [
    { "id": "uuid", "date": "2026-07-14", "kind": "BLOCK", "startTime": "00:00", "endTime": "23:59" },
    { "id": "uuid", "date": "2026-07-20", "kind": "OPEN",  "startTime": "19:00", "endTime": "20:00" }
  ]
}
```

### `PUT /api/v1/admin/availability/weekly`
Replaces the entire weekly pattern (full-set semantics).
```json
Request → { "windows": [ { "dayOfWeek": 1, "startTime": "09:00", "endTime": "12:00" }, ... ] }
200 → { "weekly": [ ...as GET... ],
        "bookingConflicts": [
          { "bookingId": "uuid", "studentEmail": "x@y.z", "slotStart": "2026-06-15T07:00:00Z" }
        ] }
```
`bookingConflicts` lists upcoming **confirmed** bookings that no longer fall inside the saved
availability (FR-010). Bookings are **not** modified; the field drives a non-blocking warning.
- `422 VALIDATION_ERROR` if any window has `endTime <= startTime` or `dayOfWeek ∉ 1..7`.

### `POST /api/v1/admin/availability/exceptions`
```json
Request → { "date": "2026-07-14", "kind": "BLOCK", "startTime": "00:00", "endTime": "23:59" }
201 → { "id": "uuid", "date": "...", "kind": "...", "startTime": "...", "endTime": "..." }
```
- `422 VALIDATION_ERROR` if the date is in the past, `kind` invalid, or `endTime <= startTime`.
- Response MAY include `bookingConflicts` (same shape) when a `BLOCK` overlaps a confirmed booking.

### `DELETE /api/v1/admin/availability/exceptions/{id}`
```
204 → (no content)   404 if not found
```

> The existing public `GET /api/v1/schedule` is unchanged in shape; its slots are now derived
> from the tables above instead of hard-coded rules.

---

## Homework (admin)

### `GET /api/v1/admin/students`
Roster for the assignment/share pickers.
```json
200 → [ { "id": "uuid", "email": "student@example.com" }, ... ]
```
(Returns `STUDENT`-role users; excludes the admin.)

### `GET /api/v1/admin/homework`
All assignments the teacher has authored, with assignees and their submission state.
```json
200 → [ {
  "id": "uuid", "title": "...", "instructions": "...", "dueOn": "2026-06-20",
  "assignees": [
    { "userId": "uuid", "email": "a@b.c", "status": "SUBMITTED",
      "responseText": "Mi respuesta…", "submittedAt": "2026-06-12T10:00:00Z" },
    { "userId": "uuid", "email": "d@e.f", "status": "PENDING",
      "responseText": null, "submittedAt": null }
  ]
} ]
```
`status` ∈ `PENDING|SUBMITTED|REVIEWED`; `PENDING` is the default when no submission row exists.

### `POST /api/v1/admin/homework`
```json
Request → { "title": "...", "instructions": "...", "dueOn": "2026-06-20" | null,
            "assigneeIds": ["uuid", ...]  // optional; empty/absent ⇒ draft }
201 → { ...item as in GET... }
```
- `422 VALIDATION_ERROR` if `title`/`instructions` blank or over length, or `dueOn` malformed.
- `404 STUDENT_NOT_FOUND` if an `assigneeId` is not a known student.

### `PUT /api/v1/admin/homework/{id}`
Edit title / instructions / due date (assignees managed separately).
```json
Request → { "title": "...", "instructions": "...", "dueOn": "..." | null }
200 → { ...item... }   404 ASSIGNMENT_NOT_FOUND
```

### `PUT /api/v1/admin/homework/{id}/assignees`
Replace the assignee set (full-set semantics).
```json
Request → { "assigneeIds": ["uuid", ...] }
200 → { ...item with updated assignees... }
404 ASSIGNMENT_NOT_FOUND | 404 STUDENT_NOT_FOUND
```
Unassigning a student removes the item from their learning space; their submission row, if
any, is left intact unless the assignment is deleted.

### `DELETE /api/v1/admin/homework/{id}`
```
204 → (no content; cascades targets + submissions)   404 ASSIGNMENT_NOT_FOUND
```

---

## Images (admin upload, authenticated fetch)

### `POST /api/v1/admin/images`  (multipart/form-data)
```
form field: file=<binary>
201 → { "id": "uuid", "contentType": "image/png", "byteSize": 48213 }
422 INVALID_IMAGE  (disallowed type or > 2 MB)
```

### `GET /api/v1/images/{id}`  (any authenticated user)
```
200 → <raw bytes>, Content-Type: image/png   404 if not found
```

---

## Presentations (admin)

### `GET /api/v1/admin/presentations`
```json
200 → [ { "id": "uuid", "title": "...", "slideCount": 5, "sharedWith": 2,
          "updatedAt": "2026-06-09T12:00:00Z" } ]
```

### `POST /api/v1/admin/presentations`
```json
Request → { "title": "..." }
201 → { "id": "uuid", "title": "...", "slides": [], "sharedWith": [] }
```

### `GET /api/v1/admin/presentations/{id}`
Full deck for editing.
```json
200 → {
  "id": "uuid", "title": "...",
  "slides": [ { "id": "uuid", "heading": "...", "body": "...", "imageId": "uuid"|null, "sortOrder": 0 } ],
  "sharedWith": [ { "userId": "uuid", "email": "a@b.c" } ]
}
404 PRESENTATION_NOT_FOUND
```

### `PUT /api/v1/admin/presentations/{id}` — rename
```json
Request → { "title": "..." }   200 → { ...deck... }
```

### `DELETE /api/v1/admin/presentations/{id}`
```
204 → (cascades slides + shares)   404 PRESENTATION_NOT_FOUND
```

### `POST /api/v1/admin/presentations/{id}/slides`
```json
Request → { "heading": "...", "body": "...", "imageId": "uuid"|null }
201 → { ...slide... }   (appended at end)
422 VALIDATION_ERROR (length/slide-count cap)
```

### `PUT /api/v1/admin/presentations/{id}/slides/{slideId}`
```json
Request → { "heading": "...", "body": "...", "imageId": "uuid"|null }
200 → { ...slide... }   404 PRESENTATION_NOT_FOUND
```

### `DELETE /api/v1/admin/presentations/{id}/slides/{slideId}`
```
204 → (no content)   404 PRESENTATION_NOT_FOUND
```

### `PUT /api/v1/admin/presentations/{id}/slides/order`
```json
Request → { "slideIds": ["uuid", "uuid", ...] }   // permutation of the deck's slides
200 → { ...deck with new order... }
422 VALIDATION_ERROR (not a permutation)
```

### `PUT /api/v1/admin/presentations/{id}/shares` — replace shared set
```json
Request → { "studentIds": ["uuid", ...] }
200 → { ...deck with sharedWith... }
404 PRESENTATION_NOT_FOUND | 404 STUDENT_NOT_FOUND
```

---

## Learning (student-facing additions)

### `GET /api/v1/learning` — extended
The existing overview response gains shared decks (homework now reflects targeting):
```json
200 → {
  "presentation": [ ...existing intro blocks (005)... ],
  "pastClasses":  [ ...existing... ],
  "homework":     [ ...existing item shape; only assignments targeted to the caller... ],
  "sharedPresentations": [ { "id": "uuid", "title": "...", "slideCount": 5 } ]
}
```

### `GET /api/v1/learning/presentations/{id}` — open a shared deck
```json
200 → {
  "id": "uuid", "title": "...",
  "slides": [ { "heading": "...", "body": "...", "imageId": "uuid"|null, "sortOrder": 0 } ]
}
404 PRESENTATION_NOT_FOUND   // when not shared with the caller (indistinguishable from missing)
```
Images referenced by `imageId` are fetched from `GET /api/v1/images/{id}`.

---

## Error codes (added)

| Code | HTTP | Meaning |
|------|------|---------|
| `ACCESS_DENIED` | 403 | Authenticated non-admin hit an `/api/v1/admin/**` route |
| `ASSIGNMENT_NOT_FOUND` | 404 | (existing) homework id unknown |
| `STUDENT_NOT_FOUND` | 404 | An assignee/share id is not a known student |
| `PRESENTATION_NOT_FOUND` | 404 | Deck id unknown or not shared with caller |
| `INVALID_IMAGE` | 422 | Upload type not allowed or exceeds 2 MB |
| `VALIDATION_ERROR` | 422/400 | (existing) field/shape validation failure |

## Security matcher summary

```
/api/v1/auth/**            permitAll        (unchanged)
GET /api/v1/schedule       permitAll        (unchanged; now data-driven)
GET /api/v1/resources/**   permitAll        (unchanged)
/api/v1/admin/**           hasRole('ADMIN') (NEW)
anyRequest()               authenticated    (covers /api/v1/learning/**, /api/v1/images/**, bookings)
```
