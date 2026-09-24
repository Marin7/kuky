# Feature Specification: Email Preferences

**Feature Branch**: `049-email-preferences`

**Created**: 2026-09-23

**Status**: Draft

**Input**: User description: "Email preferences. Allow students to customize when to receive an email. By default, everything is disabled. For now, the only option is to receive an email when a new homework is assigned to them."

## Clarifications

### Session 2026-09-23

- Q: When one teacher action assigns a student several homeworks at once (assigning a unit), is that one email or several? → A: One email per teacher action, listing all of the homework that is newly available to that student. Never one email per homework.
- Q: Which of the three paths that grant homework should send an email — unit assignment, a homework added to a unit the student already holds, or direct per-homework assignment? → A: All three. Any homework that becomes newly available to the student counts, regardless of how it got there.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Student turns on homework emails and gets notified (Priority: P1)

A student wants to know about new homework without checking the site. In their own account area they find a short list of email options. Exactly one option is offered today — "tell me by email when new homework is assigned to me" — and it is off. They switch it on. Later that day the teacher assigns them homework, and an email arrives telling them new homework is waiting and pointing them to it.

**Why this priority**: This is the whole point of the feature. Without it nothing else has value, and it is the only slice that delivers a working end-to-end outcome.

**Independent Test**: As a student, switch the option on. As the teacher, assign that student homework. Confirm the student receives an email that names the homework and links them to it.

**Acceptance Scenarios**:

1. **Given** a signed-in student whose homework-email option is off, **When** they open their email preferences, **Then** they see the option listed, clearly labelled, and shown as off.
2. **Given** the student switches the option on, **When** they leave the page and come back (or sign out and in again), **Then** the option is still on.
3. **Given** a student with the option on, **When** the teacher assigns them new homework, **Then** that student receives an email identifying the new homework and offering a way to reach it on the site.
4. **Given** a student with the option on, **When** the teacher assigns the same homework to several students who all have the option on, **Then** each of them receives their own email, and no student learns anything about the others.
5. **Given** a student with the option on and another student with it off, **When** the teacher assigns both the same homework, **Then** only the first receives an email; the second receives nothing.
6. **Given** a student with the option on, **When** the assignment email is sent, **Then** the in-site indicators and the rest of the site behave exactly as they did before this feature — the email is an addition, never a replacement.
7. **Given** a student with the option on, **When** the teacher assigns them a unit containing several homeworks, **Then** they receive exactly one email listing every one of those homeworks — not one email per homework.
8. **Given** a student with the option on who is already assigned a unit, **When** the teacher adds a new homework to that unit, **Then** the student receives an email about that homework, even though the site shows them no in-site indicator for it.
9. **Given** a student with the option on, **When** the teacher assigns them a single homework directly rather than through a unit, **Then** they receive an email about it.
10. **Given** two students with the option on are assigned the same unit, and one of them already held two of its homeworks, **When** the assignment is made, **Then** each receives one email listing only the homework that is new to them, and their two emails differ accordingly.
11. **Given** a student with the option on is re-added to a unit they were already assigned, or the teacher re-saves an assignment without changing anything for them, **Then** no email is sent.

---

### User Story 2 - Nobody is emailed unless they asked to be (Priority: P1)

Every account, including students who already exist today, starts with every email option switched off. A student who has never opened the preferences page never receives a homework email, no matter how much homework is assigned to them.

**Why this priority**: The user stated this as a hard requirement. Getting it wrong means mailing the whole student list without consent, which is far worse than the feature not shipping.

**Independent Test**: Without touching preferences for any account, have the teacher assign homework to several existing students and to a freshly registered one. Confirm no homework emails are sent to any of them.

**Acceptance Scenarios**:

1. **Given** a student account that existed before this feature, **When** they first open their email preferences, **Then** every option is shown as off.
2. **Given** a newly registered account, **When** the account becomes a student and is assigned homework, **Then** no homework email is sent.
3. **Given** a student who has never changed any preference, **When** the teacher assigns them homework repeatedly, **Then** they receive no homework emails at all.
4. **Given** any account, **When** email preferences are introduced, **Then** the emails the site already sends (account activation, password reset, booking confirmations, cancellations, reminders, student-access changes) continue to be sent exactly as before and are not affected by these options.

---

### User Story 3 - Student turns homework emails back off (Priority: P2)

