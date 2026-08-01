# Contract: Student Interests API

Base path `/api/v1`. Conventions match the existing API: JSON, HTTP-only JWT cookie auth (`credentials: 'include'`), current user via `@AuthenticationPrincipal String email`, error bodies `{"error":"CODE","message":"..."}`.

Interest **codes** are opaque product constants. Clients MUST localize labels themselves (`interests.<CODE>` in i18n). The API never returns translated labels.

---

## Catalogue (product-fixed)

Allowed codes (v1):

`TRAVEL` | `MUSIC` | `SPORTS` | `FOOD` | `CINEMA` | `READING` | `TECHNOLOGY` | `NATURE` | `ART` | `WORK` | `FAMILY` | `CULTURE`

Constraints:

- Max **10** selected codes per user.
- Optional note: max **280** characters (Unicode code units as enforced by `@Size` / DB `VARCHAR(280)`).
- Empty selection and empty/null note are both allowed.

---

## PUT `/auth/interests` *(new — authenticated; `STUDENT` or `ADMIN` role)*

Replaces the caller’s full interest selection and note (last-write-wins).

```json
{
  "interests": ["TRAVEL", "MUSIC", "FOOD"],
  "interestsNote": "Me encanta el flamenco y cocinar paella."
}
```

`interests` may be `[]`. `interestsNote` may be `null` or omitted (treat omit as null) or `""` (stored as null).

`200 OK` → `UserResponse` (existing shape extended):

```json
{
  "id": "…",
  "email": "ana@example.com",
  "role": "STUDENT",
  "firstName": "Ana",
  "lastName": "Popescu",
  "username": "ana",
  "avatarImageId": null,
  "status": "ACTIVE",
  "timezone": "Europe/Bucharest",
  "timezoneIsManual": true,
  "extendedClassEligible": false,
  "interests": ["FOOD", "MUSIC", "TRAVEL"],
  "interestsNote": "Me encanta el flamenco y cocinar paella."
}
```

Errors:

| Status | `error` | When |
|--------|---------|------|
| `401` | *(empty / unauthorized)* | No valid auth cookie (same pattern as `PUT /auth/profile`). |
| `403` | `FORBIDDEN` | Authenticated but role is not `STUDENT` or `ADMIN` (e.g. `USER`). |
| `400` | `INVALID_INTEREST` | One or more codes not in the catalogue. |
| `400` | `TOO_MANY_INTERESTS` | More than 10 codes after deduplication. |
| `400` | `VALIDATION_ERROR` | Note longer than 280 characters (existing Bean Validation mapping). |

Security note: path remains under `/api/v1/auth/**` (`permitAll` at the filter chain); the controller/service enforces authentication + role, identical to how `PUT /auth/profile` enforces a non-null principal today.

---

## GET `/auth/me`, POST `/auth/login`, PUT `/auth/profile`, PUT `/auth/timezone`, … *(existing — response extended)*

Every `UserResponse` now includes:

```json
"interests": [],
"interestsNote": null
```

For `USER` role accounts these stay empty/null. `PUT /auth/profile` does **not** accept or modify interests (FR-011 isolation).

---

## GET `/admin/students/{id}/profile` *(existing — response extended; `ADMIN` only)*

`StudentProfileResponse` gains the same two fields:

```json
{
  "id": "…",
  "email": "ana@example.com",
  "firstName": "Ana",
  "lastName": "Popescu",
  "username": "ana",
  "avatarImageId": null,
  "createdAt": "…",
  "interests": ["FOOD", "MUSIC", "TRAVEL"],
  "interestsNote": "Me encanta el flamenco y cocinar paella.",
  "bookings": [ … ],
  "homeworks": [ … ],
  "presentations": [ … ],
  "progress": { … }
}
```

Empty state for the teacher UI: `interests.length === 0 && (interestsNote == null || interestsNote === "")`.

No new admin write endpoints — teacher is view-only (FR-009).

---

## Out of scope (no contract changes)

- Booking / schedule / reminder payloads
- Public testimonials or landing pages
- Admin catalogue CRUD
