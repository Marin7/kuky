# Feature Specification: Presentation Activities

**Feature Branch**: `029-presentation-activities`

**Created**: 2026-08-07

**Status**: Draft

**Input**: User description: "Let's introduce a new type called Activities. They are very similar to homeworks, but the teacher can put a single photo (as a PDF at first) as Instructions. Another critical change is the fact that activities are linked to presentations. A presentation can contain multiple activities and an activity can pop-up after a certain page in a presentation. When the student views a unit and expands a presentation, activities of that presentation will appear, allowing the student to fulfill them."

## Clarifications

### Session 2026-08-07

- Q: When a presentation has multiple PDF files, which file’s page numbers does an activity’s page trigger use? → A: Teacher selects both the PDF file and the page number for each trigger.
- Q: For v1, what student work can an activity contain? → A: Full homework parity — free-text and all existing self-correcting exercise types.
- Q: When a student opens an activity from the in-viewer page prompt, what happens to the presentation viewer? → A: Activity opens as an overlay/modal on top of the viewer; the viewer stays underneath.
- Q: When does a newly saved activity become visible to students who can access its presentation? → A: Visible immediately once saved with required fields (title, instructions PDF, linked presentation).
- Q: How should activities be ordered when listed under an expanded presentation? → A: Teacher can reorder the activity list under each presentation.
- Q: Should activities have due dates like homework? → A: No due dates in v1 — fulfillment status only.
- Q: When a student has already fulfilled an activity and later reaches its trigger page again, what should the in-viewer prompt do? → A: Do not show the prompt again once the activity is fulfilled.
- Q: When should a page-triggered activity prompt appear relative to the configured page? → A: When the student reaches/lands on the configured page.
- Q: If the teacher deletes a presentation, what happens to its activities and student submissions? → A: Cascade — deleting the presentation also deletes its activities and their student submissions.
- Q: Should activity fulfillment appear in the existing student progress summary? → A: Yes — show activity fulfillment in the existing student progress overview.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher authors an activity linked to a presentation (Priority: P1)

The teacher creates a new Activity that behaves like homework (title, exercise or free-text work for the student to complete) but with one critical difference for instructions: she attaches a single photo provided as a PDF. She links the activity to a presentation she already manages. She can attach several activities to the same presentation and optionally set each activity to prompt when the student reaches a specific page of a specific PDF file in that presentation.

**Why this priority**: Without authoring and presentation linkage, nothing else in this feature exists. This is the foundation for student discovery and in-viewer prompts.

**Independent Test**: As the teacher, create an activity with a title and a PDF instructions file, link it to a presentation, optionally set a page trigger, save, reopen, and confirm the link, instructions PDF, and page setting persisted. Add a second activity to the same presentation and confirm both remain attached.

**Acceptance Scenarios**:

1. **Given** the teacher is managing learning content, **When** she creates a new activity, **Then** she can set a title, attach exactly one instructions PDF (a photo saved as PDF for this version), and choose the presentation it belongs to.
2. **Given** the teacher is creating or editing an activity, **When** she links it to a presentation, **Then** that presentation may already have other activities and the new one is added without replacing them.
3. **Given** a presentation with multiple activities, **When** the teacher reorders the activity list, **Then** the new order is saved and students see activities in that order when they expand the presentation.
4. **Given** the teacher links an activity to a presentation, **When** she optionally sets a page trigger, **Then** she chooses both a specific PDF file from that presentation and a page number in that file (or leaves the trigger unset if she wants no in-viewer prompt), and that choice is saved with the activity.
5. **Given** an activity without an instructions PDF (or other required fields), **When** the teacher tries to save it as student-visible, **Then** the system requires the missing fields before students can see or fulfill it.
6. **Given** the teacher saves a complete activity (title, instructions PDF, linked presentation), **When** a student who can access that presentation next expands it, **Then** the activity is already listed — no separate publish step.
7. **Given** the teacher reopens an existing activity, **When** the edit form loads, **Then** title, instructions PDF, linked presentation, page trigger (if any), and exercise/free-text content match what was saved.
8. **Given** the teacher no longer wants an activity on a presentation, **When** she removes it, **Then** students who only accessed it through that presentation no longer see it there.

---

### User Story 2 - Student discovers and fulfills activities under a presentation (Priority: P1)