A student who switched the option on decides it is too much. They return to their email preferences, switch it off, and stop receiving the emails immediately.

**Why this priority**: Opting in is only trustworthy if opting out works. It is separable from US1 — the feature is demonstrable without it — but it should not ship far behind.

**Independent Test**: With the option on and at least one email already received, switch it off, have the teacher assign more homework, and confirm no further email arrives.

**Acceptance Scenarios**:

1. **Given** a student with the option on, **When** they switch it off and the teacher then assigns new homework, **Then** no email is sent.
2. **Given** a student switches the option off, **When** they return to the page later, **Then** it is still off.
3. **Given** a student who received a homework email, **When** they read it, **Then** it tells them how to change or stop these emails and links them to their preferences.
4. **Given** a student toggles the option on and off several times in a row, **When** homework is then assigned, **Then** the outcome follows only the setting that was in effect at the moment the homework was assigned.

---

### Edge Cases

- **Assignment that grants several homeworks at once.** Assigning a unit hands the student every homework in that unit in one action; that is one email listing all of it, not one email per homework (FR-006).
- **One action, several students, different amounts of new work.** Assigning a unit to three students where one already held two of its homeworks means the emails differ per recipient — each student's email lists only what is new *to them*, and a student for whom nothing is new gets no email at all.
- **A unit being authored gradually.** Because a homework added to an already-assigned unit now sends an email (FR-007b), adding homework to a live unit over several sittings emails its assigned students each time. Assigning students only once the unit's homework is in place avoids this; the system does not try to detect or suppress it.
- **Homework the student already had.** Re-saving an assignment, or re-adding a student who was already assigned, must not produce a repeat email — only work that is genuinely new to that student counts.
- **Homework moved between units.** A homework moved into a unit is newly available to that unit's students and counts; the students of the unit it left simply lose it and are not emailed about the removal.
- **Assignment that is immediately undone.** If the teacher assigns and then removes the homework, an email may already be on its way; the site does not attempt to recall it, and the student simply finds nothing new when they follow the link.
- **Account is not (or is no longer) a student.** A non-student account cannot be assigned homework, so no email is possible. If student access is revoked, the stored preference is kept untouched and simply stops mattering; restoring student access restores the previous setting rather than resetting it.
- **Email cannot be delivered.** A failure to send must never block or roll back the assignment itself — the teacher's action succeeds and the student still sees the work on the site.
- **Preference changed while an assignment is being processed.** The setting read at the moment of assignment decides the outcome; there is no retroactive send for work assigned while the option was off, and no send for work assigned before it was switched on.
- **Student with no usable email address**, or an account still awaiting activation: no homework email is attempted.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The site MUST give each student a place, inside their own signed-in account area, where they can see and change which events cause an email to be sent to them.
- **FR-002**: Every email option MUST be off by default, for accounts that already exist and for accounts created in the future. No migration, backfill, or bulk action may switch an option on.
- **FR-003**: The preferences page MUST offer exactly one option in this release: receive an email when new homework is assigned to me. It MUST be presented so that further options can be added later without the page being redesigned.
- **FR-004**: A student MUST be able to switch an option on and off at will, as often as they like, with the change taking effect for any assignment made after it is saved.
- **FR-005**: A saved preference MUST persist across sessions, devices, and sign-outs, and MUST be visible to and changeable by only that student (and no other student).
- **FR-006**: When a single teacher action makes more than one homework newly available to the same student, the system MUST send that student exactly one email covering all of it, listing each newly assigned homework. It MUST NOT send one email per homework.
- **FR-007**: The system MUST treat a homework as "newly assigned to me" whenever it becomes available to that student for the first time, by any route: (a) the student is assigned a unit, which grants them all of that unit's homework; (b) a homework is added to a unit the student is already assigned to, which grants it to them without any in-site indicator; or (c) a homework is assigned to the student directly. The email MUST NOT depend on which route was used.
- **FR-008**: The system MUST send the homework email only to students whose option is on at the moment the homework is assigned, and MUST send nothing to students whose option is off.
- **FR-009**: The homework email MUST be written in the same language and voice as the site's existing student emails, MUST identify what was newly assigned, and MUST give the student a way to reach that work on the site.
- **FR-010**: The homework email MUST tell the recipient that they are receiving it because they asked to, and MUST link them to their email preferences so they can change or stop it.
- **FR-011**: Each recipient MUST receive their own separate email; no email may disclose the identity of, or the work assigned to, any other student.
- **FR-012**: A failure to send the email MUST NOT cause the assignment to fail, be rolled back, or be hidden from the student on the site, and MUST NOT surface as an error to the teacher.
- **FR-013**: The system MUST NOT send the same student a homework email twice for the same homework, including when the teacher re-saves an assignment that did not actually change anything for that student.
- **FR-014**: Introducing email preferences MUST NOT change whether, when, or to whom any email the site already sends is delivered, and MUST NOT change any existing in-site notification indicator.
- **FR-015**: The teacher MUST NOT be able to see or change a student's email preferences, and assigning homework MUST require no extra step or decision from the teacher because of this feature.

