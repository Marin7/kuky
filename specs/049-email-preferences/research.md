# Phase 0 Research: Email Preferences

**Feature**: `049-email-preferences` | **Date**: 2026-09-23 | **Spec**: [spec.md](./spec.md)

All Technical Context unknowns are resolved below. No `NEEDS CLARIFICATION` remains.

---

## R1 — Where the preference is stored

**Decision**: A single boolean column on `users`: `email_on_homework_assigned BOOLEAN NOT NULL DEFAULT false` (Flyway `V26`).

**Rationale**:

- `NOT NULL DEFAULT false` makes FR-002 structural rather than procedural — every existing row is off the moment the migration runs, and there is no backfill step that could get it wrong.
- It matches the established precedent on this exact table: `extended_class_eligible`, `timezone_is_manual`, and `gdpr_consent` are all per-user boolean flags stored as columns on `users`.
- Constitution Principle I (Simplicity First, "YAGNI is non-negotiable") forbids building for the second and third options while only one exists.
- Recipient selection becomes a single indexed query with no join.

**Alternatives considered**:

- **Key/value table `user_email_preferences (user_id, event_type, enabled)`** — rejected. Its only real advantage is avoiding a migration when option #2 arrives, and migrations are not a meaningful cost in this repo (V24 and V25 are both single-column feature migrations). It buys extensibility that FR-003 does not actually ask for: FR-003 constrains *presentation*, and the server-driven option list in R6 delivers that with either storage shape.
- **JSONB column `email_preferences`** — rejected. No column-level constraint, awkward to filter on when selecting recipients, and no type safety.

**Revisit trigger**: when a **third** email option is specified, migrate to the key/value table. Two columns is still simpler than a table; three is the point where the mapping boilerplate outweighs it.

---

## R2 — Detecting what is genuinely "newly assigned"

**Decision**: Change `HomeworkTargetRepository.addTargets(...)` to return the user IDs it actually inserted, by switching its existing statement to `INSERT ... ON CONFLICT (assignment_id, user_id) DO NOTHING RETURNING user_id`. Callers that do not care ignore the return value.

**Rationale**:

- FR-013 requires that a re-saved assignment, or a student re-added to a unit they already hold, sends nothing. `ON CONFLICT DO NOTHING` already encodes exactly that predicate — `RETURNING` just surfaces the answer the database already computed.
- It is race-safe with no read-then-write window: two concurrent assignments of the same homework to the same student cannot both report an insert.
- `replaceTargets(...)` delegates to `addTargets(...)`, so it inherits the behaviour with no separate change.
- All four grant call sites already funnel through this one method, so this is the single seam that covers FR-007 a, b and c without touching assignment logic.

The four call sites and the spec clause each satisfies:

| Call site | Spec clause |
|---|---|
| `UnitService.setAssignees` (student added to a unit) | FR-007a |
| `UnitService.setHomeworks` (homework added to a unit students already hold) | FR-007b |
| `HomeworkAdminService.create` (direct assign on create) | FR-007c |
| `HomeworkAdminService.setAssignees` (direct assign, editing the assignee list) | FR-007c |

**Alternatives considered**:

- **Read existing targets and diff in Java** — rejected: extra queries, and a genuine race window between the read and the insert.
- **Database trigger writing to an outbox** — rejected: invisible to readers of the service code, harder to test, and far past what one email option justifies.
- **Comparing affected-row counts** — rejected: tells you *how many* were new, never *which*, and the email must name the student.

---

## R3 — Grouping into one email per teacher action

**Decision**: Accumulate newly-granted grants in a plain per-call collector (`Map<UUID studentId, List<UUID> assignmentId>`, insertion-ordered) built up inside the service method, then flush it once by calling `HomeworkAssignmentEmailService.notifyNewlyAssigned(grants)` as the last step of that method.

**Rationale**:

- The spec defines the grouping unit as the teacher action, and explicitly rules out time-window batching (Assumptions). One HTTP request *is* one teacher action, so request-scoped accumulation is the literal reading.
- Only two service methods per service need to hold a collector, and each already loops over the homework it is granting.
- Purely in-method state means no framework machinery and no lifecycle questions — it is directly unit-testable with a mocked email service.

**Alternatives considered**:

- **Spring `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)`** — rejected under Principle I. It is the right shape at a dozen call sites; at four it adds an indirection that hides the ordering for no gain. (An earlier draft of this decision also claimed the event listener would fire per repository call because the service methods were not a single transaction. That was **wrong**: both `UnitService` and `HomeworkAdminService` are class-level `@Transactional`, so the repository calls join one service-level transaction. The YAGNI argument stands on its own; the transaction claim did not, and R4 below is corrected accordingly.)
- **Scheduled digest / outbox table** — rejected: explicitly out of scope, and SC-003's 5-minute budget does not require it.

---

## R4 — When the email is sent, and isolating failure

**Decision**: Defer the send to **after the surrounding transaction commits**, then send synchronously with each recipient's send wrapped in a `try/catch` that logs at WARN and swallows.

`HomeworkAssignmentEmailService.notifyNewlyAssigned` registers a `TransactionSynchronization` whose `afterCommit` does the work when a transaction is active, and sends immediately when none is (unit tests). Callers just call it; correctness does not depend on each call site remembering.

**Rationale**:

- **Both `UnitService` and `HomeworkAdminService` are class-level `@Transactional`**, so a service method is one transaction and the writes are *not* yet committed when the method body ends. Sending inline would email students about homework a rollback then discards — a message that cannot be recalled, about work that never existed.
- Swallowing per recipient satisfies FR-012: a send that throws cannot propagate into the teacher's request, and after-commit means it cannot roll anything back either.
- Log-and-swallow is the established convention here: `BookingEmailService.sendQuietly` and `TestimonialEmailService.sendQuietly` both do exactly this.
- Volumes are a handful of students per action; `EmailService` is already a no-op when `app.mail.enabled=false`, which keeps local dev and tests quiet for free.

