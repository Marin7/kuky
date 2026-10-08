# Feature Specification: Unit as a PDF with Page Activities

**Feature Branch**: `055-unit-pdf-activities`

**Created**: 2026-10-07

**Status**: Draft

**Input**: User description: "I want to completely change the Unit entity. Currently, from the Admin panel, a Unit contains a presentation (PDF) and homeworks. I want to be able to create a unit from scratch. I want the teacher to be able to add a PDF file which is basically the presentation and then from the same view mark the pages which represent activities. Also, completely remove the activities tab from the admin panel. An activity will become basically a page from a Unit's PDF. Keep the existing behavior of an activity, meaning that some are self-correcting, while others require teacher input. Don't worry about existing data in the database, the students are not assigned any units so far."

## Clarifications

### Session 2026-10-07

- Q: Should a unit still contain homeworks? → A: Yes — units keep homeworks from the Homework library, granted on unit assignment, ordered alongside the unit's PDF.
- Q: What happens to the Presentations tab? → A: Keep it for standalone presentations outside units (still shareable directly); they have no activities.
- Planning note: the earlier "prompt / overlay" wording now matches what ships today. The activity appears as an inline, non-blocking block right after its page in the viewer (see research D7).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher builds a unit from a single PDF (Priority: P1)

The teacher creates a new unit (level + subject, as today) and, in that same unit view, uploads one PDF that is the unit's presentation. She no longer has to create a presentation somewhere else and then attach it to the unit: the unit *is* the place where its PDF lives. She can page through the uploaded PDF right there.

**Why this priority**: Everything else (marking activities, students working through the unit) depends on a unit owning its PDF. On its own it already lets the teacher deliver a slide deck to assigned students.

**Independent Test**: As the teacher, create a unit, upload a PDF, reload the panel, reopen the unit and confirm the PDF is there and can be paged through; assign the unit to a student and confirm the student can view the PDF.

**Acceptance Scenarios**:

1. **Given** the teacher is on the Units tab, **When** she creates a new unit, **Then** she lands in that unit's view where she can upload its PDF.
2. **Given** a unit with no PDF, **When** the teacher uploads a valid PDF, **Then** the PDF is saved with the unit and its pages are shown in the unit view.
3. **Given** a unit that already has a PDF, **When** the teacher uploads a replacement PDF, **Then** the new PDF replaces the old one (see Edge Cases for what happens to marked activity pages).
4. **Given** the teacher uploads a file that is not a PDF or exceeds the size limit, **When** the upload is attempted, **Then** it is rejected with a clear message and the unit's existing PDF (if any) is unchanged.
5. **Given** a unit with a PDF, **When** the teacher adds homeworks from the Homework library in the same unit view and reorders them relative to the PDF, **Then** the order is saved and students assigned to the unit receive those homeworks.
6. **Given** a unit is deleted, **When** the deletion completes, **Then** its PDF, its activities and all student work on those activities are deleted too.

---

### User Story 2 - Teacher marks PDF pages as activities (Priority: P1)

While paging through the unit's PDF in the unit view, the teacher marks a page as an activity. The page itself is the activity's instructions (what the student sees on the slide is what they must do). For each marked page she defines the student work exactly as she does for activities today: free-text answers that she grades, or any of the existing self-correcting question types, or a mix. She can unmark a page, which removes the activity.

**Why this priority**: This is the core change requested — activities stop being separately authored items and become pages of the unit's PDF.

**Independent Test**: In a unit with a 10-page PDF, mark page 4 as a self-correcting activity and page 7 as a free-text activity, save, reopen the unit and confirm both pages show as activities with their questions intact; unmark page 7 and confirm it is no longer an activity.

**Acceptance Scenarios**:

1. **Given** the unit view showing the PDF, **When** the teacher marks the page she is looking at as an activity, **Then** that page is visibly flagged as an activity in the page navigation/thumbnails.
2. **Given** a page marked as an activity, **When** the teacher edits its student work, **Then** she can use every question kind currently available to activities (free text and all self-correcting kinds) with the same authoring controls.
3. **Given** a page marked as an activity, **When** the teacher gives it a title, **Then** that title is used wherever the activity is listed; if she leaves it blank, a default such as "Actividad – página N" is used.
4. **Given** a page already marked as an activity, **When** the teacher tries to mark it again, **Then** the system does not create a second activity for the same page (at most one activity per page).
5. **Given** a page marked as an activity with no student submissions, **When** the teacher unmarks it, **Then** the activity is removed.
6. **Given** a page marked as an activity that students have already submitted, **When** the teacher unmarks it, **Then** she is warned that the submissions will be deleted and must confirm before the activity is removed.
7. **Given** the Admin panel, **When** the teacher looks at the tabs, **Then** there is no Activities tab anymore and no other place to create an activity outside a unit.

