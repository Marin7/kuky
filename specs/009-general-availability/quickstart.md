# Quickstart: General Availability Template

End-to-end validation that the general template seeds new weeks while per-week edits remain the source of truth. See [data-model.md](data-model.md) and [contracts/availability-api.md](contracts/availability-api.md) for shapes.

## Prerequisites

- PostgreSQL `kuky_dev`, Mailpit (per `CLAUDE.md` local setup).
- Back-end: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'` (→ :8081).
- Front-end: `cd front-end && npm run dev` (→ :8080).
- Logged in as the teacher/admin account (`app.scheduling.teacher-email`).

## Automated checks

- `cd back-end && ./gradlew test` — `AvailabilityServiceTest` must cover: new-week snapshot equals template; template edit leaves a materialized week unchanged; per-day edit persists and isolates to that date; empty template ⇒ unavailable week; bootstrap reproduces pre-feature effective windows.

## Scenario A — Launch is behavior-preserving (FR-011)

1. Start the back-end against a DB that already has V7 data (rules + any exceptions).
2. `GET /api/v1/schedule` → the slot set is **identical** to before the upgrade.
3. Confirm `materialized_weeks` now has the current + next Monday, and `week_availability` reflects today's effective windows.

## Scenario B — Edit the general template (US1)

1. In `/panel` → Availability → **Disponibilidad general**, set Mon–Fri 09:00–18:00, weekends off; save.
2. Reload → the template persists.
3. `400` is returned if you submit a window whose end ≤ start.

## Scenario C — New week starts from the template (US2)

1. With the template from B, advance the clock (or wait) so a previously-out-of-horizon week enters the window — or inspect the second week now in range that had no customization.
2. `GET /api/v1/admin/availability` → that week's `days` match the template; `GET /api/v1/schedule` shows matching OPEN slots.

## Scenario D — Customize one week without side effects (US3)

1. In **Disponibilidad por semana**, for one date remove the afternoon and add an extra morning window; save (per-day PUT).
2. Reload → the edit persisted for that date only.
3. Verify the general template and a different in-range week are unchanged.
4. `GET /api/v1/schedule` reflects the customization for that date only.

## Scenario E — Template edit does not rewrite materialized weeks (US4)

1. After D, change the general template (e.g. drop Mondays).
2. The already-materialized/customized week is unchanged.
3. A not-yet-materialized future week, once it enters the horizon, reflects the updated template.

## Scenario F — Conflict surfacing, no auto-cancel (FR-010)

1. Book a slot, then remove that slot's window from the relevant week.
2. The save response lists the booking under `conflicts`; the booking still exists (not cancelled).
