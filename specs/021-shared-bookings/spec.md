# Feature Specification: Shared Bookings (Companion Student on a Class)

**Feature Branch**: `021-shared-bookings`

**Created**: 2026-07-03

**Status**: Draft

**Input**: User description: "Allow two students in the same booking. This is not something very common, but some students want to take classes together with someone else. One of them will make the appointment and the teacher will have to manually attach the 2nd student to that class"

## Clarifications

### Session 2026-07-03

- Q: When the companion student cancels a shared class themselves, what happens to the class? → A: The whole class is cancelled for both students, exactly as if the booking student had cancelled it.
- Q: Should the booking student be notified when the teacher attaches a companion student to their booking? → A: No — only the companion student is actively notified; the booking student can see it by viewing their own booking details.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher attaches a companion student to an existing class (Priority: P1)

A student has already booked a class through the normal booking flow. Separately, the teacher learns that a friend of that student wants to join the same class. From her administrative view of upcoming classes, the teacher selects the existing booking and adds the companion student to it, so the class is now shared between the two of them.

**Why this priority**: This is the entire point of the feature — without it, nothing changes. It's also the only action the teacher needs, since students never self-serve this.

**Independent Test**: Can be fully tested by having a student book a class normally, then having the teacher attach a companion (already-registered) student to that same booking, and confirming the booking now shows two students instead of one.

**Acceptance Scenarios**:

1. **Given** an upcoming confirmed class with one student, **When** the teacher attaches a companion, currently-registered student to it, **Then** the class is shown as shared between both students wherever the teacher views it.
2. **Given** an upcoming confirmed class with one student, **When** the teacher attempts to attach a student who is already the booking student on that same class, **Then** the system rejects the attempt and explains why.
3. **Given** a class that already has two students attached, **When** the teacher attempts to attach a third student, **Then** the system rejects the attempt, since a class supports at most two students.

---

### User Story 2 - Companion student sees and joins the shared class (Priority: P2)

Once attached, the companion student needs to know the class is happening and how to join it, just as the original student already does — without the teacher having to relay the details manually over email or chat.

**Why this priority**: Attaching a student with no way for them to find the class details would make the feature useless in practice, but it's a secondary step to the attachment itself.

**Independent Test**: Can be fully tested by attaching a companion student to a class and confirming that student receives the same class details (date/time, how to join) that the original student received, and can see the class in their own upcoming classes.

**Acceptance Scenarios**:

1. **Given** a student has just been attached to a class as the companion student, **When** the attachment completes, **Then** that student receives the class details (date, time, join information).
2. **Given** a student is attached as the companion student on a class, **When** that student views their own upcoming classes, **Then** the shared class appears in their list.

---

### User Story 3 - Teacher removes a companion student added by mistake (Priority: P3)

The teacher attaches the wrong student, or plans change and the companion student is no longer joining. The teacher needs to undo the attachment without cancelling the whole class.

**Why this priority**: This is a correction/edge path rather than the core flow, but is needed so a mistaken attachment isn't a dead end that forces cancelling and rebooking the entire class.

**Independent Test**: Can be fully tested by attaching a companion student to a class, then removing them, and confirming the class reverts to a single-student booking with the original student unaffected.

**Acceptance Scenarios**:

1. **Given** a class with two students attached, **When** the teacher removes the companion student, **Then** the class reverts to showing only the original booking student, and the original booking is otherwise unaffected.
2. **Given** a class with two students attached, **When** the companion student is removed, **Then** that removed student no longer sees the class in their own upcoming classes.

---

### Edge Cases

