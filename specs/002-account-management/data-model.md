# Data Model: Account Management

**Feature**: `002-account-management` | **Date**: 2026-06-08

---

## Entities

### User

Represents a registered platform user.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, not null | Generated (UUID v4) |
| email | VARCHAR(255) | unique, not null | Normalized to lowercase before storage (FR-018) |
| password_hash | VARCHAR(255) | not null | BCrypt hash, cost factor 12; never plain text (FR-005) |
| status | VARCHAR(20) | not null, default 'ACTIVE' | Allowed values: ACTIVE, INACTIVE |
| gdpr_consent | BOOLEAN | not null, default false | Must be true at registration (FR-014) |
| created_at | TIMESTAMPTZ | not null, default NOW() | Set once on insert |
| updated_at | TIMESTAMPTZ | not null, default NOW() | Updated on any field change |

**Uniqueness**: `email` is unique across all users (case-insensitive via normalization, FR-004, FR-018).

**State transitions**:
```
ACTIVE ──► INACTIVE   (manual deletion request per GDPR, FR-015 — no self-serve UI)
```
No reverse transition is defined for v1.

---

### PasswordResetToken

Represents a pending password reset request issued to a user.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, not null | Generated (UUID v4) |
| user_id | UUID | FK → users.id, not null | Cascade delete if user is deleted |
| token | VARCHAR(36) | unique, not null | UUID v4 (122 bits of entropy) — the URL-safe reset token |
| expires_at | TIMESTAMPTZ | not null | NOW() + 1 hour at creation (Assumption) |
| used | BOOLEAN | not null, default false | Set to true on successful redemption |
| created_at | TIMESTAMPTZ | not null, default NOW() | |

**Lifecycle**:
```
PENDING (used=false, expires_at in future)
  ──► REDEEMED (used=true)         on successful password change
  ──► EXPIRED  (expires_at passed)  passively; rejected on use attempt
  ──► INVALIDATED                   all PENDING tokens for a user are marked used=true
                                    when a new password is set (FR-013) or when a new
                                    reset token is issued for the same account
```

---

## Relationships

```
users (1) ──────────────< password_reset_tokens (N)
  users.id = password_reset_tokens.user_id
```

One user may have multiple password reset tokens over time (e.g., multiple requests before use), but only the most recent unexpired, unused token is valid at any given time (enforced by application logic, not DB constraint).

---

## Indexes

| Table | Index Name | Columns | Reason |
|-------|------------|---------|--------|
| users | `users_email_idx` | email | Fast lookup on login, registration duplicate check |
| password_reset_tokens | `prt_token_idx` | token | Fast token lookup on reset-password submission |
| password_reset_tokens | `prt_user_id_idx` | user_id | Fast batch-invalidation of all tokens for a user |

---

## Validation Rules

| Entity | Field | Rule | Source |
|--------|-------|------|--------|
| User | email | Must match standard email format; normalized to lowercase | FR-002, FR-018 |
| User | password | Minimum 8 characters (plain-text input only; stored as hash) | FR-003, Assumption |
| User | gdpr_consent | Must be `true`; registration is rejected if false or absent | FR-014 |
| PasswordResetToken | expires_at | Must be in the future when validated; reject if past | FR-011 |
| PasswordResetToken | used | Once `true`, token cannot be reused | FR-011 |

---

## Migration Files

```text
back-end/src/main/resources/db/migration/
├── V1__create_users.sql                  — users table + email index
└── V2__create_password_reset_tokens.sql  — password_reset_tokens table + indexes
```
