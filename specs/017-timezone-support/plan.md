# Implementation Plan: Timezone Support

**Branch**: `017-timezone-support` | **Date**: 2026-07-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/017-timezone-support/spec.md`

## Summary

Students currently see class times either in the teacher's fixed time zone (`Europe/Madrid`) or, in a few inconsistent spots, the browser's local zone with no zone label at all — and booking/reminder emails hardcode raw UTC. This feature makes every student-facing time display (schedule, bookings, confirmations, reminder emails) consistently show the *student's own* local time zone, auto-detected each session and overridable, always labeled by region/city name, while the teacher's own admin views keep showing her fixed working time zone regardless of device. The underlying booking data is already a zone-independent UTC instant (`Instant`/`TIMESTAMPTZ`), so this is purely a display/conversion problem, not a storage or scheduling-logic change: add one nullable `users.timezone` (+ `timezone_is_manual`) column pair for the override, a small sync endpoint the front-end calls once per session, a shared front-end timezone hook wired into the components that are missing it or using the wrong source, and zone-aware formatting in the two backend email services that currently print raw UTC.

## Technical Context

**Language/Version**: Java 21 (back-end), TypeScript 5 / React 19 (front-end)

**Primary Dependencies**: None new. Back-end already uses `java.time` (`ZoneId`, `ZonedDateTime`, `DateTimeFormatter`) for all existing teacher-timezone conversions (`AvailabilityService`); front-end already uses native `Intl.DateTimeFormat` with a `timeZone` option in several components — no timezone library (`date-fns-tz`, `luxon`, `dayjs`+tz) is installed or needed, consistent with Simplicity First.

**Storage**: PostgreSQL 18. One additive Flyway migration (`V27`) adds `users.timezone VARCHAR(64) NULL` and `users.timezone_is_manual BOOLEAN NOT NULL DEFAULT false`. No changes to `bookings.slot_start` (`TIMESTAMPTZ`) or `week_availability` (naive local time, already correctly interpreted server-side via `app.scheduling.teacher-timezone`).

**Testing**: JUnit 5 + Spring Boot Test (back-end, existing pattern) for zone-aware email formatting and the timezone-sync endpoint. No front-end test framework exists in this repo (per `front-end/package.json`) — front-end verification is manual, in-browser, per `CLAUDE.md`'s Development Workflow principle, using the preview tool's `Intl.DateTimeFormat` zone override (`timeZone` option is testable without changing the OS/browser zone) plus `preview_resize`/`preview_eval` for spot checks.

**Target Platform**: Existing Spring Boot back-end (`back-end/`) + existing TanStack Start front-end (`front-end/`) — full-stack feature touching both.

**Project Type**: Web application (existing front-end + back-end).

**Performance Goals**: No new network round-trip on the critical booking path; the timezone-sync call is fire-and-forget on session load (not blocking schedule/booking rendering).

**Constraints**: Must never change the real-world instant a booking refers to (FR-004) — all conversion happens only at display/formatting time, using the existing `Instant` wire format. Must not regress the already-correct UTC-only `.ics` generation (feature 016) or the existing teacher-zone template/slot-generation logic in `AvailabilityService`.

**Scale/Scope**: Same low-volume single-tutor scope as the rest of the scheduling system — no batching, no async processing.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Simplicity First**: PASS. No new dependency (native `java.time` and `Intl.DateTimeFormat` cover every case, confirmed by research). The user preference is a single nullable column pair, not a new domain concept. The admin-panel fix (replacing hardcoded `"Europe/Madrid"` literals) reuses an already-exposed config value instead of adding new plumbing.
- **Component-Driven UI**: PASS. The new "time zone" account-settings control is a proper named component (extends the existing `cuenta.tsx` profile section pattern); no raw DOM manipulation.
- **Evolution-Ready Architecture**: PASS. A new `useTimezone()` hook centralizes detection/sync/override logic (mirroring the existing hooks pattern), and existing components (`CalendarPicker`, `TimeSlotList`, `MyBookings`, `SlotGrid`, `BookingDialog`) already accept a `timezone` prop — this feature corrects the *source* of that prop rather than introducing a new data-fetching pattern.

No violations — Complexity Tracking section not needed.

## Project Structure

### Documentation (this feature)

```text
specs/017-timezone-support/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md         # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/            # Phase 1 output (/speckit-plan command)
│   └── timezone.md
└── tasks.md              # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
back-end/
├── src/main/resources/db/migration/
│   └── V27__add_user_timezone.sql        # new — users.timezone, users.timezone_is_manual
├── src/main/java/com/kuky/backend/auth/
│   ├── model/User.java                   # existing — add timezone, timezoneIsManual fields
│   ├── repository/UserRepository.java    # existing — map new columns, add updateTimezone()
│   ├── dto/UserResponse.java             # existing — expose timezone, timezoneIsManual
│   ├── dto/UpdateTimezoneRequest.java    # new — { zone, manual }
│   ├── service/AuthService.java          # existing — add updateTimezone(email, request)
│   └── controller/AuthController.java    # existing — new PUT /api/v1/auth/timezone
├── src/main/java/com/kuky/backend/scheduling/service/
│   └── BookingEmailService.java          # existing — replace hardcoded UTC formatter with
│                                          #   per-recipient zone-aware formatting + zone label
│                                          #   (student's users.timezone, falling back to
│                                          #   teacher-timezone; teacher emails use teacher-timezone)
└── src/test/java/com/kuky/backend/
    ├── auth/service/AuthServiceTimezoneTest.java        # new — sync/override persistence rules
    └── scheduling/service/BookingEmailServiceTest.java  # existing/extended — zone-aware body assertions

