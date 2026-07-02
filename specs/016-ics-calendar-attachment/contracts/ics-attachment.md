# Contract: iCalendar (.ics) Email Attachment

This is not a REST API contract — the external interface here is the email attachment itself, consumed by third-party calendar applications (Google Calendar, Outlook, Apple Calendar). This document specifies the payload shape those clients depend on.

## MIME attachment

- **Filename**: `clase.ics`
- **Content-Type**: `text/calendar; charset=UTF-8; method=REQUEST` (confirmation/teacher-notification emails) or `text/calendar; charset=UTF-8; method=CANCEL` (cancellation emails). The `method` parameter must match the `METHOD` property inside the payload — required by Outlook to auto-offer "Add to calendar" / "Remove from calendar" actions.
- Attached alongside the existing plain-text email body — never replaces it.

## Payload structure (`VCALENDAR`/`VEVENT`)

```
BEGIN:VCALENDAR
PRODID:-//Kuky//Espanol con Paula//ES
VERSION:2.0
METHOD:REQUEST                       (or CANCEL)
BEGIN:VEVENT
UID:booking-<bookingId>@kuky.es
DTSTAMP:<generation time, UTC>
DTSTART:<booking.slotStart, UTC>
DTEND:<booking.slotStart + durationMinutes, UTC>
SEQUENCE:0                           (0 for REQUEST, 1 for CANCEL of the same booking)
STATUS:CONFIRMED                     (CONFIRMED for REQUEST, CANCELLED for CANCEL)
SUMMARY:Clase de español con Paula
DESCRIPTION:<Zoom join link, plus student identifier for teacher's copy>
LOCATION:<Zoom join URL>
ORGANIZER;CN=Paula:mailto:<teacher email>
ATTENDEE;CN=<student email>:mailto:<student email>
END:VEVENT
END:VCALENDAR
```

## Contract guarantees

1. **UID stability**: For a given booking, every `VEVENT` emitted (confirmation and any later cancellation) carries the exact same `UID`, so calendar clients treat them as updates to a single event rather than separate events.
2. **Sequence ordering**: `SEQUENCE` on a `CANCEL` message is strictly greater than on the original `REQUEST`, per RFC 5546, so clients apply it as an update rather than ignoring it as stale.
3. **Time zone unambiguity**: `DTSTART`/`DTEND` are always emitted in UTC (trailing `Z`), never floating/local time.
4. **Line length / encoding**: Lines are folded at 75 octets and use CRLF line endings per RFC 5545 §3.1, since some parsers (notably older Outlook versions) reject unfolded or LF-only content.
5. **Best-effort delivery**: This attachment is always optional/additive. If it cannot be generated, the email is still sent with its existing plain-text content and no attachment — consumers (calendar apps) never see a malformed `.ics` file.

## Consumers

- Student's email client / calendar app (confirmation, cancellation).
- Teacher's (Paula's) email client / calendar app (booking notification, cancellation notification).
- Not consumed by any part of the Kuky system itself — this is a one-way export, not parsed back in.
