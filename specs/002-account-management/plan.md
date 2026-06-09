# Implementation Plan: Account Management

**Branch**: `002-account-management` | **Date**: 2026-06-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-account-management/spec.md`

## Summary

Add email/password account management to the Kuky platform. A new Java Spring Boot REST API service backed by PostgreSQL handles registration, login, password reset, session management, and GDPR compliance. The existing React/TanStack SSR frontend gains three auth screens on the `/cuenta` route. Authentication uses JWT tokens stored in HTTP-only cookies (7-day rolling sessions). Passwords are hashed with BCrypt (cost 12). Login is rate-limited per IP with Bucket4j. GDPR consent is captured at registration.

## Technical Context

**Language/Version**: TypeScript 5.x strict (frontend) / Java 21 LTS (backend — required by Spring Boot 3.x)

**Primary Dependencies**:
- Frontend: React 19, TanStack Start 1.x (SSR), TanStack Router, TailwindCSS 4, Shadcn UI, Vite 7
- Backend: Spring Boot 3.x, Spring Security 6, Spring Data JPA, Flyway (DB migrations), JavaMailSender, Bucket4j (rate limiting), jjwt (JWT tokens)

**Storage**: PostgreSQL 16 (Docker Compose for local dev; managed PostgreSQL in production)

**Testing**:
- Frontend: Vitest + React Testing Library
- Backend: JUnit 5 + Spring Boot Test + Testcontainers (integration tests against real PostgreSQL)

**Target Platform**: Browser via Cloudflare Workers (frontend SSR) + JVM server (backend REST API)

**Project Type**: Full-stack web application — SSR React frontend + REST API backend

**Performance Goals**: Registration < 2 min (SC-001), login < 30 s (SC-002), reset email < 2 min (SC-004) — easily achievable with standard Spring Boot response times

**Constraints**:
- Passwords stored as BCrypt hashes (cost factor 12)
- Sessions: JWT in HTTP-only, Secure, SameSite=Lax cookies; 7-day rolling window (FR-017)
- Login rate-limited per IP with Bucket4j in-memory; 10 attempts per 15-minute window (FR-016)
- Email normalized to lowercase before storage and comparison (FR-018)
- GDPR: consent checkbox + privacy policy link at registration; account deletion on manual request (FR-014, FR-015)

**Scale/Scope**: Small — one teacher, dozens to low hundreds of students; single-instance backend deployment

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I — Simplicity First | ✅ PASS | MVP only: registration, login, password reset. No OAuth, MFA, admin panel, or social login. |
| II — Component-Driven UI | ✅ PASS | Auth screens built as named Shadcn-based React components; no raw DOM manipulation. |
| III — Evolution-Ready Architecture | ✅ PASS | All API calls isolated in `front-end/src/lib/auth.ts`; backend under `back-end/`; no auth logic inlined in route components. |
| Technology Stack | ✅ PASS | Frontend stack unchanged; backend pre-selected by project owner (Java 21 + Spring Boot 3 + PostgreSQL). |
| Development Workflow | ✅ PASS | UI changes must be browser-verified; feature branch `002-account-management` follows naming convention. |
| Backend location | ✅ PASS | New service placed under `back-end/` at repo root per constitution. |

**Gate result**: All principles satisfied. No complexity violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/002-account-management/
├── plan.md              # This file
├── research.md          # Phase 0 — technology decisions and rationale
├── data-model.md        # Phase 1 — entity definitions and DB schema
├── quickstart.md        # Phase 1 — local validation guide
├── contracts/
│   └── api.md           # Phase 1 — REST API contract
└── tasks.md             # Phase 2 — task list (/speckit-tasks output, not yet created)
```

### Source Code

```text
kuky/
├── docker-compose.yml               # Local dev: PostgreSQL 16 + Mailpit SMTP
├── front-end/                       # Existing React/TanStack SSR app
│   └── src/
│       ├── routes/
│       │   └── cuenta.tsx           # Updated: tabbed views (login / register / reset)
│       ├── components/
│       │   └── auth/
│       │       ├── LoginForm.tsx
│       │       ├── RegistrationForm.tsx
│       │       └── PasswordResetForm.tsx
│       └── lib/
│           └── auth.ts              # Auth service module (fetch wrappers, session helpers)
└── back-end/                        # New Spring Boot REST API service
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/dev/kuky/
        │   │   ├── KukyApplication.java
        │   │   ├── auth/
        │   │   │   ├── controller/AuthController.java
        │   │   │   ├── service/AuthService.java
        │   │   │   ├── service/PasswordResetService.java
        │   │   │   ├── service/EmailService.java
        │   │   │   ├── model/User.java
        │   │   │   ├── model/PasswordResetToken.java
        │   │   │   ├── repository/UserRepository.java
        │   │   │   └── repository/PasswordResetTokenRepository.java
        │   │   └── config/
        │   │       ├── SecurityConfig.java
        │   │       ├── JwtConfig.java
        │   │       └── CorsConfig.java
        │   └── resources/
        │       ├── application.yml
        │       ├── application-local.yml
        │       └── db/migration/
        │           ├── V1__create_users.sql
        │           └── V2__create_password_reset_tokens.sql
        └── test/java/dev/kuky/
            └── auth/
                ├── AuthControllerIntegrationTest.java
                └── AuthServiceTest.java
```

**Structure Decision**: Two-service web application — SSR frontend under `front-end/` (existing, unchanged structure), REST API backend under `back-end/` (new). Docker Compose at repo root orchestrates local development infrastructure (PostgreSQL + Mailpit).