A student who has access to a unit opens that unit, expands a presentation in the learning list, and sees the activities belonging to that presentation. From there they can open an activity, view the instructions PDF, complete the work (free-text or structured exercise, same idea as homework), and submit it.

**Why this priority**: This is the primary student path described for the feature — activities live under presentations in the unit view, not as separate top-level unit items.

**Independent Test**: Assign a unit whose presentation has two activities to a student. As that student, expand the presentation and confirm both activities appear; open one, view the instructions PDF, complete and submit it, and confirm status updates.

**Acceptance Scenarios**:

1. **Given** a student can access a presentation in a unit and that presentation has one or more activities, **When** they expand that presentation in the unit view, **Then** those activities are listed under it in the teacher’s order so they can open and fulfill them.
2. **Given** a presentation with no activities, **When** the student expands it, **Then** no activity list appears (or an empty state that does not imply missing homework elsewhere).
3. **Given** the student opens an activity, **When** the activity screen loads, **Then** they can view the instructions PDF and complete the activity with full homework parity (free-text submission or any existing structured exercise type, with automatic or teacher review as applicable for that type).
4. **Given** the student has submitted an activity, **When** they return to the expanded presentation, **Then** they can see that the activity has been fulfilled (status reflecting submitted / graded / reviewed as appropriate).
5. **Given** a student who cannot access a presentation, **When** they view their unit, **Then** they do not see that presentation’s activities.

---

### User Story 3 - Activity pops up while viewing a presentation (Priority: P2)

While a student views a presentation on the site, an activity configured with a page trigger becomes available as a prompt when they land on that page of the chosen PDF. Opening the activity shows it as an overlay on top of the viewer so they can fulfill it without leaving the presentation context; they can also dismiss the prompt and later open the same activity from under the expanded presentation in the unit.

**Why this priority**: In-viewer prompts reinforce the pedagogical moment (do this exercise when you are on slide N) but depend on authored, linked activities (US1) and the list path (US2) remaining available as the durable place to complete work.

**Independent Test**: Link an activity to a multi-page presentation PDF with a page trigger; as a student with access, open the on-site viewer, navigate to that page, confirm the activity prompt appears, open the activity as an overlay on top of the viewer, close it, and confirm the same activity still appears under the presentation in the unit view.

**Acceptance Scenarios**:

1. **Given** an activity linked to a presentation with a page trigger on a specific PDF file, **When** the student reaches that page while viewing that same PDF in the on-site presentation viewer, **Then** they are shown a clear prompt that an activity is available.
2. **Given** the in-viewer activity prompt is shown, **When** the student chooses to open the activity, **Then** the activity opens as an overlay/modal on top of the presentation viewer (viewer remains underneath), and they can view instructions and fulfill the activity without navigating away from the viewer page.
3. **Given** the in-viewer activity prompt is shown, **When** the student dismisses it or continues viewing without opening the activity, **Then** presentation viewing continues and the activity remains available under the expanded presentation in the unit.
4. **Given** the student has the activity overlay open, **When** they close the overlay (after submitting or without submitting), **Then** they return to the same presentation viewer position underneath.
5. **Given** an activity linked to a presentation with no page trigger, **When** the student views the presentation page by page, **Then** no in-viewer activity prompt appears for that activity (it only appears in the unit expansion list).
6. **Given** the student already fulfilled an activity, **When** they reach its trigger page again, **Then** no in-viewer prompt appears for that activity (completion remains visible under the expanded presentation list only).

---

### User Story 4 - Teacher reviews student activity work (Priority: P3)

For activities that need teacher judgment (free-text), the teacher reviews submissions as she does for comparable homework. For self-correcting exercise activities, results are available without waiting for manual grading, consistent with homework exercises.

**Why this priority**: Closing the loop for the teacher is necessary for a complete product, but students can already fulfill work (US2) and prompts can appear (US3) before review polish is finished.

**Independent Test**: Have a student submit a free-text activity and an exercise activity; as the teacher, confirm she can review the free-text submission and see the exercise result without re-authoring either activity.

**Acceptance Scenarios**:

