# Phase 0 Research: 1-on-1 Class Scheduling

This document resolves the open technical questions implied by the Technical Context. The spec's product-level ambiguities were already resolved in `/speckit-clarify` (see `spec.md` → Clarifications). The items below are implementation decisions.

---

## 1. Slot representation: virtual vs. persisted

**Decision**: Slots are **virtual** — computed on demand from configuration, not stored in a table. Only `bookings` are persisted. A slot is identified by its UTC start instant.

**Rationale**:
- Availability this phase is a fixed rule (every day 09:00–18:00 in Paula's timezone, configurable duration). Generating candidate slots from that rule for the bounded horizon (current + next ISO week) is cheap (~126 slots max).
- Persisting a slot table would add CRUD, seeding, and a generation job with no current consumer — a YAGNI violation under Constitution Principle I.
- Keeps the availability source isolated in one `AvailabilityService` (Principle III): the future teacher-managed-availability phase replaces that service's source without touching booking logic.

**Alternatives considered**:
- *Materialized `availability_slots` table* — needed only when Paula edits arbitrary availability; deferred to the future teacher-availability phase.
- *Calendly / external scheduler embed* (the original CLAUDE.md "future phase" idea) — rejected because the requirement is a first-party schedule with our own auth gate and Zoom provisioning, which Calendly's embed does not give us cleanly.

---

## 2. Preventing double-booking under concurrency (FR-007)

**Decision**: Enforce at most one active booking per slot with a **Postgres partial unique index** on the slot start for non-cancelled rows:
`CREATE UNIQUE INDEX bookings_active_slot_uniq ON bookings (slot_start) WHERE status = 'CONFIRMED';`
The booking insert relies on this; a duplicate-key violation is translated to a `409 SLOT_UNAVAILABLE` response.

**Rationale**:
- A partial unique index is the simplest correct mechanism: the database is the single source of truth, so two concurrent confirmations cannot both succeed regardless of application-level checks.
- Cancelled rows are excluded from the constraint, so a slot frees up cleanly after cancellation (a new CONFIRMED row for the same `slot_start` is then allowed).
- No row locking, no `SELECT ... FOR UPDATE`, no distributed lock needed.

**Alternatives considered**:
- *Application-level check-then-insert* — racy; rejected.
- *Full unique constraint on `slot_start`* — would block re-booking a previously cancelled slot; rejected in favour of the partial index.

---

## 3. Booking ↔ Zoom ordering & failure handling (FR-011)

**Decision**: Reserve first, provision second, compensate on failure:
1. Insert the booking row as `CONFIRMED` (claims the slot via the unique index). On conflict → `409 SLOT_UNAVAILABLE`.
2. Call the `MeetingProvider` to create the Zoom meeting.
3. On Zoom failure, **delete** the just-inserted booking row (compensating action) and return `502 MEETING_PROVISIONING_FAILED`; the slot is therefore left open.
4. On success, persist `zoom_meeting_id` + `zoom_join_url` on the row and send notifications.

**Rationale**:
- Reserving the slot first is the cheap, authoritative step and prevents two students racing into the same Zoom-backed slot.
- The external Zoom call is the only failure-prone step; the compensating delete guarantees we never leave a CONFIRMED booking without a usable meeting (FR-011) and never strand an orphan Zoom meeting (which the create-Zoom-first ordering would risk on an insert conflict).
- Notifications are sent only after the row is fully populated, so emails always contain a working link (SC-003).

**Alternatives considered**:
- *Create Zoom first, then insert booking* — risks an orphan Zoom meeting when the slot was taken concurrently; rejected.
- *Two-phase/outbox saga* — overkill for single-instance, low-volume usage; rejected per Principle I.

---

## 4. Zoom integration mechanism

**Decision**: Use a **Zoom Server-to-Server OAuth** app and the Meetings REST API, called through Spring's `RestClient` (no new dependency).
- Token: `POST https://zoom.us/oauth/token?grant_type=account_credentials&account_id={accountId}` with HTTP Basic auth (`clientId:clientSecret`). Cache the access token in memory until shortly before its ~1 h expiry.
- Create: `POST https://api.zoom.us/v2/users/{userId}/meetings` with `{ type: 2 (scheduled), start_time (UTC ISO-8601), duration, timezone, topic }`. Capture `id` and `join_url`.
- Cancel: `DELETE https://api.zoom.us/v2/meetings/{meetingId}`.
- `userId` defaults to `me` (the app's associated user = Paula's Zoom account); configurable.

**Rationale**:
- Server-to-Server OAuth is Zoom's recommended approach for backend, account-owned automation without per-user interactive OAuth — exactly our single-teacher case.
- `RestClient` ships with `spring-web` (already a dependency) and uses the Jackson already on the classpath, so no library is added (Principle I / Technology Stack gate).

**Alternatives considered**:
- *Zoom JWT apps* — deprecated by Zoom; rejected.
- *A third-party Zoom Java SDK* — adds a dependency for a handful of calls; rejected.

**Local development fallback**: A `StubMeetingProvider` is selected when Zoom credentials are blank. It returns a placeholder `join_url` (e.g. `https://zoom.invalid/local/<bookingId>`) and logs a `WARN`, mirroring how `EmailService` logs the reset URL when Mailpit is absent. This keeps the whole flow runnable locally without Zoom credentials.

---

## 5. Timezone handling (FR-003)

**Decision**:
- Persist all times as UTC instants (`TIMESTAMPTZ`), consistent with the existing `users` table.
- Paula's availability is authored in a configured timezone (`app.scheduling.teacher-timezone`, default `Europe/Madrid`). `AvailabilityService` builds local 09:00–18:00 slots for each date in the horizon using that zone, then converts to UTC instants for comparison and storage.
- The API returns absolute UTC instants (ISO-8601); the **frontend** formats them in the viewer's browser timezone via `Intl.DateTimeFormat`. No timezone is inferred server-side for viewers.

**Rationale**: UTC-in-storage + client-side localization is the standard, DST-safe approach and matches existing conventions. Handling DST is automatic because conversion uses the IANA zone, not a fixed offset.

**Alternatives considered**: Storing wall-clock local times — rejected (ambiguous across DST, complicates cross-timezone display).

---

## 6. Booking horizon & week boundaries (FR-020)

**Decision**: The horizon is the current ISO week plus the next ISO week (Monday–Sunday), computed in Paula's timezone, rolling forward automatically by virtue of being derived from "now" on each request. Slots outside this window are neither returned nor bookable.

**Rationale**: Matches the clarified answer ("current calendar week + next week only"). Monday-start aligns with the Romanian/Spanish locale (documented assumption). Deriving from the request time means no scheduled job is needed to advance the window.

---

## 7. Lead time & cancellation cutoff (FR-021, FR-013)

**Decision**: Both are 24 h, enforced server-side against the slot start using config values (`app.scheduling.min-lead-hours`, `app.scheduling.cancel-cutoff-hours`, both default 24):
- Booking rejected with `422 BOOKING_TOO_SOON` if `slotStart - now < minLeadHours`.
- Cancellation rejected with `422 CANCELLATION_TOO_LATE` if `slotStart - now < cancelCutoffHours`.
The frontend also greys out non-bookable slots, but the server is authoritative.

**Rationale**: Server-side enforcement prevents client-clock and race exploits; config-driven values keep them tunable without code changes.

---

## 8. Notifications (FR-010, FR-014)

**Decision**: Reuse the existing `JavaMailSender` (Mailpit locally). A new `BookingEmailService` sends:
- On confirmation: an email to the student and one to Paula (`app.scheduling.teacher-email`), each containing date/time and the Zoom join link.
- On cancellation: an email to Paula noting the freed slot.
Email failure is logged and does not roll back the booking/cancellation (best-effort, consistent with the existing password-reset email behaviour).

**Rationale**: Reuses proven infrastructure; in-account display (the bookings list) is the durable record, so a transient email failure must not undo a valid booking.

---

## 9. Frontend data-fetching pattern

**Decision**: Follow the existing `lib/auth.ts` pattern — a small `lib/scheduling.ts` module with `fetch` wrappers (`credentials: "include"`), consumed from components via `useEffect`/`useState` (and TanStack Query where it simplifies refetch-after-mutation, since `QueryClientProvider` is already wired in `__root.tsx`). Auth state is read via the existing `getMe()`.

**Rationale**: Consistency with the established codebase (Principle III keeps fetch logic out of components). TanStack Query is already available for cache invalidation after booking/cancelling so the public view updates promptly (SC-005), but plain fetch remains acceptable to match `cuenta.tsx`.

---

## Resolved unknowns

All Technical Context items are resolved; **no `NEEDS CLARIFICATION` markers remain**. Ready for Phase 1 design.
