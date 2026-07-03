# Implementation Plan: Shared Bookings (Companion Student on a Class)

**Branch**: `021-shared-bookings` | **Date**: 2026-07-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/021-shared-bookings/spec.md`

## Summary

A class booking today is strictly one student (`bookings.user_id`, FK to `users`, `ON DELETE CASCADE`). This feature adds one optional additional participant, a "companion student," attached only by the teacher from her admin bookings view — never by students themselves. Because the cap is exactly two participants (never a general attendee list), the simplest model is a single nullable `second_student_id` column on `bookings` mirroring `user_id`, not a join table.

The teacher attaches/detaches a companion student via two new admin endpoints on the existing `BookingAdminController`, mirroring the `POST`/`DELETE /api/v1/admin/users/{id}/extended-class` grant/revoke pattern already established for extended-class eligibility. Attaching validates the target holds the `STUDENT` role, isn't already the booking student, and — if the class is the 90-minute duration — also holds `extended_class_eligible` (FR-014). No-show tracking becomes per-student (`second_student_no_show`, a second boolean mirroring the existing `no_show` column). Cancellation (`BookingService.cancelBooking`) is extended so either student's ownership check succeeds — both have equal standing to cancel — and cancelling by anyone (either student, or the teacher) removes the whole class for both, exactly as it does today for a single student. The confirmation/cancellation email plumbing (`BookingEmailService`) is extended to also address the companion student when one is attached, and the companion student's own `GET /api/v1/bookings` view is extended to include bookings where they're the companion student, not just the booking student.

No new backend package or domain module: this is a data column, two new admin endpoints, and email/query fan-out extensions inside the existing `scheduling` and `admin` packages — consistent with how extended-class eligibility was added as a boolean column rather than a new entity.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5 (`-web`, `-security`, `-jdbc`, `-mail`), Flyway, PostgreSQL JDBC driver. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI. **No new dependencies** — reuses the existing `StudentMultiSelect` component (`front-end/src/components/admin/homework/StudentMultiSelect.tsx`) and `getStudents()` (`front-end/src/lib/admin.ts:53`) already built for the Homework tab's student assignment flow.

**Storage**: PostgreSQL 18. New migration `V30__shared_bookings.sql`:
- `ALTER TABLE bookings ADD COLUMN second_student_id UUID REFERENCES users(id) ON DELETE SET NULL;` — deliberately `SET NULL` (not `CASCADE` like `user_id`) so a companion student's account being removed doesn't destroy the booking student's booking.
- `ALTER TABLE bookings ADD COLUMN second_student_no_show BOOLEAN;` — mirrors the existing `no_show` column (which now implicitly means "booking student no-show").
- `ALTER TABLE bookings ADD CONSTRAINT bookings_second_student_distinct CHECK (second_student_id IS NULL OR second_student_id <> user_id);` — enforces FR-004 (can't attach the booking student as their own companion student) at the database level, not just in the service layer.
- `CREATE INDEX bookings_second_student_id_idx ON bookings (second_student_id) WHERE second_student_id IS NOT NULL;` — supports the "show my shared bookings" query (FR-008).

**Testing**:
- Backend: JUnit 5 + Mockito + AssertJ (existing convention). Extend `BookingServiceTest` (attach/detach validation, per-student no-show, cancellation by either student, extended-eligibility gating, notification calls), `BookingRepositoryTest`/integration test for the two-student `findByUserId`-or-`second_student_id` query, and `BookingAdminController`/`BookingController` integration tests for the new endpoints and the widened cancellation ownership check.
- Frontend: no test framework in this repo (per CLAUDE.md) — visual verification in a running browser per the constitution's Development Workflow: teacher can attach/detach a companion student from `/panel`'s Bookings tab; the companion student sees the class in their own `/reservas` "My Bookings" and can cancel it; cancelling by either student removes it from both views.

**Target Platform**: Browser via TanStack Start SSR (frontend, `:8080`) + JVM server on `:8081` (backend).

**Performance Goals**: N/A — one additional nullable FK column and an `OR`-widened indexed lookup on a single-teacher, low-tens-of-students dataset.

**Constraints**: A booking may have at most two students (FR-003), enforced in the service layer before the update and reinforced by the `bookings_second_student_distinct` check constraint for the "same person twice" case specifically (the "already has a companion student" cap is a service-layer check against the existing column value, not something a table constraint can express as a count). Attaching to a class that is cancelled or already occurred is rejected (FR-005). Reminders (`BookingReminderScheduler`) and cancellation notifications must reach both students when a companion student is present, to satisfy SC-002's "before the class starts" guarantee — not just the initial attach-time email — so the reminder query and scheduler are extended alongside the confirmation/cancellation paths.

**Scale/Scope**: Single-teacher site, low tens of students, and shared bookings are explicitly described as rare. Two new admin endpoints, one new migration, one new frontend dialog reusing an existing student-picker component, and targeted extensions across the existing booking service/repository/email/DTO layers. No new backend domain package.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. A single nullable `second_student_id` column (not a join table) matches the actual cap of two participants; per-student no-show reuses the same one-boolean-per-student shape as the existing `no_show` column; new admin endpoints and error reasons reuse the existing `BookingAdminController` and `BookingNotAllowedException.Reason` enum rather than introducing a new controller or exception type; the frontend reuses the existing `StudentMultiSelect` picker instead of building a new one.
- **II. Component-Driven UI** — PASS. The attach/detach UI is a new named dialog component composed from existing Shadcn primitives and the existing `StudentMultiSelect`, added to `BookingsTab.tsx`, not raw DOM manipulation.
- **III. Evolution-Ready Architecture** — PASS. All new data-fetching (attach/detach companion student) is added to the existing `front-end/src/lib/admin.ts` service module, never inlined in components.
- **Technology Stack** — PASS. No new dependencies on either side.
- **Development Workflow** — PASS. Verified in a running browser before completion; branch `021-shared-bookings` follows the git extension's naming convention; no dead code left behind (no columns or endpoints introduced without being wired to a caller).

**Result: PASS — no violations. Complexity Tracking left empty.**

## Project Structure

### Documentation (this feature)

```text
specs/021-shared-bookings/
├── plan.md                              # This file (/speckit-plan output)
├── research.md                          # Phase 0 output
├── data-model.md                        # Phase 1 output
├── quickstart.md                        # Phase 1 output
├── contracts/
│   └── companion-student-api.md            # Phase 1 output
├── checklists/
│   └── requirements.md                  # /speckit-specify output
└── tasks.md                             # /speckit-tasks output (NOT created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V30__shared_bookings.sql             # NEW: second_student_id, second_student_no_show, distinct-person check constraint, index

