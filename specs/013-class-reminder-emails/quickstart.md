# Quickstart: Validating Class Reminder Emails

This feature has no new UI or API endpoint — it's a backend scheduled job. Validation is done via the backend test suite and, for an end-to-end check, Mailpit.

## Prerequisites

- Local dev environment set up per [CLAUDE.md](../../CLAUDE.md) (PostgreSQL `kuky_dev`, Mailpit on port 1025/8025).
- Migration `V24__add_booking_reminder_sent.sql` applied (runs automatically via Flyway on `bootRun`).

## Automated validation

```bash
cd back-end
./gradlew test --tests "*BookingReminder*"
```

Expected coverage (see [data-model.md](data-model.md) and [research.md](research.md) for the rules under test):
- A `CONFIRMED` booking whose `slot_start` is ≤24h away and `reminder_sent_at IS NULL` triggers exactly one reminder to the student and one to the teacher.
- A booking already reminded (`reminder_sent_at` set) is not reminded again on a subsequent poll.
- A `CANCELLED` booking never receives a reminder, even if it would otherwise be due.
- A booking not yet within 24h of `slot_start` is left untouched.
- A booking that already started (`slot_start` in the past) is not (re)reminded.

## Manual end-to-end check

1. Start the backend with the `local` profile and the front-end (`npm run dev`).
2. Book a class scheduled for tomorrow (satisfies the existing 24h minimum lead time).
3. Temporarily set the fixed test `Clock` or manually update the booking's `slot_start`/wait for the scheduler's poll interval (15 min) to elapse while within the 23–25h-before window.
4. Open Mailpit (http://localhost:8025) and confirm two new emails arrived: one to the student's address, one to the teacher's address (`TEACHER_EMAIL`), both referencing the correct class date/time and Zoom join link.
5. Cancel a different booking before its reminder window, wait past what would have been its reminder time, and confirm Mailpit shows no reminder email for it.

## Success criteria mapping

- SC-001 (95% delivered within 23–25h window) → covered by the "due" query bounds + 15 min poll interval; verified functionally via the automated tests above (the query itself guarantees the window, independent of load).
- SC-002 (0% reminders for cancelled classes) → automated test: cancelled booking case.
- SC-003 (no duplicate reminders) → automated test: already-reminded booking case, plus the conditional-claim `UPDATE` guarding concurrent poll runs.
