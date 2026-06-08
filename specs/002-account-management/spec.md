# Feature Specification: Account Management

**Feature Branch**: `002-account-management`

**Created**: 2026-06-08

**Status**: Draft

**Input**: User description: "The current application is just a presentation layer with no logic. I want to add an Account Management. The back-end part will be a Java Spring Boot service using a PostgresDB where the user data will be stored. For localhost, use Docker for generating a Postgres image. The account management will just allow users to create accounts using an email + password and a forgot password functionality."

## Clarifications

### Session 2026-06-08

- Q: Does the feature need to comply with GDPR for EU/Romanian users? → A: Apply GDPR basics — consent checkbox at registration linking to a privacy policy, and account deletion available on request via email to the platform owner (not self-serve UI).
- Q: How should the system protect against brute-force login attacks? → A: Rate-limit login requests with progressively delayed responses after repeated failures; no account lockout applied.
- Q: How long should an authenticated session last? → A: 7-day rolling session — resets on each authenticated visit; expires after 7 days of inactivity.
- Q: What language should the account management UI use? → A: Spanish for v1 (consistent with the rest of the site); multi-language support is deferred to a future phase.
- Q: Should email address comparison be case-sensitive or case-insensitive? → A: Case-insensitive — email addresses are normalized to lowercase on storage and compared case-insensitively on login and password reset.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Account Registration (Priority: P1)

A visitor to the site wants to create a personal account so they can access future student features. They provide their email address and a password of their choice, and the system creates their account and signs them in automatically.

**Why this priority**: Registration is the entry point for all account-related functionality — nothing else works without it. Every other story depends on having a registered user.

**Independent Test**: Can be fully tested by filling out the registration form with a valid email and password, submitting it, and verifying the user is created and signed in. Delivers immediate value: a visitor can now have a persistent identity on the platform.

**Acceptance Scenarios**:

1. **Given** a visitor is not logged in, **When** they submit a registration form with a valid, unique email and a password that meets the minimum requirements, **Then** their account is created, they are signed in automatically, and they see a confirmation.
2. **Given** a visitor submits a registration form with an email already associated with an existing account, **When** they submit, **Then** they see a clear error indicating the email is already in use.
3. **Given** a visitor submits a registration form with a malformed email address, **When** they submit, **Then** they see a validation error explaining the email format is invalid.
4. **Given** a visitor submits a registration form with a password that does not meet the minimum strength requirement, **When** they submit, **Then** they see a clear description of the password requirements.
5. **Given** a visitor submits a registration form where the password and confirmation do not match, **When** they submit, **Then** they see an error indicating the passwords must match.
6. **Given** a visitor submits the registration form without checking the privacy policy consent checkbox, **When** they submit, **Then** they see a validation error and the account is not created.

---

### User Story 2 - User Login (Priority: P1)

A registered user wants to access the platform again after their first visit. They enter their email and password to authenticate and regain access to their account.

**Why this priority**: Without a login flow, a registered account has no ongoing value — users could only ever use it once. Login is co-foundational with registration.

**Independent Test**: Can be fully tested by registering an account, logging out, then logging back in with the same credentials. Delivers value: a user can maintain a persistent session across visits.

**Acceptance Scenarios**:

1. **Given** a registered user is not logged in, **When** they submit the login form with their correct email and password, **Then** they are authenticated and redirected to the main area of the site.
2. **Given** a registered user submits the login form with an incorrect password, **When** they submit, **Then** they see a generic authentication error (not specifying whether the email or password is wrong).
3. **Given** a visitor submits the login form with an email that does not exist, **When** they submit, **Then** they see the same generic authentication error (no user enumeration).
4. **Given** a logged-in user chooses to log out, **When** they confirm, **Then** their session ends and they are returned to the public landing page.
5. **Given** a user's session has been inactive for more than 7 days, **When** they attempt to access a protected page, **Then** their session is expired, they are redirected to the login page, and they see a message explaining they need to log in again.

---

### User Story 3 - Password Reset (Priority: P2)

A registered user has forgotten their password and cannot log in. They request a password reset link to be sent to their registered email address, then use that link to set a new password and regain access.

**Why this priority**: Password recovery is a critical trust feature — without it, a forgotten password permanently locks a user out. It is secondary to registration and login but essential for any real-world use.

**Independent Test**: Can be fully tested independently of other stories by having a registered account, navigating to "Forgot password", entering the registered email, receiving the reset link, and completing the password change.

**Acceptance Scenarios**:

1. **Given** a user is not logged in and has forgotten their password, **When** they request a reset using their registered email address, **Then** they receive a password reset link at that address and see a confirmation message.
2. **Given** a user requests a reset for an email address not associated with any account, **When** they submit, **Then** they see the same neutral confirmation message (no user enumeration).
3. **Given** a user follows a valid, unexpired password reset link, **When** they set a new password that meets requirements, **Then** their password is updated and they are signed in.
4. **Given** a user attempts to use an expired or already-used password reset link, **When** they try to submit, **Then** they see an error and are prompted to request a new reset link.
5. **Given** a user follows a valid reset link but enters a new password that does not meet requirements, **When** they submit, **Then** they see a clear description of the password requirements.

