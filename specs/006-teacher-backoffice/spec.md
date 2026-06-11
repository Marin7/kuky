# Feature Specification: Teacher Backoffice — Control Panel

**Feature Branch**: `006-teacher-backoffice`

**Created**: 2026-06-10

**Status**: Draft

**Input**: User description: "We need to build the control panel/backoffice. The place where the administrator, the teacher, can do things, like setting up their calendar, so that the students or potential students can check availability. The place where the teacher generates homeworks and assigns the homeworks to students. The place where the teacher can generate presentations for 1-1 classes, similar to PowerPoints, but with obviously much fewer functionalities."

## Clarifications

### Session 2026-06-10

- Q: How should the system identify the single teacher/administrator account that may access the backoffice? → A: A persisted role/admin designation stored on the user account (the teacher account is promoted via migration/seed); authorization checks read the stored role.
- Q: How are slide images (and any content images) provided? → A: The teacher uploads image files; the system stores and serves them, enforcing allowed type and maximum size (no external-URL-only mode).
- Q: Which availability-management model does the teacher use? → A: A recurring weekly pattern (per-weekday time windows) plus one-off date-specific exceptions that block or open time on a particular date.
- Q: What happens to the existing shared/seeded homework from the learning feature (005) when per-student assignment is introduced? → A: Switch fully to per-student assignment — the shared-to-everyone model is retired/migrated, and every homework item is shown only to its explicitly assigned students.

## User Scenarios & Testing *(mandatory)*

This feature introduces a private, teacher-only **control panel** ("backoffice") where Paula — the single teacher/administrator — manages the content and availability that students experience on the public site. It is the authoring counterpart to the student-facing pages (`/reservas`, `/recursos`, `/aprendizaje`): everything students see as read-only is something Paula creates and maintains here. The panel is reachable only by the designated teacher account and is invisible and inaccessible to students and guests.

### User Story 1 - Manage availability so students can book accurate times (Priority: P1)

Paula opens the control panel and edits when she is available to teach. She sets a normal weekly pattern (e.g. "Mondays and Wednesdays 09:00–13:00 and 16:00–18:00") and can make one-off exceptions for specific dates (block a day off when she is on holiday, or open an extra evening). After she saves, the public scheduling calendar that students and prospective students browse immediately reflects her real availability — open times appear, blocked times disappear — without any developer involvement.

**Why this priority**: Availability is the gateway to bookings, which is the core revenue activity of the business. Today availability is hard-coded and can only be changed by editing configuration and redeploying; letting Paula control it herself is the single highest-value capability of the backoffice and unblocks her from depending on a developer for routine schedule changes.

**Independent Test**: Sign in as the teacher, set a weekly availability pattern and one date-specific exception, save, then open the public schedule as a student and confirm the bookable slots match exactly what was configured (and that an exception date shows blocked/extra slots accordingly). Delivers value on its own: Paula can run her real booking schedule.

**Acceptance Scenarios**:

1. **Given** the teacher is signed in to the control panel, **When** she adds an availability window for a weekday and saves, **Then** the public schedule offers bookable slots inside that window on every matching upcoming day (subject to the existing minimum-lead-time rule).
2. **Given** the teacher has a weekly availability window, **When** she adds a one-off block for a specific date that overlaps it, **Then** the public schedule shows no open slots on that date for the blocked time, while other dates are unaffected.
3. **Given** a student already has a confirmed booking inside a time range, **When** the teacher tries to remove availability that would cover that booking, **Then** the system warns her that an existing booking falls in that range and the confirmed booking is preserved.
4. **Given** the teacher opens an extra one-off window on a date outside her normal weekly pattern, **When** she saves, **Then** the public schedule offers bookable slots on that date.

---

### User Story 2 - Author homework and assign it to students (Priority: P2)

Paula creates a homework item — a title, instructions, and an optional due date — and assigns it to one or more specific students. The assigned students then see the new homework in their personal "Mi aprendizaje" space, where they can read it and submit their response. Paula can review the list of homework she has created, see which students it was assigned to, see who has submitted, edit an item, and unassign or delete it.

**Why this priority**: Homework drives learning continuity between classes and is a differentiator for the teaching service. The student-facing homework experience already exists but is fed by fixed seed data; this story gives Paula the ability to actually create and target homework, turning a static demo into a usable teaching tool. It depends on the backoffice access established in P1 but is otherwise independent of availability.

