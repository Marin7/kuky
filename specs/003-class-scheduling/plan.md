# Implementation Plan: 1-on-1 Class Scheduling

**Branch**: `003-class-scheduling` | **Date**: 2026-06-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-class-scheduling/spec.md`

## Summary

Add a public class schedule and authenticated booking flow to the Kuky platform. Logged-out visitors can browse Paula's open 1-on-1 slots for the current and next calendar week on the `/reservas` page; logged-in students can book an open slot, which reserves it exclusively and automatically provisions a Zoom meeting whose join link is shown to the student and emailed to both parties. Students can view and cancel their upcoming bookings (up to 24 h before start), which frees the slot and cancels the Zoom meeting.

The backend extends the existing Java 26 / Spring Boot 4 service (plain JDBC + Flyway, no JPA) with a new `scheduling` package. Slots are **virtual** — computed on demand from a fixed daily 09:00–18:00 window in Paula's timezone (configurable duration, default 60 min) minus existing confirmed bookings — so no availability table is needed for this phase and the availability source stays isolated behind a service for the future teacher-managed phase. Concurrency safety comes from a Postgres partial unique index on the booked slot start. Zoom meetings are created via the Zoom Server-to-Server OAuth Meetings API using Spring's `RestClient`; when Zoom credentials are absent locally the integration degrades to a logged placeholder (mirroring the existing Mailpit-optional email behaviour). The frontend converts the placeholder `/reservas` route into a real schedule + bookings experience built from named React components, with a new isolated API client in `src/lib/scheduling.ts`.

## Technical Context

**Language/Version**: TypeScript 5.x strict (frontend) / Java 26 (backend, per `back-end/build.gradle` toolchain)

**Primary Dependencies**:
- Frontend: React 19, TanStack Start 1.x (SSR), TanStack Router, TanStack Query (already wired in `__root.tsx`), TailwindCSS 4, Shadcn UI, Vite 7
- Backend: Spring Boot 4.0.x (`spring-boot-starter-web`, `-security`, `-jdbc`, `-mail`, `-validation`), Flyway, PostgreSQL driver, jjwt 0.12.6. **No new dependencies** — Zoom calls use Spring's built-in `RestClient` (part of `spring-web`); JSON via the Jackson already on the classpath.

**Storage**: PostgreSQL 18 — one new table `bookings` (Flyway `V3__create_bookings.sql`). Slots are not persisted (computed).

**Testing**:
- Backend: JUnit 5 + Spring Boot Test (`spring-boot-starter-test` already present). Unit tests for slot generation and booking validation; slice/integration tests for the controller.
- Frontend: visual verification in a running browser per the constitution (no test framework currently configured in the repo).

**Target Platform**: Browser via TanStack Start SSR (frontend) + JVM server on port 8081 (backend REST API)

**Project Type**: Full-stack web application — SSR React frontend (`front-end/`) + REST API backend (`back-end/`)

**Performance Goals**: Schedule view loads and is understandable < 30 s (SC-001); booking completes < 2 min (SC-002); booked/cancelled slots reflected in the public view within seconds (SC-005). All comfortably met with standard request latencies; the only slow path is the Zoom API call during booking (acceptable, user is shown progress).

**Constraints**:
- Booking requires authentication (reuse existing JWT-cookie auth); slot viewing is public.
- Booking horizon = current calendar week + next calendar week, rolling automatically (FR-020); week starts Monday.
- Minimum booking lead time = 24 h before slot start (FR-021); cancellation cutoff = 24 h before start (FR-013).
- Standard class duration = 60 min, configurable via app config (FR-017).
- A slot may have at most one active (CONFIRMED) booking, enforced under concurrency (FR-007).
- A booking is never finalized without a usable Zoom meeting; on Zoom failure the slot stays open (FR-011).
- Times stored as UTC instants (`TIMESTAMPTZ`); Paula's availability authored in her timezone; viewers see their local timezone (FR-003).

**Scale/Scope**: Small — one teacher, dozens to low hundreds of students; single-instance backend. At most ~9 slots/day × 14 days ≈ 126 candidate slots per view.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I — Simplicity First | ✅ PASS | Virtual slots (no slot table/CRUD), single new `bookings` table, no new dependencies (RestClient + Jackson already present), fixed availability from config. Zoom degrades to a local stub. No waitlist, no payment, no teacher UI this phase. |
| II — Component-Driven UI | ✅ PASS | Schedule, slot grid, booking dialog, and my-bookings list are named Shadcn-based React components; no raw DOM manipulation. |
| III — Evolution-Ready Architecture | ✅ PASS | API calls isolated in `front-end/src/lib/scheduling.ts`; slot generation isolated in a backend `AvailabilityService` (swappable for teacher-managed availability later) and Zoom behind a `MeetingProvider` abstraction; booking data model designed to accept a future paid-booking flow. |
| Technology Stack | ✅ PASS | Frontend stack unchanged; backend continues Java 26 + Spring Boot 4 + plain JDBC + Flyway as already established. No stack additions. |
| Development Workflow | ✅ PASS | UI changes browser-verified before completion; feature branch `003-class-scheduling` follows the naming convention. |
| Backend location | ✅ PASS | New code under `back-end/` and `front-end/` only. |

**Gate result**: All principles satisfied. No complexity violations requiring justification — Complexity Tracking section omitted.

## Project Structure

### Documentation (this feature)

```text
specs/003-class-scheduling/
├── plan.md              # This file
├── research.md          # Phase 0 — Zoom integration, slot strategy, concurrency, timezone decisions
├── data-model.md        # Phase 1 — bookings table + virtual slot model
├── quickstart.md        # Phase 1 — local validation guide
├── contracts/
│   └── api.md           # Phase 1 — REST API contract for schedule + bookings
├── checklists/
│   └── requirements.md  # Spec quality checklist (from /speckit-specify)
└── tasks.md             # Phase 2 — task list (/speckit-tasks output, not created here)
```

### Source Code (repository root)

```text
kuky/
├── front-end/                                  # Existing React/TanStack SSR app
│   └── src/
│       ├── routes/
│       │   └── reservas.tsx                     # REPLACED: schedule + booking + my-bookings page
│       ├── components/
│       │   └── scheduling/                      # NEW component folder
│       │       ├── ScheduleView.tsx             # Week navigation + slot grid container
│       │       ├── SlotGrid.tsx                 # Renders open/unavailable slots for a week
│       │       ├── BookingDialog.tsx            # Confirm-booking modal (Shadcn Dialog)
│       │       └── MyBookings.tsx               # Logged-in user's upcoming/past bookings + cancel
│       └── lib/
│           └── scheduling.ts                    # NEW: API client (getSchedule, createBooking, listBookings, cancelBooking)
└── back-end/                                    # Existing Spring Boot service
    └── src/
        ├── main/
        │   ├── java/com/kuky/backend/
        │   │   ├── scheduling/                  # NEW package (mirrors auth/ layout)
        │   │   │   ├── controller/ScheduleController.java   # GET /api/v1/schedule (public)
        │   │   │   ├── controller/BookingController.java    # POST/GET/DELETE /api/v1/bookings (auth)
        │   │   │   ├── dto/                                 # SlotResponse, BookingResponse, CreateBookingRequest
        │   │   │   ├── model/Booking.java                   # POJO
        │   │   │   ├── model/Slot.java                      # value object (virtual)
        │   │   │   ├── repository/BookingRepository.java    # NamedParameterJdbcTemplate
        │   │   │   ├── service/AvailabilityService.java     # generates virtual slots from config + bookings
        │   │   │   ├── service/BookingService.java          # reserve → Zoom → notify; cancel
        │   │   │   ├── service/BookingEmailService.java     # confirmation/cancellation emails (reuses JavaMailSender)
        │   │   │   ├── meeting/MeetingProvider.java         # interface: create/cancel meeting
        │   │   │   ├── meeting/ZoomMeetingProvider.java     # Server-to-Server OAuth via RestClient
        │   │   │   ├── meeting/StubMeetingProvider.java     # local fallback when Zoom creds absent
        │   │   │   └── exception/                           # SlotUnavailableException, BookingNotAllowedException, MeetingProvisioningException
        │   │   └── config/
        │   │       ├── SecurityConfig.java                  # UPDATED: permit GET /api/v1/schedule; bookings stay authenticated
        │   │       ├── GlobalExceptionHandler.java          # UPDATED: map new scheduling error codes
        │   │       └── SchedulingProperties.java            # NEW: @ConfigurationProperties for app.scheduling / app.zoom
        │   └── resources/
        │       ├── application.yaml                         # UPDATED: app.scheduling + app.zoom blocks
        │       ├── application-local.yaml                   # UPDATED: local-friendly defaults
        │       └── db/migration/
        │           └── V3__create_bookings.sql              # NEW migration
        └── test/java/com/kuky/backend/
            └── scheduling/
                ├── AvailabilityServiceTest.java
                └── BookingControllerIntegrationTest.java
```

**Structure Decision**: Extend the existing two-service layout. The backend gains a self-contained `scheduling` package that mirrors the established `auth` package conventions (controller / dto / model / repository / service, plain JDBC repositories, POJO models, records for DTOs). The frontend converts the existing `/reservas` placeholder into the schedule experience and adds a `components/scheduling/` folder plus an isolated `lib/scheduling.ts` API client following the existing `lib/auth.ts` pattern. No new top-level projects or infrastructure are introduced.
