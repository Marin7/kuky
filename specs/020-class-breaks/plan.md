# Implementation Plan: Class Booking Buffer Time

**Branch**: `020-class-breaks` | **Date**: 2026-07-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/020-class-breaks/spec.md`

## Summary

No two confirmed classes may sit closer than 15 minutes apart on the schedule — enforced purely as a wider overlap check, not a new data column. `AvailabilityService` already computes "is this candidate window free" as an exact-interval overlap test against confirmed bookings (`overlapsAny`), used both to mark a generated `Slot` `BOOKED` in `generateSchedule()` and to reject a booking attempt in `validateBookable()`. This feature parameterizes that overlap test with a buffer (`SchedulingProperties.Scheduling.bufferMinutes`, default 15): the exact-overlap check keeps marking truly occupied slots `BOOKED` (unchanged semantics), while a second, buffer-widened check marks near-adjacent slots `UNAVAILABLE` — the same status already used for past/lead-time-blocked slots, so the frontend requires **no changes at all** (`UNAVAILABLE` already renders as a plain disabled slot with no "Reservado" label, which is exactly the desired appearance for a slot that isn't actually booked but is off-limits for spacing reasons). `validateBookable()`'s single overlap check is replaced by the buffer-widened version, which is a strict superset of the exact-overlap case, so no separate "buffer-only" error path is needed — it's still the existing `409 SLOT_UNAVAILABLE`.

Because the buffer is evaluated only when generating candidate slots and validating new bookings — never by scanning or mutating existing `bookings` rows — pre-existing bookings that are already closer than 15 minutes apart are untouched by construction (FR-009), and the teacher's availability-window boundaries are never treated as an implicit neighboring booking (FR-008), satisfying both spec decisions with zero extra branching.

## Technical Context

**Language/Version**: Java 21 (backend only — no frontend change required).

**Primary Dependencies**: Spring Boot 3.5 (`-web`, `-jdbc`), existing `SchedulingProperties`, `AvailabilityService`, `BookingRepository`. **No new dependencies.**

**Storage**: PostgreSQL 18. **No migration** — the buffer is a runtime computation over existing `bookings.slot_start`/`duration_minutes` (via the existing `slot_end` column added in `V29__extended_booking_duration.sql`), not a stored fact. `bookings_no_overlap` (the GiST exclusion constraint) is left as exact-overlap only — see research.md Decision 3 for why it is not widened to the buffer.

**Testing**: Backend: JUnit 5 + Mockito + AssertJ (existing convention) — extend `AvailabilityServiceTest` (buffer widens `BOOKED`→ no, widens candidate rejection → `UNAVAILABLE`/`SlotUnavailableException`; boundary cases at exactly 15 minutes; day-window-edge slots unaffected) and `BookingServiceTest` (creating a booking 5–14 minutes from an existing one is rejected; exactly 15 minutes succeeds). Frontend: no test framework in this repo (per CLAUDE.md) — visual verification in a running browser per the constitution's Development Workflow, though this feature is expected to need no frontend code changes at all (see Summary).

**Target Platform**: JVM server on `:8081` (backend only for code changes); browser via TanStack Start SSR (`:8080`) for verification only.

**Performance Goals**: N/A — the buffer check reuses the same O(bookings in horizon) linear scan `overlapsAny` already performs; only the comparison bounds change, not the algorithm's order.

**Constraints**: The buffer must hold for every pair of confirmed bookings regardless of duration mix (60/90 minutes) and regardless of which student holds either booking (FR-003, FR-004) — satisfied automatically because the check operates on `(slotStart, durationMinutes)` intervals already duration-agnostic and student-agnostic since feature 019. The buffer must not apply at availability-window boundaries (FR-008) and must not retroactively affect bookings made before rollout (FR-009) — both satisfied by construction, not extra code (see Summary).

**Scale/Scope**: Single-teacher site, low tens of students, low tens of bookings per horizon. One new configuration property, one widened comparison function reused in two call sites, zero new endpoints, zero new database objects, zero frontend files touched.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Fixed, non-configurable-by-UI 15-minute constant sourced the same way `classDurationMinutes`/`minLeadHours` already are; reuses the existing overlap-check function with an added parameter instead of introducing a new concept (no new status, no new table, no new endpoint).
- **II. Component-Driven UI** — PASS (vacuously — no UI components are touched; `UNAVAILABLE` slots already render correctly via the existing `SlotGrid.tsx` without modification).
- **III. Evolution-Ready Architecture** — PASS. The new `bufferMinutes` property lives in `SchedulingProperties`, the same service-module location every other scheduling constant already lives in; no logic is inlined into a component.
- **Technology Stack** — PASS. No new dependencies on either side.
- **Development Workflow** — PASS. Branch `020-class-breaks` follows the git extension's naming convention. Browser verification will still be performed (schedule shows fewer OPEN slots around an existing booking) even though no frontend source changes.

**Result: PASS — no violations. Complexity Tracking left empty.**

## Project Structure

### Documentation (this feature)

```text
specs/020-class-breaks/
├── plan.md                       # This file (/speckit-plan output)
├── research.md                   # Phase 0 output
├── data-model.md                 # Phase 1 output
├── quickstart.md                 # Phase 1 output
├── contracts/
│   └── schedule-buffer.md        # Phase 1 output
├── checklists/
│   └── requirements.md           # /speckit-specify output
└── tasks.md                      # /speckit-tasks output (NOT created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/
├── config/
│   └── SchedulingProperties.java          # EDIT: add bufferMinutes (default 15) alongside classDurationMinutes
└── scheduling/service/
    └── AvailabilityService.java           # EDIT: parameterize overlapsAny() by bufferMinutes; generateSchedule()
                                            #       gains a second (buffer-widened) check → UNAVAILABLE;
                                            #       validateBookable()'s overlap check uses the buffer-widened
                                            #       version and widens its BookingRepository query range

back-end/src/test/java/com/kuky/backend/scheduling/
├── AvailabilityServiceTest.java           # EXTEND: buffer-widened BOOKED-vs-UNAVAILABLE cases, boundary cases
└── BookingServiceTest.java                # EXTEND: booking rejected within buffer, accepted at exactly 15 min

# NOT edited: no frontend files, no database migration, no new controller/DTO/exception —
# the existing 409 SLOT_UNAVAILABLE path already used for overlap already covers buffer
# rejection (validateBookable throws the same SlotUnavailableException); Slot.Status.UNAVAILABLE
# already exists and already renders correctly in SlotGrid.tsx.
```

**Structure Decision**: Single-package edit within the existing `scheduling` domain — no new backend package, no new frontend directory. This is the smallest possible change that satisfies every functional requirement in the spec: one new configuration constant and one widened comparison used at the two existing call sites that already gate slot visibility and booking creation.

## Complexity Tracking

> No Constitution Check violations. No entries required.