**Independent Test**: Sign in as the teacher, create a homework item, assign it to a specific student, then sign in as that student and confirm the homework appears in their learning space; sign in as a different (unassigned) student and confirm it does not appear. Delivers value on its own: Paula can run real homework assignments.

**Acceptance Scenarios**:

1. **Given** the teacher is in the control panel, **When** she creates a homework item with a title, instructions, and due date and assigns it to a chosen student, **Then** that student sees the homework in their learning space with the correct details.
2. **Given** a homework item assigned to Student A only, **When** Student B views their learning space, **Then** the homework does not appear for Student B.
3. **Given** a homework item assigned to a student who has submitted a response, **When** the teacher views the homework in the control panel, **Then** she can see that it was submitted and read the student's response.
4. **Given** an existing homework item, **When** the teacher edits its instructions or due date and saves, **Then** assigned students see the updated details.
5. **Given** an existing homework item, **When** the teacher unassigns a student or deletes the item, **Then** it no longer appears in that student's learning space.

---

### User Story 3 - Build and share simple class presentations (Priority: P3)

Paula builds a lightweight slide presentation for a 1-on-1 class — a title and an ordered set of slides, each slide holding a heading, some text, and optionally an image. She can add, reorder, edit, and remove slides, and preview the deck full-screen the way a student would see it. She can share a finished presentation with a specific student so it appears in that student's learning space for them to review before or after the class. Presentations are intentionally simple compared with full presentation software: no animations, transitions, charts, or collaborative editing.

**Why this priority**: Presentations enrich the live teaching experience and give students reusable study material, but classes can run without them, so this is valuable rather than essential. It is the most build-intensive of the three areas and is best delivered after availability and homework are in place.

**Independent Test**: Sign in as the teacher, create a presentation with several slides, reorder and edit them, preview it full-screen, then share it with a student and confirm the student can open and page through it from their learning space. Delivers value on its own: Paula has reusable teaching decks.

**Acceptance Scenarios**:

1. **Given** the teacher is in the control panel, **When** she creates a presentation and adds several slides with headings, text, and an image, **Then** the presentation is saved and listed among her presentations.
2. **Given** a presentation with multiple slides, **When** the teacher reorders or removes slides and saves, **Then** the presentation reflects the new slide order and contents.
3. **Given** a saved presentation, **When** the teacher opens the preview, **Then** she can page through the slides full-screen as a student would.
4. **Given** a finished presentation, **When** the teacher shares it with a specific student, **Then** that student can open and navigate the presentation from their learning space and other students cannot.

---

### Edge Cases

- **Non-teacher access attempt**: A signed-in student or a guest who navigates directly to a control-panel URL is denied and redirected away; the backoffice and its data are never exposed to non-teachers.
- **Availability vs. existing bookings**: Removing or narrowing availability that overlaps an already-confirmed booking must not silently cancel that booking; the teacher is warned and the booking is preserved.
- **Overlapping or invalid availability windows**: Entering a window whose end is before its start, or overlapping/duplicate windows on the same day, is rejected or normalized rather than producing inconsistent public slots.
- **Past dates**: The teacher cannot meaningfully set availability, due dates, or exceptions in the past; such input is rejected or ignored for public display.
- **Assigning to no one**: Saving a homework item or presentation without selecting any student keeps it as an unshared draft visible only to the teacher.
- **Deleting content students depend on**: Deleting a homework item a student already submitted, or a shared presentation, removes it from the student view; the teacher is asked to confirm because student-visible content is affected.
- **Empty states**: The control panel shows clear empty states when there is no availability set, no homework created, and no presentation built yet.
- **Long or large content**: Very long instructions, very long slide text, or oversized images are bounded by limits and rejected with a clear message rather than breaking the layout or storage.
- **Concurrent edits / stale views**: If the teacher edits the same item from two places, the later save wins and earlier conflicting state is not silently resurrected.

## Requirements *(mandatory)*

### Functional Requirements

#### Access & navigation

