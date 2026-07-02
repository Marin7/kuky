# Phase 0 Research: Class Reminder Emails

## Decision: Late-booking edge case is unreachable — no branching logic needed

**Question this resolves**: the spec's Edge Cases section and FR-005 describe a class "booked with less than 24 hours' notice before it starts" and specify that no reminder should be sent in that case (the open `/speckit-clarify` question about immediate-vs-no reminder for this scenario).

**Finding**: `SchedulingProperties.Scheduling.minLeadHours` (default `24`, enforced in `AvailabilityService.validateBookable`, [AvailabilityService.java:108-112](../../back-end/src/main/java/com/kuky/backend/scheduling/service/AvailabilityService.java)) already rejects any booking attempt whose `slotStart` is not more than 24 hours in the future at creation time (`BookingNotAllowedException.Reason.LEAD`). Consequently every `CONFIRMED` booking is, by construction, created with more than 24 hours of lead time — the scenario FR-005 describes cannot occur in the current system.

**Rationale**: because the reminder job's own trigger point (24h before start) is guaranteed to be *after* every booking's creation time, a plain "send once when a confirmed booking crosses the 24h-before mark" job satisfies FR-003, FR-004 and FR-005 with no special-case code. No decision between "skip" vs "send immediately" is needed — it's moot under the existing `minLeadHours` invariant.

**Alternatives considered**: adding an explicit guard/branch for late bookings — rejected as dead code (Simplicity First / YAGNI, per project constitution). If `minLeadHours` is ever lowered below 24 in the future, this becomes reachable again and should be revisited.

## Decision: Scheduling mechanism — single `@Scheduled` poller, no new infrastructure

**Decision**: Use Spring's `@Scheduled(fixedDelay = ...)` on a new `BookingReminderScheduler`, polling every 15 minutes, backed by a plain SQL query against the existing `bookings` table (no queue, no Quartz, no external scheduler).

**Rationale**: The backend runs as a single instance (no evidence of multi-instance deployment or distributed locking elsewhere in the codebase). A 15-minute poll interval comfortably satisfies SC-001's 23–25 hour delivery window (2-hour tolerance) with wide margin. This mirrors the project's existing patterns — `AvailabilityService` already does time-driven work off an injected `Clock` bean ([ClockConfig.java](../../back-end/src/main/java/com/kuky/backend/config/ClockConfig.java)), no scheduling library exists yet, and adding one would violate the constitution's Simplicity First principle for a single-teacher app at this scale.

**Alternatives considered**:
- Quartz / distributed job scheduler — rejected, massive overkill for one query every 15 minutes on a single instance.
- Event-driven (e.g., schedule a one-off delayed task at booking-creation time) — rejected; requires a persistent delayed-job mechanism (not present) to survive app restarts, whereas a poller is naturally restart-safe by re-querying state.

**Query shape**: catch-up style rather than a narrow time window — select `CONFIRMED` bookings where `reminder_sent_at IS NULL AND slot_start > now AND slot_start <= now + 24h`. This is self-healing (a missed poll cycle, e.g. due to a deploy, is caught on the next run) rather than relying on hitting an exact narrow window.

## Decision: Duplicate-send prevention — `reminder_sent_at` column + conditional update

**Decision**: Add `reminder_sent_at TIMESTAMPTZ` to `bookings`. Before sending, the scheduler claims a booking with `UPDATE bookings SET reminder_sent_at = :now WHERE id = :id AND reminder_sent_at IS NULL`; it only proceeds to send email if that update affected a row.

**Rationale**: Satisfies FR-004/SC-003 (at most one reminder per booking) even if two poll cycles overlap (e.g., a slow run plus the next tick), without needing a separate lock table. Matches the existing pattern of tracking state directly on `bookings` (e.g., `cancelled_at`) rather than introducing a new entity.

**Alternatives considered**: a separate `reminders` table keyed by booking+recipient — rejected as unnecessary; the spec's "Reminder" key entity is a per-booking event (student + teacher are always reminded together, from the same row), not an independently-tracked per-recipient record.

## Decision: Email content and delivery — extend `BookingEmailService`

**Decision**: Add a `sendReminder(String studentEmail, String teacherEmail, Instant slotStart, String joinUrl)` method to the existing `BookingEmailService` ([BookingEmailService.java](../../back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java)), following the exact pattern of `sendConfirmation` (one `sendQuietly` call per recipient, same UTC `dd/MM/yyyy HH:mm` formatting, same fire-and-log-on-failure semantics via `MailException` catch).

**Rationale**: Reuses the established convention (`JavaMailSender`, fixed UTC display format, best-effort delivery that never throws) rather than introducing a new email abstraction. Directly satisfies FR-006 (same info as confirmation email), FR-007 (delivery failure doesn't propagate), and FR-008 (same timezone convention as existing booking emails — plain UTC, no per-recipient timezone lookup needed since none is stored).

## Summary of resolved unknowns

| Area | Resolution |
|---|---|
| Late-booking edge case (FR-005) | Structurally unreachable given `minLeadHours=24`; no special-case code |
| Scheduling mechanism | `@Scheduled` poller, 15 min interval, no new dependency |
| Duplicate prevention | `reminder_sent_at` column + conditional `UPDATE ... WHERE reminder_sent_at IS NULL` |
| Email content/delivery | New method on existing `BookingEmailService`, same conventions as confirmation/cancellation |
| Timezone display | Reuses existing fixed-UTC formatting; no new timezone data needed |
