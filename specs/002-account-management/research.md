# Research: Account Management

**Feature**: `002-account-management` | **Date**: 2026-06-08

---

## Decision 1: Session / Token Mechanism

**Decision**: JWT tokens issued by the backend, stored in HTTP-only cookies on the browser client.

**Rationale**: HTTP-only cookies prevent JavaScript access (XSS protection). SameSite=Lax mitigates CSRF for same-site navigation, which is sufficient for this application. Cookies are sent automatically on SSR requests — TanStack Start's server-side loaders can read them without extra client setup. The 7-day rolling window is implemented by refreshing the cookie's `MaxAge` on each authenticated request. Stateless JWT avoids the need for a session store (Redis/DB), keeping the backend simple per Principle I.

**Alternatives considered**:
- Server-side sessions + session ID cookie: requires session storage (DB or Redis), adds infrastructure complexity. Rejected — Principle I, no concrete need at this scale.
- JWT in Authorization header + localStorage: localStorage is vulnerable to XSS and doesn't work in SSR loaders without extra plumbing. Rejected.
- Refresh token rotation: adds complexity (two tokens, rotation logic) without meaningful benefit at this scale. Deferred to a future phase if needed.

---

## Decision 2: Password Hashing

**Decision**: BCrypt with cost factor 12 via Spring Security's `BCryptPasswordEncoder`.

**Rationale**: BCrypt is the Spring Security standard, well-understood, and resists brute-force attacks with configurable cost. Cost 12 is the current industry default, balancing security and latency (~300 ms on modern hardware — acceptable for a login endpoint).

**Alternatives considered**:
- Argon2id: more memory-hard, slightly better security profile. Spring Security has `Argon2PasswordEncoder` but BCrypt is the more established default for Spring Boot projects. Deferred — BCrypt at cost 12 is sufficient for this scale.
- SHA-256/MD5: completely unsuitable for password storage. Rejected.

---

## Decision 3: Password Reset Token

**Decision**: UUID v4 (cryptographically random, 122 bits of entropy), stored in the `password_reset_tokens` table with an expiry timestamp. Tokens are single-use: invalidated on redemption and when a new token is generated for the same account.

**Rationale**: UUID v4 provides sufficient entropy for this use case. DB-stored tokens enable server-side invalidation (single-use enforcement, expiry checks, full per-user revocation). No HMAC-signed token is needed — a DB lookup is fast at this scale and keeps the logic simple.

**Alternatives considered**:
- HMAC-based signed tokens (e.g., signed with a server secret): eliminates the DB lookup, but makes server-side revocation (e.g., invalidate all tokens for a user) impossible without a denylist. Rejected — revocation is a spec requirement (FR-013).
- Short numeric codes (6-digit): lower entropy, worse UX (users must copy/type the code). Rejected.

---

## Decision 4: Rate Limiting

**Decision**: Bucket4j with Caffeine-backed in-memory storage, keyed by remote IP address. Configuration: 10 login attempts per 15-minute window per IP; requests beyond the limit return HTTP 429.

**Rationale**: Bucket4j is the de-facto Spring Boot rate limiting library, well-documented and lightweight. In-memory storage (Caffeine) is sufficient for a single-instance deployment. No Redis or external dependency is introduced.

**Alternatives considered**:
- Spring Cloud Gateway rate limiter: requires a gateway infrastructure component. Overkill for this deployment size. Rejected.
- Manual `ConcurrentHashMap` counter: reinventing the wheel with worse semantics. Rejected.
- Infrastructure-level (AWS WAF / Cloudflare): complementary in production but not portable to local development; cannot be the sole mechanism. Noted for production hardening.

---

## Decision 5: Email Delivery

**Decision**: Spring Mail (`JavaMailSender`) with configurable SMTP credentials in `application.yml`. Local development uses Mailpit (Docker container, port 1025 SMTP + port 8025 web UI) as an SMTP sink.

**Rationale**: Spring Mail is the standard Spring Boot email integration with no external SDK dependencies. Mailpit is a lightweight local SMTP trap that captures all outgoing emails in a browser-accessible UI, enabling testing without real email delivery. Production deployment can use any SMTP provider (SendGrid, Amazon SES, Gmail SMTP) with zero code changes — only configuration changes.

**Alternatives considered**:
- Vendor SDK (SendGrid, Mailgun): introduces vendor lock-in at the library level. SMTP abstraction avoids this. Rejected for library dependency; SMTP provider is a deployment concern.
- GreenMail (for tests): a test-scope in-JVM SMTP server — useful for unit/integration tests but not for local dev workflow. Complementary, not a replacement for Mailpit.

---

## Decision 6: CORS Configuration

**Decision**: Spring Boot global `WebMvcConfigurer` CORS configuration. Allowed origins configured via environment variable (default: `http://localhost:8080` for dev). `allowCredentials = true` is required for cookie-based auth.

**Rationale**: Credentials (cookies) require explicit `allowCredentials` and a non-wildcard origin. Global config via `WebMvcConfigurer` is cleaner and easier to maintain than per-controller `@CrossOrigin` annotations. The allowed origin for production is injected via an environment variable to avoid hardcoding.

---

## Decision 7: Database Migrations

**Decision**: Flyway for versioned SQL migrations under `src/main/resources/db/migration/`.

**Rationale**: Flyway integrates natively with Spring Boot (auto-runs on startup), uses plain SQL (no ORM DSL to learn), and is the standard for Spring Boot projects. Migration files are versioned and repeatable.

**Alternatives considered**:
- Liquibase: more XML/YAML ceremony for a simple two-table schema. Rejected for this scale.
- Manual DDL scripts: no version tracking, error-prone. Rejected.

---

## Decision 8: Frontend Auth State (TanStack Start SSR)

**Decision**: Auth state flows from backend cookie → TanStack Start server-side loader → React context. Route protection uses TanStack Router `beforeLoad` guards that redirect unauthenticated users to the `/cuenta` login view.

**Implementation pattern**:
1. Backend sets `auth-token` HTTP-only cookie on successful login/registration.
2. TanStack Start root loader (`__root.tsx`) calls `GET /api/v1/auth/me` on every server render — the cookie is sent automatically by the browser/SSR runtime.
3. The current user object is passed as router context and is available to all route loaders and components.
4. Client-side mutations (login, logout, register) call the backend via `src/lib/auth.ts` and call `router.invalidate()` to trigger a fresh server render with the updated cookie.

**Rationale**: This pattern avoids SSR/client hydration mismatches (server and client derive auth state from the same cookie), integrates naturally with TanStack Start's server rendering model, and keeps all auth logic out of components (Principle III — Evolution-Ready Architecture).
