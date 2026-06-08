# API Contract: Account Management

**Feature**: `002-account-management` | **Date**: 2026-06-08
**Base URL** (local dev): `http://localhost:8081/api/v1`
**Authentication**: HTTP-only cookie `auth-token` (JWT HS256, 7-day rolling)
**Content-Type**: `application/json` for all requests and responses
**Language**: All user-facing message strings are in Spanish (v1)

---

## Endpoints

### POST /auth/register

Register a new user account.

**Request body**:
```json
{
  "email": "alumna@ejemplo.com",
  "password": "contraseña123",
  "gdprConsent": true
}
```

**Success — 201 Created**:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alumna@ejemplo.com",
  "createdAt": "2026-06-08T10:00:00Z"
}
```
Sets `auth-token` cookie (HttpOnly, Secure, SameSite=Lax, MaxAge=604800 s).

**Error — 400 Bad Request** (validation failure):
```json
{
  "error": "VALIDATION_ERROR",
  "message": "El correo electrónico no es válido."
}
```

**Error — 400 Bad Request** (missing GDPR consent):
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Debes aceptar la política de privacidad para crear una cuenta."
}
```

**Error — 409 Conflict** (email already registered):
```json
{
  "error": "EMAIL_ALREADY_EXISTS",
  "message": "Este correo electrónico ya está registrado."
}
```

---

### POST /auth/login

Authenticate an existing user.

**Request body**:
```json
{
  "email": "alumna@ejemplo.com",
  "password": "contraseña123"
}
```

**Success — 200 OK**:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alumna@ejemplo.com"
}
```
Refreshes `auth-token` cookie (MaxAge=604800 s reset).

**Error — 401 Unauthorized** (wrong credentials or no account — generic, no enumeration, FR-007):
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Correo electrónico o contraseña incorrectos."
}
```

**Error — 429 Too Many Requests** (rate limit exceeded, FR-016):
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Demasiados intentos. Por favor, espera un momento e inténtalo de nuevo."
}
```

---

### POST /auth/logout

End the current session.

**Request**: No body. Requires a valid `auth-token` cookie.

**Success — 204 No Content**: Clears `auth-token` cookie (MaxAge=0, same flags).

**Error — 401 Unauthorized**: No active session (cookie absent or expired).

---

### GET /auth/me

Return the currently authenticated user. Called by SSR loaders on every page render.

**Request**: No body. Requires a valid `auth-token` cookie.

**Success — 200 OK**:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "alumna@ejemplo.com"
}
```

**Error — 401 Unauthorized** (no session or expired):
```json
{
  "error": "UNAUTHORIZED",
  "message": "No autenticado."
}
```

---

### POST /auth/forgot-password

Request a password reset link by email.

**Request body**:
```json
{
  "email": "alumna@ejemplo.com"
}
```

**Success — 200 OK** (always, regardless of whether the email exists — FR-010):
```json
{
  "message": "Si el correo está registrado, recibirás un enlace de recuperación en breve."
}
```

No error response is returned for unknown emails (prevents user enumeration).

---

### POST /auth/reset-password

Submit a new password using a valid reset token.

**Request body**:
```json
{
  "token": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "newPassword": "nuevaContraseña123"
}
```

**Success — 200 OK**:
```json
{
  "message": "Contraseña actualizada correctamente."
}
```
Sets `auth-token` cookie (user is automatically signed in after reset, FR-012).

**Error — 400 Bad Request** (token invalid, expired, or already used — FR-011):
```json
{
  "error": "INVALID_OR_EXPIRED_TOKEN",
  "message": "El enlace de recuperación no es válido o ha expirado. Solicita uno nuevo."
}
```

**Error — 400 Bad Request** (new password too weak):
```json
{
  "error": "VALIDATION_ERROR",
  "message": "La contraseña debe tener al menos 8 caracteres."
}
```

---

## Error Code Reference

| Code | HTTP Status | Meaning |
|------|-------------|---------|
| `VALIDATION_ERROR` | 400 | Input fails format or length validation |
| `EMAIL_ALREADY_EXISTS` | 409 | Duplicate email on registration |
| `INVALID_CREDENTIALS` | 401 | Wrong email or password on login (generic, no enumeration) |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many login attempts from the same IP |
| `UNAUTHORIZED` | 401 | Missing or expired session cookie |
| `INVALID_OR_EXPIRED_TOKEN` | 400 | Password reset token is invalid, expired, or already used |

---

## Cookie Specification

| Name | Value | Flags | MaxAge |
|------|-------|-------|--------|
| `auth-token` | JWT (HS256, signed with server secret) | HttpOnly, Secure, SameSite=Lax | 604800 s (7 days); 0 on logout |

The cookie `MaxAge` is refreshed on every authenticated request (rolling 7-day window, FR-017).
Secure flag is omitted in local HTTP development (`http://localhost`).
