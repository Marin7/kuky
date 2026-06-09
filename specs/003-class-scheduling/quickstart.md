# Quickstart & Validation: 1-on-1 Class Scheduling

This guide validates the scheduling feature end-to-end locally. It assumes the account-management feature already runs (see `specs/002-account-management/quickstart.md`). It references `contracts/api.md` and `data-model.md` rather than restating details.

## Prerequisites

- PostgreSQL 18 running with the `kuky_dev` database (Flyway applies `V3__create_bookings.sql` on backend start).
- Mailpit running (`mailpit`, SMTP 1025, UI http://localhost:8025) to inspect confirmation/cancellation emails. Optional — without it, the backend logs email contents.
- **Zoom is optional locally.** Leave `ZOOM_CLIENT_ID` / `ZOOM_CLIENT_SECRET` / `ZOOM_ACCOUNT_ID` unset to use the `StubMeetingProvider`, which returns a placeholder join URL and logs a `WARN`. To exercise real Zoom, set those env vars (Server-to-Server OAuth app credentials) plus optional `ZOOM_USER_ID`.
- Node 22+/24+ and the backend env vars from CLAUDE.md (`DB_*`, `APP_JWT_SECRET`, `CORS_ALLOWED_ORIGIN`, `FRONTEND_BASE_URL`).

## Setup / run

```bash
# Backend (from back-end/) — Flyway runs V3 automatically
./gradlew bootRun --args='--spring.profiles.active=local'

# Frontend (from front-end/)
npm run dev        # http://localhost:8080
```

New local config (in `application-local.yaml`): `app.scheduling.teacher-timezone`, `teacher-email`, `class-duration-minutes`, `min-lead-hours`, `cancel-cutoff-hours`, and an empty `app.zoom.*` block (stub provider).

---

## Scenario A — Public can view slots while logged out (US1, FR-001)

1. Open an incognito window at `http://localhost:8080/reservas` (not signed in).
2. **Expected**: the current + next week's slots render, each marked open or unavailable, in your browser's local timezone. No booking is possible — attempting to book prompts sign-in.
3. API check: `curl http://localhost:8081/api/v1/schedule` returns the slot list per `contracts/api.md` §1 with no cookie.

**Pass**: open/unavailable slots visible while logged out; booking gated behind auth.

---

## Scenario B — Book a slot and get a Zoom meeting (US2, FR-005..FR-011)

1. Sign in at `/cuenta` (register a student if needed).
2. On `/reservas`, pick an OPEN slot at least 24 h out and confirm in the booking dialog.
3. **Expected**:
   - The booking succeeds (`201`); the dialog shows the Zoom join link.
   - That slot now shows as BOOKED for everyone (refresh the incognito window — SC-005).
   - With the stub provider, the backend log shows a `WARN` placeholder Zoom URL; with real Zoom, a real `join_url`.
   - Mailpit shows two emails (student + Paula) containing the date/time and join link (FR-010, SC-003).
4. Negative — lead time: try booking a slot less than 24 h away → `422 BOOKING_TOO_SOON`, slot stays open (FR-021).
5. Negative — concurrency: fire two `POST /api/v1/bookings` for the same `slotStart` near-simultaneously → exactly one `201`, the other `409 SLOT_UNAVAILABLE` (FR-007, SC-004).

**Pass**: slot reserved exclusively, Zoom link present, notifications sent, guards enforced.

---

## Scenario C — Zoom failure does not finalize a booking (FR-011)

1. Configure invalid Zoom credentials (non-empty but wrong) so the real provider is selected and fails.
2. Attempt to book an open slot.
3. **Expected**: `502 MEETING_PROVISIONING_FAILED`; the booking row is not retained (compensating delete); the slot remains OPEN on refresh; no confirmation email is sent.

**Pass**: no CONFIRMED booking exists without a usable meeting.

---

## Scenario D — View and cancel my bookings (US3, FR-012..FR-014)

1. Signed in, with the Scenario B booking present, view your bookings on `/reservas` (My bookings section).
2. **Expected**: `GET /api/v1/bookings` returns it under `upcoming` with `cancellable: true` and the Zoom link (contract §3).
3. Cancel it (≥24 h before start). **Expected**: `204`; the slot reappears as OPEN in the public view (FR-014); the Zoom meeting is deleted (real Zoom) or stub-logged; Paula receives a cancellation email.
4. Negative — cutoff: with a booking whose start is < 24 h away (insert directly or adjust config), attempt cancel → `422 CANCELLATION_TOO_LATE` (FR-013).

**Pass**: cancellation frees the slot, cancels the meeting, notifies Paula, and respects the cutoff.

---

## Constitution / quality gate

- **UI verified in a running browser** (Principle: Development Workflow) for Scenarios A, B, D — not just type-check/build.
- Backend unit tests: `./gradlew test` covers slot generation (horizon, alignment, lead-window marking) and booking validation; the controller integration test covers the happy path + `409`/`422`/`502` cases.

## Success-criteria mapping

| Criterion | Validated by |
|-----------|--------------|
| SC-001 (view < 30 s, no sign-in) | Scenario A |
| SC-002 (book < 2 min) | Scenario B |
| SC-003 (working link for both parties) | Scenario B (Mailpit) |
| SC-004 (never double-booked) | Scenario B step 5 |
| SC-005 (public view updates in seconds) | Scenarios B & D refresh |
| SC-006 (90% first-attempt completion) | Scenario B (no dead-ends in flow) |
| SC-007 (find join link < 15 s) | Scenario D |