---

### Edge Cases

- What happens if a user requests multiple password reset links before using the first one? (Only the most recent link should be valid.)
- What happens if the email delivery service is temporarily unavailable during a password reset request?
- What happens if a user tries to access the registration or login page while already logged in? (They should be redirected to the main area.)
- What happens after several consecutive failed login attempts from the same source? (Login requests are rate-limited with progressively delayed responses; the account is never locked out.)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow visitors to register a new account using a unique email address and a password.
- **FR-002**: System MUST validate that submitted email addresses conform to a standard email format.
- **FR-003**: System MUST enforce a minimum password strength (e.g., minimum length; exact rules documented in Assumptions).
- **FR-004**: System MUST reject registration attempts using an email address already associated with an existing account.
- **FR-005**: System MUST never store passwords in plain text; all passwords must be stored in a secure, irreversible hashed form.
- **FR-006**: System MUST authenticate users by verifying their submitted email and password against stored credentials.
- **FR-007**: System MUST respond to failed login attempts with a generic error message that does not reveal whether the email or password was incorrect.
- **FR-008**: System MUST allow authenticated users to end their session (log out).
- **FR-009**: System MUST allow users to request a password reset link delivered to their registered email address.
- **FR-010**: System MUST respond to password reset requests with a neutral confirmation message regardless of whether the email is registered.
- **FR-011**: Password reset links MUST be single-use and MUST expire after a defined time window.
- **FR-012**: System MUST allow users to set a new password by submitting a valid, unexpired reset link together with the new password.
- **FR-013**: System MUST invalidate all other pending reset links for an account once a new password is set.
- **FR-014**: System MUST display a consent checkbox at registration that links to the privacy policy; users MUST actively check this box before the account can be created.
- **FR-015**: User accounts MUST be deletable on request to comply with GDPR right-to-erasure; for v1, deletion is fulfilled manually by the platform owner — no self-serve deletion UI is required.
- **FR-016**: System MUST rate-limit login attempts from the same source; repeated failed attempts MUST result in progressively delayed responses. No account lockout is applied.
- **FR-017**: Authenticated sessions MUST persist for up to 7 days; the session window MUST reset on each authenticated visit. Sessions MUST expire automatically after 7 consecutive days of inactivity, requiring the user to log in again.
- **FR-018**: Email addresses MUST be normalized to lowercase before storage; all lookups (login, password reset, duplicate detection) MUST compare emails case-insensitively.

### Key Entities

- **User Account**: Represents a registered person on the platform. Key attributes: unique email address (stored normalized to lowercase), securely hashed password, account status (active/inactive), and registration timestamp.
- **Password Reset Token**: Represents a pending password reset request. Key attributes: unique token value, expiration timestamp, usage status (used/unused), and association with a user account.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new visitor can complete account registration in under 2 minutes from navigating to the registration page.
- **SC-002**: A returning user can log in in under 30 seconds.
- **SC-003**: A user can complete the full password reset flow — from requesting the link to setting a new password — in under 5 minutes.
- **SC-004**: Password reset emails arrive in the user's inbox within 2 minutes of the request under normal conditions.
- **SC-005**: All form validation errors are shown inline and are understood without external help by 95% of users attempting the flows.
- **SC-006**: No account-related security incidents (user enumeration, credential leakage) occur as a result of the implementation.

## Assumptions

- Email verification (confirming email ownership via a verification link after registration) is **out of scope** for this version. Users are trusted to provide their own valid email.
- Social login / OAuth2 (Google, Facebook, etc.) is **out of scope** for this version.
- Multi-factor authentication (MFA/2FA) is **out of scope** for this version.
- Self-serve account management features beyond registration, login, and password reset (profile editing, email change) are **out of scope** for this version. Account deletion is available on request per GDPR but handled manually by the platform owner — no self-serve deletion UI is required for v1.
- Minimum password requirements are assumed to be: at least 8 characters. Additional complexity rules (uppercase, numbers, symbols) are deferred to the planning phase.
- Password reset links are assumed to expire after 1 hour. This can be adjusted during planning.
- The backend service and primary database technology have been pre-selected by the project owner (noted in the feature description).
- A working email delivery service will be available in the target environment for sending password reset emails. The specific provider is to be determined during planning.
- The local development environment will use Docker to run the database, matching the production database engine.
- The backend service will live under `back-end/` at the repository root, consistent with the project constitution.
- The front-end `/cuenta` route (currently a placeholder) will be updated to expose the registration, login, and password reset flows.
- A privacy policy document must exist and be publicly reachable via URL before the registration form goes live. Authoring the privacy policy content is **out of scope** for this feature.
- All account management UI copy (labels, instructions, error messages, validation text) is in **Spanish** for v1, consistent with the rest of the site. Multi-language support (e.g., Romanian) is **out of scope** for this version and deferred to a future phase.
