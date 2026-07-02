# Quickstart: Validating Timezone Support

## Prerequisites

- Local dev environment running per `CLAUDE.md` (PostgreSQL 18 with `kuky_dev`, Mailpit on port 1025/8025, back-end on `local` profile at `:8081`, front-end `npm run dev` at `:8080`).
- Flyway migration `V27__add_user_timezone.sql` applied (runs automatically on back-end startup).
- A student test account (or promote one via the admin panel per `CLAUDE.md`'s User vs. student notes).

## Scenario 1 — Student sees local time, not teacher time (User Story 1, P1)

1. Log in as a student.
2. In the browser devtools console, confirm the detected zone: `Intl.DateTimeFormat().resolvedOptions().timeZone`.
3. Open `/reservas`. Every slot on the public schedule and in "Mis Reservas" must show times consistent with that detected zone, each labeled with a region/city name (not `Europe/Madrid`, unless that happens to match the detected zone).
4. Cross-check one slot's underlying instant: open Network tab, find the `/api/v1/schedule` (or bookings) response, note the `start`/`slotStart` ISO instant, and manually convert it to the detected zone — it must match what's displayed.

**Expected**: Displayed time = instant converted to the browser's zone, zone label present, no manual conversion needed.

## Scenario 2 — Manual override persists and stops auto-resync (Clarification Q2)

1. As the same student, use the new "Time zone" control (`/cuenta`) to manually select a different zone (e.g. `America/New_York`).
2. Reload `/reservas` — times must now reflect the manually chosen zone, not the browser's detected zone.
3. Call `GET /api/v1/auth/me` (or reload the page, which calls it) — `timezoneIsManual` must be `true` and `timezone` must be the chosen value.
4. Confirm the override survives a fresh login (new session) without being silently overwritten by auto-detection.
5. Clear the override in `/cuenta` — subsequent sessions must resume auto-detecting from the browser.

## Scenario 3 — Booking confirmation, reminder email, and .ics agree (User Story 2, P2)

1. As the student from Scenario 1, book an available class.
2. On-screen confirmation: note the displayed local time + zone label.
3. Check Mailpit (`http://localhost:8025`) for the confirmation email — the stated time and zone label must match the on-screen confirmation (not raw UTC).
4. Open the `.ics` attachment in a calendar app (or inspect its `DTSTART`/`DTEND` — should still be UTC `Z`-suffixed, unchanged from feature 016) — the calendar app's rendered local time must also match.
5. Trigger (or wait for, in a lower environment with a shortened lead time) the class reminder email — same zone-consistency check.

**Expected**: All three (on-screen, email text, calendar app rendering) agree, in the student's zone.

## Scenario 4 — Teacher's admin views stay anchored to her working zone (User Story 3, P3)

1. In browser devtools, override the page's effective time zone for testing (e.g. via `preview_resize`'s emulation or a Chrome DevTools Sensors override) to something other than `Europe/Madrid`.
2. Log in as the teacher (ADMIN role) and open the availability editor (`/panel`) and the Bookings tab.
3. All displayed hours must remain in `Europe/Madrid` (or whatever `app.scheduling.teacher-timezone` is configured to), unaffected by the emulated device zone.
4. Open a student's profile from the admin panel (`panel_.alumnos.$studentId`) — booking history times must also show `Europe/Madrid`, not the emulated zone.

## Scenario 5 — DST transition regression check (Edge Case / FR-006)

1. Pick a date near a known DST transition in the teacher's zone (`Europe/Madrid` switches last Sunday of March/October) and, separately, a date near a transition in a different zone (e.g. `America/New_York`, first Sunday of November/second Sunday of March).
2. As a student with a manually-set zone in the *other* region, view the schedule for a week spanning the teacher's DST transition — slot count and times must shift correctly with no duplicate/missing/ambiguous slots.
3. Repeat with the student's own zone's DST transition week — same check from the display side.

## Scenario 6 — Fallback when zone can't be detected (Edge Case / FR-008)

1. Simulate detection failure (e.g. stub `Intl.DateTimeFormat().resolvedOptions().timeZone` to throw, or test with a fresh account where no sync has ever occurred and `timezone` is `NULL`).
2. Confirm the UI falls back to a clearly labeled default (the teacher's zone) rather than silently guessing or crashing, and that the label makes clear this is a fallback, not the student's actual zone.
