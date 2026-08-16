# Feature Specification: Student-Based Homework Deadlines

**Feature Branch**: `044-student-homework-deadlines`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "Change the concept of homework deadline from overall to student-based. Existing deadlines don't exist, so don't worry about backwards compatibility"

## Clarifications

### Session 2026-08-16

- Q: When Paula assigns a unit (which grants that unit’s homeworks to a student), should those new homework assignments get a due date? → A: No. Unit assignment never sets due dates. She sets each homework’s date later if she wants one.
- Q: Where can Paula set, change, or clear a student’s due date? → A: Homework assignee list only. Student profile shows the date (and overdue) but is not an editor.
- Q: When Paula assigns students to a homework, how does a due date get onto those new assignments? → A: Optional date on that assign action, applied only to students newly added. Already-assigned students keep their existing dates.

## User Scenarios & Testing *(mandatory)*

Today a homework has one optional due date that applies to every student assigned to it. That model does not match how Paula actually assigns work: students receive the same homework at different times, work at different paces, and sometimes need an extension that must not move everyone else’s date. This feature replaces the homework-wide due date with an optional due date on each student–homework assignment. There is no overall deadline to keep or migrate.

### User Story 1 - Teacher sets a due date for a specific student (Priority: P1)

Paula assigns a homework to a student (directly, or because she assigned the unit that contains it). That student can have an optional due date that belongs to **them** for **that** homework — not to the homework as a whole. She can leave the date empty; the homework is still assigned and the student can still complete it. When she assigns several students at once, she may optionally give those newly assigned students the same due date, then treat each student’s date independently afterwards.

The homework itself no longer has a due-date field. Creating or editing the homework’s title, instructions, questions, and other content does not include a shared deadline.

**Why this priority**: Until a due date lives on the student–homework assignment, nothing else (student view, overdue, extensions) can be correct. This is the smallest change that replaces the old concept.

**Independent Test**: Assign a homework to one student with a due date and to another with no due date. Confirm the first student’s assignment shows that date and the second’s shows none. Confirm the homework editor no longer offers a single due date for the whole homework.

**Acceptance Scenarios**:

1. **Given** Paula is assigning a homework to a student, **When** she sets a due date for that student and saves, **Then** that student–homework assignment has that due date and the homework definition itself has no overall due date.
2. **Given** Paula is assigning a homework to a student, **When** she leaves the due date empty, **Then** the student is assigned the homework with no deadline.
3. **Given** Paula assigns the same homework to several students in one action and optionally enters a due date, **When** she saves, **Then** each newly assigned student receives that due date (or none, if she left it empty), and the homework still has no overall due date.
4. **Given** a homework already assigned to Student A with a due date, **When** she later assigns Student B (with or without a date for B), **Then** Student A’s due date is unchanged.
5. **Given** Paula is creating or editing a homework’s content, **When** she looks at the homework details, **Then** there is no field for a single due date that would apply to every assigned student.

---

### User Story 2 - Student sees only their own deadline (Priority: P1)

A student looking at an assigned homework in their learning space sees **their** due date for that homework, if they have one. They never see another student’s date. If they have no date, the homework appears without a deadline. If their date is in the past and they have not yet submitted, the homework is visibly overdue — the same overdue treatment that already exists, but computed from **their** date only.

Submitting after the due date remains allowed; overdue is a signal, not a lock.

**Why this priority**: The student-facing deadline is the reason the date exists. Without this story the teacher can store dates that students cannot act on.

**Independent Test**: Give Student A a past due date and Student B a future due date on the same homework. Sign in as each: A sees their date and overdue; B sees their date and is not overdue. A third assigned student with no date sees neither a due date nor overdue.

**Acceptance Scenarios**:

1. **Given** a homework assigned to a student with a due date, **When** that student opens it in their learning space, **Then** they see that due date.
2. **Given** two students assigned the same homework with different due dates, **When** each views the homework, **Then** each sees only their own date.
3. **Given** a student assigned a homework with no due date, **When** they view it, **Then** no due date is shown and the homework is not overdue.
4. **Given** a student whose due date is in the past and who has not submitted, **When** they view the homework, **Then** it is visibly marked overdue.
5. **Given** a student whose due date is in the past, **When** they submit (or continue an allowed resubmit) after that date, **Then** the submission is accepted; overdue did not block them.
6. **Given** a student who has already submitted, **When** their due date is in the past, **Then** the homework is not shown as overdue (overdue applies only while the work is still pending).

---

### User Story 3 - Teacher changes one student’s deadline without affecting others (Priority: P2)

Paula can change or clear a student’s due date from that homework’s assignee list — for example to give an extension — without changing any other student’s date for that homework, and without changing the homework content or anyone’s submitted answers, scores, or review state.

