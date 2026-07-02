# Phase 1 Data Model: Class Reminder Emails

## Modified entity: `bookings` (existing table, existing `Booking` model)

New column, added via migration `V24__add_booking_reminder_sent.sql`:

| Column | Type | Nullable | Default | Purpose |
|---|---|---|---|---|
| `reminder_sent_at` | `TIMESTAMPTZ` | yes | `NULL` | Set once the 24h-before reminder has been sent for this booking; `NULL` means not yet sent. Doubles as the claim marker that prevents duplicate sends (see [research.md](research.md)). |

No other columns change. `status` (`CONFIRMED` / `CANCELLED`) and `slot_start` continue to be the source of truth for whether/when a reminder is due.

**State transitions** (reminder-relevant subset of the existing booking lifecycle):

```
CONFIRMED, reminder_sent_at = NULL
  --(24h-before mark reached, still CONFIRMED)--> CONFIRMED, reminder_sent_at = <sent time>
  --(cancelled before 24h-before mark)-----------> CANCELLED, reminder_sent_at stays NULL (no reminder ever sent — FR-003)
```

A booking cancelled *after* its reminder was already sent simply moves to `CANCELLED` with `reminder_sent_at` already set — no reversal needed (matches the spec's Edge Cases: the existing cancellation email is the system of record for that change, not this feature).

`Booking.java` gets one new field: `reminderSentAt` (`Instant`, nullable), mirroring the existing `cancelledAt` field's shape.

## New: reminder query (`BookingRepository`)

Not a new entity — a new read/claim pair of queries against `bookings`:

- **Find due bookings**: `CONFIRMED` bookings where `reminder_sent_at IS NULL AND slot_start > :now AND slot_start <= :now + 24h`, joined to `users` for student email (same join style as `findUpcomingConfirmedBookings`).
- **Claim** a booking for sending: `UPDATE bookings SET reminder_sent_at = :now WHERE id = :id AND reminder_sent_at IS NULL`, only send email if the update affects exactly one row.

## Key Entities (from spec) → implementation mapping

- **Class / Booking** → existing `Booking` model/`bookings` table, extended with `reminder_sent_at`.
- **Reminder** → not a standalone row; represented as the `(bookings.reminder_sent_at IS NOT NULL)` state on the booking itself, since student + teacher are always reminded together for the same booking in a single scheduler pass.
