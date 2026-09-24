# Implementation Plan: Email Preferences

**Branch**: `049-email-preferences` | **Date**: 2026-09-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/049-email-preferences/spec.md`

## Summary

Give every account one opt-in switch — *email me when new homework is assigned* — off by default, living in the signed-in account panel at `/cuenta`. When any of the three routes that grant homework actually makes new work available to a student who has opted in, send that student exactly one Spanish plain-text email listing everything new from that action.

The technical spine is one seam: all four grant call sites already funnel through `HomeworkTargetRepository.addTargets`, whose `ON CONFLICT DO NOTHING` already encodes "was this genuinely new?". Adding `RETURNING user_id` surfaces that answer, each admin service accumulates the per-student delta across its loop, and flushes once at the end. Storage is a single `NOT NULL DEFAULT false` column on `users`, which makes "everyone starts off" a property of the migration rather than a step that can be got wrong.

## Technical Context

**Language/Version**: Java 21 (back-end), TypeScript 5 strict (front-end), Node.js 22+

**Primary Dependencies**: Spring Boot 3.5 (Web, Security, Mail), plain JDBC via `NamedParameterJdbcTemplate`, Flyway 11, Gradle · React 19, TanStack Start 1.x (SSR), TanStack Router (file-based), TailwindCSS 4, Shadcn UI, Vite 7, i18next

**Storage**: PostgreSQL 18. One new column, `users.email_on_homework_assigned` (Flyway `V26`). No new table.

**Testing**: JUnit 5 + Mockito for unit tests (`./gradlew test`, runs locally); integration tests extend `AbstractIntegrationTest` and run only under GitHub Actions against a throwaway Postgres service

**Target Platform**: Linux server (Spring Boot REST API on `:8081`) + SSR web front-end on `:8080`

**Project Type**: Web application — separate `back-end/` and `front-end/` roots

**Performance Goals**: No new hot path. Recipient lookup is one indexed query per teacher action; sends are O(students granted new work), typically 1–10 per action

**Constraints**: Email delivery is best-effort and MUST NOT be able to fail, delay materially, or roll back a teacher's assignment (FR-012). No email may be sent to a student whose switch was off at the moment of assignment (FR-008). No student may be emailed twice for the same homework (FR-013)

**Scale/Scope**: Single-teacher site, tens of students. Back-end: 1 migration, 1 controller, 1 service, 1 enum, 2 DTOs, edits to 3 existing classes. Front-end: 1 new component, 1 new service module, 1 edit to `cuenta.tsx`, 3 locale files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Gate | Pre-Phase 0 | Post-Phase 1 |
|---|---|---|---|
| **I. Simplicity First** | No abstraction without a concrete present need; YAGNI non-negotiable | ✅ PASS | ✅ PASS |
| **II. Component-Driven UI** | All UI from named React components on the established stack; no raw DOM | ✅ PASS | ✅ PASS |
| **III. Evolution-Ready Architecture** | Data-fetching isolated in hooks/service modules, never inlined in components | ✅ PASS | ✅ PASS |

**I — Simplicity First**: The two places this feature invited over-engineering were both refused, and both refusals are recorded with a revisit trigger:

- Storage is one boolean column, not a key/value preferences table ([R1](./research.md)). Building a generic preference store for a single option is exactly the speculative generality the principle forbids. Revisit at the third option.
- Grouping is a plain in-method collector, not an application-event bus ([R3](./research.md)). At four call sites the indirection costs more than it saves — and `@TransactionalEventListener(AFTER_COMMIT)` would actively break the grouping, since the `@Transactional` boundaries here sit on repository methods rather than service methods.

Likewise no async executor, no outbox, no retry layer, and no email templating engine — sending follows the `sendQuietly` convention already in `BookingEmailService` and `TestimonialEmailService` ([R4](./research.md)).

**II — Component-Driven UI**: the toggle is a named `EmailPreferencesSection` component built from the existing Shadcn `Switch`, `Label` and `Separator` primitives, rendered inside `ProfileView`. Named even though it has a single call site, as the principle requires. No raw DOM access.

**III — Evolution-Ready Architecture**: all fetch logic lives in `front-end/src/lib/emailPreferences.ts`, modelled on the existing `lib/notifications.ts`; the component receives and calls it but never fetches inline. The server drives the option list ([R6](./research.md)), so the page structure survives a second option without edits.

**Observation — the constitution is stale, not violated.** `.specify/memory/constitution.md` still records "Package manager: Bun" and "Future backend: to be determined", while the project has since moved to npm and a Spring Boot back-end (both documented in `CLAUDE.md`). This plan follows the project as it actually is. Flagged here rather than silently ignored; correcting it is a `/speckit-constitution` job, not this feature's.

**Gate result: PASS** — no entries in Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/049-email-preferences/
├── plan.md                              # This file
├── spec.md                              # Feature specification
├── research.md                          # Phase 0 output — 9 decisions
├── data-model.md                        # Phase 1 output
├── quickstart.md                        # Phase 1 output — validation guide
├── contracts/
│   └── email-preferences-api.md         # Phase 1 output
├── checklists/
│   └── requirements.md                  # Spec quality checklist
└── tasks.md                             # Phase 2 — created by /speckit-tasks, NOT by this command
```

