# Feature Specification: Timezone Support

**Feature Branch**: `017-timezone-support`

**Created**: 2026-07-02

**Status**: Draft

**Input**: User description: "Add Timezone support"

## Clarifications

### Session 2026-07-02

- Q: How should the time zone be labeled alongside displayed times? → A: Zone/city name (e.g., "Europe/Bucharest", "hora de Bucarest")
- Q: How should the student's time zone preference behave over time — auto-detected vs. manually overridden? → A: Auto-detected zone re-syncs each session unless the student has an active manual override, which persists until explicitly changed/cleared
- Q: SC-003 assumed no-show/reschedule "reasons" are tracked, but no such field exists today — how should this success criterion be stated? → A: Restate as a qualitative outcome (no confirmed reports of missed/double-booked classes due to time zone confusion), reviewed manually, without assuming new tracking infrastructure

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Student sees class times in their own local time (Priority: P1)

A student browsing the public schedule or managing their bookings sees every class time expressed in their own local time, not the teacher's fixed working time zone. They never have to manually convert hours to know whether a slot works for them.

**Why this priority**: This is the core value of the feature — without it, students risk booking (or missing) classes at the wrong hour, which directly causes no-shows and lost trust. It is the highest-frequency touchpoint (every schedule view and every booking).

**Independent Test**: Can be fully tested by loading the public schedule and "Mis Reservas" (My Bookings) list from a browser/device set to a different time zone than the teacher's, and confirming every displayed slot and booking reflects the viewer's local wall-clock time with the zone clearly labeled.

**Acceptance Scenarios**:

1. **Given** a student's device is set to a time zone different from the teacher's working time zone, **When** the student opens the public schedule, **Then** every available slot is displayed in the student's local time, with the time zone's region/city name (e.g., "Europe/Bucharest") clearly labeled.
2. **Given** a student has an existing booking, **When** they view "Mis Reservas," **Then** the booking's date and time are shown in the student's current local time zone.
3. **Given** a student books a slot shown as, e.g., "18:00" in their local time, **When** the booking is confirmed, **Then** the class occurs at the exact same real-world instant the student selected (no shift introduced by the conversion).

---

### User Story 2 - Booking confirmations and reminders show the correct local time (Priority: P2)

A student who books a class receives an on-screen confirmation, a class reminder email, and a calendar invite (.ics) that all state the class time in a way the student can act on correctly, regardless of where they are.

**Why this priority**: Confirmations and reminders are read outside the app (email, calendar), often without the context available to a logged-in session. Getting these wrong undermines the trust the P1 fix establishes and directly causes missed classes.

**Independent Test**: Can be fully tested by completing a booking as a student in a non-teacher time zone and inspecting the confirmation screen, the reminder email content, and the imported calendar event's displayed time, confirming all three agree with each other and with the student's local time.

**Acceptance Scenarios**:

