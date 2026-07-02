# Implementation Plan: Student Progress on Teacher Profile View

**Branch**: `015-student-progress` | **Date**: 2026-07-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/015-student-progress/spec.md`

## Summary

The teacher's existing student-profile page (`/panel` → Students → a student) gains a "Progreso" section that answers "how is this student doing?" without cross-referencing the page's separate bookings/homework/unit lists: per-unit curriculum completion, a pending/submitted/completed homework breakdown, an attended-classes count, and the student's latest CEFR level (reusing the already-fetched placement result). Curriculum completion and the homework breakdown are entirely derived from existing data (one new SQL query for units, in-memory grouping for homework — no new tables). Attendance needed one small schema addition: bookings gained a `no_show` boolean (default attended, per the clarification), with a new teacher-only endpoint to mark/unmark it, since the existing `CONFIRMED`/`CANCELLED` booking status has no way to represent "the class happened but the student didn't show."

This is an extension of an existing feature (the student-profile page and its backing service/controller/DTOs), not a new domain — one migration, edits to the existing `StudentProfileAdminService`/`BookingService`/`BookingRepository`/`UnitRepository`, one new endpoint on the existing `BookingAdminController`, and frontend additions to the existing profile route and `admin.ts`.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5 (`-web`, `-security`, `-jdbc`), Flyway, PostgreSQL driver. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI. **No new dependencies.**

**Storage**: PostgreSQL 18. New migration `V26__add_booking_no_show.sql` adds one column, `bookings.no_show BOOLEAN NOT NULL DEFAULT FALSE`. No new tables — curriculum/homework progress is computed from existing `units`, `unit_assignments`, `homework_assignments`, `homework_targets`, `homework_submissions` tables.

**Testing**:
- Backend: JUnit 5 + Spring Boot Test + `spring-security-test`. New/extended: `BookingServiceTest` (no-show eligibility: past+confirmed required, rejects future/cancelled), `BookingAdminControllerTest` (new endpoint auth + happy path), a test covering `UnitRepository.findProgressForStudent` (unit with untargeted homeworks doesn't inflate totals; unit with zero targeted homeworks isn't "complete"), and an extension of the existing student-profile service/controller test to cover the new `progress` field and homework-breakdown grouping.
- Frontend: visual verification in a running browser per the constitution (no test framework in repo) — view a seeded student's progress section, submit/grade homework and confirm counts move, mark/unmark a no-show and confirm the attended count updates.

**Target Platform**: Browser via TanStack Start SSR (frontend, `:8080`) + JVM server on `:8081` (backend).

**Performance Goals**: N/A — single-teacher site, low tens of students, a handful of units/bookings/homeworks each; the new unit-progress query is a single indexed join, no pagination or caching needed.

**Constraints**: Curriculum completion must route through `homework_targets` (not just `homework_assignments.unit_id`), because the codebase explicitly documents `unit_id` as organisational-only and never authoritative for student assignment (`V18__create_units.sql`) — see [research.md](research.md) §2. No-show eligibility (past + `CONFIRMED` only) is enforced in the service layer, mirroring the existing `cancelBookingAsAdmin` validation shape, not as a DB constraint.

**Scale/Scope**: Single-teacher site. One migration (one column), edits to four existing backend classes (`Booking`, `BookingRepository`, `BookingService`, `UnitRepository`, `StudentProfileAdminService`) plus their DTOs, one new endpoint on an existing controller, one new `BookingNotAllowedException.Reason`, and frontend edits to one existing route (`panel_.alumnos.$studentId.tsx`) and `admin.ts` — no new pages, packages, or security matchers.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. No new "progress" table or history/audit log — everything except the one no-show boolean is computed on read from data that already exists. Homework breakdown reuses an existing query and aggregates in Java rather than adding a second SQL query for the same rows.
- **II. Component-Driven UI** — PASS. The new "Progreso" section and the no-show toggle are additions within the existing `panel_.alumnos.$studentId.tsx` route, reusing its existing `Section`/`StatusBadge` components; no raw DOM manipulation, no new component library.
- **III. Evolution-Ready Architecture** — PASS. All new data-fetching (`setBookingNoShow`) lives in `front-end/src/lib/admin.ts`, the existing service module for this page — never inlined in a component.
- **Technology Stack** — PASS. No new dependencies; reuses `NamedParameterJdbcTemplate`, the existing `/api/v1/admin/**` Spring Security matcher, and the existing exception/`GlobalExceptionHandler` pattern for the new error case.
- **Development Workflow** — PASS. UI changes verified in a running browser before completion; branch `015-student-progress` follows convention; no dead code.

**Result: PASS — no violations. Complexity Tracking left empty.**

## Project Structure

### Documentation (this feature)

```text
specs/015-student-progress/
├── plan.md              # This file (/speckit-plan output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── progress-api.md  # Phase 1 output
├── checklists/
│   └── requirements.md  # /speckit-specify output
└── tasks.md              # /speckit-tasks output (NOT created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V26__add_booking_no_show.sql              # NEW: bookings.no_show BOOLEAN DEFAULT FALSE

back-end/src/main/java/com/kuky/backend/
├── scheduling/
│   ├── model/Booking.java                     # EDIT: add noShow field
│   ├── repository/BookingRepository.java       # EDIT: map no_show column; add setNoShow(id, noShow)
│   ├── service/BookingService.java             # EDIT: add setNoShow(bookingId, noShow) with eligibility check
│   └── exception/BookingNotAllowedException.java  # EDIT: add Reason.NOT_ELIGIBLE_FOR_NO_SHOW
├── units/
│   └── repository/UnitRepository.java          # EDIT: add findProgressForStudent(studentId) + UnitProgressView record
├── admin/
│   ├── controller/BookingAdminController.java  # EDIT: add PUT /{id}/no-show
│   ├── controller/StudentAdminController.java  # unchanged (delegates to service)
│   ├── service/StudentProfileAdminService.java # EDIT: assemble StudentProgressDto (units, homework breakdown, attended count)
│   ├── dto/StudentProfileResponse.java         # EDIT: add progress field
│   ├── dto/StudentProfileBookingDto.java       # EDIT: add noShow field
│   ├── dto/StudentProgressDto.java             # NEW
│   ├── dto/UnitProgressDto.java                # NEW
│   ├── dto/HomeworkBreakdownDto.java           # NEW
│   └── dto/SetNoShowRequest.java               # NEW: { noShow: boolean }
└── config/GlobalExceptionHandler.java          # EDIT: map NOT_ELIGIBLE_FOR_NO_SHOW → 422 BOOKING_NOT_ELIGIBLE_FOR_NO_SHOW

back-end/src/test/java/com/kuky/backend/
├── scheduling/service/BookingServiceTest.java              # EDIT: add setNoShow cases
├── admin/controller/BookingAdminControllerTest.java         # EDIT: add no-show endpoint cases
├── units/repository/UnitRepositoryTest.java                 # EDIT: add findProgressForStudent cases
└── admin/service/StudentProfileAdminServiceTest.java        # EDIT: add progress assembly cases

front-end/src/
├── lib/admin.ts                                # EDIT: StudentProfileBooking.noShow, StudentProgress/UnitProgress/HomeworkBreakdown types, StudentProfile.progress, setBookingNoShow()
└── routes/panel_.alumnos.$studentId.tsx        # EDIT: render Progreso section (unit list, homework breakdown, attended count, CEFR level); no-show toggle on past-booking rows
```

**Structure Decision**: Web application (existing `front-end/` + `back-end/`). This feature deliberately touches no new top-level backend package — it extends the existing `scheduling` (bookings), `units`, and `admin` (student-profile assembly, booking admin controller) packages exactly where their equivalent existing functionality already lives, since "student progress" isn't a new domain but a read (and one small write) over data three existing domains already own.

## Complexity Tracking

> No Constitution Check violations. No entries required.