### Key Entities

- **Email preference set**: The collection of email options belonging to one account. Holds one on/off value per supported event, each defaulting to off. Exists conceptually for every account; an account that has never visited the page behaves identically to one with every option explicitly off.
- **Email event type**: A named occasion on which the site may email a student. One type exists today — *new homework assigned* — and the model must accommodate more being added without disturbing the ones already in use.
- **Homework assignment event**: The moment a specific homework becomes available to a specific student for the first time. It is the trigger the email hangs off, and it is per student, not per teacher action.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Before any student has opted in, assigning homework to every student on the site produces zero homework emails.
- **SC-002**: A student can find their email preferences and switch the homework option on in under 60 seconds, without help and without leaving their account area.
- **SC-003**: For a student who has opted in, a homework email arrives within 5 minutes of the teacher assigning the work, in at least 95% of assignments.
- **SC-004**: For a student who has opted out (or never opted in), 100% of homework assignments produce no email.
- **SC-005**: No student ever receives more than one homework email for the same homework, measured across repeated and re-saved assignments.
- **SC-006**: Assigning an opted-in student a unit containing N homeworks produces exactly 1 email, for any N — never N emails.
- **SC-007**: For an opted-in student, every route that makes homework newly available to them produces an email: verified across unit assignment, homework added to a unit they already hold, and direct per-homework assignment.
- **SC-008**: Every homework email lets the recipient reach both the newly assigned work and their own preferences in one click.
- **SC-009**: Every email the site sent before this feature is still sent, unchanged, after it — verified across account activation, password reset, booking confirmation, booking cancellation, booking reminder, and student-access change.

## Assumptions

- Only students are offered these preferences; accounts without student access cannot be assigned homework, so the page is either hidden from them or shown with nothing that applies.
- Preferences are stored per account and survive student access being revoked and later restored, rather than being reset.
- The preference set is keyed on the account, so a student's setting follows their email address as recorded on the account; changing the account's email address changes where these emails go, with no separate confirmation step in this release.
- Emails are sent through the site's existing mail delivery, in Spanish, matching the tone of existing student emails; no new delivery provider, template system, or branding work is in scope.
- Sending is best-effort, matching how the site's existing notification emails behave: a delivery failure is logged and swallowed rather than retried or surfaced.
- Grouping into one email is per teacher action, not per time window: two separate assignment actions a minute apart produce two emails. No digest or quiet-period batching is implied by FR-006.
- Emailing on homework added to an already-assigned unit (FR-007b) is accepted with its known consequence — authoring a live unit over several sittings emails its students each time. The teacher avoids this by assigning students after the unit's homework is in place; the system does not detect or suppress it.
- FR-007b deliberately diverges from the in-site indicators, which do not mark that case unseen (decided in `specs/041-notification-system/`). Email and in-site dots are therefore not expected to agree in that one case.
- Quizzes / pruebas de evaluación, grading and feedback, booking events, and teacher-facing emails are deliberately not offered as options yet; the user asked for homework assignment only.
- The teacher's own email behaviour is unchanged — this feature is about what students receive.
- The in-site unseen dots on **Mi aprendizaje** are a separate mechanism and keep working exactly as they do today; the email neither replaces them nor clears them.

## Out of Scope

- Any email option other than "new homework assigned" (including quiz assignment, grading, deadlines, and class reminders).
- Teacher-side preferences or a teacher view of student preferences.
- Digest, frequency, or quiet-hours controls — the option is a plain on/off.
- One-click unsubscribe headers, a public unsubscribe page reachable without signing in, or suppression-list management; opting out is done by signing in and switching the option off.
- Push notifications, SMS, or any channel other than email.
- Retroactive emails for homework assigned before the student opted in.