1. **Given** a student submitted a free-text activity, **When** the teacher opens review for that student/activity, **Then** she can see the response and mark it reviewed (same conceptual outcome as reviewing manual homework).
2. **Given** a student submitted a self-correcting exercise activity, **When** the teacher views that student’s result, **Then** she sees the automatic outcome without needing to grade it by hand.
3. **Given** the teacher is looking at a presentation’s activities, **When** she checks student progress on them, **Then** she can tell which students have fulfilled each activity.
4. **Given** a student has fulfilled one or more activities, **When** the teacher opens that student’s existing progress overview, **Then** activity fulfillment appears there alongside the other progress information she already uses (not only on the presentation’s activity screens).

---

### Edge Cases

- A presentation with many activities: all appear under the expanded presentation in the teacher-defined order; page-triggered ones also prompt at their pages (prompt timing follows file/page, not list order).
- Changing an activity’s linked presentation moves it away from the old presentation’s expansion list and (if a page trigger was set) away from that presentation’s viewer prompts.
- Changing or clearing the page trigger stops future in-viewer prompts without removing the activity from the unit expansion list.
- If the instructions PDF fails to load, the student sees a clear error and can return without a broken unit or viewer state.
- If the chosen trigger PDF has fewer pages than the configured trigger page, or the chosen file is removed from the presentation, the system does not silently pretend the prompt fired; the teacher is prevented from saving an invalid file/page combination, or the student never sees a spurious mid-view prompt.
- Viewing a different PDF file in the same presentation does not fire page triggers that belong to another file.
- Revoking a student’s access to the presentation (or unit material that grants it) also hides its activities.
- Activities are not separate top-level items in the unit’s mixed presentation/homework sequence; they nest under presentations and do not reorder that sequence on their own.
- Deleting a presentation MUST also delete its activities and their student submissions (cascade); students must not retain access to orphaned activities without a presentation context.
- Deleting only a presentation PDF file that is referenced by a page trigger clears or invalidates that trigger so prompts do not fire for a missing file; activities themselves remain unless the whole presentation is deleted.
- Multiple activities triggered on the same page: each is offered without blocking the other; the student can open them one at a time in the overlay (one activity overlay at a time).
- Closing or submitting via the overlay must not break the underlying viewer (page position and PDF file remain).
- Opening an activity from the unit expansion list uses the normal full activity fulfillment screen (not the viewer overlay); the overlay applies when opening from an in-viewer prompt.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST support a distinct content type called Activity that students can fulfill with full homework parity: free-text submission and all existing structured self-correcting exercise types, with the same conceptual grading and review outcomes as homework.
- **FR-002**: Every activity available to students MUST point at instructions already present on a specific page of a PDF belonging to its linked presentation (file + page). Students MUST NOT be required to upload or download a separate instructions PDF for the activity.
- **FR-003**: Every activity MUST be linked to exactly one presentation; a presentation MUST be allowed to have zero or many activities.
- **FR-004**: The teacher MUST be able to create, edit, and remove activities, including setting title, instructions PDF, linked presentation, optional page trigger, and the work the student must complete. A complete saved activity MUST become visible immediately to students who can access its presentation (no separate publish step).
- **FR-005**: The teacher MUST be able to optionally set a page trigger by selecting both a specific PDF file belonging to the linked presentation and a page number in that file; the in-viewer prompt MUST fire when the student lands on (reaches) that page while viewing that file — not when leaving it or on a later page.
- **FR-006**: When a student expands a presentation they can access in a unit, the system MUST list that presentation’s activities in the teacher-defined order and allow the student to open and fulfill each one.
- **FR-015**: The teacher MUST be able to reorder activities belonging to a presentation; that order MUST be what students see under the expanded presentation.
- **FR-007**: When a student reaches a configured trigger page while viewing the trigger’s specific PDF in the on-site presentation viewer, the system MUST show a non-blocking prompt for each incomplete (not yet fulfilled) activity triggered for that file and page; dismissing the prompt MUST NOT remove the activity from the unit expansion list. The system MUST NOT show an in-viewer prompt again for an activity the student has already fulfilled.
- **FR-014**: Opening an activity from an in-viewer prompt MUST present the activity as an overlay/modal on top of the presentation viewer so the student can fulfill it without navigating away; closing the overlay MUST return them to the same viewer position. Opening the same activity from the unit expansion list MUST use the normal full activity screen (not the viewer overlay).
- **FR-008**: Students MUST only see and fulfill activities for presentations they are allowed to access; access follows the same sharing/assignment rules already used for those presentations.
- **FR-009**: Activities MUST NOT appear as separate top-level entries in the unit’s mixed presentation/homework sequence; they appear nested under their presentation.
- **FR-010**: After submission, activity status MUST reflect fulfilled / graded / reviewed outcomes consistently with comparable homework behavior so both student and teacher can see progress.
- **FR-011**: The teacher MUST be able to review free-text activity submissions and view results of self-correcting exercise activities.
- **FR-018**: Activity fulfillment MUST appear in the teacher’s existing student progress overview so she can see completed/incomplete activities without relying solely on per-presentation activity screens.
- **FR-012**: Existing homework and presentation flows MUST continue to work unchanged for content that is not an activity; this feature adds activities alongside them, not as a replacement for homework.
- **FR-017**: Deleting a presentation MUST cascade-delete all of its activities and those activities’ student submissions so no orphaned student-facing activities remain.