- **FR-001**: The system MUST restrict the entire control panel and all its actions to accounts carrying a persisted administrator/teacher designation (stored on the user account, granted to Paula's account once via migration/seed); students and guests MUST be denied access to every backoffice view and action. Authorization MUST be enforced from the stored designation, not from any client-supplied value.
- **FR-002**: The system MUST hide all control-panel navigation entry points from students and guests, showing them only to the signed-in teacher.
- **FR-003**: The system MUST redirect or deny any non-teacher who attempts to reach a control-panel location directly, without exposing backoffice data.
- **FR-004**: The control panel MUST present the three management areas — availability, homework, and presentations — as clearly separated sections the teacher can move between.

#### Availability management

- **FR-005**: The teacher MUST be able to define a recurring weekly availability pattern as one or more time windows per weekday (including marking a weekday as entirely unavailable).
- **FR-006**: The teacher MUST be able to add date-specific exceptions that override the weekly pattern: blocking time on a date she would normally be available, or opening time on a date she would normally be unavailable.
- **FR-007**: The system MUST generate the public bookable schedule from the teacher's saved availability so that changes are reflected to students without code changes or redeployment.
- **FR-008**: Saved availability changes MUST take effect for students promptly (within the same browsing session, on next schedule load).
- **FR-009**: The system MUST continue to honor existing scheduling safeguards when deriving public slots from teacher availability (minimum booking lead time, no past slots, and slots already taken by confirmed bookings shown as unavailable).
- **FR-010**: The system MUST warn the teacher and preserve any already-confirmed booking when an availability change would otherwise remove the time of that booking.
- **FR-011**: The system MUST reject invalid availability input (end before start, windows in the past for public display) with a clear message.

#### Homework authoring & assignment

- **FR-012**: The teacher MUST be able to create a homework item consisting of a title, instructions, and an optional due date.
- **FR-013**: The teacher MUST be able to assign a homework item to one or more specific students, and to unassign students from it.
- **FR-014**: The system MUST show an assigned homework item only to the students it is assigned to, in their learning space, and MUST NOT show it to unassigned students or guests.
- **FR-015**: The teacher MUST be able to view all homework items she has created, including each item's assigned students and, for each assignee, whether the student has submitted and the submitted response.
- **FR-016**: The teacher MUST be able to edit an existing homework item's title, instructions, and due date, with changes reflected to assigned students.
- **FR-017**: The teacher MUST be able to delete a homework item, after confirmation, removing it from all assigned students' learning spaces.
- **FR-018**: The system MUST preserve and integrate with the existing student homework experience (students continue to view and submit homework in their learning space) rather than introducing a parallel mechanism. The previous shared-to-all-students homework model MUST be retired/migrated so that all homework is targeted per student; a student MUST see only homework explicitly assigned to them, with no remaining shared-to-everyone items.

#### Presentation builder

- **FR-019**: The teacher MUST be able to create a presentation with a title and an ordered list of slides.
- **FR-020**: Each slide MUST support a heading, a block of text, and an optional single image. Images MUST be provided by the teacher uploading an image file, which the system stores and later serves to authorized viewers; the system MUST reject uploads whose type is not in the allowed set or whose size exceeds the configured maximum (external-URL-only images are not supported).
- **FR-021**: The teacher MUST be able to add, edit, reorder, and remove slides within a presentation, and the saved order MUST be preserved.
- **FR-022**: The teacher MUST be able to preview a presentation full-screen, paging through slides as a student would view them.
- **FR-023**: The teacher MUST be able to share a presentation with one or more specific students so it appears in their learning space, and to unshare it.
- **FR-024**: The system MUST show a shared presentation only to the students it is shared with, and MUST NOT expose it to other students or guests.
- **FR-025**: The teacher MUST be able to edit and delete presentations; deleting a shared presentation (after confirmation) removes it from the students' learning space.
- **FR-026**: The presentation builder MUST be limited in scope to ordered slides with heading/text/image only — no animations, transitions, charts, embedded media beyond a static image, or multi-user editing.

#### Cross-cutting

- **FR-027**: All teacher-facing control-panel copy MUST be in Spanish, consistent with the rest of the site, and dates MUST display in the site's Spanish locale.
- **FR-028**: The system MUST validate and bound all teacher-entered content (text lengths, image size/type, number of slides) and reject out-of-bound input with a clear message rather than failing silently or breaking the display.
- **FR-029**: The system MUST show clear empty states in each area when no availability, homework, or presentation has yet been created.
- **FR-030**: All control-panel actions that create, change, or delete student-visible content MUST persist server-side so that the resulting student experience is consistent across reloads, sessions, and devices.

### Key Entities *(include if data involved)*

- **Teacher/Administrator**: The single privileged account (Paula) that owns the control panel. Distinguished from regular student accounts by an administrative designation. Owns all availability, homework, and presentation content.
- **Availability Rule**: The teacher's recurring weekly pattern — a set of time windows per weekday (or "unavailable") — from which the public schedule is generated.
- **Availability Exception**: A date-specific override that blocks normally-available time or opens normally-unavailable time on a particular date.
- **Homework Item**: A teacher-authored assignment with a title, instructions, and optional due date. Related to the students it is assigned to and to their submissions.
- **Homework Assignment**: The link between a homework item and a specific student, carrying that student's submission state (pending/submitted and their response) as already modeled in the student learning experience.
- **Presentation**: A teacher-authored deck with a title and an ordered collection of slides. Related to the students it is shared with.
- **Slide**: An ordered element of a presentation containing a heading, text, and an optional image.
- **Student**: An existing account that receives homework and shared presentations and is the audience for availability. Referenced by, but not created in, the control panel.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Paula can change her published availability for the coming weeks entirely on her own — with no developer involvement and no redeployment — and the public schedule reflects the change on the next load.
- **SC-002**: After Paula saves an availability change, a student loading the public schedule sees the updated bookable times within the same browsing session (no manual cache clearing or waiting beyond a normal page load).
- **SC-003**: Paula can create a homework item and assign it to a student in under 3 minutes, and the assigned student sees it in their learning space while unassigned students do not.
- **SC-004**: 100% of homework items and presentations are visible only to their assigned/shared students — verified by checking that non-targeted students and guests never see them.
- **SC-005**: Paula can build a presentation of at least 5 slides (heading, text, image), reorder them, preview it full-screen, and share it with a student in a single uninterrupted session without external tools.
- **SC-006**: No non-teacher account can reach any control-panel view or perform any control-panel action — confirmed by attempting direct access as a student and as a guest and being denied each time.
- **SC-007**: All teacher-created content (availability, homework, presentations) persists across reloads, new sessions, and different devices with no loss of data.
- **SC-008**: An availability change never cancels or hides an already-confirmed student booking; confirmed bookings survive 100% of availability edits.

## Assumptions

- **Single teacher/admin**: There is exactly one teacher/administrator (Paula). The control panel is built for a single privileged account; a multi-teacher or fine-grained role system is out of scope. The teacher is identified by an administrative designation on her existing account.
- **Builds on existing student features**: This feature extends, and does not replace, the existing scheduling (`/reservas`) and learning (`/aprendizaje`) experiences. Homework authored here flows into the existing student homework view and submission model; the existing virtual-slot scheduling logic is fed by teacher-managed availability instead of hard-coded configuration.
- **Availability model**: The teacher manages a recurring weekly pattern plus date-specific exceptions; this replaces the current hard-coded working-hours/lunch/weekend rules as the source of public availability. The existing minimum-lead-time, no-past-slots, and booked-slot rules are retained as safeguards layered on top of teacher availability.
- **Homework targeting**: Homework is assigned to specific selected students (individual targeting), fully replacing the earlier shared-to-everyone sample model. Existing seeded/sample shared homework is migrated or retired so that, after this change, every homework item is shown only to its explicitly assigned students (no shared-to-all items remain).
- **Presentation delivery**: Presentations are authored in the panel and optionally shared to specific students, appearing read-only in those students' learning space; full-screen "presenting" during a live class is done by the teacher previewing/screen-sharing the same deck. Real-time co-presentation or remote slide-advance control of the student's screen is out of scope.
- **Images**: Slide and content images are teacher-uploaded files stored and served by the system, bounded by allowed type and maximum size; external-URL-only images, video, audio, and animated media are out of scope.
- **Scope of "fewer functionalities"**: The presentation builder deliberately excludes animations, transitions, themes/templates beyond basic styling, charts, and collaborative editing, per the user's "much fewer functionalities" framing.
- **Authentication reuse**: The existing account/authentication system is reused; the control panel adds an authorization (teacher-only) gate on top of it rather than a new login mechanism.
- **Scale**: One teacher, dozens to low hundreds of students, a handful of availability windows, and a modest number of homework items and presentations — no high-scale or concurrency-heavy requirements.
