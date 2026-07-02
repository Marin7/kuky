# Quickstart: Validate Calendar (.ics) Attachment

## Prerequisites

- Local dev environment set up per the repo `CLAUDE.md` (PostgreSQL `kuky_dev`, Mailpit running on port 1025 / UI at http://localhost:8025).
- Back-end running: `./gradlew bootRun --args='--spring.profiles.active=local'` (http://localhost:8081).
- Front-end running: `npm run dev` in `front-end/` (http://localhost:8080).
- A test user account with `STUDENT` role (see `CLAUDE.md` — teacher promotes via the admin panel, or seed data already grants this to a known test account).

## Scenario 1 — Booking confirmation carries a valid attachment (User Story 1)

1. Log in as the student test account and go to `/reservas`.
2. Book any open slot.
3. Open Mailpit (http://localhost:8025) and find the "Reserva confirmada" email sent to the student.
4. Confirm the email has a `clase.ics` attachment (in addition to the existing plain-text body).
5. Download the attachment and open it with a calendar app (or inspect its contents directly) and verify:
   - `DTSTART`/`DTEND` match the booked slot's date/time (in UTC, `Z` suffix) and the class duration.
   - `SUMMARY` identifies it as a class with Paula.
   - `DESCRIPTION`/`LOCATION` contains the Zoom join link shown in the booking confirmation.
6. Import the file into a calendar app and confirm a new event appears at the correct **local** time (i.e., correctly converted from UTC by the calendar app).

**Expected result**: A single new calendar event, correctly timed, with the Zoom link included — matching FR-001–FR-003, SC-001, SC-002.

## Scenario 2 — Teacher notification also carries the attachment (User Story 3)

1. Repeat step 2 above (book a class as the student).
2. In Mailpit, find the "Nueva reserva" email sent to the teacher address (`app.scheduling.teacher-email`).
3. Confirm it also has a `clase.ics` attachment describing the same class at the same time.

**Expected result**: Matches FR-006.

## Scenario 3 — Cancellation updates/cancels the calendar entry (User Story 2 + FR-010)

1. As the student, cancel the booking made in Scenario 1 (from `/reservas` → "Mis reservas").
2. In Mailpit, find the cancellation email(s) sent to both the teacher and (if teacher-initiated instead) the student.
3. Confirm the attached `.ics` file has:
   - The same `UID` as the confirmation email's attachment for that booking.
   - `METHOD:CANCEL` and `STATUS:CANCELLED`.
   - A `SEQUENCE` value greater than the original confirmation's.
4. In a calendar app that already imported the Scenario 1 event, import this cancellation file and verify the event is removed or shown as canceled — not duplicated.

**Expected result**: Matches FR-004, FR-005, FR-010.

## Scenario 4 — Attachment failure never blocks the email (FR-007)

This is best verified via the unit test `BookingEmailServiceTest` (simulate an ICS-generation exception and assert the email is still sent, without an attachment, and no exception propagates to the caller) rather than manually — there's no easy way to force a real generation failure through the UI.

## Scenario 5 — Reminder emails remain unchanged (FR-009)

1. Book a class starting ~24h out (or adjust the reminder lead time in local config for testing).
2. Wait for / manually trigger `BookingReminderScheduler` (existing scheduled job).
3. In Mailpit, confirm the reminder email to the student and teacher does **not** have a `.ics` attachment — only the original confirmation/notification emails do.

**Expected result**: Matches FR-009 (no regression to existing reminder behavior).