### Source Code (repository root)

```text
back-end/src/main/
├── java/com/kuky/backend/
│   ├── preferences/                                  # NEW package
│   │   ├── controller/EmailPreferencesController.java      # GET + PUT /api/v1/me/email-preferences
│   │   ├── service/EmailPreferencesService.java
│   │   ├── repository/EmailPreferencesRepository.java      # reads/writes the users column
│   │   ├── model/EmailPreferenceType.java                  # enum: NEW_HOMEWORK_ASSIGNED
│   │   └── dto/
│   │       ├── EmailPreferenceResponse.java
│   │       └── UpdateEmailPreferenceRequest.java
│   ├── learning/
│   │   ├── service/HomeworkAssignmentEmailService.java     # NEW — composes + sends, swallows failures
│   │   ├── service/NewHomeworkGrants.java                  # NEW — per-action accumulator (student → assignments)
│   │   └── repository/HomeworkTargetRepository.java        # EDIT — addTargets returns inserted user IDs
│   ├── admin/service/HomeworkAdminService.java             # EDIT — collect + flush on create/setAssignees
│   ├── units/service/UnitService.java                      # EDIT — collect + flush on setHomeworks/setAssignees
│   └── config/SecurityConfig.java                          # EDIT — /api/v1/me/** authenticated
└── resources/db/migration/
    └── V26__email_preferences.sql                     # NEW — users.email_on_homework_assigned

back-end/src/test/java/com/kuky/backend/
├── learning/HomeworkAssignmentEmailServiceTest.java   # NEW — unit, mocked JavaMailSender
├── preferences/EmailPreferencesServiceTest.java       # NEW — unit
└── learning/NewHomeworkGrantIntegrationTest.java      # NEW — integration, CI-only

front-end/src/
├── lib/emailPreferences.ts                            # NEW — fetch layer (Principle III)
├── components/account/EmailPreferencesSection.tsx     # NEW — named component (Principle II)
├── routes/cuenta.tsx                                  # EDIT — render section inside ProfileView
└── i18n/locales/{es,ro,en}.ts                         # EDIT — labels for the section
```

**Structure Decision**: The existing two-root web layout (`back-end/` + `front-end/`) is kept unchanged. Preferences get their own top-level back-end package rather than joining `auth`, for one concrete reason recorded in [R5](./research.md): `/api/v1/auth/**` is `permitAll()` in `SecurityConfig:47`, so anything mounted under it would be reachable anonymously. The email-sending code goes in `learning` beside the homework domain that triggers it, following the precedent of `TestimonialEmailService` living in `testimonials` rather than in `auth`.

