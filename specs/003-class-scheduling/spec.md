# Feature Specification: 1-on-1 Class Scheduling

**Feature Branch**: `003-class-scheduling`

**Created**: 2026-06-09

**Status**: Draft

**Input**: User description: "We need to integrate a scheduling capability where students can see the teacher's schedule and available slots and they will be able to schedule a 1-1 class. You will have to be logged-in in order to be able to schedule a class, but the open spots can be viewed even by the logged-out users. Scheduling a class will generate a Zoom meeting too"

## Clarifications

### Session 2026-06-09

- Q: How far into the future are slots visible/bookable (booking horizon)? → A: Current calendar week + next week only.
- Q: Is there a minimum lead time before a slot's start at which booking closes? → A: 24 hours before slot start (no same-day booking).
- Q: What is the standard class duration? → A: 60 minutes by default, but configurable (not hard-coded).
- Q: How does a booking become "completed"? → A: No "completed" state — booking is confirmed/cancelled; "past" is derived from the slot time.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse available class slots (Priority: P1)

Any visitor — whether signed in or not — can open the bookings page and see Paula's
upcoming availability: which dates and times have open 1-on-1 class slots. The view
makes it obvious which slots are free and which are already taken, so a prospective
student can judge whether Paula's schedule fits theirs before committing to creating
an account.

**Why this priority**: Visibility of availability is the foundation of the whole
feature and the primary draw for prospective students. It delivers value on its own —
it lets visitors evaluate fit and acts as a conversion funnel toward registration —
and every other story depends on the slot data this story surfaces.

**Independent Test**: Load the bookings page while logged out and confirm that open
and taken slots for the coming period are displayed accurately in the viewer's local
time, with no ability to book until signed in.

**Acceptance Scenarios**:

1. **Given** a logged-out visitor and Paula has published open slots, **When** they open the bookings page, **Then** they see the list/calendar of upcoming open slots with date and time.
2. **Given** a logged-out visitor, **When** they attempt to book an open slot, **Then** they are prompted to sign in or register before they can proceed.
3. **Given** a slot that another student has already booked, **When** any visitor views the schedule, **Then** that slot is shown as unavailable (not bookable).
4. **Given** a visitor in a different timezone than Paula, **When** they view the schedule, **Then** slot times are shown in the visitor's local timezone.

---

### User Story 2 - Book a 1-on-1 class with automatic video meeting (Priority: P1)

A signed-in student selects an open slot and books it as a 1-on-1 class. On confirmation
the system reserves the slot exclusively for that student, automatically creates a video
meeting (Zoom) for the session, and makes the meeting link available to both the student
and Paula. The booked slot immediately stops being available to anyone else.

**Why this priority**: This is the core action the feature exists to enable — turning
an open slot into a confirmed, joinable class. Without it the schedule is read-only.
It pairs with Story 1 as the minimum viable product.

**Independent Test**: While signed in, book an open slot and confirm that the slot
becomes unavailable to others, a confirmation with a Zoom meeting link is produced, and
the booking appears in the student's account.

**Acceptance Scenarios**:

1. **Given** a signed-in student viewing an open slot, **When** they confirm a booking, **Then** the slot is reserved for them and is no longer offered to other users.
2. **Given** a successful booking, **When** the confirmation is generated, **Then** a Zoom meeting is created for that date/time and its join link is associated with the booking.
3. **Given** a successful booking, **When** it completes, **Then** both the student and Paula receive the booking details and Zoom join link (e.g. by email and/or in their account view).
4. **Given** two students attempting to book the same slot at nearly the same time, **When** both confirm, **Then** only one booking succeeds and the other is told the slot is no longer available.
5. **Given** a Zoom meeting cannot be created, **When** the student confirms the booking, **Then** the system does not silently reserve a class without a usable meeting — the student is informed and the slot remains bookable.

---

### User Story 3 - View and manage my bookings (Priority: P2)

A signed-in student can see a list of their upcoming and past booked classes, each with
its date, time, and Zoom join link, and can cancel an upcoming class within the allowed
cancellation window. Cancelling frees the slot so it becomes available to others again.

**Why this priority**: Students need a reliable place to retrieve their join link and to
back out of a class they can no longer attend. It is essential for a trustworthy booking
experience but is not required to demonstrate the core book-a-class flow, so it ranks
just below the P1 stories.

**Independent Test**: As a signed-in student with at least one upcoming booking, view the
booking list with its Zoom link, cancel an upcoming class, and confirm the freed slot
reappears as open for other users.

**Acceptance Scenarios**:

1. **Given** a signed-in student with bookings, **When** they open their account/bookings view, **Then** they see their upcoming and past classes with date, time, and Zoom link.
2. **Given** a signed-in student with an upcoming class inside the cancellation window, **When** they cancel it, **Then** the booking is marked cancelled and the slot becomes available again.
3. **Given** a cancellation, **When** it completes, **Then** Paula is notified and the associated Zoom meeting is invalidated/removed.
4. **Given** a class outside the allowed cancellation window, **When** the student tries to cancel, **Then** the system prevents it and explains the policy.

---

### Edge Cases

- **Past or in-progress slots**: Slots whose start time has already passed MUST NOT be bookable and SHOULD be excluded from the open-slot view.
- **Concurrent booking race**: Two users confirming the same slot simultaneously — exactly one succeeds; the loser gets a clear "no longer available" message.
- **Authentication expiry mid-flow**: A user whose session expires between viewing and confirming a booking is asked to sign in again, and no booking is created until they do.
- **Video-meeting provider outage**: If the Zoom meeting cannot be created, the booking is not finalized and the slot stays open (see US2 scenario 5).
- **Double booking by the same student**: Behaviour when a student books overlapping slots — the system SHOULD prevent a student from holding two classes at the same time.
- **Timezone & daylight-saving boundaries**: Slots spanning a daylight-saving change are displayed at the correct wall-clock time for each party.
- **Cancellation after Zoom link issued**: Cancelling removes/invalidates the meeting so a stale link is not left active.
- **No availability published**: The bookings page communicates clearly when there are currently no open slots, rather than appearing broken.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display Paula's upcoming open 1-on-1 class slots to all visitors, including those who are not signed in.
- **FR-002**: System MUST clearly distinguish open (bookable) slots from unavailable (already booked or past) slots in the schedule view.
- **FR-003**: System MUST display slot times in the viewing user's local timezone while preserving the correct absolute time of each slot.
- **FR-004**: System MUST require a user to be signed in before they can book a slot, and MUST guide a logged-out user to sign in or register when they attempt to book.
- **FR-005**: Signed-in students MUST be able to book an available slot as a 1-on-1 class.
- **FR-006**: System MUST reserve a booked slot exclusively, making it unavailable to all other users immediately upon successful booking.
- **FR-007**: System MUST guarantee that a given slot can be held by at most one active booking, even under concurrent booking attempts.
- **FR-008**: System MUST automatically create a Zoom video meeting for each confirmed booking, scheduled for the slot's date and time.
- **FR-009**: System MUST associate the Zoom join link with the booking and make it available to both the student and Paula.
- **FR-010**: System MUST notify the student and Paula of a confirmed booking with its details and the Zoom join link.
- **FR-011**: System MUST NOT finalize a booking if the Zoom meeting cannot be created; in that case the slot MUST remain bookable and the student MUST be informed.
- **FR-012**: Signed-in students MUST be able to view their upcoming and past bookings, each showing date, time, and Zoom join link.
- **FR-013**: Students MUST be able to cancel an upcoming booking up to 24 hours before its start time; cancellation MUST be prevented once the class is within 24 hours of starting. Rescheduling is not supported in this phase — to move a class, a student cancels the booking (freeing the slot) and books another open slot.
- **FR-014**: When a booking is cancelled, System MUST release the slot back to availability, invalidate/remove the associated Zoom meeting, and notify Paula.
- **FR-015**: System MUST prevent booking of slots whose start time has already passed.
- **FR-016**: System MUST generate Paula's bookable slots from a fixed daily availability window of 09:00–18:00 in Paula's timezone for this phase. A teacher-only interface for Paula to define and edit her own availability is deferred to a future phase; the availability source MUST be isolated so it can later be replaced by teacher-managed availability without reworking the booking flow.
- **FR-017**: System MUST use a standard 1-on-1 class duration of 60 minutes by default for slot generation and Zoom meeting creation, and this duration MUST be configurable (not hard-coded) so it can be changed without altering the scheduling/booking logic. Changing the duration adjusts how many slots the daily availability window yields.
- **FR-018**: System MUST treat booking as a free reservation in this phase (no payment is taken at booking time). The booking data model MUST be designed so that a paid-booking flow can be added in a later phase without reworking the core scheduling and booking logic.
- **FR-019**: System MUST communicate clearly to the user when no open slots are currently available.
- **FR-020**: System MUST limit the visible and bookable booking horizon to the current calendar week plus the following calendar week; slots beyond that horizon MUST NOT be shown or bookable, and the horizon MUST advance automatically as time passes.
- **FR-021**: System MUST close booking for a slot once it is within 24 hours of its start time (no same-day or last-minute booking); such slots MUST be presented as not bookable while remaining visible per the horizon rules.

