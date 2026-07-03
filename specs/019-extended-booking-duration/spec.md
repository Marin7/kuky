# Feature Specification: Extended Booking Duration (1.5-Hour Classes)

**Feature Branch**: `019-extended-booking-duration`

**Created**: 2026-07-03

**Status**: Draft

**Input**: User description: "Enable one hour and a half bookings, not just 1h"

## Clarifications

### Session 2026-07-03

- Q: Should the 1.5-hour option be available for all class bookings, or only specific booking types? → A: Only regular classes — the placement-test full evaluation appointment keeps its own separate duration rules and is unaffected by this feature.
- Q: Does every student get the 1.5-hour option automatically, or is it restricted? → A: Restricted — only students the teacher has explicitly granted "extended class" eligibility can book 1.5-hour classes. The teacher grants/revokes this per student from the admin panel (mirroring how STUDENT status is granted today), after agreeing with that student off-platform. Students without this eligibility only see the 1-hour option.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher grants a student permission for 1.5-hour classes (Priority: P1)

Paula (the teacher) wants to grant a specific student permission to book 1.5-hour classes, after they've agreed to it directly (outside the system), so that only students she's approved can access the longer session length.

**Why this priority**: This is a prerequisite for the feature to have any real effect — without a way to grant eligibility, no student can ever see or use the 1.5-hour option.

**Independent Test**: Can be fully tested by having the teacher open a student's profile in the admin panel, grant "extended class" eligibility, and confirming that student (and only that student) subsequently sees the 1.5-hour option when booking.

**Acceptance Scenarios**:

1. **Given** the teacher is viewing a student's profile in the admin panel, **When** she grants that student "extended class" eligibility, **Then** the student can from then on choose a 1.5-hour class when booking.
2. **Given** a student currently has "extended class" eligibility, **When** the teacher revokes it from that student's profile, **Then** the student no longer sees the 1.5-hour option on future bookings (existing 1.5-hour bookings already made are unaffected).
3. **Given** a student has never been granted eligibility, **When** they open the booking flow, **Then** they only see the 1-hour class option.

---

### User Story 2 - Eligible student books a 1.5-hour class (Priority: P1)

A student who has been granted "extended class" eligibility wants the option of a longer, 90-minute session instead of being limited to the standard 60-minute class, so they can get extended instruction time when they need it (e.g. exam prep, a deeper topic, or making up missed time).

**Why this priority**: This is the core of the request — without it, no 1.5-hour booking is possible at all. Together with User Story 1 it delivers the entire value of the feature.

**Independent Test**: Can be fully tested by granting a test student eligibility, opening the booking page, choosing the 1.5-hour option, picking an offered start time, and confirming — the resulting reservation blocks a full 90-minute window and cannot be double-booked.

**Acceptance Scenarios**:

1. **Given** the student has been granted "extended class" eligibility and is on the booking page, **When** they start booking a class, **Then** they can choose between a 1-hour class and a 1.5-hour class.
2. **Given** the student has selected the 1.5-hour option, **When** they view the list of available start times, **Then** only start times with a full, uninterrupted 90 minutes of open availability are offered.
3. **Given** the student selects a valid 1.5-hour start time and confirms, **When** the booking is created, **Then** the full 90-minute window is reserved and becomes unavailable to every other student.
4. **Given** a start time only has 60 minutes of open availability before the next booking or the end of the teacher's available window, **When** the student is choosing a 1.5-hour class, **Then** that start time is not offered as a 1.5-hour option (it may still be offered for a 1-hour class).

---

### User Story 3 - Booking length is visible after booking (Priority: P2)

A student who has booked a class — of either length — wants to see how long that class will run when they look at their upcoming bookings or their confirmation, so they know how much time to set aside.

**Why this priority**: Without visible duration, a student who booked a 1.5-hour class has no way to tell it apart from a 1-hour one after the fact, which undermines the value delivered by User Story 2.

**Independent Test**: Can be fully tested by booking a class of each length and confirming that the confirmation and "My Bookings" view each show the correct start and end time for that class.

**Acceptance Scenarios**:

1. **Given** a student has booked a 1.5-hour class, **When** they view their booking confirmation or "My Bookings" list, **Then** the listed time range reflects the full 90-minute duration.
2. **Given** a student has booked a 1-hour class, **When** they view their booking confirmation or "My Bookings" list, **Then** the listed time range reflects the 60-minute duration.

---

### User Story 4 - Teacher sees class length in her schedule (Priority: P3)

Paula (the teacher) wants to see how long each booked class is directly in her schedule/admin view, so she can plan her day and avoid assuming every class is the same length.

**Why this priority**: Improves the teacher's ability to plan around mixed-length classes, but the feature is still usable end-to-end without it since the underlying time is already blocked correctly.

**Independent Test**: Can be fully tested by having a student book a 1.5-hour class and confirming the teacher's schedule view shows the correct duration/end time for that booking, distinguishing it from 1-hour classes.

**Acceptance Scenarios**:

1. **Given** a mix of 1-hour and 1.5-hour classes booked on the same day, **When** the teacher opens her schedule, **Then** each class visibly shows its own duration/end time so back-to-back classes of different lengths are easy to tell apart.

---

### Edge Cases

