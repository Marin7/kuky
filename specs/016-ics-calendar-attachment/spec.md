# Feature Specification: Calendar (.ics) Attachment for Bookings

**Feature Branch**: `016-ics-calendar-attachment`

**Created**: 2026-07-02

**Status**: Draft

**Input**: User description: "Attach a calendar .ics file when booking an appointment"

## Clarifications

### Session 2026-07-02

- Q: Should the pre-class reminder email (sent separately from the booking confirmation) also carry a calendar attachment? → A: No — only confirmation and cancellation emails get the calendar attachment; reminder emails are unchanged.
- Q: When a booking is canceled (by the student or by the teacher/admin), should the teacher's own cancellation notification also carry a calendar-cancel update, matching the student-facing behavior in FR-005? → A: Yes — the teacher receives a matching cancellation calendar update regardless of who initiated the cancellation.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add booked class to personal calendar (Priority: P1)

A student books a 1-on-1 Spanish class through the schedule. After the booking is confirmed, the confirmation email includes a calendar file the student can open to add the class directly to their personal calendar app (Google Calendar, Outlook, Apple Calendar, etc.), with the correct date, time, duration, and a link to join the Zoom class.

**Why this priority**: This is the core of the feature — without it, nothing else matters. It directly reduces no-shows and missed classes by making it effortless for students to remember their booking.

**Independent Test**: Book a class as a student, receive the confirmation email, open the attached calendar file, and verify it creates a correctly-timed calendar event containing the class details and join link.

**Acceptance Scenarios**:

1. **Given** a student has an available slot open on the schedule, **When** they complete a booking, **Then** the booking confirmation email they receive includes an attached calendar file for that class.
2. **Given** a student opens the attached calendar file in a standard calendar application, **When** it is imported, **Then** a new event appears at the correct local date and start/end time, titled to identify it as a Spanish class with Paula, and including the Zoom join link in the event description/location.
3. **Given** a booking is made for a class more than a year in the future, **When** the confirmation email is generated, **Then** the calendar file still reflects the correct date and time with no errors.

---

### User Story 2 - Calendar stays accurate after cancellation (Priority: P2)

A student cancels a class they previously added to their calendar. They receive a cancellation notice that, when opened, removes or marks the event as canceled in their calendar app instead of leaving a stale entry behind.

**Why this priority**: Prevents confusion and clutter from orphaned calendar entries, but the feature still delivers value without it (the student can manually delete the event).

**Independent Test**: Book a class, cancel it, and verify the cancellation email's calendar file removes/cancels the matching event when imported into a calendar app that was used to add the original booking.

**Acceptance Scenarios**:

1. **Given** a student previously added a booked class to their calendar via the confirmation email, **When** they cancel the booking, **Then** they receive an email whose calendar file cancels the matching event in supporting calendar apps.

---

### User Story 3 - Teacher's own calendar stays in sync (Priority: P3)

Paula (the teacher) also receives a calendar file attachment on the booking notification email she gets whenever a student books, so she can add the class to her own calendar the same way.

**Why this priority**: Convenience for the teacher; the booking is already recorded in the system and visible in her admin panel, so this is a nice-to-have rather than essential.

**Independent Test**: Book a class as a student and verify the teacher notification email also contains a valid calendar attachment for the same class.

**Acceptance Scenarios**:

1. **Given** a student books a class, **When** the teacher notification email is sent, **Then** it includes a calendar file attachment describing the same class at the same time.
2. **Given** a booking is canceled by either the student or the teacher, **When** the teacher's cancellation notification is sent, **Then** it includes a calendar file that cancels the matching event using the same identifier as the original teacher notification.

---

### Edge Cases

- What happens when a booking is made very close to its start time (e.g., minutes before class)? The calendar file must still generate correctly and the email must not be delayed or blocked by attachment generation.
- How does the system handle a student's email client that doesn't support calendar attachments? The email must still be readable and useful without the attachment (attachment is additive, not a replacement for the existing textual booking details).
- What happens if a student wants a class at a different time than originally booked? There is no distinct "reschedule" flow today — the student cancels the existing booking (triggering the cancellation calendar file) and books a new slot (triggering a new confirmation calendar file with a new identifier).
- What happens for recurring/multiple bookings by the same student on the same day? Each booking must produce its own distinct calendar event (not merged or overwritten).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST generate a standards-compliant calendar file for each confirmed booking, containing the class start time, end time, a descriptive title identifying it as a Spanish class with Paula, and the Zoom join link.
- **FR-002**: System MUST attach the generated calendar file to the booking confirmation email sent to the student at the time of booking.
- **FR-003**: The calendar file's event time MUST use the correct time zone so the event appears at the true local class time regardless of the recipient's device time zone settings.
- **FR-004**: System MUST use a stable, unique identifier per booking in the calendar file so that later updates (e.g., cancellation) can reference and modify the same calendar event rather than creating a duplicate.
- **FR-005**: When a booking is canceled — whether initiated by the student or by the teacher/admin — the system MUST send the student a cancellation email containing a calendar file that cancels the previously sent event using the matching identifier.
- **FR-006**: System MUST also attach a calendar file to the teacher's booking notification email, describing the same class, so the teacher can add it to her own calendar.
- **FR-007**: The calendar attachment generation MUST NOT block or fail the sending of the underlying booking/cancellation email — if attachment generation fails, the email must still be sent without the attachment.
- **FR-008**: The calendar file MUST be generated on demand at send time from the current booking data (not stored), so it always reflects the latest known details for that email.
- **FR-009**: The pre-class reminder email MUST NOT be modified to include a calendar attachment — the calendar file is only sent on the initial booking confirmation, on the teacher's booking notification, and on cancellation.
- **FR-010**: When a booking is canceled, the system MUST also send the teacher a cancellation calendar update matching the one sent to the student (per FR-005), regardless of whether the student or the teacher/admin initiated the cancellation.

### Key Entities

- **Booking**: An existing entity representing a scheduled 1-on-1 class between a student and the teacher; supplies the date, start/end time, and Zoom join link used to populate the calendar file.
- **Calendar Event Attachment**: A generated file representing one class occurrence, derived from a Booking, sent as part of confirmation/cancellation emails; not persisted independently of the Booking it represents.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A student can go from receiving a booking confirmation email to having the class on their personal calendar in under 30 seconds, without manually entering any details.
- **SC-002**: 100% of booking confirmation emails sent for successful bookings include a calendar attachment that opens without error in mainstream calendar applications.
- **SC-003**: Attaching the calendar file adds no perceptible delay to the booking flow — the student sees their booking confirmed within the same time as before this feature existed.
- **SC-004**: Students who add the class to their calendar via the attachment have no reduction in event accuracy — 100% of generated events show the correct date, time, and duration compared to the booking record.

## Assumptions

- Only 1-on-1 class bookings (via `/reservas`) are in scope; there are no other appointment types in the system today that would need calendar attachments.
- The existing booking confirmation and teacher notification email flows (already sent today) are extended with an attachment rather than replaced with new emails.
- Standard calendar file format support (openable by Google Calendar, Outlook, and Apple Calendar) is sufficient; no in-app "Add to Calendar" button or native app integration is required for this feature.
- The class duration used for the event end time is the same duration already recorded for the booking in the existing scheduling system.
- Existing bookings made before this feature ships are out of scope — the calendar attachment only applies to bookings confirmed after the feature is live.
