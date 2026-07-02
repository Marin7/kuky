# Feature Specification: Student Progress on Teacher Profile View

**Feature Branch**: `015-student-progress`

**Created**: 2026-07-02

**Status**: Draft

**Input**: User description: "Create a student progress which can be viewed on their profile by the teacher"

## Clarifications

### Session 2026-07-02

- Q: How should the homework status breakdown in the progress summary be grouped? → A: 3 buckets — Pending, Submitted (awaiting review), Completed (REVIEWED or GRADED merged).
- Q: What should "classes attended" mean, given the system has no separate no-show/attendance tracking today? → A: Requires new tracking — attendance is not simply the raw count of past confirmed bookings; the teacher must be able to correct it (mark a class as a no-show).
- Q: How does a past class get marked attended vs. no-show? → A: Every past confirmed booking counts as attended by default; the teacher can flag a specific class as a no-show to exclude it (and revert the flag if needed).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher sees an at-a-glance progress summary (Priority: P1)

Paula opens a student's profile in the admin panel and, without cross-referencing separate lists of bookings, homework, and units, immediately sees a summary of how that student is progressing: how much of their assigned curriculum is complete, how their homework is trending, and how many classes they've attended.

**Why this priority**: This is the core value of the feature — a single, scannable view that answers "how is this student doing?" without manual reconciliation. Without it, the feature doesn't exist.

**Independent Test**: Can be fully tested by opening the profile of a student with assigned units, homework, and past bookings, and confirming a progress summary section renders with accurate counts/percentages derived from that student's existing data.

**Acceptance Scenarios**:

1. **Given** a student with 2 assigned units, 5 assigned homeworks (3 graded, 2 pending), and 4 past confirmed classes, **When** the teacher opens the student's profile, **Then** the teacher sees a progress summary showing curriculum completion, a homework status breakdown, and a count of classes attended.
2. **Given** a student with no assigned units, homeworks, or bookings yet, **When** the teacher opens the student's profile, **Then** the teacher sees a clear "no activity yet" state instead of an empty or broken section.

---

### User Story 2 - Teacher spots homework needing attention (Priority: P2)

Paula wants to quickly see, per student, how many homeworks are still pending or awaiting her review versus already reviewed/graded, so she can prioritize which students to follow up with.

**Why this priority**: Homework review is Paula's most frequent recurring task; surfacing the breakdown on the profile saves her from opening each homework assignment individually.

**Independent Test**: Can be fully tested by assigning a mix of homeworks in different statuses (PENDING, SUBMITTED, REVIEWED, GRADED) to a student and confirming the profile shows correct counts per status.

**Acceptance Scenarios**:

1. **Given** a student has homeworks in PENDING, SUBMITTED, and GRADED status, **When** the teacher views the student's profile, **Then** the progress summary shows a count for each of the pending, submitted, and completed groups.
2. **Given** a homework submission moves from SUBMITTED to REVIEWED, **When** the teacher reloads the student's profile, **Then** the updated status count is reflected.

---

### User Story 3 - Teacher reviews curriculum and attendance progress (Priority: P3)

Paula wants to see how far a student has advanced through their assigned units (curriculum) and how many classes they've actually attended, to gauge overall engagement and pacing. Since not every booked class results in the student showing up, Paula needs to be able to correct the record when a student is a no-show, so the attendance count stays accurate.

**Why this priority**: Useful context for planning future lessons and units, but secondary to the homework/at-a-glance summary that drives day-to-day follow-up.

**Independent Test**: Can be fully tested by assigning units to a student, marking associated homeworks complete for one unit, and confirming the profile reflects that unit as complete while others remain in progress; separately, confirming past confirmed bookings are counted as attended classes by default and that marking one as a no-show removes it from the count.

**Acceptance Scenarios**:

1. **Given** a student is assigned to 2 units and has completed all homework in 1 of them, **When** the teacher views the profile, **Then** that unit is shown as complete and the other as in progress.
2. **Given** a student has 6 past confirmed bookings and none have been marked as a no-show, **When** the teacher views the profile, **Then** the progress summary shows 6 classes attended.
3. **Given** a past confirmed booking, **When** the teacher marks it as a no-show, **Then** it is excluded from the student's attended-classes count.
4. **Given** a booking previously marked as a no-show, **When** the teacher reverts that marking, **Then** the class is counted as attended again.

---

### Edge Cases

