# Feature Specification: My Learning — Classes & Homework Tab

**Feature Branch**: `005-classes-homework-tab`

**Created**: 2026-06-10

**Status**: Draft

**Input**: User description: "I want to create another tab on homepage that will help the teacher present its classes to a student while also giving the student the possibility to either see their past classes or do homeworks assigned by the teacher through the backoffice we will build later. This tab should only be visible to logged-in users and you can put some dummy data for now."

## Clarifications

### Session 2026-06-10

- Q: For this iteration, where does the sample data and homework-state persistence live? → A: Full backend now — new backend endpoints and seeded database tables; homework submissions persisted server-side per user (not front-end-only or local-storage).
- Q: Who do the seeded "dummy" past classes and homework belong to? → A: Shared sample, per-user state — every logged-in student sees the same seeded past classes and homework definitions; each student's submission/done state is stored individually.
- Q: What is the homework status lifecycle? → A: Student drives Pending → Submitted; a Reviewed status is reserved and read-only, set only by the future teacher backoffice; overdue is a derived flag, not a stored status.
- Q: Where does the teacher presentation content come from? → A: Backend-seeded — a dedicated table/endpoint serves the presentation content, consistent with the rest of the feature's backend.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Logged-in student sees the new tab; guests do not (Priority: P1)

A logged-in student sees a new navigation tab (for example, "Mi aprendizaje" / "My Learning") in the site header. When a visitor is not logged in, the tab is hidden, and any attempt to open its page directly redirects them to log in instead of exposing student content.

**Why this priority**: This is the gating requirement of the whole feature — the section must exist and be visible only to authenticated users. Without it, none of the other content can be shown safely. It establishes the protected surface everything else builds on.

**Independent Test**: Log in and confirm the tab appears and its page loads; log out and confirm the tab disappears and direct navigation to the page sends you to the login/account page. Delivers a working, access-controlled section even with no inner content.

**Acceptance Scenarios**:

1. **Given** a visitor who is not logged in, **When** they view the site header, **Then** the new tab is not shown.
2. **Given** a visitor who is not logged in, **When** they navigate directly to the tab's URL, **Then** they are redirected to the login/account page (and may be returned to the tab after logging in).
3. **Given** a logged-in student, **When** they view the site header, **Then** the new tab is shown and clicking it opens the section.
4. **Given** a logged-in student on the tab, **When** they log out, **Then** the tab is hidden and they are taken to a public page.

---

### User Story 2 - Student reviews their past classes (Priority: P2)

A logged-in student opens the tab and sees a list of their past (completed) classes, each showing the topic/title, the date it took place, and a short summary or note from the teacher, so they can recall what was covered and track their progress.

**Why this priority**: Reviewing past classes is a core ongoing value for a returning student and reuses data the platform already conceptually owns (bookings/lessons). It is the most-used "look back" activity and is independently demonstrable with sample data.

**Independent Test**: As a logged-in student, open the tab and confirm a list of past classes renders with title, date, and teacher note for each; confirm an empty-state message appears when the student has no past classes.

**Acceptance Scenarios**:

1. **Given** a logged-in student with past classes, **When** they open the tab, **Then** they see each past class with its title, date, and teacher note/summary.
2. **Given** a logged-in student with past classes, **When** the list is shown, **Then** classes are ordered with the most recent first.
3. **Given** a logged-in student with no past classes, **When** they open the tab, **Then** they see a friendly empty-state message instead of an empty list.

---

### User Story 3 - Student views and completes assigned homework (Priority: P3)

A logged-in student sees homework that the teacher has assigned to them, with a title, instructions, an optional due date, and a status (for example, pending / submitted / reviewed). The student can mark an assignment as done and/or submit a short written response, so they can act on what the teacher asked and the teacher can later review it through the backoffice.

**Why this priority**: Homework adds active engagement beyond passive review, but it depends on the protected section (P1) existing and is the most involved interaction. It is valuable but can ship after past-classes review, and the teacher-side authoring lives in a future backoffice.

**Independent Test**: As a logged-in student, open the tab, see a list of assigned homework with status, mark one as done / submit a response, and confirm its status updates and persists across reloads; confirm an empty-state when no homework is assigned.

**Acceptance Scenarios**:

1. **Given** a logged-in student with assigned homework, **When** they open the tab, **Then** they see each assignment's title, instructions, due date (if any), and current status.
2. **Given** an assignment that is pending, **When** the student marks it done and/or submits a response, **Then** its status updates to reflect the submission and the change persists on reload.
3. **Given** an assignment with a due date in the past that is still pending, **When** it is displayed, **Then** it is visibly flagged as overdue.
4. **Given** a logged-in student with no assigned homework, **When** they open the tab, **Then** they see a friendly empty-state message.

---

### Edge Cases

