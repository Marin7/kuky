# Feature Specification: Class Reminder Emails

**Feature Branch**: `013-class-reminder-emails`

**Created**: 2026-07-02

**Status**: Draft

**Input**: User description: "Send a reminder email 24h before class to the teacher and the student as well"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Student receives a class reminder (Priority: P1)

A student has booked a class with the teacher. As the class approaches, the student receives an email reminding them of the upcoming class so they don't forget to attend.

**Why this priority**: Missed classes are the direct cost this feature prevents. Reminding the student is the core value of the feature.

**Independent Test**: Book a class scheduled ~24 hours out, wait for the reminder window to be reached, and confirm the student receives a reminder email containing the correct class date/time and join details.

**Acceptance Scenarios**:

1. **Given** a student has a confirmed class scheduled 24 hours from now, **When** the reminder window is reached, **Then** the student receives an email reminder identifying the class date, time, and how to join.
2. **Given** a student has a confirmed class scheduled more than 24 hours away, **When** the reminder window has not yet been reached, **Then** no reminder email has been sent for that class yet.

---

### User Story 2 - Teacher receives a class reminder (Priority: P1)

The teacher has one or more classes booked by students. As each class approaches, the teacher receives an email reminding them of the upcoming class so they can prepare and be available on time.

**Why this priority**: The teacher relies on reminders just as much as the student to avoid missing a scheduled class; this is equally critical to the feature's purpose and ships alongside the student reminder.

**Independent Test**: Book a class scheduled ~24 hours out, wait for the reminder window to be reached, and confirm the teacher receives a reminder email identifying which student and which class it concerns.

**Acceptance Scenarios**:

1. **Given** a confirmed class scheduled 24 hours from now, **When** the reminder window is reached, **Then** the teacher receives an email reminder identifying the student, the class date/time, and how to join.
2. **Given** the teacher has multiple confirmed classes scheduled around the same time, **When** each class reaches its reminder window, **Then** the teacher receives one reminder email per class.

---

### User Story 3 - Cancelled classes don't generate reminders (Priority: P2)

A class that was cancelled (by either the student or the teacher) before the 24-hour mark should never trigger a reminder email to either party, since there is nothing to be reminded of.

**Why this priority**: Sending a reminder for a class that no longer exists would confuse and annoy both parties and undermine trust in the notification. This is a correctness guardrail on top of the core reminders in Stories 1 and 2.

**Independent Test**: Book a class, cancel it before the reminder would go out, and confirm neither the student nor the teacher receives a reminder email for it.

**Acceptance Scenarios**:

1. **Given** a class was cancelled before its reminder window was reached, **When** the reminder window arrives, **Then** no reminder email is sent to either the student or the teacher for that class.

---

### Edge Cases

- A class is booked with less than 24 hours' notice before it starts (the reminder window has already passed at booking time): no reminder is sent for that class, since the booking confirmation email already served that purpose.
- A class is cancelled *after* its reminder has already been sent: the existing cancellation email is the source of truth for the change; no retraction or "class cancelled" follow-up is required from this feature.
- The reminder email fails to send (e.g., mail server unavailable): the failure must not affect the underlying class/booking record, and must not block or delay other reminders.
- A student or teacher has back-to-back classes close together: each class gets its own independent reminder.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST send a reminder email to the student approximately 24 hours before the scheduled start time of each of their confirmed classes.
- **FR-002**: System MUST send a reminder email to the teacher approximately 24 hours before the scheduled start time of each confirmed class on their schedule.
- **FR-003**: System MUST NOT send a reminder for a class whose status is cancelled by the time the reminder would go out.
- **FR-004**: System MUST send at most one reminder email per recipient per class (no duplicates).
- **FR-005**: System MUST NOT send a reminder for a class that was booked with less than 24 hours' notice before its start time.
- **FR-006**: The reminder email MUST identify the class date and time and MUST include the information needed to join the class (consistent with what's provided in the existing booking confirmation email).
- **FR-007**: A failure to deliver a reminder email MUST NOT alter the status of the underlying class/booking or prevent other reminders from being sent.
- **FR-008**: Reminder times MUST be presented to each recipient in a clear, consistent timezone (consistent with how class times are already communicated elsewhere, e.g. in booking confirmations).

### Key Entities

- **Class / Booking**: An existing confirmed session between the teacher and a student at a specific date and time. The reminder feature reads this entity's schedule and status; it does not change what a class is, only adds a notification tied to its start time.
- **Reminder**: A one-time notification event tied to a specific class and recipient (student or teacher), sent once the class reaches its ~24-hour-before window, provided the class is still confirmed at that time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 95% of confirmed classes result in both the student and the teacher receiving a reminder email within a window of 23–25 hours before the class start time.
- **SC-002**: 0% of cancelled classes generate a reminder email to either party.
- **SC-003**: No recipient receives more than one reminder email for the same class.

## Assumptions

- "24h before class" means a single reminder sent once, targeted at roughly 24 hours ahead of the class start time, not a repeating series of reminders.
- There is exactly one teacher in the system today (matches the current single-teacher domain model), so "the teacher" always refers to this one recipient; multi-teacher support is out of scope.
- Reminder emails reuse the same delivery channel (email) and general presentation style as the existing booking confirmation and cancellation emails, for consistency.
- Classes booked less than 24 hours before their start time do not receive a reminder, since the original booking confirmation email already gave the only notice possible.
- No in-app or SMS reminder is in scope — email only, matching the feature description.
