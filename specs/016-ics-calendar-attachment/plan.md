# Implementation Plan: Calendar (.ics) Attachment for Bookings

**Branch**: `016-ics-calendar-attachment` | **Date**: 2026-07-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/016-ics-calendar-attachment/spec.md`

## Summary

When a student books or cancels a 1-on-1 class, the booking confirmation, teacher notification, and cancellation emails will carry a generated iCalendar (`.ics`) attachment so the recipient can add/remove the class in their personal calendar app in one click. Generation is on-demand from the existing `Booking` record (no new persistence), uses a stable per-booking UID so cancellation updates the same calendar entry, and never blocks the underlying email send if generation fails. Reminder emails are explicitly out of scope.

## Technical Context

**Language/Version**: Java 21 (existing back-end)

**Primary Dependencies**: Spring Boot 3.5 `spring-boot-starter-mail` (already present). No new iCalendar library — the event payload is a single non-recurring VEVENT, well within reach of a small hand-written generator, consistent with Simplicity First. `JavaMailSender` will be used via `MimeMessage`/`MimeMessageHelper` instead of the current `SimpleMailMessage`, since attachments require a multipart MIME message.

**Storage**: N/A — the calendar file is generated on demand at send time from the existing `bookings` table row (per FR-008); no new tables or columns.

**Testing**: JUnit 5 + Spring Boot Test (existing back-end pattern in `back-end/src/test/java`), plus manual validation via Mailpit per `quickstart.md`.

**Target Platform**: Existing Spring Boot back-end (`back-end/`), no front-end changes.

**Project Type**: Web application (existing front-end + back-end) — this feature is back-end-only.

**Performance Goals**: Attachment generation must add no perceptible delay to the booking flow (SC-003); string-based ICS generation for a single event is sub-millisecond, well within this bound.

**Constraints**: Must never block or fail the underlying email send if ICS generation fails (FR-007); event times must be unambiguous across recipient time zones (FR-003); reminder emails must remain unmodified (FR-009).

**Scale/Scope**: Low volume (a single tutor's personal booking calendar, on the order of a handful of bookings per day) — no batching or async processing needed.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Simplicity First**: PASS. No new dependency is introduced; the ICS payload is generated with a small, purpose-built string builder rather than a general-purpose calendar library. Email sending switches from `SimpleMailMessage` to `MimeMessage` only because attachments require it — the minimum change needed.
- **Component-Driven UI**: N/A. This feature has no front-end/UI surface; it is entirely a back-end email change.
- **Evolution-Ready Architecture**: PASS. The existing `BookingEmailService` already isolates all email-composition logic from `BookingService`; the new ICS generation logic will live alongside it as a small collaborator (e.g. `IcsEventFactory`), keeping the same separation.

No violations — Complexity Tracking section not needed.

## Project Structure

### Documentation (this feature)

```text
specs/016-ics-calendar-attachment/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── ics-attachment.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
back-end/
├── src/main/java/com/kuky/backend/scheduling/
│   ├── service/
│   │   ├── BookingService.java          # existing — passes booking id/duration/joinUrl to email calls
│   │   ├── BookingEmailService.java     # existing — extended to build MimeMessage + attach ICS
│   │   └── IcsEventFactory.java         # new — builds the iCalendar VEVENT payload (REQUEST/CANCEL) from booking data
│   └── model/
│       └── Booking.java                 # existing — no schema change, read-only source of ICS fields
└── src/test/java/com/kuky/backend/scheduling/
    └── service/
        ├── IcsEventFactoryTest.java     # new — unit tests for ICS payload correctness
        └── BookingEmailServiceTest.java # existing/extended — verifies attachment is present and send-failure isolation
```

**Structure Decision**: Single existing Spring Boot back-end (`back-end/`). The feature adds one new collaborator class (`IcsEventFactory`) inside the existing `scheduling` package and extends `BookingEmailService`; no new packages, modules, or front-end changes.

## Complexity Tracking

*No Constitution Check violations — this section is intentionally empty.*