## Phase 0 — Research

**Status**: ✅ Complete → [research.md](./research.md)

Nine decisions, each with rationale and rejected alternatives. The load-bearing ones:

| # | Decision |
|---|---|
| R1 | Store as `users.email_on_homework_assigned BOOLEAN NOT NULL DEFAULT false` — default-off becomes structural |
| R2 | `addTargets` returns actually-inserted user IDs via `ON CONFLICT DO NOTHING RETURNING user_id` — race-free, covers FR-007a/b/c through one seam |
| R3 | Group per teacher action with an in-method collector flushed once — no event bus |
| R4 | Send synchronously after writes, log-and-swallow per recipient, matching `sendQuietly` |
| R5 | Mount at `/api/v1/me/email-preferences`, **never** under the `permitAll()` `/api/v1/auth/**` |
| R6 | Server drives the option list so a second option needs no page redesign |
| R8 | Spanish plain text matching the other seven emails; recipients fetched in one query gated on `status = 'ACTIVE'` |

## Phase 1 — Design & Contracts

**Status**: ✅ Complete

- [data-model.md](./data-model.md) — the `users` column, the `EmailPreferenceType` enum, the transient `NewHomeworkGrants` accumulator, and the exact `RETURNING` semantics that make FR-013 hold
- [contracts/email-preferences-api.md](./contracts/email-preferences-api.md) — `GET` and `PUT /api/v1/me/email-preferences`, payloads, status codes, error codes, and the email's own contract
- [quickstart.md](./quickstart.md) — runnable validation covering all three grant routes, batching, opt-out, and the default-off guarantee

### Post-design constitution re-check

Re-evaluated after the design artifacts: **PASS, unchanged**. The design added no new dependency, no new framework mechanism, and no new abstraction layer — the only structural additions are one column, one enum with one constant, and one accumulator record. Component and data-fetching boundaries hold as designed.

### Agent context

`CLAUDE.md` updated between the `<!-- SPECKIT START -->` / `<!-- SPECKIT END -->` markers to point at this plan.

## Requirements Traceability

| Requirement | Where it is satisfied |
|---|---|
| FR-001 account area | `EmailPreferencesSection` in `ProfileView` (`cuenta.tsx`) — R7 |
| FR-002 off by default | `NOT NULL DEFAULT false` in `V26`; no backfill — R1 |
| FR-003 one option, extensible presentation | `EmailPreferenceType` enum + server-driven `GET` — R6 |
| FR-004 toggle freely | `PUT /api/v1/me/email-preferences/{type}` |
| FR-005 persists, private to the owner | Column on `users`; principal from JWT, no user ID in path — R5 |
| FR-006 one email per action | `NewHomeworkGrants` accumulator flushed once per service method — R3 |
| FR-007 a/b/c all trigger | All four call sites funnel through `addTargets` — R2 |
| FR-008 only opted-in students | `findRecipientsFor(...)` filters `email_on_homework_assigned = true` — R8 |
| FR-009 language, names the work, links to it | Spanish plain text listing titles, links `/aprendizaje` — R8 |
| FR-010 explains why, links to preferences | Body links `/cuenta` — R8 |
| FR-011 one email each, no cross-disclosure | Accumulator is keyed per student; one `SimpleMailMessage` per recipient |
| FR-012 send failure is isolated | Per-recipient try/catch after commit — R4 |
| FR-013 never twice for the same homework | `ON CONFLICT DO NOTHING RETURNING` — R2 |
| FR-014 existing emails/dots unchanged | No edit to `EmailService`, `BookingEmailService`, or notification code; `addTargets` signature widens, behaviour does not change |
| FR-015 teacher cannot see or change | No admin endpoint exists; `/me` resolves from the principal — R5 |

## Complexity Tracking

No constitution violations. Table intentionally empty.