### Key Entities *(include if feature involves data)*

- **Availability Slot**: A discrete window of time Paula has made available for a 1-on-1 class. Key attributes: start time, duration, status (open / booked / expired). Relates to at most one active Booking.
- **Booking**: A confirmed reservation of a slot by a student. Key attributes: the student who booked, the reserved slot (date/time), status (confirmed / cancelled), creation time, cancellation time. There is no separate "completed" status — whether a class is upcoming or past is derived by comparing the slot's end time to the current time. Relates to one Student (existing user account) and one Availability Slot, and owns one Video Meeting.
- **Video Meeting (Zoom)**: The auto-generated online meeting for a booking. Key attributes: join link, scheduled start/duration, provider meeting identifier, lifecycle status (active / cancelled). Relates one-to-one with a Booking.
- **Student**: An existing registered user (from the account-management feature) who can book classes. Key attributes reused from the existing account: identity, email for notifications.
- **Teacher (Paula)**: The single instructor whose availability is published and who attends every booked class. Receives booking and cancellation notifications and the Zoom link.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A logged-out visitor can find and understand Paula's open slots within 30 seconds of opening the bookings page, with no sign-in required to view them.
- **SC-002**: A signed-in student can complete a booking — from selecting a slot to receiving a confirmation with a join link — in under 2 minutes.
- **SC-003**: 100% of confirmed bookings have a working video-meeting join link available to both parties before the class start time.
- **SC-004**: A slot is never double-booked: across all concurrent attempts, at most one active booking exists per slot, verified to hold 100% of the time under simultaneous-attempt testing.
- **SC-005**: Booked slots disappear from the public open-slot view, and cancelled slots reappear, within seconds of the action.
- **SC-006**: At least 90% of students who start a booking successfully complete it on the first attempt.
- **SC-007**: Students can locate the join link for an upcoming class from their account in under 15 seconds.

## Assumptions

- **Reuses existing accounts**: Authentication and user accounts from the account-management feature (`/cuenta`) are reused; this feature adds no new sign-in mechanism.
- **Single teacher**: There is exactly one teacher (Paula); the schedule represents her availability only. No multi-instructor support is in scope.
- **Class format**: Each bookable slot is a single 1-on-1 (one student, one teacher) class; group classes are out of scope.
- **Default class duration**: A class is 60 minutes by default, used for slot length and Zoom meeting duration. The duration is configurable; with the default 60 minutes the 09:00–18:00 window yields 9 slots/day (09:00, 10:00 … 17:00).
- **Fixed availability this phase**: Paula is assumed available every day from 09:00 to 18:00 in her own timezone, yielding consecutive 60-minute bookable slots. Day-of-week restrictions (e.g. excluding weekends) are not applied unless later specified. A teacher-managed availability interface is a deferred future phase.
- **Booking horizon**: Only the current calendar week plus the next calendar week are shown and bookable; the window rolls forward automatically. The week is assumed to start on Monday per the Romanian/Spanish locale.
- **Free booking this phase**: Booking takes no payment now; the data model is designed so a paid-booking flow can be introduced later without reworking scheduling/booking logic.
- **Cancellation policy**: Students may cancel up to 24 hours before a class start; rescheduling is not offered this phase (cancel and rebook instead).
- **Minimum booking lead time**: A slot stops being bookable 24 hours before its start (no same-day booking). This mirrors the cancellation window, so any confirmed booking is effectively locked in by the 24-hour mark.
- **Timezone handling**: Paula's availability is authored in her own timezone; all viewers see times converted to their local timezone. Absolute time is the source of truth.
- **Notifications channel**: Booking and cancellation notifications reuse the existing email capability; in-account display of bookings supplements email.
- **Video provider**: "Generate a Zoom meeting" means integrating with Zoom specifically as the video-meeting provider; each booking gets its own meeting.
- **New bookings page**: The existing `/reservas` placeholder becomes the home for the schedule and booking experience.
- **No waitlist**: If a slot is taken, there is no waitlist/queue in this phase; the student picks another open slot.
- **Reminders out of scope (this phase)**: Automated pre-class reminder emails are not required for the initial release (the confirmation/join link is provided at booking time).

## Dependencies

- Existing account-management feature for authentication, user identity, and email notifications.
- A Zoom account/integration with the ability to programmatically create and cancel meetings.
