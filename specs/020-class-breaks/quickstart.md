# Quickstart: Class Booking Buffer Time

Manual/browser validation — no automated frontend test framework exists in this repo (per CLAUDE.md); this is the acceptance script to run before marking the feature complete, alongside the backend JUnit suite.

## Prerequisites

1. Local dev environment running per the root `CLAUDE.md` setup: PostgreSQL 18 (`kuky_dev`), Mailpit on `:1025`/`:8025`, backend on `:8081` (`./gradlew bootRun --args='--spring.profiles.active=local'`), frontend on `:8080` (`npm run dev`).
2. No migration needed — this feature has no schema change.
3. A test account promoted to `STUDENT` via `/panel` → Users tab (or reuse an existing dev seed student): **Student A**.
4. Teacher's weekly availability (`/panel` → Availability tab) has at least one window of 2+ contiguous hours, so there's room to book two classes and observe the gap between them.

## Scenario 1 — Buffer hides near-adjacent slots (User Story 1)

1. Log in as Student A, open `/reservas`, and book a 1-hour class at some open time `T`.
2. Reload the schedule for that day. Confirm no slot between `T` and `T + 1h15m` is offered as bookable (`OPEN`), and no slot between `T - 15m` and `T` is offered either.
3. Confirm the slot exactly at `T + 1h15m` (if it falls on the existing grid and inside the availability window) **is** offered.
4. Confirm the just-booked slot itself still shows the "Reservado"/booked label — only the newly hidden neighbors lost their offered status, not the booking itself.

## Scenario 2 — Attempting to force a too-close booking is rejected (User Story 1)

1. As Student A (or via browser devtools `fetch`), attempt `POST /api/v1/bookings` with `slotStart` 5–14 minutes after the booking from Scenario 1 ends.
2. Confirm the server rejects it with `409 SLOT_UNAVAILABLE` — the block holds even when the UI (which already hid the slot) is bypassed.
3. Repeat with a `slotStart` exactly 15 minutes after the prior booking ends. Confirm it **succeeds**.

## Scenario 3 — Buffer holds regardless of who books adjacent (User Story 2)

1. As the same Student A who already has a booking ending at some time `E`, attempt to book another class starting 5 minutes after `E`.
2. Confirm it is rejected the same way (`409 SLOT_UNAVAILABLE`) as it would be for a different student — the buffer isn't bypassable by booking against your own class.

## Scenario 4 — Cancellation frees the buffer (User Story 3)

1. Cancel the booking made in Scenario 1.
2. Reload the schedule. Confirm slots that were previously `UNAVAILABLE` only because of that booking's buffer are now `OPEN` again (subject to lead time / any other remaining bookings).

## Scenario 5 — Day-window boundaries need no buffer (FR-008, edge case)

1. Find a day where the teacher's availability window opens with no earlier booking that day.
2. Confirm the very first slot of that window is `OPEN` at exactly the window's opening time — it is not pushed 15 minutes later just because it's the first class of the day.

## Scenario 6 — Pre-existing close bookings are unaffected (FR-009, regression check)

1. This scenario cannot be produced fresh after rollout (the buffer now prevents creating such a pair); if any pre-existing seed/dev data already contains two confirmed bookings less than 15 minutes apart, confirm neither is cancelled, modified, or flagged by this feature — they remain visible and unchanged in "My Bookings" / the admin schedule.
2. If no such pre-existing pair exists in the dev database, skip this step; it is a non-regression guarantee that falls out of the design (research.md Decision 5) rather than a new code path to exercise.

## Regression check

1. As Student A, book, view, and cancel a plain 1-hour class with no other nearby bookings end-to-end. Confirm no behavior differs from before this feature (SC-001–SC-003 all describe the *presence* of a new constraint, not a change to the unconstrained case).
