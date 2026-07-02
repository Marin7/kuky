# Implementation Plan: Class Reminder Emails

**Branch**: `013-class-reminder-emails` | **Date**: 2026-07-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/013-class-reminder-emails/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Send a one-time email reminder to both the student and the teacher roughly 24 hours before each confirmed class. Implemented as a Spring `@Scheduled` poller that checks the existing `bookings` table for `CONFIRMED` rows crossing the 24h-before mark, claims each via a new `reminder_sent_at` column (preventing duplicates), and sends via the existing `BookingEmailService`. Research confirmed the spec's "booked with <24h notice" edge case is structurally impossible today because `SchedulingProperties.minLeadHours=24` already blocks such bookings at creation time — no branching logic needed for it.

## Technical Context

**Language/Version**: Java 21 (Gradle toolchain), Spring Boot 3.5.1

**Primary Dependencies**: `spring-boot-starter-mail` (`JavaMailSender`, already in use), `spring-boot-starter-jdbc` (`NamedParameterJdbcTemplate`), Flyway 11 — all existing, no new dependencies added

**Storage**: PostgreSQL 18 — one new nullable column (`bookings.reminder_sent_at`) via Flyway migration `V24`

**Testing**: JUnit 5 + Mockito + AssertJ (existing `back-end` test stack), pure-logic unit tests against a fixed `java.time.Clock` (matches `AvailabilityServiceTest` convention) — no Spring context needed for the scheduler's decision logic

**Target Platform**: Existing Spring Boot backend, single instance, Linux server deployment

**Project Type**: Web application (existing `front-end` + `back-end`); this feature is back-end only, no front-end changes

**Performance Goals**: N/A beyond existing request load — this is a low-frequency background poll (every 15 min) over a small table, not a request-path feature

**Constraints**: Reminders must land within a 23–25 hour window before class start (SC-001); at most one reminder per booking (SC-003); must not affect booking/cancellation correctness on mail failure (FR-007)

**Scale/Scope**: Single teacher, current booking volume (personal tutoring business) — no scale concerns

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The project constitution ([constitution.md](../../.specify/memory/constitution.md)) targets the front-end (React/TanStack/Tailwind) and predates the back-end's introduction ("Future backend: to be determined"). Its concrete, checkable principles don't directly bind back-end-only work, but the spirit applies:

- **Simplicity First (YAGNI)**: Satisfied — reuses the existing `@Scheduled`-free-but-Clock-based pattern with a single poller and one new column, no new library (Quartz, message queue) introduced. See research.md's "no new infrastructure" decision.
- **Component-Driven UI**: N/A — no UI changes in this feature.
- **Evolution-Ready Architecture**: N/A — no new front-end data-fetching concerns; back-end changes stay within the existing `scheduling` package/service-repository layering already used by `BookingService`/`BookingRepository`.
- **Dead code MUST NOT be committed**: The research phase deliberately avoids adding a no-op branch for the unreachable "<24h notice" case rather than writing dead code to satisfy FR-005 literally.

No violations; Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/013-class-reminder-emails/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

No `contracts/` directory — this feature adds no new external API endpoint (no front-end, no new REST surface); its only interfaces are a DB migration and outbound email, both covered by data-model.md and quickstart.md.

### Source Code (repository root)

```text
back-end/
├── src/main/java/com/kuky/backend/
│   ├── scheduling/
│   │   ├── model/Booking.java                    # + reminderSentAt field
│   │   ├── repository/BookingRepository.java      # + findBookingsDueForReminder(), + claimReminder()
│   │   └── service/
│   │       ├── BookingEmailService.java           # + sendReminder(...)
│   │       └── BookingReminderScheduler.java       # NEW — @Scheduled poller
│   └── config/
│       └── BackEndApplication.java                 # + @EnableScheduling
├── src/main/resources/db/migration/
│   └── V24__add_booking_reminder_sent.sql          # NEW
└── src/test/java/com/kuky/backend/scheduling/
    └── BookingReminderSchedulerTest.java           # NEW — pure-logic, fixed Clock, mocked repo/email

front-end/                                           # unaffected — no changes
```

**Structure Decision**: Back-end-only change inside the existing `scheduling` package (mirrors where `Booking`, `BookingService`, `BookingRepository`, `BookingEmailService` already live). No new package, no front-end changes.

## Complexity Tracking

> Not applicable — no Constitution Check violations.