### Key Entities

- **Activity**: Student work item similar to homework, always belonging to one presentation; has a title, a single PDF instructions file, optional page trigger, student work definition (free-text or structured exercise), and a position in that presentation’s teacher-ordered activity list; tracks fulfillment/status per student.
- **Presentation**: Existing learning material that may own many activities; when expanded in a unit or viewed on-site, exposes its activities and optional page-triggered prompts.
- **Activity Instructions PDF**: The single photo-as-PDF file that conveys what the student should do; required for student fulfillment.
- **Page Trigger**: Optional association of an activity with one specific PDF file of the linked presentation plus a page number in that file; when the student lands on that page while viewing that file in the on-site viewer, prompts them about the activity.
- **Activity Submission / Progress**: Per-student record of fulfilling an activity (answers, score or review state), analogous to homework submission outcomes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can create an activity with PDF instructions, link it to a presentation, and (optionally) set a page trigger in under 5 minutes without leaving the admin learning-content workflow.
- **SC-002**: In usability checks, at least 90% of students who open a unit with a presentation that has activities successfully expand the presentation and open an activity on the first try.
- **SC-003**: When a page-triggered activity is configured, students who navigate to that page in the on-site viewer see the activity prompt within 2 seconds of the page becoming visible, without the viewer becoming unusable if they dismiss the prompt.
- **SC-004**: Students can complete and submit an activity (view instructions PDF + submit answers) in one continuous session without needing a separate homework assignment for that same task.
- **SC-005**: Teachers can identify, for a given presentation activity, which students have fulfilled it and which have not, without exporting data elsewhere — including from the existing student progress overview.
- **SC-006**: Zero regressions in existing homework fulfillment or presentation viewing for users who are not using activities (spot-check: open a homework and a presentation without activities end-to-end).

## Assumptions

- Activities reuse the same student-work models as homework with full parity in v1 (manual free-text and all existing self-correcting exercise types, with the same scoring/review outcomes). Differences in this feature focus on instructions (PDF photo) and presentation linkage, not inventing a new exercise engine or deferring exercise types.
- Instructions for activities are the single PDF only in this version (no requirement for rich-text instructions alongside the PDF). A short title remains required for lists and prompts.
- Activities inherit student access from their presentation (via existing unit/presentation sharing). There is no separate per-activity assignee list in this version. A complete saved activity is visible immediately (no draft/publish workflow). Activities have no due dates in v1.
- In-viewer prompts are non-blocking: the student may continue reading slides and complete the activity later from the unit expansion list. Opening from a prompt uses an overlay on the viewer; opening from the unit list uses the dedicated full activity screen.
- Page triggers apply to the on-site PDF presentation viewer only (not to downloaded PowerPoint files opened elsewhere). For multi-file presentations, each trigger names both the PDF file and the page within that file.
- Activities nest under presentations in the student unit UI and are not mixed into the unit-level presentation/homework ordering list as peer items. Within a presentation, the teacher explicitly orders the activity list.
- Teacher authoring lives in the existing admin learning area (dedicated activity management surfaced near presentations/homeworks); exact tab placement is a presentation detail for planning. Activity progress is included in the existing student progress overview.
- “Photo as a PDF at first” means v1 accepts a single PDF upload used as the instructions image/document; broader image formats can wait for a later iteration.
- Homework remains the vehicle for stand-alone assigned work not tied to a presentation slide moment; activities do not replace homework. Homework-style due dates and assignee lists stay out of scope for activities in this version.
