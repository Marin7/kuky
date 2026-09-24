# API Contract: Email Preferences

**Feature**: `049-email-preferences` | **Date**: 2026-09-23 | **Plan**: [plan.md](../plan.md)

Base: `/api/v1/me/email-preferences` · Auth: `auth-token` HTTP-only cookie · All responses `application/json`

Errors follow the project-wide shape `{"error": "ERROR_CODE", "message": "..."}` (`GlobalExceptionHandler`).

---

## Security placement

Add to `SecurityConfig`, beside the existing `/api/v1/notifications/**` entry:

```java
.requestMatchers("/api/v1/me/**").authenticated()
```

Two properties of this placement are load-bearing and must not be changed casually:

- **Not under `/api/v1/auth/**`.** That prefix is `permitAll()` (`SecurityConfig:47`). Mounting preferences there would expose a signed-in user's settings to anonymous callers.
- **No user ID anywhere in the path or body.** The account is resolved from the JWT principal, which is what makes FR-005 (only the owner can read or change it) and FR-015 (the teacher cannot) structural rather than enforced by a check someone could forget.

`anyRequest().authenticated()` would already cover this path; the explicit matcher exists so the intent is visible in the config.

**Roles**: any authenticated account ([R5](../research.md)). Not gated on `STUDENT` — a `USER` promoted later keeps what they set, and the email can only fire on homework assignment, which non-students never receive.

---

## `GET /api/v1/me/email-preferences`

Returns every supported option with the caller's effective value. The client renders one toggle per entry and never hardcodes the list (FR-003).

**Request**: no body, no parameters.

**`200 OK`**

```json
{
  "preferences": [
    { "type": "NEW_HOMEWORK_ASSIGNED", "enabled": false }
  ]
}
```

| Field | Type | Notes |
|---|---|---|
| `preferences` | array | One entry per `EmailPreferenceType` constant. Never empty; never omits a supported type. |
| `preferences[].type` | string | Enum constant name — stable wire value |
| `preferences[].enabled` | boolean | Effective value. `false` for an account that has never touched it (FR-002) |

**Errors**: `401` with `{"error":"UNAUTHENTICATED","message":"Debes iniciar sesión."}` when not signed in (the code `SecurityConfig`'s entry point already emits).

**Notes**

- Ordering follows enum declaration order — stable across calls.
- No caching headers; the value is per-user and cheap.
- An account that has never called `PUT` is indistinguishable from one explicitly set to `false`, by design.

---

## `PUT /api/v1/me/email-preferences/{type}`

Sets one option for the calling account. Idempotent — writing the value it already holds is a success, not a conflict (FR-004).

**Path**: `type` — an `EmailPreferenceType` constant, e.g. `NEW_HOMEWORK_ASSIGNED`.

**Request**

```json
{ "enabled": true }
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `enabled` | boolean | yes | `null` or missing → `VALIDATION_ERROR` |

**`200 OK`** — the stored state after the write:

```json
{ "type": "NEW_HOMEWORK_ASSIGNED", "enabled": true }
```

**Errors**

| Status | `error` | When |
|---|---|---|
| `400` | `VALIDATION_ERROR` | `enabled` missing or null (bean validation on the body) |
| `422` | `VALIDATION_ERROR` | `{type}` is not a known constant |
| `401` | `UNAUTHENTICATED` | Not signed in |

Unknown `{type}` reuses the existing `VALIDATION_ERROR` via `GlobalExceptionHandler`'s `IllegalArgumentException` handler — no new exception class or error code is added. Note the two different statuses: that handler returns **422**, while the bean-validation handler for a malformed body returns **400**. Both are pre-existing project behaviour, verified against the running service rather than assumed.

**Notes**

- Takes effect for assignments made **after** the write commits. Nothing is sent retroactively, and nothing already in flight is recalled (FR-004, spec edge cases).
- No admin or teacher-facing counterpart exists anywhere in the API (FR-015).

---

## Email contract — *new homework assigned*

Not an HTTP endpoint, but the feature's real output, so its contract is pinned here.

**Trigger**: one or more `homework_targets` rows were actually inserted for a student during a single teacher action, and that student has `email_on_homework_assigned = true` and `status = 'ACTIVE'`.

**Transport**: `SimpleMailMessage` through the existing `JavaMailSender`. No-op when `app.mail.enabled=false`, exactly like every other email in the project.

| Property | Value |
|---|---|
| From | `${app.mail.from}` (default `noreply@kuky.es`) |
| To | Exactly one recipient — never CC, never BCC (FR-011) |
| Subject | Spanish, in the house pattern `"… — Destino: Español"` |
| Body | Spanish plain text: greeting, one line per newly assigned homework title, link to the work, the why-am-I-getting-this line, link to preferences, `"Saludos,\nDestino: Español"` |
| Work link | `${app.frontend.base-url}/aprendizaje` (FR-009) |
| Preferences link | `${app.frontend.base-url}/cuenta` (FR-010) |

### Guarantees

| # | Guarantee | Requirement |
|---|---|---|
| E1 | **Exactly one** message per student per teacher action, however many homework it granted | FR-006 |
| E2 | Homework listed in the order the action granted it | — |
| E3 | Two students granted different amounts in one action receive different bodies | spec edge case |
| E4 | No message names, or leaks the work of, any other student | FR-011 |
| E5 | Nothing sent for a homework the student already held | FR-013 |
| E6 | Nothing sent when the switch is off at flush time | FR-008 |
| E7 | Nothing sent to a `PENDING` account | spec edge case |
| E8 | A send failure is logged at WARN and swallowed — never propagates to the teacher's request, never rolls back the assignment | FR-012 |
| E9 | Sent identically whichever of the three routes granted the work | FR-007 |

---

## Endpoints deliberately **not** added

Stated so a later reader does not assume they were forgotten:

- ❌ Any `/api/v1/admin/**` route reading or writing a student's preferences — forbidden by FR-015.
- ❌ An unauthenticated unsubscribe URL or one-click `List-Unsubscribe` header — spec Out of Scope; opting out means signing in and switching it off.
- ❌ A bulk endpoint writing several preferences at once — one option exists; `PUT` per type is enough (Principle I).
- ❌ Any change to `GET /api/v1/auth/me` — the preference is not added to the session payload, so nothing about existing auth responses changes (FR-014).
