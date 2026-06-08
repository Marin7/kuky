# Tasks: Account Management

**Input**: Design documents from `specs/002-account-management/`

**Prerequisites**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/api.md ✅ quickstart.md ✅

**Tests**: Not included — no TDD requirement in spec. End-to-end validation via quickstart.md (T036).

**Organization**: Tasks grouped by user story (US1 = Registration, US2 = Login, US3 = Password Reset) to enable independent implementation and delivery.

## Format: `[ID] [P?] [Story?] Description — file path`

- **[P]**: Can run in parallel (different files, no incomplete task dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Setup and Foundational phases carry no story label

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialise both services and local dev tooling. No user story work begins here.

- [x] T001 Initialise back-end Gradle project — updated `back-end/build.gradle` with Spring Security, JPA, Mail, Validation, PostgreSQL, Flyway, jjwt 0.12.x, Bucket4j, Spring Security Test dependencies
- [x] T002 [P] Local infrastructure — PostgreSQL 18 via pgAdmin (DB: `kuky_dev`, user: `kuky`, password: `kuky`); Mailpit native binary for SMTP capture (SMTP port 1025, web UI port 8025)
- [x] T003 [P] Create `back-end/src/main/resources/application.yaml` — `server.port: 8081`, `spring.application.name: kuky-backend`, JPA `ddl-auto: validate` + open-in-view false, Flyway `enabled: true`, placeholders for `app.jwt.secret`, `app.jwt.expiry-seconds: 604800`, `app.cors.allowed-origin`, `spring.mail.host` / `spring.mail.port`
- [x] T004 [P] Create `back-end/src/main/resources/application-local.yaml` — `spring.datasource.url: jdbc:postgresql://localhost:5432/kuky_dev`, `spring.datasource.username: kuky`, `spring.datasource.password: kuky`, `spring.mail.host: localhost`, `spring.mail.port: 1025`, `app.jwt.secret: local-dev-secret-at-least-32-chars-long`, `app.cors.allowed-origin: http://localhost:8080`
- [x] T005 Update `back-end/src/main/java/com/kuky/backend/BackEndApplication.java` — added `public` modifier to main method

**Checkpoint**: PostgreSQL running via pgAdmin with `kuky_dev` database created; Mailpit binary running; `./gradlew bootRun --args='--spring.profiles.active=local'` starts the backend on port 8081

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: DB schema, JPA entities, repositories, security config, and frontend auth plumbing that every user story depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T006 Create `back-end/src/main/resources/db/migration/V1__create_users.sql` — `CREATE TABLE users` with columns per `data-model.md`: `id UUID DEFAULT gen_random_uuid() PRIMARY KEY`, `email VARCHAR(255) UNIQUE NOT NULL`, `password_hash VARCHAR(255) NOT NULL`, `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`, `gdpr_consent BOOLEAN NOT NULL DEFAULT false`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`; plus `CREATE INDEX users_email_idx ON users(email)`
- [x] T007 [P] Create `back-end/src/main/resources/db/migration/V2__create_password_reset_tokens.sql` — `CREATE TABLE password_reset_tokens` with columns per `data-model.md`; plus `CREATE INDEX prt_token_idx` and `CREATE INDEX prt_user_id_idx`
- [x] T008 Create `back-end/src/main/java/com/kuky/backend/auth/model/User.java` — `@Entity @Table(name="users")`, fields per `data-model.md`, `@PrePersist`/`@PreUpdate` lifecycle hooks
- [x] T009 [P] Create `back-end/src/main/java/com/kuky/backend/auth/model/PasswordResetToken.java` — `@Entity @Table(name="password_reset_tokens")`, fields per `data-model.md`, `@ManyToOne @JoinColumn(name="user_id")` to `User`
- [x] T010 Create `back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java` — `JpaRepository<User, UUID>` with `findByEmailIgnoreCase` and `existsByEmailIgnoreCase`
- [x] T011 [P] Create `back-end/src/main/java/com/kuky/backend/auth/repository/PasswordResetTokenRepository.java` — `JpaRepository<PasswordResetToken, UUID>` with `findByToken` and `findAllByUserAndUsedFalse`
- [x] T012 [P] Create `back-end/src/main/java/com/kuky/backend/config/JwtConfig.java` — reads `app.jwt.secret` and `app.jwt.expiry-seconds`; methods: `generateToken`, `validateToken`, `extractEmail`, `extractUserId`, `getExpirySeconds`; jjwt 0.12.x API
- [x] T013 [P] Create `back-end/src/main/java/com/kuky/backend/config/CorsConfig.java` — `WebMvcConfigurer` bean with `allowedOrigins`, `allowCredentials(true)`, `maxAge(3600)`
- [x] T014 Create `back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java` — CSRF disabled, stateless session, `BCryptPasswordEncoder` bean (cost 12), JWT filter before `UsernamePasswordAuthenticationFilter`; `JwtCookieAuthenticationFilter.java` reads `auth-token` cookie and sets `SecurityContext`
- [x] T015 [P] Create DTO records in `back-end/src/main/java/com/kuky/backend/auth/dto/` — `RegisterRequest`, `LoginRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `AuthResponse`, `UserResponse` with Jakarta Validation annotations
- [x] T016 [P] Create `front-end/src/lib/auth.ts` — typed async functions for all 6 endpoints; `credentials: 'include'`; throws typed `ApiError`
- [x] T017 No server-side auth loader needed — `cuenta.tsx` manages auth state client-side via `useEffect` + `getMe()`; avoids SSR cookie-forwarding complexity in Cloudflare Workers

**Checkpoint**: Backend starts cleanly (Flyway runs V1 + V2 migrations), `GET /api/v1/auth/me` returns 401, `front-end/src/lib/auth.ts` compiles without errors

---

## Phase 3: User Story 1 — Account Registration (Priority: P1) 🎯 MVP

**Goal**: A visitor fills out a registration form (email, password, GDPR consent), submits it, and is automatically signed in. Duplicate emails, weak passwords, and missing consent are rejected with inline errors.

**Independent Test**: Navigate to `/cuenta`, fill the registration form with a unique email and 8+ char password with consent checked, submit — verify `auth-token` HttpOnly cookie is set. See quickstart.md Scenario 1.

- [x] T018 Create `back-end/src/main/java/com/kuky/backend/auth/service/AuthService.java` — `register()`: normalize email lowercase, check duplicate via `existsByEmailIgnoreCase`, BCrypt hash, save `User`, return `AuthResponse`
- [x] T019 [P] Add `POST /register` to `back-end/src/main/java/com/kuky/backend/auth/controller/AuthController.java` — `@Valid` request body, set `auth-token` HttpOnly SameSite=Lax cookie `maxAge(604800)`, return 201
- [x] T020 [P] [US1] Create `front-end/src/components/auth/RegistrationForm.tsx` — email, password, confirm-password, GDPR consent checkbox (links to `/politica-privacidad`); Spanish labels and inline errors; calls `register()` from `auth.ts`; Shadcn `Input`, `Button`, `Checkbox`, `Label`
- [x] T021 [US1] Update `front-end/src/routes/cuenta.tsx` — Shadcn `Tabs` default to `register` tab; render `RegistrationForm`; on success refresh auth state

**Checkpoint**: Registration form submits, account is created in DB, `auth-token` cookie is set — verify in browser per quickstart.md Scenario 1

---

## Phase 4: User Story 2 — User Login (Priority: P1)

**Goal**: A registered user enters their email and password, is authenticated, and has a 7-day rolling session maintained via an HttpOnly cookie. They can log out to end the session.

**Independent Test**: Register an account, log out, navigate back to `/cuenta`, log in — verify `auth-token` cookie is present with MaxAge ~604800. See quickstart.md Scenarios 2 and 3.

- [x] T022 Create `back-end/src/main/java/com/kuky/backend/auth/service/LoginRateLimiter.java` — Bucket4j `ConcurrentHashMap<String, Bucket>`, capacity 10, refill greedy 10/15min; `tryConsume(ip)` returns boolean
- [x] T023 Add `login()` to `back-end/src/main/java/com/kuky/backend/auth/service/AuthService.java` — find user by email case-insensitive, BCrypt verify, throw generic `AuthException` on any failure
- [x] T024 Add `POST /login`, `POST /logout`, `GET /me` to `back-end/src/main/java/com/kuky/backend/auth/controller/AuthController.java` — login: IP rate limit check, set cookie, return 200; logout: maxAge(0); me: read cookie, validate JWT, return `UserResponse` or 401
- [x] T025 [P] [US2] Create `front-end/src/components/auth/LoginForm.tsx` — email + password; Spanish errors for `INVALID_CREDENTIALS` and `RATE_LIMIT_EXCEEDED`; "¿Olvidaste tu contraseña?" button calls `onForgotPassword` prop; Shadcn `Input`, `Button`
- [x] T026 [US2] Update `front-end/src/routes/cuenta.tsx` — add `login` tab; show authenticated dashboard with logout button when `getMe()` succeeds; on logout call `apiLogout()` and reset state

**Checkpoint**: Login, session persistence, and logout work in browser per quickstart.md Scenarios 2 and 3

---

## Phase 5: User Story 3 — Password Reset (Priority: P2)

**Goal**: A user who cannot log in requests a reset link by email. Mailpit captures the email locally. Following the link opens a form to set a new password. The link is single-use and expires in 1 hour.

**Independent Test**: Request a reset for a registered email, open Mailpit at http://localhost:8025, follow the reset link, set a new password, verify the old password no longer works. See quickstart.md Scenario 4.

- [x] T027 [P] [US3] Create `back-end/src/main/java/com/kuky/backend/auth/service/EmailService.java` — `JavaMailSender` wrapper; `sendPasswordResetEmail(toEmail, resetToken)` builds `{baseUrl}/cuenta?token={token}` URL; sends `SimpleMailMessage`
- [x] T028 [P] [US3] Create `back-end/src/main/java/com/kuky/backend/auth/service/PasswordResetService.java` — `requestReset()`: silent no-op if email not found (FR-010), invalidate existing tokens, save new UUID token with 1hr expiry, send email; `consumeToken()`: validate token (not found/used/expired → `InvalidTokenException`), hash new password, save, mark token used, invalidate remaining
- [x] T029 [US3] Add `POST /forgot-password` and `POST /reset-password` to `back-end/src/main/java/com/kuky/backend/auth/controller/AuthController.java` — forgot-password always returns 200; reset-password sets cookie on success
- [x] T030 [P] [US3] Create `front-end/src/components/auth/PasswordResetForm.tsx` — two modes: request mode (no token, submits `forgotPassword()`, shows neutral confirmation) and reset mode (token present, submits `resetPassword()`, handles `INVALID_OR_EXPIRED_TOKEN`); Spanish labels/errors
- [x] T031 [US3] Update `front-end/src/routes/cuenta.tsx` — read `?token=` URL search param via `validateSearch`; if present, show `PasswordResetForm` in reset mode; "¿Olvidaste tu contraseña?" in login tab switches to forgot-password view

**Checkpoint**: Full password reset flow works in browser including Mailpit email inspection per quickstart.md Scenario 4

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Harden error handling, add rolling session refresh, update project docs, and run full end-to-end validation.

- [x] T032 [P] Create custom exception classes in `back-end/src/main/java/com/kuky/backend/auth/exception/` — `DuplicateEmailException`, `AuthException`, `InvalidTokenException`, `RateLimitException` (each extends `RuntimeException`)
- [x] T033 Create `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java` — `@RestControllerAdvice`; map exceptions to HTTP status codes per `contracts/api.md`
- [x] T034 [P] Rolling session refresh implemented in `JwtCookieAuthenticationFilter.java` — after successful JWT validation, sets new `auth-token` cookie with `maxAge(604800)` reset on every authenticated request (FR-017)
- [x] T035 [P] Update `CLAUDE.md` — added `back-end/` subtree and `docker-compose.yml` to Project Structure; updated `/cuenta` status to Live
- [ ] T036 Run all 6 quickstart.md validation scenarios in a browser — Scenario 1 (Registration), Scenario 2 (Login), Scenario 3 (Session expiry), Scenario 4 (Password reset + Mailpit), Scenario 5 (Rate limiting), Scenario 6 (GDPR consent); fix any integration issues; mark complete only after all scenarios pass visually

**Checkpoint**: All 3 user stories pass end-to-end browser validation; backend error responses match `contracts/api.md`; CLAUDE.md reflects current repo state

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **BLOCKS all user story phases**
- **Phase 3 (US1 Registration)**: Depends on Phase 2 ← MVP stopping point
- **Phase 4 (US2 Login)**: Depends on Phase 2 (and practically on Phase 3 since AuthService is extended)
- **Phase 5 (US3 Password Reset)**: Depends on Phase 2 (and practically on Phase 4 since AuthController is extended)
- **Phase 6 (Polish)**: Depends on Phases 3–5

### User Story Dependencies

- **US1 (Registration, P1)**: Starts after Phase 2 — foundation only
- **US2 (Login, P1)**: Starts after Phase 2; extends AuthService/AuthController from US1
- **US3 (Password Reset, P2)**: Starts after Phase 2; adds new services, extends AuthController from US2

---

## Notes

- `[P]` tasks = different files, no blocking incomplete dependencies in the same phase
- `[Story]` label maps each task to its user story for traceability against spec.md
- Each phase ends with an explicit browser-verification checkpoint
- Do not mark a task complete without running the relevant quickstart.md scenario
- Tests are not included; quickstart.md Scenario 1–6 serve as the acceptance test suite
- All Spanish-language strings in frontend components must match the error codes defined in `contracts/api.md`
- `auth-token` cookie must have `HttpOnly` flag set — verify in browser DevTools → Application → Cookies