- **Session expiry while viewing**: If a student's session expires while they are on the tab, the next action that needs authentication returns them to the login page rather than showing a broken or empty view.
- **Teacher presentation area with no content yet**: The "teacher presents their classes" intro/offering area shows a sensible default when no presentation content exists, rather than an empty gap.
- **Long lists**: When a student has many past classes or many assignments, the lists remain readable (e.g., most-recent/most-relevant first) and do not overwhelm the page.
- **Homework already submitted**: A student should not be able to re-submit in a way that loses or contradicts a prior submission; the displayed status always reflects the latest state.
- **Mixed states**: The tab behaves correctly when the student has past classes but no homework, or homework but no past classes, or neither.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The site MUST present a new top-level navigation tab (in both desktop and mobile navigation) that links to a dedicated "My Learning" section.
- **FR-002**: The new tab and its section MUST be visible and accessible ONLY to logged-in users; it MUST be hidden from the navigation for visitors who are not logged in.
- **FR-003**: The system MUST prevent unauthenticated access to the section's page via direct navigation by redirecting such visitors to the login/account page.
- **FR-004**: The section MUST include a teacher-facing presentation area where the teacher's class offering is introduced to the student (e.g., what the classes are about / what to expect). This presentation content MUST be served from the backend (seeded) and is read-only to the student.
- **FR-005**: The section MUST display the logged-in student's past (completed) classes, each showing at minimum a title/topic, the date, and a teacher note or summary. Past classes MUST be served from the backend.
- **FR-006**: Past classes MUST be ordered with the most recent first.
- **FR-007**: The section MUST display homework assigned to the logged-in student, each showing at minimum a title, instructions, an optional due date, and a current status. Homework MUST be served from the backend.
- **FR-008**: A student MUST be able to act on a pending assignment by marking it done and/or submitting a short written response. The resulting submission and status MUST be persisted server-side, scoped to the individual student, and remain correct across page reloads and across sessions/devices.
- **FR-009**: Assignments with a past due date that are not yet submitted MUST be visibly indicated as overdue. Overdue MUST be a derived indicator computed from the due date and current status, not a separately stored status value.
- **FR-010**: Each list (past classes, homework) MUST show a clear, friendly empty-state message when the student has no items of that type.
- **FR-011**: The section MUST be populated with representative sample ("dummy") data seeded in the backend for past classes, homework, and the teacher presentation content for now, structured so it can later be authored/assigned by the teacher via a future backoffice without redesigning the student-facing experience or the data contract.
- **FR-012**: All student-facing text in the section MUST follow the site's existing Spanish-language presentation, consistent with the rest of the site.
- **FR-013**: The section MUST remain usable and readable on both mobile and desktop viewports.
- **FR-014**: Homework status MUST follow the lifecycle Pending → Submitted, where only the student drives the transition to Submitted. A Reviewed status MUST be reserved for the future teacher backoffice and is read-only to students (students cannot set it).
- **FR-015**: Past-class and homework *definitions* MUST be the same seeded sample for every authenticated student, while each student's homework submission/status state MUST be stored and returned per individual student so that one student's submission does not affect another's view.

### Key Entities *(include if feature involves data)*

- **Student**: The authenticated learner viewing the section. Identified by their existing account; the owner of the per-student homework submission state.
- **Past Class**: A completed lesson presented to students as seeded sample data. Key attributes: title/topic, date held, teacher note/summary. The same seeded definitions are shown to every authenticated student this iteration; conceptually related to the existing booking/lesson model and to future teacher-authored content.
- **Homework Assignment**: A task definition shown to students (seeded sample, shared across students this iteration). Key attributes: title, instructions, optional due date. Future backoffice will make these teacher-authored/assigned.
- **Homework Submission (per student)**: An individual student's state for a given assignment. Key attributes: status (pending → submitted; reviewed reserved for the future backoffice, read-only to students), optional short written response, submission timestamp. Stored and returned per student so submissions are isolated between students.
- **Teacher Presentation Content**: Introductory material describing the teacher's class offering shown at the top of the section. Served from the backend (seeded); read-only to the student; future backoffice will make it teacher-authored.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of unauthenticated visitors are unable to see the tab in navigation or reach its content; every direct-access attempt results in being routed to login.
- **SC-002**: A logged-in student can locate and open the new section from the homepage navigation in under 10 seconds on first visit.
- **SC-003**: A logged-in student can identify the topic and date of any of their past classes at a glance, with 90% of test users correctly stating what a given past class covered.
- **SC-004**: A logged-in student can mark a piece of homework as done (or submit a response) in under 30 seconds, and the updated status is still correct after reloading the page.
- **SC-005**: Each list (past classes, homework) shows an unambiguous empty state when there is nothing to display, with zero blank/broken screens reported in testing.
- **SC-006**: The section renders correctly with no layout breakage on common mobile and desktop screen sizes.

## Assumptions

- **Reuses existing authentication**: The site already has a working login/account system and a known "logged-in" state; this feature reuses it to gate visibility and access (and to scope per-student homework submissions) rather than introducing a new auth mechanism.
- **Backend now, with seeded sample data; backoffice later**: This iteration adds backend endpoints and seeded database tables for past classes, homework, and teacher presentation content. Seeded definitions are shared across all authenticated students; each student's homework submission/status is persisted server-side per student. The teacher-facing backoffice that authors/assigns this content is explicitly out of scope for this feature and will be built later.
- **Single teacher, many students**: The platform models one teacher (Paula) and multiple students; "assigned by the teacher" means assigned to the individual logged-in student.
- **Homework submission is lightweight**: For this iteration, a student's response is a short text submission and/or a done flag; rich attachments (file uploads), grading, and teacher feedback threads are out of scope.
- **Spanish-language UI**: All labels and copy follow the site's existing Spanish presentation, consistent with the other sections.
- **Tab placement**: The new tab lives in the same primary navigation used by the rest of the site (desktop header and mobile menu) and is conditionally rendered based on login state.
- **Naming**: The exact tab label (e.g., "Mi aprendizaje") can be finalized during planning; the requirement is that it clearly communicates a personal learning/classes-and-homework space.