---

### User Story 3 - Student works through a unit and completes its activities (Priority: P1)

A student assigned to the unit opens it and views the PDF. When they reach a page marked as an activity, the activity's questions appear right below that page in the viewer (the page itself is the instructions, as with today's in-viewer activity block), they answer and submit. Self-correcting activities show their result immediately; activities needing teacher input show as submitted until the teacher grades them. The unit also lists its activities with their status so the student can return to any of them.

**Why this priority**: Without the student side the teacher's authoring has no effect. It keeps the existing activity experience but driven by the unit's pages.

**Independent Test**: Assign the unit from US2 to a student; as the student, open the unit, page to page 4, complete and submit the self-correcting activity and see the score; page to page 7, submit a free-text answer and see it as awaiting correction; confirm both appear with their status in the unit's activity list.

**Acceptance Scenarios**:

1. **Given** a student assigned to a unit, **When** they open it, **Then** they see the unit's PDF and its homeworks in the teacher's order, can view the PDF, and see the list of its activities in page order with each one's status (pending / submitted / graded).
2. **Given** the student reaches a page marked as an activity they have not completed, **When** that page is shown, **Then** a non-blocking activity block appears right after that page, as today, where they can open and answer it.
3. **Given** the student opens the activity, **When** it is displayed, **Then** the page content is visible as the instructions alongside the questions to answer.
4. **Given** a fully self-correcting activity, **When** the student submits, **Then** it is graded immediately and the result is shown, as today.
5. **Given** an activity with any free-text question, **When** the student submits, **Then** it waits for the teacher's grade, as today.
6. **Given** the student already completed an activity, **When** they reach its page again, **Then** its block shows the completed status and result instead of an answer form, as today.
7. **Given** a student not assigned to the unit, **When** they browse their learning area, **Then** they cannot see the unit, its PDF or its activities.

---

### User Story 4 - Teacher grades and tracks activity work (Priority: P2)

The teacher reviews activity submissions needing her input and sees self-correcting results, as she does today, now reached through the unit / student views instead of the removed Activities tab. Existing notification dots and student-profile progress keep working for activities.

**Why this priority**: Closing the grading loop matters, but the grading behavior itself is unchanged; only where it is reached from changes.

