# Phase 0 Research: Timezone Support

All Technical Context items were resolvable from the existing codebase — no external unknowns remained, so this phase consolidates the codebase inventory (gathered via targeted source reads) into decisions rather than open research questions.

## Decision: Keep `Instant`/`TIMESTAMPTZ` as the sole source of truth; add zone info only at the edges

**Rationale**: `bookings.slot_start` is already `TIMESTAMPTZ` and every DTO that crosses the wire (`SlotResponse`, `BookingResponse`) already uses `Instant`. This means FR-004 ("preserve the exact real-world instant") is already satisfied by the existing architecture — the feature only needs to change how that instant is *formatted* for a given viewer, never how it's stored or computed. `week_availability` stays naive (`DATE` + `TIME`), interpreted through `app.scheduling.teacher-timezone` at the service boundary exactly as today (`AvailabilityService.zone()`).

**Alternatives considered**: Storing a zone-aware timestamp type change or duplicating times per zone in the DB — rejected as unnecessary; the data model is already correct, only presentation is inconsistent.

## Decision: No new timezone library on either side

**Rationale**: Back-end already does all its teacher-zone math with `java.time` (`ZoneId`, `ZonedDateTime`) — DST-safe by construction, since `ZonedDateTime` resolves gaps/overlaps automatically per Java's standard rules. Front-end already uses native `Intl.DateTimeFormat(locale, { timeZone })` in the components that do this correctly today (`CalendarPicker`, `TimeSlotList`, `MyBookings`) — no polyfill or library needed for IANA zone names, formatting, or DST. `front-end/package.json` has no `date-fns-tz`/`luxon`/`dayjs`-tz dependency and none is needed.

**Alternatives considered**: `date-fns-tz` or `luxon` for the front-end — rejected per Simplicity First; native `Intl` already does everything FR-001–FR-009 require.

## Decision: Fix the *source* of the `timezone` prop, not the prop pattern itself

**Rationale**: `ScheduleView.tsx` already threads a `timezone` prop into `CalendarPicker`, `TimeSlotList`, and `MyBookings` — but it currently sources that prop from `schedule.teacherTimezone` (the API's teacher-zone config), which is backwards for student-facing views per FR-001. `SlotGrid.tsx` and `BookingDialog.tsx` don't take/apply a `timezone` prop at all and fall back to the browser's zone implicitly via unqualified `Intl.DateTimeFormat`/`Date` calls — inconsistent with the rest of the schedule UI and with no zone label shown (violates FR-002). The fix is: (1) add a `useTimezone()` hook that resolves the *student's* zone (auto-detected, with manual-override support per the Session 2026-07-02 clarification), (2) pass that through in place of `schedule.teacherTimezone`, and (3) bring `SlotGrid`/`BookingDialog` in line with the `timeZone`-aware pattern the other three components already use.

**Alternatives considered**: A new component library or context provider for time formatting — rejected; the existing prop-drilling pattern already exists and works, it's just fed the wrong value in two places and skipped in two others.

## Decision: Admin views read the teacher's zone from config, not a hardcoded literal

**Rationale**: `WeeklyAvailabilityEditor.tsx` falls back to the browser's local zone (a latent bug if the admin travels — violates FR-007), while `BookingsTab.tsx` and `panel_.alumnos.$studentId.tsx` hardcode the literal `"Europe/Madrid"` — which happens to match `app.scheduling.teacher-timezone` today but would silently go stale if that config ever changes (the spec's teacher-relocation edge case). The existing public `/schedule` endpoint already returns `teacherTimezone` sourced from that same config (`ScheduleController.java:33`); admin components should read it from there (or an equivalent already-authenticated admin fetch) instead of a literal, and `WeeklyAvailabilityEditor` should apply it explicitly instead of relying on the browser default.

**Alternatives considered**: A brand-new `/admin/config` endpoint — rejected as redundant; the value is already served publicly and admin views can consume the same source.

## Decision: Persist a per-student timezone preference; auto-resync unless manually overridden

**Rationale**: Booking/reminder emails are generated server-side, asynchronously, with no browser session present — the backend cannot detect a browser zone at send time. Per the Session 2026-07-02 clarification (auto-detected zone re-syncs each session unless a manual override is active), the front-end must report the detected zone to the backend at least once per session so it's available when emails are composed later. This requires a new persisted field on `users` (no existing column can be repurposed — confirmed via `User.java` and its migrations) and a lightweight sync endpoint, separate from the existing `PUT /api/v1/auth/profile` (which validates/serializes a different, user-initiated form) so the auto-sync call can fire silently on every session load without going through profile-edit semantics.

**Alternatives considered**: Deriving zone from IP geolocation server-side — rejected as a new external dependency for a problem the browser already solves via `Intl.DateTimeFormat().resolvedOptions().timeZone`; also less accurate for travelers, which the spec explicitly cares about (edge case: "student travels before class occurs").

## Decision: `.ics` calendar invites need no change

**Rationale**: `IcsEventFactory` (feature 016) already emits all times in UTC with a `Z` suffix and no `VTIMEZONE`/floating time — by design, "unambiguous for every recipient regardless of their calendar app's configured time zone" (per its own class javadoc). Every mainstream calendar app renders a UTC-suffixed `DTSTART`/`DTEND` in the viewer's own local zone automatically. This already satisfies FR-005 for the calendar-invite surface; User Story 2's acceptance scenario 3 is already true today and only needs a regression check, not new code.

**Alternatives considered**: Adding an explicit `TZID`/`VTIMEZONE` block — rejected as unnecessary complexity; would not improve correctness and risks introducing timezone-database drift issues that UTC avoids entirely.

## Decision: Booking/reminder emails are the one surface with a real bug to fix

**Rationale**: `BookingEmailService` hardcodes `DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC)` for every email (confirmation, cancellation, both reminders) — every recipient, student or teacher, currently sees raw UTC with a literal `"UTC"` label, matching neither the student's zone (FR-005) nor even the teacher's own configured zone. This is corrected by formatting per-recipient: student-facing emails use the student's persisted `users.timezone` (falling back to the teacher's zone if the student has no session-synced preference yet, e.g. an email sent very soon after registration), teacher-facing emails use the existing `app.scheduling.teacher-timezone`; both get a region/city label per the Session 2026-07-02 clarification (e.g., "12/08/2026 18:00 (Europe/Bucharest)").

**Alternatives considered**: Leaving reminder/confirmation emails in UTC and only fixing on-screen displays — rejected; explicitly in scope per User Story 2 and the spec's Assumptions section, and the current UTC-with-no-real-label behavior is a clear defect relative to FR-002/FR-005.

## Decision: DST correctness requires a regression test, not new logic

**Rationale**: Both `java.time` (back-end slot generation) and `Intl.DateTimeFormat` (front-end display) are DST-safe by construction when given a real IANA zone id and a UTC instant — the risk isn't algorithmic, it's the *inconsistent zone sourcing* described above (browser-local fallbacks, hardcoded literals) which could coincidentally paper over a DST bug in one zone while exposing it in another. FR-006 is satisfied by finishing the zone-sourcing fixes above; a dedicated test (e.g., a slot near an EU DST transition, displayed in a non-EU student zone with a different transition date) is the verification step, not a new mechanism.

**Alternatives considered**: A dedicated DST-handling utility/library — rejected; `java.time`/`Intl` already own this correctly.
