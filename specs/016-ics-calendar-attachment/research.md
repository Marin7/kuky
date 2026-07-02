# Phase 0 Research: Calendar (.ics) Attachment for Bookings

No `NEEDS CLARIFICATION` markers remain in the Technical Context — all decisions below were resolved from the existing codebase and standard iCalendar practice rather than open questions.

## Decision: Hand-written ICS generation, no new dependency

**Rationale**: The feature only ever needs a single, non-recurring `VEVENT` per booking. A general-purpose calendar library (e.g. ical4j) pulls in a non-trivial dependency tree for functionality this feature doesn't use (recurrence rules, timezone databases, parsing). Per the Simplicity First principle, a small string-building helper (`IcsEventFactory`) that emits a minimal valid `VCALENDAR`/`VEVENT` block is sufficient and easiest to unit test.

**Alternatives considered**:
- **ical4j**: full iCalendar library — rejected as over-scoped for one event type with no recurrence.
- **biweekly**: lighter iCalendar library — still an unnecessary dependency for a handful of fixed fields.

## Decision: Switch `BookingEmailService` from `SimpleMailMessage` to `MimeMessage`

**Rationale**: `SimpleMailMessage` cannot carry attachments. `JavaMailSender.createMimeMessage()` + `MimeMessageHelper` (already transitively available via `spring-boot-starter-mail`) is the standard Spring mechanism for multipart emails with attachments, and is a drop-in replacement for the existing `sendQuietly` helper.

**Alternatives considered**:
- **Separate follow-up email containing only the .ics file**: rejected — worse UX (two emails instead of one), and doesn't satisfy "included in the confirmation email" from the spec.

## Decision: Stable UID = `booking-<bookingId>@kuky.es`

**Rationale**: `Booking.id` (UUID) already exists as the durable identifier for a booking and is available at both booking-creation and cancellation time. Using it as the iCalendar `UID` lets calendar apps recognize a later `METHOD:CANCEL` message as referring to the same event created by the original `METHOD:REQUEST` message, satisfying FR-004/FR-005/FR-010.

**Alternatives considered**:
- **Random UUID generated at send time**: rejected — would not match between the confirmation and cancellation messages, breaking the update/cancel semantics required by the spec.

## Decision: Cancellation via `METHOD:CANCEL` + `STATUS:CANCELLED` + incremented `SEQUENCE`

**Rationale**: This is the standard iCalendar (RFC 5546) mechanism mainstream clients (Google Calendar, Outlook, Apple Calendar) recognize to remove/mark-cancelled a previously imported event with a matching `UID`. `SEQUENCE` starts at `0` on the confirmation `VEVENT` and is `1` on the cancellation `VEVENT` for the same booking, per RFC guidance that CANCEL messages carry a higher sequence number than the original.

**Alternatives considered**:
- **Sending a second `METHOD:REQUEST` with `STATUS:CANCELLED` only**: rejected — some clients only fully process cancellation semantics via `METHOD:CANCEL`.

## Decision: All event times in UTC (`Z` suffix), no `VTIMEZONE` block

**Rationale**: `Booking.slotStart`/`slotEnd` are stored as `Instant` (already UTC) and the existing email templates already format times as `'UTC'` (see `BookingEmailService.FMT`). Emitting `DTSTART`/`DTEND` as `YYYYMMDDTHHMMSSZ` is unambiguous for every recipient regardless of their device time zone, satisfying FR-003 without needing to embed or reference an IANA timezone database entry.

**Alternatives considered**:
- **Embedding a `VTIMEZONE` component for Europe/Madrid**: rejected — adds complexity for no behavioral gain, since UTC `Z` timestamps are already correctly interpreted by all mainstream calendar clients.

## Decision: ICS generation failure never blocks the underlying email

**Rationale**: FR-007 requires the booking/cancellation email to still be sent even if attachment generation fails. The existing `sendQuietly` pattern already swallows `MailException`; the new logic wraps ICS generation in its own try/catch so a generation failure falls back to sending the email without the attachment (logged as a warning), rather than the whole send failing.

**Alternatives considered**:
- **Letting a generation failure propagate and abort the send**: rejected — directly contradicts FR-007 and would leave the student without any booking confirmation.

## Decision: Reminder emails (`sendReminderToStudent`/`sendReminderToTeacher`) are unchanged

**Rationale**: Per the resolved clarification (FR-009), reminder emails intentionally do not get the ICS attachment — the recipient already received it on the original confirmation email.

**Alternatives considered**: N/A — this was a scope decision, not a technical one.
