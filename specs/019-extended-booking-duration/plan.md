# Implementation Plan: Extended Booking Duration (1.5-Hour Classes)

**Branch**: `019-extended-booking-duration` | **Date**: 2026-07-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/019-extended-booking-duration/spec.md`

## Summary

Students who the teacher has explicitly marked "extended class eligible" can book a 90-minute class alongside the existing 60-minute class. The `duration_minutes` column and `Booking.getSlotEnd()` derivation already exist on every booking (`V3__create_bookings.sql`) — today they're just always fed `60` from a hardcoded property. This feature makes duration a real per-request value: `SchedulingProperties` gains a second fixed duration (90), `AvailabilityService`/`BookingService` become duration-parameterized instead of reading a single hardcoded value, and a new per-student `extended_class_eligible` flag (granted/revoked by the teacher from the admin panel, mirroring the existing `USER`↔`STUDENT` promote/revoke pattern) gates who may request the 90-minute option.

The one real architectural gap this surfaces: today's overlap protection is a unique index on `slot_start` alone, which only works because every booking is exactly 60 minutes on a shared grid. Once a 90-minute booking can start at a time that only partially overlaps a 60-minute booking's window, `slot_start` equality is no longer sufficient — this plan replaces that index with a Postgres range-exclusion constraint (`EXCLUDE USING gist` on `tstzrange(slot_start, slot_end)`) and adds an explicit interval-overlap check in `AvailabilityService` before showing a start time as open, so both the UI and the database independently enforce "no two confirmed bookings may occupy overlapping time."

Zoom meeting creation, the confirmation email, and `BookingResponse`/`BookingSummary` already thread `durationMinutes`/`slotEnd` end-to-end — most of User Stories 2–4 (showing duration after booking) are a front-end display change (render the existing `slotEnd`), not new backend plumbing.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5 (`-web`, `-security`, `-jdbc`, `-mail`), Flyway, PostgreSQL JDBC driver. **No new dependencies** — the range-exclusion constraint uses PostgreSQL's built-in `gist` support for `tstzrange`, no extension required.
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new dependencies.**

**Storage**: PostgreSQL 18. New migration `V29__extended_booking_duration.sql`:
- `ALTER TABLE users ADD COLUMN extended_class_eligible BOOLEAN NOT NULL DEFAULT false;`
- Drop `bookings_active_slot_uniq` (unique index on `slot_start` only) and replace it with `EXCLUDE USING gist (tstzrange(slot_start, slot_start + (duration_minutes || ' minutes')::interval, '[)') WITH &&) WHERE (status = 'CONFIRMED')` — prevents any two CONFIRMED bookings (any mix of 60/90 minutes) from occupying overlapping time, at the database level, regardless of what the application layer already checked.

**Testing**:
- Backend: JUnit 5 + Mockito + AssertJ (existing convention). New/extended tests: `AvailabilityServiceTest` (schedule generation and `validateBookable` at both durations, overlap rejection across mixed durations), `BookingServiceTest` (duration validation, eligibility gating, Zoom/email calls receive the requested duration), `BookingControllerIntegrationTest` (end-to-end 200/403/422 cases), `StudentAdminControllerIntegrationTest`-style test for the new grant/revoke eligibility endpoints (403 for non-admin, idempotency).
- Frontend: no test framework in this repo (per CLAUDE.md) — visual verification in a running browser per the constitution's Development Workflow: an eligible student sees and can complete a 90-minute booking, a non-eligible student only sees 60 minutes, "My Bookings" and the admin schedule view show the correct time range for each duration, the teacher can grant/revoke eligibility from `/panel`.

**Target Platform**: Browser via TanStack Start SSR (frontend, `:8080`) + JVM server on `:8081` (backend).

**Performance Goals**: N/A — schedule generation stays O(availability windows × horizon days) per request, unchanged in order of magnitude; the added overlap check is a linear scan over one horizon's worth of bookings (low tens, single-teacher scale).

**Constraints**: A class booking's window must never overlap another CONFIRMED booking's window regardless of duration — enforced redundantly at both the application layer (`AvailabilityService`, before offering/accepting a start time) and the database layer (exclusion constraint), since the unique-index approach the codebase used for the single-duration case is provably insufficient once durations differ (FR-002, FR-009). Only two durations are in scope, both fixed values sourced from `SchedulingProperties` — no free-form duration input (per spec Assumptions).

**Scale/Scope**: Single-teacher site, low tens of students. One migration, one new admin grant/revoke endpoint pair (mirroring an existing one almost exactly), duration-parameterization of two existing services, a handful of frontend display/selection updates. No new backend domain package.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Exactly two fixed durations (no configurable/arbitrary duration input), eligibility is a single boolean column reusing the exact grant/revoke/email pattern already built for `STUDENT` status, and the overlap fix reuses PostgreSQL's built-in exclusion-constraint feature rather than introducing an interval-tree library or a new locking scheme.
- **II. Component-Driven UI** — PASS. The duration toggle, the eligibility badge/action on the admin Students tab, and the updated time-range displays are additions to existing named components (`BookingDialog`, `TimeSlotList`, `MyBookings`, `StudentsTab`), not raw DOM manipulation.
- **III. Evolution-Ready Architecture** — PASS. All new data-fetching (schedule-by-duration, create-booking-with-duration, grant/revoke eligibility) is added to the existing `front-end/src/lib/scheduling.ts` and `front-end/src/lib/admin.ts` service modules, never inlined in components.
- **Technology Stack** — PASS. No new dependencies on either side.
- **Development Workflow** — PASS. Verified in a running browser before completion (no frontend test framework exists); branch `019-extended-booking-duration` follows the git extension's naming convention; no dead code (the old unique index is dropped, not left alongside the new constraint).

**Result: PASS — no violations. Complexity Tracking left empty.**

## Project Structure

### Documentation (this feature)

```text
specs/019-extended-booking-duration/
├── plan.md                              # This file (/speckit-plan output)
├── research.md                          # Phase 0 output
├── data-model.md                        # Phase 1 output
├── quickstart.md                        # Phase 1 output
├── contracts/
│   └── booking-duration-api.md          # Phase 1 output
├── checklists/
│   └── requirements.md                  # /speckit-specify output
└── tasks.md                             # /speckit-tasks output (NOT created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V29__extended_booking_duration.sql   # NEW: extended_class_eligible column + overlap-exclusion constraint

