# Quickstart: Extended Booking Duration

Manual/browser validation — no automated frontend test framework exists in this repo (per CLAUDE.md); this is the acceptance script to run before marking the feature complete, alongside the backend JUnit suite.

## Prerequisites

1. Local dev environment running per the root `CLAUDE.md` setup: PostgreSQL 18 (`kuky_dev`), Mailpit on `:1025`/`:8025`, backend on `:8081` (`./gradlew bootRun --args='--spring.profiles.active=local'`), frontend on `:8080` (`npm run dev`).
2. Migration `V29__extended_booking_duration.sql` applied (Flyway runs automatically on backend startup).
3. Two test accounts already promoted to `STUDENT` via `/panel` → Users tab (or reuse existing dev seed students): call them **Student A** (will be granted extended-class eligibility) and **Student B** (will remain ineligible).
4. Teacher's weekly availability (`/panel` → Availability tab) has at least one window ≥ 90 minutes long, so a 1.5-hour slot has somewhere to exist.

## Scenario 1 — Teacher grants and revokes eligibility (User Story 1)

1. Log in as the teacher (`ADMIN`), open `/panel` → Students tab.
2. Find Student A, trigger the new "extended class" grant action.
3. Confirm: the action succeeds, Student A's row reflects eligible status, and Mailpit (`:8025`) shows a notification email sent to Student A.
4. Revoke it, confirm the row updates back and a revocation email appears in Mailpit.
5. Re-grant it (leave Student A eligible for the rest of this script). Confirm Student B is **not** granted anything.

## Scenario 2 — Eligible student books a 1.5-hour class (User Story 2)

1. Log in as Student A, open `/reservas`.
2. Confirm a 1-hour/1.5-hour duration toggle is visible (it should not be visible, or should be disabled, for Student B in Scenario 4).
3. Switch to 1.5-hour. Confirm the offered start times differ from the 1-hour list — only times with a full 90 contiguous free minutes appear (verify against the availability window from Prerequisite 4).
4. Pick a start time, confirm. Confirm the booking succeeds and a Zoom join link is returned.
5. In Mailpit, confirm the confirmation email (to both student and teacher) states the correct 90-minute time range.

## Scenario 3 — Duration is visible after booking (User Story 3)

1. Still as Student A, reload `/reservas` and check "My Bookings": the new booking should show a start–end range spanning 90 minutes (e.g. "14:00–15:30"), not just the start time.
2. Log in as the teacher, open `/panel` → Bookings (admin schedule) tab: the same booking should already show the correct 90-minute end time — this view needs no code change per `research.md` Decision 5, so this step is a regression check, not new behavior.

## Scenario 4 — Ineligible student is blocked (edge cases / FR-012)

1. Log in as Student B, open `/reservas`. Confirm only the 1-hour option is offered (no 1.5-hour toggle, or a disabled one).
2. Attempt to force a 1.5-hour booking directly against the API (e.g. via browser devtools `fetch` to `POST /api/v1/bookings` with `durationMinutes: 90`) while authenticated as Student B.
3. Confirm the server rejects it with `403 EXTENDED_CLASS_NOT_ELIGIBLE` — the block must hold even when the UI is bypassed.

## Scenario 5 — Overlap correctness across mixed durations (FR-002, FR-009)

1. As Student A, book a 1-hour class starting at some time T within an open availability window.
2. Reload the schedule with the 1.5-hour toggle active. Confirm no start time between `T - 90min` and `T + 60min` that would overlap the just-made booking is offered.
3. Attempt (via devtools, as in Scenario 4) to force-create a 90-minute booking starting at `T - 30min` (which would overlap the existing 1-hour booking from `T` to `T+60min`). Confirm it's rejected with `409 SLOT_UNAVAILABLE`, not silently accepted or a 500 error.

## Scenario 6 — Cancellation frees the full window (edge case)

1. Cancel the 1.5-hour booking made in Scenario 2.
2. Reload the schedule at both 60- and 90-minute durations. Confirm the full 90-minute window is now offered again at both durations.

## Regression check

1. As a student with no extended-class eligibility, book, view, and cancel a plain 1-hour class end-to-end. Confirm no behavior differs from before this feature (SC-004).