back-end/src/main/java/com/kuky/backend/
├── scheduling/
│   ├── model/Booking.java                          # EDIT: add companionStudentId, companionStudentNoShow
│   ├── dto/
│   │   ├── BookingSummary.java                     # EDIT: add isCompanionStudent (so a student's own "My Bookings" can distinguish their role)
│   │   └── SetNoShowRequest.java                   # EDIT: add which student the no-show applies to (default: booking student, for backward compatibility)
│   ├── exception/BookingNotAllowedException.java   # EDIT: add reasons COMPANION_ALREADY_ATTACHED, COMPANION_SAME_AS_BOOKING_STUDENT, COMPANION_NOT_STUDENT, COMPANION_NOT_ATTACHED, BOOKING_NOT_ATTACHABLE (reuses the existing NOT_ELIGIBLE_FOR_EXTENDED reason for the "target not extended-eligible" case only)
│   ├── repository/BookingRepository.java           # EDIT: findByUserId → also matches second_student_id; ADD setCompanionStudentId(), clearCompanionStudentId(), setCompanionStudentNoShow(); EDIT ReminderDueView to carry the companion student's email when present
│   └── service/
│       ├── BookingService.java                     # EDIT: cancelBooking() ownership check accepts companion student too; ADD attachCompanionStudent(), detachCompanionStudent(); EDIT setNoShow() to target the booking student or the companion
│       ├── BookingEmailService.java                # EDIT: sendConfirmation/sendCancellation/sendCancellationByTeacher gain an optional companion-student recipient; ADD sendCompanionStudentAttached()
│       └── BookingReminderScheduler.java            # EDIT: also send the reminder to the companion student's email when present on the due booking
├── admin/
│   ├── controller/BookingAdminController.java       # ADD: POST/DELETE /api/v1/admin/bookings/{id}/companion-student
│   └── dto/
│       ├── AdminBookingDto.java                     # EDIT: add nullable companionStudentId/Email/FirstName/LastName/Username/NoShow
│       └── AttachCompanionStudentRequest.java          # NEW: record(UUID studentId)
└── config/GlobalExceptionHandler.java               # EDIT: map the five new reasons to error codes

back-end/src/test/java/com/kuky/backend/
├── scheduling/BookingServiceTest.java                # EXTEND
├── scheduling/BookingControllerIntegrationTest.java  # EXTEND (cancellation by companion student)
└── admin/BookingAdminControllerIntegrationTest.java  # EXTEND (or created if it doesn't already exist) — attach/detach, 403/404/409 cases

front-end/src/
├── lib/
│   ├── admin.ts                          # EDIT: AdminBooking type gains companion-student fields; ADD attachCompanionStudent(), detachCompanionStudent(); EDIT setBookingNoShow() to target the booking student or the companion
│   └── scheduling.ts                     # EDIT: BookingSummary type gains isCompanionStudent
└── components/
    ├── admin/bookings/
    │   ├── BookingsTab.tsx                # EDIT: show companion student (or "Attach companion student" action) per row; per-student no-show toggle
    │   └── AttachCompanionStudentDialog.tsx  # NEW: reuses StudentMultiSelect (capped to one selection) + getStudents()
    └── scheduling/MyBookings.tsx          # EDIT: show a small "shared with <name>" / "joining <name>'s class" indicator using isCompanionStudent
```

**Structure Decision**: Web application (existing `front-end/` + `back-end/`). No new backend domain package — a companion student is a nullable state on the existing `bookings` row (exactly like `extended_class_eligible` was a nullable-then-defaulted state on `users`), and the attach/detach actions are added to the existing `scheduling`/`admin` packages, not a new bounded context.

## Complexity Tracking

> No Constitution Check violations. No entries required.