**Why this priority**: Extensions and corrections are the main reason a shared deadline fails. This story is usable as soon as P1 storage exists.

**Independent Test**: Two students share a homework with different dates. Change only Student A’s date (including to empty). Confirm A’s view updates, B’s date is unchanged, and existing submissions are untouched.

**Acceptance Scenarios**:

1. **Given** Students A and B assigned the same homework with different due dates, **When** Paula changes only A’s date, **Then** A sees the new date and B’s date is unchanged.
2. **Given** a student with a due date, **When** Paula clears that student’s due date, **Then** that student no longer has a deadline or overdue flag, and other assignees are unaffected.
3. **Given** a student who has already submitted or been graded, **When** Paula changes or clears that student’s due date, **Then** the submission, score, and review state stay as they were.
4. **Given** Paula changes only due dates (no homework content), **When** a student who has not submitted opens or continues the homework, **Then** they are not told the homework content was updated and they do not lose in-progress answers (due-date changes stay operational, as they are today).

---

### User Story 4 - Teacher sees each student’s deadline in admin views (Priority: P2)

When Paula looks at who is assigned to a homework, each assignee shows that student’s due date (or none) and whether that student is overdue; she sets, changes, or clears dates on those rows. When she looks at a student’s profile homework list, each homework shows **that student’s** due date (or none) and overdue, not a shared homework date — view only; she does not edit dates from the profile. The homework list cards no longer show a single due date for the whole item.

**Why this priority**: Without teacher-visible per-student dates, she cannot confirm what each student was given. Extensions happen on the assignee list; the profile is for seeing that student’s dates at a glance. The list remains usable after P1 even if this polish ships second.

**Independent Test**: Open a homework assigned to two students with different dates (one overdue, one not). Confirm each assignee row shows the right date and overdue state and can be edited. Open each student’s profile and confirm that student’s date is shown and cannot be changed there. Confirm the homework list no longer shows one due date for the item.

**Acceptance Scenarios**:

1. **Given** a homework with several assignees, **When** Paula views the assignee list, **Then** each row shows that student’s due date if they have one, and does not invent a shared date.
2. **Given** an assignee whose due date is past and whose work is still pending, **When** Paula views that assignee row, **Then** that student is marked overdue; assignees who submitted or have no date or a future date are not.
3. **Given** a student with several assigned homeworks, **When** Paula views that student’s profile homework list, **Then** each item shows that student’s due date (or none) and overdue for them, and she cannot change those dates from the profile.
4. **Given** Paula is browsing the admin Homework tab list, **When** she looks at a homework card, **Then** she does not see a single overall due date for that homework.
5. **Given** Paula changes a student’s due date on the homework’s assignee list, **When** she then views that student’s profile or the student views the homework, **Then** the same date appears in those places.

---

### Edge Cases

