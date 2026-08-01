# Contract: University Student Portal API

Base path `/api/v1`. JSON; JWT HttpOnly cookie (`credentials: 'include'`); errors `{"error":"CODE","message":"..."}`.

Roles relevant here: `USER`, `STUDENT`, `UNIVERSITY_STUDENT`, `ADMIN`.

---

## Auth / me (extended)

### `GET /auth/me` *(existing — response extended)*

`UserResponse.role` may be `"UNIVERSITY_STUDENT"`.

```json
{
  "id": "…",
  "email": "…",
  "role": "UNIVERSITY_STUDENT",
  "universityLevel": "BEGINNER",
  "…": "…"
}
```

`universityLevel` is `null` unless `role === "UNIVERSITY_STUDENT"`.

Registration/login endpoints unchanged; available from both front-end origins (CORS + cookie Domain).

---

## Public informative (no auth)

### `GET /university/schedule`

Query: optional `from` / `to` (ISO dates) for exception window; if omitted, server returns template + exceptions for a sensible default horizon (e.g. current week ± N days — document in implementation).

Response shape:

```json
{
  "viewerMode": "FULL_LABELED" | "LEVEL_FILTERED",
  "level": "BEGINNER" | "INTERMEDIATE" | null,
  "templateSessions": [
    {
      "id": "…",
      "level": "BEGINNER",
      "dayOfWeek": 1,
      "startTime": "09:00:00",
      "endTime": "10:30:00",
      "title": "Gramática"
    }
  ],
  "exceptions": [
    {
      "id": "…",
      "level": "BEGINNER",
      "exceptionDate": "2026-08-10",
      "kind": "CANCEL",
      "sessionId": "…",
      "startTime": null,
      "endTime": null,
      "title": null
    }
  ]
}
```

- Anonymous / non–`UNIVERSITY_STUDENT`: `viewerMode = FULL_LABELED`, `level = null`, both levels’ sessions included.
- Authenticated `UNIVERSITY_STUDENT`: `viewerMode = LEVEL_FILTERED`, `level` = their level, only that level’s template + exceptions.
- `ADMIN`: may use full labeled or filtered via optional query `level=` (implementation choice; default full labeled).

### `GET /university/exams`

Returns published exam dates, chronological.

```json
[
  {
    "id": "…",
    "title": "Examen parcial",
    "examAt": "2026-09-01T09:00:00Z",
    "description": "…"
  }
]
```

### `GET /university/news`

Returns published news, newest first.

```json
[
  {
    "id": "…",
    "title": "…",
    "body": "…",
    "publishedAt": "2026-08-01T12:00:00Z"
  }
]
```

---

## University learning *(requires `UNIVERSITY_STUDENT` or `ADMIN`)*

### `GET /university/learning`

Overview for the caller’s university level (ADMIN must pass `?level=BEGINNER|INTERMEDIATE` or defaults to empty/full — prefer required query for ADMIN).

```json
{
  "level": "BEGINNER",
  "presentations": [
    { "id": "…", "title": "…", "hasFile": true }
  ],
  "homework": [
    {
      "id": "…",
      "title": "…",
      "format": "MANUAL",
      "status": "PENDING",
      "dueOn": "2026-08-15"
    }
  ]
}
```

Lists only catalog items with availability for that level.

### Presentation file / homework take-submit

Reuse private learning semantics under university paths **or** reuse existing learning URLs with university authorization — prefer dedicated university routes that delegate to shared services:

- `GET /university/learning/presentations/{id}/file`
- `GET /university/learning/homework/{assignmentId}` (EXERCISE payload; hide answer key pre-grade)
- `PUT /university/learning/homework/{assignmentId}` (MANUAL submit)
- `PUT /university/learning/homework/{assignmentId}/answers` (EXERCISE submit)

Access: availability row for student’s level (or ADMIN with matching level query). Errors: `404` if not available (same privacy pattern as private targets).

---

## Admin — roster *(ADMIN only)*

### `GET /admin/university/students`

Lists `UNIVERSITY_STUDENT` accounts with level.

### `GET /admin/users` *(existing — behavior)*

Promotion candidates remain non-student registered users (`USER`). University students do not appear as private-student candidates.

### `POST /admin/users/{id}/university-student`

```json
{ "level": "BEGINNER" }
```

Sets `UNIVERSITY_STUDENT` + level. **No email.**

Errors:
- `404 USER_NOT_FOUND`
- `409 ROLE_CONFLICT` — target is `STUDENT` or `ADMIN` or already `UNIVERSITY_STUDENT` (idempotent OK for already same level; conflict if `STUDENT`)
- `422 INVALID_LEVEL`

### `DELETE /admin/users/{id}/university-student`

Revokes to `USER`, clears level. **No email.** Idempotent if already `USER`.

### `PUT /admin/users/{id}/university-level`

```json
{ "level": "INTERMEDIATE" }
```

Only when currently `UNIVERSITY_STUDENT`.

### Private student grant/revoke *(existing — extended guard)*

`POST /admin/users/{id}/student` and `DELETE …/student` must reject targets that are `UNIVERSITY_STUDENT` with `409 ROLE_CONFLICT` until university status is revoked.

---

## Admin — schedule / exams / news / availability *(ADMIN only)*

### Schedule template

- `GET /admin/university/schedule/sessions`
- `POST /admin/university/schedule/sessions` — body: level, dayOfWeek, startTime, endTime, title?
- `PUT /admin/university/schedule/sessions/{id}`
- `DELETE /admin/university/schedule/sessions/{id}`

### Schedule exceptions

- `GET /admin/university/schedule/exceptions?from=&to=`
- `POST /admin/university/schedule/exceptions` — CANCEL (sessionId + date) or EXTRA (level, date, times, title?)
- `DELETE /admin/university/schedule/exceptions/{id}`

### Exams

- CRUD under `/admin/university/exams` with `published` flag

### News

- CRUD under `/admin/university/news` with `published` / `publishedAt`

### Availability

- `PUT /admin/university/levels/{level}/homeworks` — replace list of `assignmentId`s available for that level  
  `{ "assignmentIds": ["…", "…"] }`
- `PUT /admin/university/levels/{level}/presentations` — replace list of `presentationId`s  
  `{ "presentationIds": ["…", "…"] }`
- `GET` counterparts to read current availability

---

## SecurityConfig summary

| Matcher | Access |
|---------|--------|
| `GET /api/v1/university/schedule`, `/exams`, `/news` | permitAll |
| `/api/v1/university/learning/**` | `UNIVERSITY_STUDENT` or `ADMIN` |
| `/api/v1/admin/university/**` | `ADMIN` |
| Existing `STUDENT` matchers | unchanged (do **not** include `UNIVERSITY_STUDENT`) |

---

## Config (ops)

| Property / env | Purpose |
|----------------|---------|
| `CORS_ALLOWED_ORIGINS` (comma-separated) or dual props | Private + university front origins |
| `COOKIE_DOMAIN` | e.g. `.kuky.es` in prod; empty for local host-only |
| Front `VITE_UNIVERSITY_HOST` / runtime host detect | Select university shell |

Exact property names finalized in implementation to match Boot/`application.yaml` style.
