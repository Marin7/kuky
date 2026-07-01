# Contract: User/Student Roles

Base path `/api/v1`. Conventions match the existing API: JSON, HTTP-only JWT cookie auth (`credentials: 'include'`), current user via `@AuthenticationPrincipal String email`, error bodies `{"error":"CODE","message":"..."}`.

`role` is now one of `USER` | `STUDENT` | `ADMIN` everywhere it appears in a response (previously `STUDENT` | `ADMIN`).

---

## Admin endpoints (existing `/api/v1/admin/**` matcher — `ADMIN` role; non-admin → `403 ACCESS_DENIED`)

### GET `/admin/users`
Returns all accounts currently at `role = 'USER'` (registered, not yet a student) — the promotion picker.

`200 OK`
```json
[
  { "id": "uuid", "email": "ana@example.com", "firstName": "Ana", "lastName": "Pop", "username": "anap" }
]
```

### POST `/admin/users/{id}/student`
Grants student status: `role` `USER` → `STUDENT`. Idempotent — calling it on an already-`STUDENT` user succeeds without change. Sends the "you're now a student" email to the user.

`200 OK` → `{ "id": "uuid", "role": "STUDENT" }`

Errors: `404 USER_NOT_FOUND` (no such id, or the id belongs to an `ADMIN` account).

### DELETE `/admin/users/{id}/student`
Revokes student status: `role` `STUDENT` → `USER`. Idempotent — calling it on an already-`USER` user succeeds without change. Does **not** delete or hide the user's bookings, purchases, or homework history. Sends the "student access removed" email to the user.

`200 OK` → `{ "id": "uuid", "role": "USER" }`

Errors: `404 USER_NOT_FOUND` (no such id, or the id belongs to an `ADMIN` account).

### GET `/admin/students` *(existing, unchanged)*
Still returns accounts at `role = 'STUDENT'` — semantics unchanged by this feature.

---

## Gated student actions (new `STUDENT`-or-`ADMIN` requirement)

These endpoints already exist; only their access control changes. Anonymous → `401 UNAUTHENTICATED` (unchanged). Authenticated but `role = 'USER'` → `403 ACCESS_DENIED` (new).

| Endpoint | Change |
|---|---|
| `POST /bookings` (create booking) | Now `hasAnyRole("STUDENT","ADMIN")`. |
| `POST /purchases` (purchase/unlock a resource) | Now `hasAnyRole("STUDENT","ADMIN")`. |
| `GET /learning`, `GET /learning/homework/{id}`, `PUT /learning/homework/{id}`, `PUT /learning/homework/{id}/answers`, `GET /learning/presentations/{id}/file` (all coursework) | Now `hasAnyRole("STUDENT","ADMIN")` as a path-prefix rule on `/learning/**`. |

## Unchanged (stay open to any authenticated user, or public)

| Endpoint | Why |
|---|---|
| `GET /schedule` | Already `permitAll()` — public browsing (FR-006). |
| `GET /resources/**` | Already `permitAll()` — public browsing (FR-006). |
| `GET /bookings` (my bookings), `GET /purchases` (my purchases), `GET /purchases/{id}/receipt` | Own history must stay visible after revocation (FR-012). |
| `GET /placement/**`, `POST /placement/**` | Placement test stays open to any logged-in user regardless of role (FR-008) — untouched by this feature. |
| `GET /audio/{id}` | Shared listening-clip endpoint used by both coursework and the placement test; stays `anyRequest().authenticated()` (unguessable UUIDs, per existing docstring). |

---

## Frontend contract notes

- `UserResponse.role` (front-end/src/lib/auth.ts) widens from `"STUDENT" | "ADMIN"` to `"USER" | "STUDENT" | "ADMIN"`.
- A `403 ACCESS_DENIED` from a gated action surfaces the shared `StudentOnlyNotice` message (FR-009): "El acceso de alumno lo concede la profesora." — not a generic error toast.