back-end/src/main/java/com/kuky/backend/
├── config/
│   └── SchedulingProperties.java                 # EDIT: add extendedClassDurationMinutes (default 90)
├── auth/
│   ├── model/User.java                           # EDIT: add extendedClassEligible field
│   ├── dto/UserResponse.java                     # EDIT: add extendedClassEligible
│   └── repository/UserRepository.java            # ADD: grantExtendedClassById(), revokeExtendedClassById(); EDIT: USER_MAPPER
├── auth/service/EmailService.java                # ADD: sendExtendedClassGrantedEmail(), sendExtendedClassRevokedEmail()
├── admin/
│   ├── controller/StudentAdminController.java    # ADD: POST/DELETE /admin/users/{id}/extended-class
│   └── dto/ExtendedClassEligibilityResponse.java # NEW: (id, extendedClassEligible)
├── scheduling/
│   ├── dto/CreateBookingRequest.java              # EDIT: add durationMinutes field
│   ├── controller/
│   │   ├── ScheduleController.java                # EDIT: accept ?durationMinutes= query param
│   │   └── BookingController.java                 # EDIT: pass request.durationMinutes() through
│   ├── service/
│   │   ├── AvailabilityService.java                # EDIT: parameterize generateSchedule()/validateBookable() by duration; add overlap-vs-existing-bookings check
│   │   └── BookingService.java                     # EDIT: accept/validate durationMinutes, eligibility check
│   ├── repository/BookingRepository.java           # EDIT: findConfirmedBookingIntervalsBetween() replacing findConfirmedSlotStartsBetween()
│   └── exception/BookingNotAllowedException.java   # EDIT: add INVALID_DURATION, NOT_ELIGIBLE_FOR_EXTENDED reasons
└── config/GlobalExceptionHandler.java              # EDIT: map the two new reasons to error codes

back-end/src/test/java/com/kuky/backend/
├── scheduling/AvailabilityServiceTest.java         # EXTEND
├── scheduling/BookingServiceTest.java              # EXTEND
├── scheduling/BookingControllerIntegrationTest.java # EXTEND
└── admin/StudentAdminControllerIntegrationTest.java # EXTEND (or sibling test for extended-class endpoints)

front-end/src/
├── lib/
│   ├── scheduling.ts                    # EDIT: getSchedule(durationMinutes), createBooking(slotStart, durationMinutes)
│   ├── auth.ts                          # EDIT: UserResponse.extendedClassEligible
│   └── admin.ts                         # EDIT: grantExtendedClass(id), revokeExtendedClass(id)
├── components/scheduling/
│   ├── ScheduleView.tsx                 # EDIT: duration toggle state, refetch schedule per duration, pass eligibility to children
│   ├── TimeSlotList.tsx                 # EDIT: no logic change beyond receiving already-filtered slots for the active duration
│   ├── BookingDialog.tsx                # EDIT: pass durationMinutes to createBooking(); show start–end range
│   └── MyBookings.tsx                   # EDIT: BookingCard shows start–end range (slotEnd already in the API response)
└── components/admin/students/
    └── StudentsTab.tsx                  # EDIT: add "extended class" toggle/action per student row

# NOT edited: components/admin/bookings/BookingsTab.tsx already renders
# formatSlot(b.slotStart, b.slotEnd, ...) — the teacher's schedule view already
# shows each booking's real end time, and needs no change once duration is real.
```

**Structure Decision**: Web application (existing `front-end/` + `back-end/`). No new backend domain package — extended-class eligibility is a state on the existing `User`/`users` row (exactly like `STUDENT` status was), and duration is a parameter threaded through the existing `scheduling` package, not a new bounded context.

## Complexity Tracking

> No Constitution Check violations. No entries required.
