# API Contract: Mi aprendizaje — Classes & Homework Tab

Base path: `/api/v1/learning`. All endpoints **require authentication** (JWT in the `auth-token` HTTP-only cookie, as elsewhere). There are no public endpoints in this feature, so no `permitAll` rule is added to `SecurityConfig`.

**Guest rejection status**: The app configures no `httpBasic`/`formLogin`, so Spring Security's default entry point rejects unauthenticated requests with **`403 Forbidden`** (and an empty body), not `401`. This is the existing app-wide behavior for every authenticated endpoint (auth/scheduling/resources). Guests are blocked either way; the frontend additionally redirects them to `/cuenta`. Treat any `4xx` here as "not signed in."

Error envelope (consistent with the rest of the API):

```json
{ "error": "ERROR_CODE", "message": "Human-readable message" }
```

The current user is resolved server-side from the cookie via `@AuthenticationPrincipal String email`; clients never send a user id.

---

## GET `/api/v1/learning`

Returns the entire section for the calling student: presentation blocks, past classes (most recent first), and homework with the caller's per-student status.

**Auth**: required.

**200 OK**

```json
{
  "presentation": [
    { "heading": "Cómo son mis clases", "body": "Clases individuales por videollamada, adaptadas a tu nivel…" }
  ],
  "pastClasses": [
    {
      "id": "f1d2…",
      "title": "El pretérito indefinido",
      "heldOn": "2026-05-28",
      "teacherNote": "Repasamos las formas regulares e irregulares. Muy buen progreso."
    }
  ],
  "homework": [
    {
      "id": "a7c9…",
      "title": "Escribe sobre tus últimas vacaciones",
      "instructions": "Redacta 8–10 frases usando el pretérito indefinido.",
      "dueOn": "2026-06-05",
      "status": "PENDING",
      "response": null,
      "submittedAt": null,
      "overdue": true
    }
  ]
}
```

Field notes:
- `presentation`: ordered by `sort_order`; `[]` when none published (frontend shows a sensible default).
- `pastClasses`: ordered by `heldOn` descending (FR-006); `[]` ⇒ empty state.
- `homework`: ordered by `sort_order`; each item's `status` is the caller's effective status (`PENDING` when the student has no submission yet); `dueOn` may be `null`; `overdue` is derived (`dueOn` in the past AND `status = PENDING`); `[]` ⇒ empty state.

**403 Forbidden** — guest (empty body; see "Guest rejection status" above).

---

## PUT `/api/v1/learning/homework/{assignmentId}`

Mark an assignment done / submit a short written response for the calling student. Idempotent upsert of the per-student submission keyed by `(user, assignmentId)`.

**Auth**: required.

**Path param**: `assignmentId` — UUID of a published `homework_assignments` row.

**Request body** (both fields optional; an empty body means "mark done" with no text):

```json
{ "response": "Fui a la playa con mi familia. Nadamos y comimos paella…" }
```

- `response`: optional string, **max 2000 characters** (`@Size(max = 2000)`). Omitted/null ⇒ submit/mark-done without text.

**200 OK** — returns the updated homework item (same shape as an element of `homework` above):

```json
{
  "id": "a7c9…",
  "title": "Escribe sobre tus últimas vacaciones",
  "instructions": "Redacta 8–10 frases usando el pretérito indefinido.",
  "dueOn": "2026-06-05",
  "status": "SUBMITTED",
  "response": "Fui a la playa con mi familia…",
  "submittedAt": "2026-06-10T14:05:00Z",
  "overdue": false
}
```

Behavior:
- Transitions effective status `PENDING → SUBMITTED` and sets `submittedAt`. Re-submitting while `PENDING`/`SUBMITTED` updates `response`/`submittedAt` idempotently (latest wins — Edge case "homework already submitted").
- Once the submission is `REVIEWED` (set only by the future backoffice), further student submits are rejected (see `409`).

**Errors**:

| Status | `error` | When |
|--------|---------|------|
| `400` | `VALIDATION_ERROR` | `response` exceeds 2000 chars (or malformed body) |
| `403` | _(empty body)_ | guest (default security entry point) |
| `404` | `ASSIGNMENT_NOT_FOUND` | `assignmentId` does not match a published assignment |
| `409` | `SUBMISSION_NOT_ALLOWED` | the student's submission is already `REVIEWED` (read-only) |

---

## Error code summary (added to `GlobalExceptionHandler`)

| Code | HTTP | Meaning |
|------|------|---------|
| `ASSIGNMENT_NOT_FOUND` | 404 | No published assignment with that id |
| `SUBMISSION_NOT_ALLOWED` | 409 | Submission is `REVIEWED`; student cannot modify it |
| `VALIDATION_ERROR` | 400 | Request body failed validation (e.g. response too long) — reuses existing handler |
| _(empty body)_ | 403 | Guest / missing session — Spring Security's default entry point (app-wide behavior) |

---

## Mapping to requirements

| Endpoint / behavior | Requirements |
|---------------------|--------------|
| Both endpoints require auth; guests rejected (`403`) | FR-002, FR-003, SC-001 |
| `presentation` in overview | FR-004 |
| `pastClasses` (title, date, note), most recent first | FR-005, FR-006 |
| `homework` (title, instructions, due, status) | FR-007 |
| `PUT` upsert persists per student | FR-008, FR-015, SC-004 |
| `overdue` derived flag | FR-009 |
| Empty arrays drive empty states | FR-010, SC-005 |
| `PENDING → SUBMITTED`; `REVIEWED` read-only | FR-014 |
