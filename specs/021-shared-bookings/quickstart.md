# Quickstart: Shared Bookings

Manual/browser validation — no automated frontend test framework exists in this repo (per CLAUDE.md); this is the acceptance script to run before marking the feature complete, alongside the backend JUnit suite.

## Prerequisites

1. Local dev environment running per the root `CLAUDE.md` setup: PostgreSQL 18 (`kuky_dev`), Mailpit on `:1025`/`:8025`, backend on `:8081` (`./gradlew bootRun --args='--spring.profiles.active=local'`), frontend on `:8080` (`npm run dev`).
2. Migration `V30__shared_bookings.sql` applied (Flyway runs automatically on backend startup).
3. Three test accounts promoted to `STUDENT` via `/panel` → Users tab: **Student A** (will book the class), **Student B** (will be attached as the companion student), **Student C** (a spare, for the "cap of two" and "not eligible" checks). Ensure Student B does **not** have extended-class eligibility granted.

## Scenario 1 — Teacher attaches a companion student (User Story 1)

1. Log in as Student A, book a normal 1-hour class via `/reservas`.
2. Log in as the teacher (`ADMIN`), open `/panel` → Bookings tab. Find Student A's new booking.
3. Trigger "attach companion student," pick Student B from the picker, confirm.
4. Confirm the row now shows both Student A and Student B on the same class.
5. Attempt to attach Student C to the same booking. Confirm it's rejected (`409 COMPANION_ALREADY_ATTACHED`) — a class supports at most two students.
6. Attempt to attach Student A (the booking student) as the companion student on their own booking. Confirm it's rejected (`409 COMPANION_SAME_AS_BOOKING_STUDENT`).

## Scenario 2 — Companion student sees and can join the class (User Story 2)

1. Check Mailpit (`:8025`) — confirm Student B received an email with the class's date, time, and Zoom join link, sent at attach time (Scenario 1 step 3).
2. Confirm Student A did **not** receive a new email for the attachment itself (FR-015 — passive discovery only).
3. Log in as Student B, open `/reservas` → "My Bookings." Confirm the class appears, distinguishable as a class they're joining rather than one they booked themselves.

## Scenario 3 — Teacher removes a companion student added by mistake (User Story 3)

1. As the teacher, on the same booking, trigger "remove companion student."
2. Confirm the booking reverts to showing only Student A.
3. Log in as Student B, confirm the class no longer appears in their "My Bookings."
4. Confirm Student A's booking is otherwise untouched (same slot, same Zoom link, still confirmed).

## Scenario 4 — Extended-class eligibility gates the companion student (FR-014)

1. Grant Student A extended-class eligibility (`/panel` → Students tab) and have Student A book a 1.5-hour class.
2. As the teacher, attempt to attach Student B (not extended-eligible) to that 1.5-hour booking. Confirm it's rejected (`403 EXTENDED_CLASS_NOT_ELIGIBLE`).
3. Grant Student B extended-class eligibility, retry the attach. Confirm it now succeeds.

## Scenario 5 — Either student can cancel a shared class, and both are notified (FR-011, FR-013)

1. Re-attach Student B to a fresh 1-hour booking made by Student A.
2. Log in as Student B, cancel the class from their own "My Bookings."
3. Confirm the class is fully cancelled: it disappears from Student A's "My Bookings" too, and from the teacher's Bookings tab (or shows as cancelled).
4. In Mailpit, confirm **both** Student A and Student B received a cancellation email, even though only Student B initiated it.

## Scenario 6 — Per-student no-show tracking (FR-010)

1. Book and attach a companion student to a class whose `slotStart` is in the past (or use an admin/dev shortcut if one exists for backdating a booking; otherwise wait for a real past class in a dev scenario).
2. As the teacher, mark only the booking student as a no-show. Confirm the companion student's attendance remains unaffected (not marked).
3. Independently mark the companion student as a no-show. Confirm both flags are now set correctly and independently.

## Regression check

1. As a student with no companion student ever attached, book, view, and cancel a plain single-student class end-to-end. Confirm no behavior differs from before this feature (unchanged FR-001).
2. As the teacher, confirm the existing single-student no-show toggle (no `studentRole` specified) still defaults to the booking student, unchanged from today.