- What happens if the booking student cancels the class after a companion student has been attached? The companion student should be notified that the class they were joining is no longer happening.
- What happens if the teacher tries to attach a companion student to a class that has already been cancelled or has already taken place? The system should reject the attempt.
- What happens if the companion student is attached but the class is later marked as a no-show? Attendance is recorded separately for each student (see FR-010), so one can be marked present while the other is marked absent.
- What happens if the teacher tries to attach a student who does not have an active student account? The system should reject the attempt (see Assumptions).
- What happens if the same student is attached to more than one class at overlapping times as a "companion student"? Out of scope for this feature to detect — see Assumptions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST continue to let a single student book a class through the existing booking flow, unchanged.
- **FR-002**: The system MUST let the teacher manually attach one additional student ("companion student") to an existing, still-upcoming, confirmed class.
- **FR-003**: The system MUST prevent a class from ever having more than two students attached in total (the original student plus at most one additional).
- **FR-004**: The system MUST prevent attaching a student who is already associated with that same class (either as the original student or as the already-attached companion student).
- **FR-005**: The system MUST prevent attaching a companion student to a class that is cancelled or has already taken place.
- **FR-006**: The system MUST let the teacher remove a previously attached companion student from a class, reverting it to a single-student booking without affecting the original student's booking.
- **FR-007**: The system MUST send the companion student the same class details (date, time, and how to join) that the original student receives, once they are attached.
- **FR-008**: The system MUST show the shared class in the companion student's own list of upcoming classes, alongside the original student's view of the same class.
- **FR-009**: The system MUST let the teacher see, for any class in her administrative view, whether it is shared and who both students are.
- **FR-010**: The system MUST record attendance (no-show) for the booking student and the companion student independently, so one can be marked present while the other is marked absent for the same class.
- **FR-011**: The system MUST notify both students of a shared class whenever it is cancelled by either student or by the teacher, so the student who did not initiate the cancellation still learns the class is no longer happening.
- **FR-012**: When the teacher removes the companion student from a shared class (without cancelling the class itself), the system MUST NOT affect the original student's own booking history.
- **FR-013**: The system MUST let either the booking student or the companion student cancel a shared class themselves (subject to the same cancellation rules that apply today), in addition to the teacher. Cancelling by either student cancels the entire class for both students, exactly as a booking student's cancellation does today.
- **FR-014**: The system MUST require the companion student to hold the same extended-class eligibility as the booking student before they can be attached to a longer-than-standard-length class.
- **FR-015**: The system MUST NOT actively notify the booking student when a companion student is attached to their class; the booking student can see the companion student by viewing their own booking details.

### Key Entities

Both students on a shared class have identical rights to it — either can view the class details, join it, and cancel it, and each has their own independent attendance record. The only distinction between them is procedural: one made the booking, the other was attached to it afterward by the teacher. Neither is more "in charge" of the class than the other.

- **Class Booking**: An upcoming or past scheduled class. Until now, associated with exactly one student; with this feature, may optionally have a companion student attached alongside the original one.
- **Booking Student**: The student who originally booked the class through the normal booking flow. Unaffected by the companion student being attached or removed.
- **Companion Student**: A student manually attached to an existing class booking by the teacher, distinct from the booking student, sharing the same class time and joining details, with equal standing on the class.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The teacher can attach a companion student to an existing class in a single, direct action from her administrative view of upcoming classes.
- **SC-002**: 100% of companion students attached to a class receive that class's date, time, and joining details before the class starts.
- **SC-003**: The teacher can tell, at a glance while reviewing upcoming classes, which classes are shared and who both students are, without needing to cross-reference other records.
- **SC-004**: No class ever ends up with more than two students attached, verified across all attachment attempts.
- **SC-005**: The teacher can undo a mistaken attachment in a single action, with the class returning to a normal single-student booking immediately.

## Assumptions

- The companion student must already be a registered student of Paula's (i.e., hold an active student account) — the teacher picks them from existing students rather than inviting an outside person by email at attach-time.
- Attaching a companion student does not check that student's own schedule for conflicts; because this is a manual, infrequent arrangement between the teacher and both students, avoiding a double-booking for the companion student is treated as the teacher's responsibility, not an automated system check.
- Pricing/payment for classes is unaffected by this feature — attaching a companion student does not change any cost, split any cost, or require a separate purchase from the companion student.
- A student can simultaneously be the companion student on more than one class if the teacher chooses to add them to several classes.