front-end/
└── src/
    ├── hooks/
    │   └── use-timezone.ts               # new — detects browser zone, loads/syncs preference
    │                                      #   via getMe()/PUT timezone, exposes {zone, isManual, setZone}
    ├── lib/
    │   └── auth.ts                       # existing — UserResponse gains timezone/timezoneIsManual,
    │                                      #   add updateTimezone() API call
    ├── components/scheduling/
    │   ├── ScheduleView.tsx              # existing — feed useTimezone() zone instead of
    │                                      #   schedule.teacherTimezone into student-facing children
    │   ├── SlotGrid.tsx                  # existing — accept & apply timezone prop (currently
    │                                      #   silently browser-local)
    │   ├── BookingDialog.tsx             # existing — accept & apply timezone prop (currently
    │                                      #   hardcoded "es" locale, no zone)
    │   ├── CalendarPicker.tsx            # existing — no change (already zone-aware); source changes
    │   ├── TimeSlotList.tsx              # existing — no change (already zone-aware); source changes
    │   └── MyBookings.tsx                # existing — no change (already zone-aware); source changes
    ├── components/admin/availability/
    │   └── WeeklyAvailabilityEditor.tsx  # existing — use teacher timezone (currently browser-local)
    ├── components/admin/bookings/
    │   └── BookingsTab.tsx               # existing — replace hardcoded "Europe/Madrid" literal
    │                                      #   with the config-sourced teacher timezone
    ├── routes/
    │   ├── panel_.alumnos.$studentId.tsx # existing — same hardcoded-literal fix as BookingsTab
    │   └── cuenta.tsx                    # existing — add "Time zone" preference control
    └── components/account/
        └── TimezoneSetting.tsx           # new — dropdown/toggle: auto-detected vs manual override
```

**Structure Decision**: Existing two-project web app (`back-end/` + `front-end/`). No new packages/modules on either side — the feature extends the existing `auth` slice (user preference) and `scheduling` slice (email formatting) on the back-end, and adds one new hook + one new small settings component on the front-end, wiring existing scheduling components to the corrected data source.

## Complexity Tracking

*No Constitution Check violations — this section is intentionally empty.*