1. **Given** a student completes a booking, **When** the confirmation is shown, **Then** the stated class time matches the student's local time zone.
2. **Given** a class reminder email is sent ahead of a booked class, **When** the student reads it, **Then** the stated time matches their local time zone at the time the class occurs.
3. **Given** a student adds the calendar invite (.ics) to their personal calendar, **When** they view the event, **Then** their calendar application shows the correct local start/end time (calendar apps interpret the invite using the device's own time zone).

---

### User Story 3 - Teacher's own schedule stays anchored to her working time zone (Priority: P3)

The teacher (admin) manages her weekly availability and reviews bookings always in her own fixed working time zone, regardless of the device or browser she happens to use, so her mental model of "my day" never shifts.

**Why this priority**: Lower frequency of impact than the student-facing views (one user, not many), but a regression here (e.g., her own panel suddenly showing browser-local time while traveling) would break her ability to manage availability correctly.

**Independent Test**: Can be fully tested by loading the admin availability editor and bookings tab from a browser/device set to a time zone different from the teacher's configured working time zone, and confirming all times still display in the teacher's working time zone, not the device's.

**Acceptance Scenarios**:

1. **Given** the admin's device is set to a different time zone than her configured working time zone, **When** she opens the availability editor, **Then** the weekly grid displays her working time zone's hours, unchanged.
2. **Given** a student books a class, **When** the teacher views it in the admin bookings tab, **Then** the time shown matches her working time zone.

---

### Edge Cases

- What happens when a slot falls in the one hour that is skipped or repeated during a Daylight Saving Time transition (in either the teacher's or the student's time zone)? The system must not display a duplicate, missing, or ambiguous slot.
- What happens when a student's local time zone cannot be determined (e.g., detection blocked or unavailable)? The system must fall back to a clearly labeled default rather than silently guessing.
- What happens when a student books a class, then travels and their device's time zone changes before the class occurs? Previously booked times must still resolve to the same real-world instant, only redisplayed in the new local time.
- What happens when the teacher changes her configured working time zone (e.g., permanently relocates)? Already-booked classes must keep their original real-world instant; only the teacher's display zone changes going forward.
- What happens for a class that starts before midnight in one time zone and after midnight in another (calendar date differs by time zone)? The displayed date must match each viewer's own local date, not a shared reference date.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display all class schedule and booking-related times to students in the student's own local time zone rather than a single fixed time zone.
- **FR-002**: System MUST clearly label the time zone by region/city name (e.g., "Europe/Bucharest") alongside any displayed time, so viewers are never left guessing which zone a time is shown in.
- **FR-003**: System MUST determine each student's time zone automatically from their device/browser on each session, and allow the student to manually override it if the automatic detection is wrong or they want to plan for a different location. Once set, a manual override MUST persist and take precedence over auto-detection until the student explicitly changes or clears it.
- **FR-004**: System MUST preserve the exact real-world instant of a class through any time zone conversion — converting for display must never change what moment a booking actually refers to.
- **FR-005**: System MUST show the correct local time in booking confirmations, class reminder emails, and calendar invites (.ics), consistent with the recipient's time zone.
- **FR-006**: System MUST correctly handle Daylight Saving Time transitions for both the teacher's and any student's time zone, without producing duplicate, missing, or ambiguous slots across the transition.
- **FR-007**: System MUST continue to display the teacher's/admin's own schedule (availability editor, bookings tab, student profiles) in the teacher's configured working time zone, independent of the device the admin is using.
- **FR-008**: System MUST provide a clearly labeled fallback time zone when a student's time zone cannot be automatically determined.
- **FR-009**: System MUST reflect a student's calendar date (not just time) correctly for their own time zone when a class instant falls on a different calendar date locally than in the teacher's time zone.
- **FR-010**: System MUST retain a single, unambiguous time zone as the reference for the teacher's availability configuration and business rules, unaffected by how it's displayed to students.

### Key Entities

- **Student Time Zone Preference**: The time zone used to display times to a given student, tied to the student's account. Re-detected automatically from the device each session unless the student has set a manual override, which persists across sessions until explicitly changed or cleared.
- **Teacher Working Time Zone**: The single fixed time zone the teacher's own availability and admin views are always anchored to, configured once for the business.
- **Scheduled Class Instant**: The canonical, zone-independent real-world moment a class occurs; the source of truth that all time-zone-specific displays (student view, teacher view, emails, calendar invite) are derived from.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Students booking from a time zone different from the teacher's can correctly identify the class time in their own local time on their first view, without needing to perform any manual conversion.
- **SC-002**: 100% of booking confirmations, reminder emails, and calendar invites display a class time that matches the recipient's own local time zone.
- **SC-003**: No confirmed reports (via student support inquiries or teacher-reported incidents, reviewed manually) of a class being missed or double-booked due to time zone confusion after the feature ships.
- **SC-004**: All class times remain accurate (no shifted, duplicated, or missing slots) across Daylight Saving Time transitions in both the teacher's and students' time zones.
- **SC-005**: The teacher's own view of her schedule remains unchanged in wall-clock terms regardless of which device or location she accesses it from.

## Assumptions

- The teacher's working time zone remains a single, fixed value configured for the business (as today); this feature does not add support for the teacher operating across multiple time zones simultaneously.
- Every class booking already has (or will have) a single canonical real-world instant as its source of truth; time zone support only changes how that instant is *displayed*, not how it is stored or scheduled.
- Automatic time zone detection uses the student's device/browser settings; students who want a different display (e.g., planning ahead for travel) can override it manually.
- This feature applies to all existing student-facing surfaces where class times appear: public schedule, booking flow, "Mis Reservas," class reminder emails, and calendar invites — as well as the admin/teacher views, which stay anchored to the teacher's working time zone.
- No new business logic around *when* the teacher is willing to teach is introduced; this is purely about correct, unambiguous time zone display and conversion for the existing scheduling system.