- **No assignees**: A homework with nobody assigned has no due dates (there is no student to attach one to).
- **Unassign**: Removing a student from a homework removes that student’s due date with the assignment. Re-assigning the same student later starts with no due date unless Paula sets one again.
- **Unit assignment**: Assigning a unit that grants its homeworks to a student creates those assignments **without** a due date and does not offer a due-date field. Paula sets per-student dates afterwards if she wants them.
- **Homework with mixed dates**: Some assignees may have a date, others none; dates may differ. That is the intended model, not an error.
- **Assign-time date vs existing assignees**: An optional due date supplied when adding students applies only to those newly added. Saving that action never changes dates already stored for current assignees.
- **Past calendar dates**: Paula may set a due date in the past (for example to record that work was already due). The student is then overdue if they have not submitted.
- **Clear vs empty**: Clearing a due date is equivalent to never having set one: no date shown, never overdue from a date.
- **Status and overdue**: Overdue is derived, not stored. It applies only when the student has a due date, that date is before today in the teacher’s working calendar, and the student’s work is still pending (not submitted, reviewed, or graded).
- **Presentation activities and quizzes**: Unchanged. They have no homework due dates today and do not gain them here.
- **No historical overall dates**: There are no existing homework-wide deadlines to copy onto students. The overall due-date concept is removed rather than migrated.
- **Student profile is view-only for dates**: The profile homework list shows that student’s due date and overdue; changing a date requires opening the homework’s assignee list.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A homework MUST NOT have a single due date that applies to all assigned students.
- **FR-002**: Each student–homework assignment MUST be able to carry its own optional due date.
- **FR-003**: The teacher MUST be able to set, change, and clear a student’s due date for a homework independently of every other student’s due date for that homework.
- **FR-004**: When the teacher assigns one or more students to a homework in one action (including the first assignees when creating the homework), she MUST be able to optionally supply a due date that applies only to the students **newly** assigned in that action; students who were already assigned MUST keep the due dates they already had. That optional date MUST NOT overwrite existing assignees and MUST NOT become a due date on the homework itself.
- **FR-005**: A newly created student–homework assignment with no date supplied MUST start with no due date.
- **FR-005a**: Assigning a unit MUST grant that unit’s homeworks without setting a due date on any of those new assignments. The unit-assign action MUST NOT offer a due date. The teacher sets per-student homework dates afterwards if she wants them.
- **FR-006**: Unassigning a student MUST remove that student’s due date for that homework together with the assignment.
- **FR-007**: A student MUST see only their own due date for a homework (or no date). They MUST NOT see another student’s due date.
- **FR-008**: Assignments whose due date is before today (teacher’s working calendar) and whose student work is still pending MUST be visibly overdue for that student. Overdue MUST be derived from that student’s date and status, not stored as a separate status, and MUST NOT apply when there is no due date or when the student has already submitted, been reviewed, or been graded.
- **FR-009**: A past due date MUST NOT prevent the student from submitting (or performing any resubmit the homework already allows).
- **FR-010**: Changing or clearing a student’s due date MUST NOT change homework content, other students’ dates, or any student’s submission, score, or review state.
- **FR-011**: Changing only due dates MUST NOT be treated as a content revision of the homework (students in progress MUST NOT be forced to discard answers or told the homework was updated solely because a due date changed).
- **FR-012**: The teacher MUST see each assignee’s due date (or none) and that assignee’s overdue state on the homework’s assignee list.
- **FR-013**: The teacher MUST see that student’s due date (or none) and overdue state for each homework on the student profile homework list.
- **FR-014**: The teacher MUST be able to set, change, or clear a student’s due date from the homework’s assignee list. The student profile homework list MUST show that student’s due date and overdue state and MUST NOT let the teacher edit due dates there.
- **FR-015**: The admin Homework tab list MUST NOT display an overall due date on the homework item.
- **FR-016**: The homework create/edit details MUST NOT include an overall due-date field.
- **FR-017**: Only the teacher (admin) MUST be able to set, change, or clear due dates. Students MUST NOT edit their own deadline.
- **FR-018**: A due date, when present, MUST be a calendar date (day precision), consistent with how due dates are shown to students today.

### Key Entities

- **Homework**: Teacher-authored work (title, instructions, questions, and so on). It is assigned to students. It does **not** own a due date.
- **Student–homework assignment**: The link between one homework and one student. Carries that student’s optional due date for that homework, along with the existing assignment/submission relationship. Removing the assignment removes the due date.
- **Due date**: An optional calendar day by which a **specific student** is expected to complete a **specific homework**. Absence means no deadline for that student.
- **Overdue**: A derived flag for a student–homework assignment: due date present, date before today in the teacher’s working calendar, and the student’s work still pending.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The teacher can give two students different due dates on the same homework, and 100% of checks show each student only their own date.
- **SC-002**: Extending one student’s due date takes under 30 seconds from the homework’s assignee list and leaves every other student’s date unchanged.
- **SC-003**: After the overall due-date field is gone, 0% of homework create/edit screens and homework list cards still present a single shared deadline.
- **SC-004**: In a mixed set (past date + pending, future date, no date, already submitted), overdue is shown only for the pending student whose date is past — 100% agreement with that rule in review.
- **SC-005**: A student with no due date can still open and complete the homework; missing a deadline is never required to assign or submit work.
- **SC-006**: Changing only due dates never causes a student to lose in-progress answers or to be told the homework content was updated.

## Assumptions

- Existing homework-wide due dates are unused in practice; they can be removed with no copy onto students and no compatibility period.
- Due dates stay optional per student, matching today’s optional homework-wide date.
- Day precision and the teacher’s working calendar for “today” / overdue stay as they are for homework due dates today.
- Overdue remains a visual indicator; it does not lock submission (current product behavior).
- Assigning a unit grants that unit’s homeworks without setting due dates and without a due-date field on unit assign; Paula sets dates afterwards if she wants them.
- Newly assigned students may share one date at the moment they are added (including the first assignees when the homework is created); that is a convenience, not a new overall homework deadline. Already-assigned students are never overwritten by that convenience date. There is no separate action that writes one date onto every existing assignee at once; changing an existing student’s date is per row on the assignee list.
- Teacher edits a student’s date only on that homework’s assignee list. The student profile shows the date and overdue for visibility; it is not a second editor.
- Presentation activities, quizzes, bookings, and notifications are out of scope. This feature does not add deadline reminders.
- Students cannot set or request their own deadline in this feature; only the teacher changes dates.
- Re-assigning a student after unassign does not restore a previous due date.
- Homework content freeze / “homework was updated” behavior is unchanged except that due-date-only edits continue not to count as content changes, now at per-student granularity.