- What happens when a student without "extended class" eligibility tries to book a 1.5-hour class (e.g. by tampering with the request directly rather than through the normal UI)? The system rejects it — eligibility is enforced server-side, not just hidden in the UI.
- What happens when a student selects the 1.5-hour option but no start time on the visible schedule has 90 contiguous free minutes? The system shows no available 1.5-hour start times (or an explicit "no availability" message) rather than allowing an invalid selection.
- What happens when a 1.5-hour booking is cancelled? The full 90-minute window reopens and becomes available again for both 1-hour and 1.5-hour bookings.
- What happens when a student books a 1-hour class immediately followed by another student booking a 1.5-hour class right after it? Both are allowed as long as their windows don't overlap and each fits within the teacher's open availability.
- What happens to classes already booked (at 60 minutes) before this feature ships? They are unaffected and remain 60-minute sessions.
- How does the system behave if the teacher's open availability window itself is shorter than 90 minutes on a given day? No 1.5-hour start times are offered for that window; 1-hour start times remain available as today.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The booking flow for a regular class MUST let an eligible student choose between a 1-hour class and a 1.5-hour class before selecting a start time. This choice does not apply to the placement-test full evaluation appointment, which keeps its own separate duration and is unaffected by this feature.
- **FR-002**: The system MUST only offer a start time for a 1.5-hour class when there are 90 contiguous, unbooked minutes available starting at that time within the teacher's open availability.
- **FR-003**: The system MUST continue to offer 1-hour start times exactly as it does today, for every student, independent of 1.5-hour availability or eligibility.
- **FR-004**: When a booking is created, the system MUST reserve the full selected duration (60 or 90 minutes) so that no other booking can overlap any part of that window.
- **FR-005**: The system MUST record and retain the chosen duration for each booking so it can be displayed and relied on later (e.g. after cancellation/rebooking flows).
- **FR-006**: Booking confirmations and the student's list of upcoming/past bookings MUST display the start and end time (or clearly state the duration) for each booking, for both 1-hour and 1.5-hour classes.
- **FR-007**: The teacher's schedule/admin view MUST display the duration (or end time) of each booked class, so classes of different lengths are visually distinguishable.
- **FR-008**: Cancelling a 1.5-hour booking MUST free the entire 90-minute window for other students to book, at either duration (subject to each booking student's own eligibility).
- **FR-009**: The system MUST prevent a 1.5-hour booking from being created if it would overlap any existing booking, regardless of that existing booking's own duration.
- **FR-010**: Existing bookings made before this feature is available MUST remain valid and continue to reflect their original 1-hour duration.
- **FR-011**: The teacher MUST be able to grant or revoke "extended class" eligibility for an individual student from the admin panel.
- **FR-012**: A student without "extended class" eligibility MUST NOT be able to book a 1.5-hour class — the 1.5-hour option MUST NOT be offered to them, and the system MUST reject any attempt to create a 1.5-hour booking for that student regardless of how the request is made.
- **FR-013**: Revoking a student's "extended class" eligibility MUST NOT affect 1.5-hour bookings that student already made before the revocation.

### Key Entities

- **Class Booking**: A reserved 1-on-1 session between a student and the teacher. Now carries a session length (60 or 90 minutes) in addition to its start time, which together determine the reserved window.
- **Available Start Time**: A candidate time a student can choose to begin a class. Now evaluated per requested duration — a given moment may be offered for a 1-hour class, a 1.5-hour class, both, or neither, depending on how much contiguous open time follows it and the requesting student's eligibility.
- **Extended Class Eligibility**: A per-student permission, granted or revoked by the teacher, that determines whether that student is allowed to book 1.5-hour classes. Independent of the STUDENT role — a student can hold STUDENT access without extended-class eligibility.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Students can book a 1.5-hour class end-to-end (choose duration, pick a time, confirm) in the same number of steps and about the same time as booking today's 1-hour class.
- **SC-002**: 100% of start times offered for a 1.5-hour class have the full 90 minutes actually free at the moment of booking — zero double-bookings caused by insufficient duration.
- **SC-003**: 100% of booked classes (both lengths) show their correct duration/end time to the student and to the teacher after booking.
- **SC-004**: Existing 1-hour booking behavior shows no regression — students can still book, view, and cancel 1-hour classes exactly as before.
- **SC-005**: 100% of students without granted "extended class" eligibility are unable to create a 1.5-hour booking, whether through the normal booking flow or a direct request.
- **SC-006**: The teacher can grant or revoke a student's "extended class" eligibility in a single action from the student's profile, with the change taking effect on that student's very next booking attempt.

## Assumptions

- Only two class lengths are in scope for this feature: the existing 60-minute class and a new 90-minute class. Other lengths (e.g. 30 minutes, 2 hours) are out of scope.
- This feature introduces no change to pricing or payment — the current booking flow does not attach a price to a class, so a 1.5-hour class is booked the same way (no extra charge/step) as a 1-hour one.
- The teacher's existing weekly availability windows do not need to be restructured; a 1.5-hour class is simply only offered where enough contiguous open time already exists within those windows.
- The 1-hour option remains available to every student wherever booking is available today; the 1.5-hour option is additionally gated by per-student "extended class" eligibility, on top of the contiguous-availability check.
- The teacher grants "extended class" eligibility one student at a time from the admin panel (mirroring the existing STUDENT-status grant/revoke pattern); there is no bulk or self-service path for a student to grant it to themselves.
- The agreement between teacher and student to use a 1.5-hour class happens outside the system (e.g. in conversation or by message) before the teacher grants eligibility; the system does not need to capture or verify that agreement itself.
- Rescheduling an existing booking to a different duration is out of scope for this feature; a student who wants a different length cancels and creates a new booking through the existing flow.
