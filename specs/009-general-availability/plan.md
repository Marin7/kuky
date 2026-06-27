# Implementation Plan: General Availability Template

**Branch**: `009-general-availability` | **Date**: 2026-06-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/009-general-availability/spec.md`

## Summary

Split availability into two layers. The **general template** (the existing `availability_rules` weekly pattern, today editable only indirectly) becomes a first-class, directly-editable "default week." Each concrete week within the existing ~2-week bookable horizon gets its own **materialized snapshot** of that template the first time it enters the window; from then on the snapshot is the booking source of truth and is edited per-day, independently of the template and of other weeks.

Technical approach: add a per-date `week_availability` table plus a `materialized_weeks` marker, lazily snapshot weeks from the template at read time, compute the public schedule and booking validation from the snapshot instead of `rules ∪ OPEN − BLOCK`, and replace the per-date BLOCK/OPEN exception editing with absolute per-day window editing. A one-time startup bootstrap materializes the current horizon from today's rules+exceptions so launch is behavior-preserving.

## Technical Context

**Language/Version**: Java 21 (back-end), TypeScript 5 strict (front-end)

**Primary Dependencies**: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway 11 (back-end); React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI (front-end)

**Storage**: PostgreSQL 18 — new tables `week_availability`, `materialized_weeks`; existing `availability_rules` repurposed as the template; `availability_exceptions` retained read-only for the migration bootstrap then deprecated

**Testing**: JUnit (`./gradlew test`), focused on `AvailabilityServiceTest`; manual browser verification for the admin UI per constitution

**Target Platform**: Linux server (Spring Boot) + SSR web front-end

**Project Type**: Web application (separate `back-end/` and `front-end/`)

**Performance Goals**: Schedule generation stays a single-request synchronous operation over a 2-week horizon (≤ 14 days × a handful of windows); materialization adds at most one bounded write per new week, amortized to near-zero per request

**Constraints**: All times in teacher timezone (`Europe/Madrid`); 60-min slots, 24-h min lead unchanged; snapshot semantics must not retroactively alter already-materialized weeks; launch must preserve the exact schedule students see today

**Scale/Scope**: Single teacher, single calendar; tens of bookings/week; 2-week rolling window

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)**: PASS. New storage and the materialization step are a concrete present need created by the clarified "snapshot per week" requirement — without persisted per-week state, "the existing calendar is the source of truth" cannot hold when the template changes. We deliberately reject the rejected-by-clarification options (configurable horizon, one-click reset) and **remove** the BLOCK/OPEN exception delta machinery, a net simplification. Interval merge logic is reused, not reinvented.
- **II. Component-Driven UI**: PASS. The general-template editor is a new named, reusable React component; the fortnight grid is refactored in place. No raw DOM work.
- **III. Evolution-Ready Architecture**: PASS. All data fetching stays in `front-end/src/lib/admin.ts`; components consume typed service functions. Back-end keeps the controller→service→repository layering.
- **Development Workflow**: Admin UI changes will be verified in a running browser before completion; branch already follows the `009-general-availability` convention.

No violations → Complexity Tracking table intentionally empty.

## Project Structure

### Documentation (this feature)

```text
specs/009-general-availability/
├── plan.md              # This file
├── spec.md              # Feature spec (with Clarifications)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── availability-api.md   # Admin availability endpoints (updated)
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
back-end/src/main/
├── java/com/kuky/backend/
│   ├── config/
│   │   └── WeekAvailabilityBootstrap.java        # NEW one-time migration runner
│   ├── scheduling/
│   │   ├── model/
│   │   │   └── DayWindow.java                     # NEW (date + start/end window)
│   │   ├── repository/
│   │   │   └── AvailabilityRepository.java        # +week_availability & materialized_weeks methods
│   │   └── service/
│   │       └── AvailabilityService.java           # materialization + snapshot-based computation
│   └── admin/
│       ├── controller/AvailabilityAdminController.java   # template PUT + new per-day PUT; drop /exceptions
│       └── dto/
│           ├── AvailabilityResponse.java          # shape change: weekly + days
│           ├── DayAvailabilityDto.java            # NEW (date + windows)
│           ├── DayWindowDto.java                  # NEW (startTime/endTime)
│           └── UpdateDayRequest.java              # NEW
└── resources/db/migration/
    └── V17__week_availability.sql                 # NEW tables (next free version after V16)

front-end/src/
├── components/admin/availability/
│   ├── AvailabilityTab.tsx                        # two sections: general + fortnight
│   ├── GeneralAvailabilityEditor.tsx             # NEW weekly Mon–Sun template grid
│   ├── WeeklyAvailabilityEditor.tsx              # refactor → absolute per-day windows
│   └── BookingConflictNotice.tsx                 # unchanged
├── lib/admin.ts                                  # types + getAvailability/updateWeekly/setDayAvailability
└── i18n/locales/{es,ro,en}.ts                    # new strings
```

**Structure Decision**: Existing two-project web layout (`back-end/`, `front-end/`). All changes land in the already-established `scheduling` and `admin` back-end packages and the `admin/availability` front-end folder; no new top-level modules.

## Phase 0 — Research

See [research.md](research.md). All Technical Context items are resolved (no NEEDS CLARIFICATION); research focuses on the three design decisions that carry risk: snapshot storage shape, materialization trigger, and behavior-preserving migration.

## Phase 1 — Design & Contracts

- [data-model.md](data-model.md) — entities, tables, and the availability-resolution algorithm.
- [contracts/availability-api.md](contracts/availability-api.md) — updated admin availability endpoints.
- [quickstart.md](quickstart.md) — end-to-end validation scenarios.

**Post-design Constitution re-check**: PASS — no new violations introduced; the design removes more complexity (exception deltas) than it adds.

## Complexity Tracking

No constitution violations — table intentionally empty.