- A student with no units, homeworks, or bookings assigned shows an explicit empty/no-activity state, not a blank or erroring section.
- A unit assigned to a student has no homeworks attached — that unit is treated as having no completion criteria yet, not as complete or blocking overall progress.
- A student's role is revoked (loses STUDENT access) — their progress summary continues to reflect their full historical activity, consistent with existing behavior where revoking access never alters past bookings/homework history.
- A student is assigned to the same unit twice or a unit is unassigned after being partially completed — progress reflects only current assignments at time of viewing.
- A homework is unpublished or removed after being assigned — it no longer counts toward the student's progress totals.
- A booking is marked as a no-show after already being counted as attended — the attendance count updates to exclude it.
- A booking that is cancelled (not confirmed) or still in the future cannot be marked as a no-show — only past confirmed classes are eligible.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The teacher-facing student profile MUST display a student progress summary covering curriculum (unit) completion, homework status breakdown, and class attendance.
- **FR-002**: For each unit assigned to the student, the system MUST show whether the unit is complete (all of its assigned homeworks reviewed/graded) or still in progress.
- **FR-003**: The system MUST show a count of the student's assigned homeworks broken down into three groups: pending, submitted (awaiting teacher review), and completed (reviewed or graded, merged into one group).
- **FR-004**: The system MUST show a count of the student's attended classes, where every past confirmed booking counts as attended by default unless the teacher has marked it as a no-show.
- **FR-005**: The system MUST show the student's most recent recorded CEFR/placement level, if one exists, as part of the progress context.
- **FR-006**: The progress summary MUST reflect the student's current data at the time the teacher views the profile (no manual refresh or separate report generation required).
- **FR-007**: The progress summary MUST be visible only to the teacher/admin viewing the student's profile; it is not exposed as a self-service view for the student in this feature.
- **FR-008**: When a student has no assigned units, homeworks, or completed classes, the system MUST show an explicit empty state rather than omitting the section or displaying an error.
- **FR-009**: Progress figures MUST remain accurate after a student's STUDENT role is revoked, continuing to reflect their historical activity.
- **FR-010**: The teacher MUST be able to mark a specific past confirmed booking as a no-show, which excludes it from the student's attended-classes count.
- **FR-011**: The teacher MUST be able to revert a no-show marking back to attended.
- **FR-012**: The system MUST NOT allow a future or non-confirmed (e.g., cancelled) booking to be marked as a no-show.

### Key Entities

- **Student Progress Summary**: A derived, mostly read-only view aggregating a student's existing curriculum, homework, and attendance data for display on their teacher-facing profile. Composed of: curriculum completion per assigned unit, homework counts by status group, attended-classes count, and most recent CEFR level.
- **Unit Completion Status**: Per assigned unit, whether all of that unit's homeworks have reached a reviewed/graded state for the student.
- **Homework Status Breakdown**: Counts of the student's assigned homeworks grouped into pending, submitted, and completed (reviewed or graded).
- **Class Attendance Marking**: A new per-booking attribute recording whether a past confirmed class is attended (default) or a teacher-marked no-show; drives the attended-classes count and is the one piece of progress data the teacher actively sets rather than only views.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The teacher can assess a student's overall progress (curriculum, homework, attendance) within 10 seconds of opening the student's profile, without navigating to any other page.
- **SC-002**: The progress summary accounts for 100% of the student's currently assigned units, homeworks, and past confirmed classes — no relevant activity is left out of the totals.
- **SC-003**: The teacher can identify how many of a student's homeworks still need review without manually opening or counting individual homework assignments.
- **SC-004**: Students with no curriculum, homework, or class activity yet are presented with a clear "no activity" indication, with zero reports of a broken or blank profile section.
- **SC-005**: The teacher can correct a class's attendance status (mark or unmark a no-show) in a single action without leaving the student's profile, and the attended-classes count updates immediately.

## Assumptions

- Progress is shown as a current-state summary (counts, statuses, completion) rather than a historical trend chart or time-series graph; that is a possible future enhancement, not part of this feature.
- The progress summary is added to the existing teacher-facing student profile view in the admin panel rather than introducing a new standalone page.
- A student-facing "my own progress" view is out of scope for this feature; only the teacher sees this summary.
- "Complete" for a unit means all homeworks currently assigned within that unit have reached a reviewed/graded (terminal) status; units with no homeworks attached are not counted as complete.
- The most recent placement/CEFR result already recorded for the student is reused for context; this feature does not change how that level is calculated.
- No new roles or permissions are introduced — visibility follows the existing admin/teacher access already used for the student profile page.
- Only the teacher can mark/unmark a no-show; there is no student-facing dispute or notification flow for attendance corrections in this feature.
- No-show marking is a simple two-state flag per booking (attended/no-show); reasons/notes for a no-show are out of scope.