**Independent Test**: With the submissions from US3, as the teacher open the unit (or the student's profile), find the free-text activity awaiting correction, grade it, and confirm the student then sees the grade.

**Acceptance Scenarios**:

1. **Given** a submitted activity needing teacher input, **When** the teacher opens it from the unit or the student's profile, **Then** she can grade it with the same controls as today and the student sees the result once finalized.
2. **Given** activities exist in a unit, **When** the teacher views that unit, **Then** she can see per activity which assigned students have submitted, are awaiting correction, or are graded.
3. **Given** a student submits an activity needing correction, **When** the teacher next looks at the panel, **Then** she is alerted with the same in-site unseen indicators used today for submitted work.

---

### Edge Cases

- **Replacing the PDF with marked pages**: activities whose page number still exists in the new PDF are kept on that page number; activities on page numbers beyond the new PDF's page count are removed together with their submissions — the teacher is told how many activities will be removed and must confirm before the replacement happens.
- **Unit without a PDF**: it can exist (e.g. just created) but cannot have activities; students assigned to it see an empty state rather than an error.
- **PDF fails to load for the student**: a clear error is shown; activity list remains usable.
- **Two prompts on consecutive pages**: each page shows only its own activity block; opening one does not affect the other.
- **Editing an activity's questions after students submitted**: follows today's rules for submitted work (submitted answers are judged against what the student saw at submission).
- **Unassigning a student from a unit**: they lose access to the unit and its activities; their past submissions are kept, matching today's homework/activity rules for revoked access.
- **Revoking STUDENT status**: does not delete activity history (existing rule).
- **Very large PDF**: upload is limited by the same maximum size already applied to presentation PDFs.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The teacher MUST be able to create a unit from scratch (level and subject, as today) and manage its whole content from a single unit view in the Admin panel.
- **FR-002**: A unit MUST have at most one PDF, uploaded, viewed and replaced from that unit view. The unit's PDF is its presentation; units no longer link to separately created presentations.
- **FR-003**: In the unit view the teacher MUST be able to page through the unit's PDF and mark or unmark any page as an activity; at most one activity may exist per page.
- **FR-004**: Each activity MUST belong to exactly one unit and one page of that unit's PDF; the page content is the activity's instructions. A separate instructions file or text MUST NOT be required.
- **FR-005**: For each activity the teacher MUST be able to define the student work with the same question kinds and authoring behavior activities support today (free text and all self-correcting kinds, single or mixed), plus an optional title defaulting to the page number.
- **FR-006**: Activity grading MUST keep today's behavior: fully self-correcting activities are graded on submit; any activity containing teacher-graded questions waits for the teacher's grade; answer keys stay hidden until submission.
- **FR-007**: The Activities tab and any other way to create or edit an activity outside a unit MUST be removed from the Admin panel.
- **FR-008**: Students MUST be able to access a unit's PDF and activities only when the unit is assigned to them (and they hold student access), using the existing unit assignment.
- **FR-009**: When a student reaches an activity page they have not completed while viewing the unit's PDF, the system MUST show today's non-blocking activity block directly after that page, so the page above serves as the instructions; completed activities MUST show their status and result there instead of an answer form.
- **FR-010**: *(Dropped 2026-10-08 at the teacher's request: no separate activity list or activity page — activities are answered only in the viewer, right after their page.)*
- **FR-011**: The teacher MUST be able to review and grade activity submissions and see self-correcting results, reachable from the unit view and from the student's profile, with today's unseen indicators for newly submitted work.
- **FR-012**: Replacing a unit's PDF MUST keep activities whose page still exists and MUST remove (after explicit teacher confirmation that states how many) activities whose page no longer exists, together with their submissions.
- **FR-013**: Deleting a unit MUST delete its PDF, its activities and all submissions on those activities; its homeworks MUST stay in the Homework library. Unmarking an activity page that has submissions MUST require explicit confirmation and then delete those submissions.
- **FR-014**: Existing unit data (units, their presentation/homework links, existing activities and activity submissions) MAY be discarded; standalone presentations and homeworks themselves are kept when this feature is introduced; no migration of existing unit content is required.
- **FR-015**: A unit MUST still be able to contain homeworks chosen from the Homework library, managed from the same unit view. Assigning a unit MUST keep granting its homeworks to the student (with today's new-homework email behavior), and the student's unit view MUST show the unit's PDF together with its homeworks in a teacher-defined order.
- **FR-016**: The Presentations tab MUST remain for standalone presentations that do not belong to any unit, including sharing them directly with students. Standalone presentations MUST NOT have activities; activities exist only on unit PDF pages. Presentations can no longer be attached to units.

### Key Entities

- **Unit**: A teaching unit with level, subject and position (as today), at most one PDF, its page activities, its homeworks (ordered together with the PDF), and a set of assigned students.
- **Standalone Presentation**: Existing presentation managed in the Presentations tab, shareable directly with students; not linked to any unit and has no activities.
- **Unit PDF**: The single presentation document of a unit; has a page count used to validate activity pages.
- **Activity**: Student work tied to one page of one unit's PDF; has an optional title, its questions (free-text and/or self-correcting kinds), and per-student submissions. Ordered by page number.
- **Activity Submission**: A student's answers to an activity, with today's statuses (submitted / graded), auto score and/or teacher grade and feedback.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can go from "new unit" to a unit with an uploaded PDF and two marked activities in under 5 minutes, without leaving the unit view.
- **SC-002**: The Admin panel has zero entry points for creating an activity outside a unit.
- **SC-003**: A student assigned to a unit can open it, reach an activity page and submit the activity in one continuous session, with self-correcting results visible immediately.
- **SC-004**: 100% of activity question kinds available before this change can still be authored and are graded the same way after it.
- **SC-005**: No regressions in homework, quizzes, or student progress views for content unrelated to units (spot-check end-to-end).

## Assumptions

- One PDF per unit (the previous multi-file presentations are not carried over into units). PDF upload limits match today's presentation PDF limits.
- Units keep their current fields (level, subject, position/ordering within level) and their student assignment mechanism; assigning a unit grants its PDF and activities.
- Activities have no due dates and no per-activity assignees (unchanged from today); access always follows the unit.
- Existing data in units, presentations linked to units, activities and activity submissions can be dropped — no students hold units today.
- The student-facing activity experience (in-viewer block after the page, status, grading results) stays as it is today; only its source (unit page instead of separately authored activity) changes.
- Homework authoring (Homework tab), quizzes and testimonials are out of scope and unchanged. Existing standalone-presentation sharing is unchanged except that presentations can no longer be added to units and lose their activities.
