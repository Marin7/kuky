# Contract: Testimonials API

Base path `/api/v1`. Conventions match the existing API: JSON, HTTP-only JWT cookie auth (`credentials: 'include'`), current user via `@AuthenticationPrincipal String email`, error bodies `{"error":"CODE","message":"..."}`.

---

## Public / student endpoints (`/api/v1/testimonials`)

### GET `/testimonials`
Public homepage read. No auth required (`permitAll()`).

Returns only `APPROVED` testimonials, ordered by `displayOrder`. Empty array (never an error) when none are approved — the frontend hides the section entirely on `[]` (FR-002).

`200 OK`
```json
[
  { "text": "Con Paula aprendí más en tres meses que en dos años de instituto.", "studentName": "Ana Popescu", "displayOrder": 0 }
]
```

### POST `/testimonials`
Submit (or resubmit, replacing the existing one) a testimonial. Requires `hasAnyRole("STUDENT","ADMIN")`.

Request:
```json
{ "text": "Con Paula aprendí más en tres meses que en dos años de instituto." }
```

`200 OK` → `{ "text": "...", "status": "PENDING", "submittedAt": "2026-07-02T10:00:00Z" }`

Errors:
- `400 VALIDATION_ERROR` — `text` blank or below the minimum length (FR-010).
- `401 UNAUTHENTICATED` — not logged in.
- `403 ACCESS_DENIED` — logged in but not `STUDENT`/`ADMIN` (a plain `USER` account).

Side effect: sends a plain-text email to the teacher (`SchedulingProperties.getTeacherEmail()`) notifying her a new/updated testimonial is pending review (FR-008).

### GET `/testimonials/me`
The caller's own testimonial status, if any. Requires `hasAnyRole("STUDENT","ADMIN")`.

`200 OK` → `{ "text": "...", "status": "PENDING" | "APPROVED" | "REJECTED" | "UNPUBLISHED", "submittedAt": "..." }`

`204 No Content` — the student has never submitted one.

No edit/delete endpoint here by design — per the clarified spec, changing the text means submitting again via `POST /testimonials` (FR-007).

---

## Admin endpoints (existing `/api/v1/admin/**` matcher — `ADMIN` role; non-admin → `403 ACCESS_DENIED`)

### GET `/admin/testimonials`
All testimonials regardless of status, ordered by `displayOrder`, for the review queue + published list.

`200 OK`
```json
[
  {
    "id": "uuid",
    "text": "...",
    "studentName": "Ana Popescu",
    "status": "PENDING",
    "displayOrder": 0,
    "submittedAt": "2026-07-02T10:00:00Z",
    "reviewedAt": null
  }
]
```

### POST `/admin/testimonials/{id}/approve`
`PENDING`/`REJECTED`/`UNPUBLISHED` → `APPROVED`. Sets `reviewedAt`.

`200 OK` → updated `TestimonialAdminResponse`. Errors: `404 TESTIMONIAL_NOT_FOUND`.

### POST `/admin/testimonials/{id}/reject`
`PENDING` → `REJECTED`. Sets `reviewedAt`.

`200 OK` → updated `TestimonialAdminResponse`. Errors: `404 TESTIMONIAL_NOT_FOUND`.

### POST `/admin/testimonials/{id}/unpublish`
`APPROVED` → `UNPUBLISHED`. Removes it from the public homepage without deleting it.

`200 OK` → updated `TestimonialAdminResponse`. Errors: `404 TESTIMONIAL_NOT_FOUND`.

### PUT `/admin/testimonials/{id}`
Edit the testimonial text (e.g., fix a typo) without changing its status.

Request: `{ "text": "..." }`

`200 OK` → updated `TestimonialAdminResponse`. Errors: `400 VALIDATION_ERROR`, `404 TESTIMONIAL_NOT_FOUND`.

### PUT `/admin/testimonials/reorder`
Reassigns `displayOrder` for the given ids, in list order (mirrors `PUT /admin/units/reorder`).

Request: `{ "orderedIds": ["uuid1", "uuid2", "uuid3"] }`

`200 OK` → `TestimonialAdminResponse[]` in new order.

### DELETE `/admin/testimonials/{id}`
Hard-removes a testimonial entirely (FR-003's "remove").

`204 No Content`. Errors: `404 TESTIMONIAL_NOT_FOUND`.

---

## Security matcher additions (`SecurityConfig`)

| Endpoint | Rule |
|---|---|
| `GET /api/v1/testimonials` | `permitAll()` — new. |
| `POST /api/v1/testimonials` | `hasAnyRole("STUDENT","ADMIN")` — new. |
| `GET /api/v1/testimonials/me` | `hasAnyRole("STUDENT","ADMIN")` — new. |
| `/api/v1/admin/testimonials/**` | Already covered by the existing `/api/v1/admin/**` → `hasRole("ADMIN")` matcher — no change needed. |

## Frontend contract notes

- `front-end/src/lib/testimonials.ts` (new): `getTestimonials()`, `getMyTestimonial()`, `submitTestimonial(text)` — same `apiCall<T>` fetch wrapper shape as `scheduling.ts`/`learning.ts` (`credentials: "include"`, `{error,message}` error type).
- `front-end/src/lib/admin.ts` (extended): `getAdminTestimonials()`, `approveTestimonial(id)`, `rejectTestimonial(id)`, `unpublishTestimonial(id)`, `updateTestimonialText(id, text)`, `reorderTestimonials(orderedIds)`, `deleteTestimonial(id)`.
- A `403 ACCESS_DENIED` from `POST /testimonials` (a plain `USER` submitting) surfaces the existing shared `StudentOnlyNotice` message, consistent with every other student-gated action in the app.