**Known trade-off, accepted**: a crash between commit and send leaves homework assigned with no email ever sent. That is consistent with FR-012 and the spec's "best-effort" assumption — the site, not the inbox, is the source of truth.

**Correction**: an earlier draft of this decision asserted the service methods were "not a single transaction" and sent inline on that basis. Reading the classes during implementation showed both carry a class-level `@Transactional`, which makes after-commit deferral necessary rather than optional. Verified end to end afterwards (see quickstart S9 evidence).

**Alternatives considered**:

- **`@Async` or an outbox with retry** — rejected as premature. No requirement asks for delivery guarantees, and SC-003 allows 5 minutes against a send that completes in well under a second.

---

## R5 — Endpoint placement and authorisation

**Decision**: A new controller at `/api/v1/me/email-preferences`, with an explicit `.requestMatchers("/api/v1/me/**").authenticated()` entry in `SecurityConfig`. The acting user is resolved from the JWT principal; no user ID ever appears in the path or body.

**Rationale**:

- **This must not live under `/api/v1/auth/**`**, which is `permitAll()` in `SecurityConfig:47`. Mounting it there would expose a signed-in user's preferences to anonymous callers.
- Deriving the user from the principal makes FR-005 (only that student can see or change it) and FR-015 (the teacher cannot) structural — there is no parameter an admin could supply to reach another account.
- `anyRequest().authenticated()` would already cover the path; the explicit matcher is added for the same reason `/api/v1/notifications/**` has one — so the intent is legible in the security config rather than implied.

**Role gate**: authenticated, not `hasAnyRole("STUDENT","ADMIN")`. A `USER` who is later promoted keeps whatever they set (spec Assumptions: the preference survives student access being revoked and restored), and the email can only ever fire on homework assignment, which non-students never receive. Gating the endpoint would add a failure mode with no protective value.

---

## R6 — Keeping the option list extensible (FR-003)

**Decision**: `GET /api/v1/me/email-preferences` returns an array of every supported option with its effective value, driven by a Java enum `EmailPreferenceType` (one constant today: `NEW_HOMEWORK_ASSIGNED`). The frontend renders one toggle per returned entry and looks its label up by key.

**Rationale**: adding a future option becomes a backend enum constant plus three translation keys, with no change to the page's structure or layout — which is precisely what FR-003 asks for. It also keeps the client from hardcoding a list that can drift from what the server actually honours.

---

## R7 — Where the UI lives

**Decision**: A new named component `EmailPreferencesSection`, rendered inside the existing `ProfileView` in `front-end/src/routes/cuenta.tsx` (the panel shown at `cuenta.tsx:138` when `user` is set). Data access goes in a new `front-end/src/lib/emailPreferences.ts`, modelled on `lib/notifications.ts`.

**Rationale**:

- `/cuenta` is the only signed-in account surface that exists today; `ProfileView` is already where the account's own settings (name, username, avatar) are edited. FR-001 asks for "their own account area", and this is it — no new route needed.
- Constitution Principle II requires a named component even for single use; Principle III requires the fetch layer to sit in a service module rather than inline in the component, which `lib/emailPreferences.ts` satisfies and which makes it portable if the section later moves to its own route.

**Alternatives considered**: a dedicated `/preferencias` route — rejected as unnecessary surface for one toggle (Principle I). The section is self-contained, so promoting it later is a move, not a rewrite.

---

## R8 — Email content and language

**Decision**: Plain-text `SimpleMailMessage` in Spanish, added as `sendNewHomeworkAssignedEmail(...)` on a new `HomeworkAssignmentEmailService` in the `learning` package, following `EmailService`'s existing subject/body/sign-off shape (`"… — Destino: Español"`, closing `"Saludos,\nDestino: Español"`).

- Subject names that new homework is waiting; body lists each newly assigned homework by `homework_assignments.title`.
- Work link: `{app.frontend.base-url}/aprendizaje` (FR-009).
- Preferences link: `{app.frontend.base-url}/cuenta`, with the "you are receiving this because you asked to" line (FR-010).

**Rationale**: every email the site sends today is Spanish-only plain text, even though the UI ships es/ro/en. Matching that keeps one voice and avoids introducing an email templating/localisation layer for a single message.

**Noted, not in scope**: per-recipient email language is a reasonable future improvement, but it would apply to all seven existing emails, not just this one, so it does not belong in this feature.

**Recipient query**: a single `findRecipientsFor(Collection<UUID> userIds)` returning `(userId, email, firstName)` filtered by `email_on_homework_assigned = true AND status = 'ACTIVE'`. One query rather than `UserRepository.findById` in a loop, and the `status = 'ACTIVE'` predicate implements the spec's "account still awaiting activation → no email attempted" edge case.

---

## R9 — Testing approach

**Decision**:

- **Unit tests** (run locally and in CI) for the grouping and gating logic: `HomeworkAssignmentEmailServiceTest` with a mocked `JavaMailSender`, mirroring the existing `EmailServiceTest` / `TestimonialEmailServiceTest` shape — asserts one message per student, correct recipient, non-empty body, and nothing sent when the flag is off.
- **Integration tests** extending `AbstractIntegrationTest` (GitHub Actions only, by the existing `GitHubActionsOnly` condition) for the delta semantics that only the database can prove: `RETURNING` reports an insert exactly once, and re-saving an unchanged assignment reports none.

**Rationale**: FR-013 and FR-006 are the requirements most likely to regress, and both are cheap to pin. The split follows the repo's existing convention that DB-dependent tests are CI-only.
