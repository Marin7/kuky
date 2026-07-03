# Feature Specification: Class Booking Buffer Time

**Feature Branch**: `020-class-breaks`

**Created**: 2026-07-03

**Status**: Draft

**Input**: User description: "I want to add a 15-minutes breaks before and after a class"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic breathing room between classes (Priority: P1)

As a student browsing the schedule to book a class, I should not be able to select a time slot that starts less than 15 minutes after an existing class ends, or that ends less than 15 minutes before another class begins, so that the teacher always has a buffer between any two consecutive classes.

**Why this priority**: This is the core value of the feature. Without it, the teacher can end up with back-to-back bookings and no time to prepare, take notes, or rest between sessions. This alone delivers the requested behavior.

**Independent Test**: Book a class ending at 10:00, then view the schedule for that day and confirm no slot is offered starting before 10:15. Attempt to book a class ending at 9:50 immediately before it and confirm it is rejected.

**Acceptance Scenarios**:

1. **Given** a confirmed class scheduled 10:00–11:00, **When** a student views available slots for that day, **Then** no slot is offered that would start before 11:15 or end after 9:45.
2. **Given** a confirmed 90-minute class scheduled 14:00–15:30, **When** a student views available slots for that day, **Then** no slot is offered that would start before 15:45 or end after 13:45.
3. **Given** two existing classes separated by at least a class-length gap plus 15 minutes on each side, **When** a student books a slot that exactly fills the remaining gap while preserving a 15-minute buffer against both neighboring classes, **Then** the booking succeeds.

---

### User Story 2 - Buffer holds no matter who books the adjacent class (Priority: P2)

As the teacher, I want the 15-minute buffer enforced between any two classes — whether booked by the same student or by different students — so my schedule always has consistent breathing room no matter who books around a given class.

**Why this priority**: Closes an obvious loophole: a single student booking two sessions of their own back-to-back would still deny the teacher a break if the rule only applied across different students.

**Independent Test**: Have one student book a class ending at 11:00, then have that same student attempt to book another class starting at 11:05 and confirm it is rejected the same way it would be for a different student.

**Acceptance Scenarios**:

1. **Given** a student already has a confirmed class ending at 11:00, **When** that same student tries to book another class starting at 11:05, **Then** the slot is not offered and the booking is rejected.

---

### User Story 3 - Cancelled classes free up their buffer (Priority: P3)

As a student, when a nearby class is cancelled, I want the previously blocked buffer time to become available for booking, so cancellations don't waste schedule capacity.

**Why this priority**: A correctness guarantee. The main flow (P1/P2) works without this, but skipping it would leave unusable, "orphaned" gaps behind every cancellation.

**Independent Test**: Book two classes with a valid gap between them, cancel one, and confirm slots within its former buffer become bookable again (as long as no other confirmed class still requires them to stay blocked).

**Acceptance Scenarios**:

1. **Given** a confirmed class from 10:00–11:00 that is blocking slots up to 11:15, **When** that class is cancelled, **Then** slots starting as early as 10:00 become available again, subject to any other remaining confirmed bookings.

---

### Edge Cases

- The buffer only applies when there is another confirmed class adjacent to it — the first bookable slot of the day may still start right when the teacher's availability window opens, and the last slot may still end right when it closes.
- Bookings confirmed before this feature ships that already sit less than 15 minutes apart are left untouched; the buffer rule governs only bookings made after rollout.
- When a 60-minute and a 90-minute class are adjacent, the buffer applies identically regardless of the mix of durations (assumed default, not blocking).
- If a student's cancellation and a different student's new booking race for the same freed-up buffer slot, the normal booking conflict handling (first confirmed booking wins) applies unchanged.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST prevent a class from being booked when it would start less than 15 minutes after the end of another confirmed class.
- **FR-002**: System MUST prevent a class from being booked when it would end less than 15 minutes before the start of another confirmed class.
- **FR-003**: The 15-minute buffer MUST be enforced regardless of booking duration (both 60-minute standard classes and 90-minute extended classes).
- **FR-004**: The 15-minute buffer MUST be enforced regardless of which student holds the adjacent class — a student MUST NOT be able to bypass the buffer by booking two of their own classes back-to-back.
- **FR-005**: The schedule of available times shown to students MUST exclude any slot that would violate the 15-minute buffer around an existing confirmed class.
- **FR-006**: Cancelling a confirmed class MUST release its associated buffer time so adjacent slots become bookable again, subject to any other remaining confirmed bookings still requiring a buffer.
- **FR-007**: The 15-minute buffer MUST NOT be counted as part of the class duration itself — it is not billed, purchased, or presented to the student as class time.
- **FR-008**: System MUST NOT require a 15-minute buffer at the boundaries of the teacher's daily availability window — the first bookable slot of a window MAY start exactly when the window opens, and the last bookable slot MAY end exactly when it closes, as long as no other confirmed class is adjacent.
- **FR-009**: System MUST apply the 15-minute buffer rule only to bookings made after this feature is released; confirmed classes booked before rollout that are already less than 15 minutes apart MUST remain unchanged and MUST NOT be cancelled, rescheduled, or otherwise modified by this change.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of classes booked after rollout have at least a 15-minute gap before and after every other confirmed class on the schedule.
- **SC-002**: Students never encounter a booking failure caused by the buffer rule after selecting a slot from the schedule — 100% of slots shown as available are actually bookable, because conflicting slots are excluded from the schedule up front.
- **SC-003**: After a class cancellation, the freed buffer time becomes bookable again within the same page load / next schedule refresh, with no manual intervention needed.

## Assumptions

- The buffer duration is fixed at 15 minutes on both sides of every class; it is not configurable per teacher, per student, or per class type in this feature.
- The buffer applies uniformly to both the 60-minute standard class and the 90-minute extended class durations.
- The buffer is not a purchasable or separately visible line item — it simply reduces the set of times offered as bookable on the schedule.
- The buffer is only required between two confirmed classes; the teacher's configured availability window boundaries are not treated as an implicit adjacent class.
- The rule is forward-looking only: bookings confirmed before this feature ships are grandfathered in, even if already closer than 15 minutes together.
