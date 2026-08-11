# Feature Specification: Admin Student View Overhaul

**Feature Branch**: `036-admin-student-view`

**Created**: 2026-08-11

**Status**: Draft

**Input**: User description: "Overhaul the student view by admin. Remove the Progres part, we don't have it properly defined at the moment. Make the tareas box expand if clicked on to see all the homeworks, if they are pending/submitted + the score + link to open the responses too (similar to what can be seen in the Tareas lower down -> remove this part too afterwards)"

## Clarifications

### Session 2026-08-11

- Q: What should the collapsed Tareas box show before expand? → A: Count plus pending / submitted / completed breakdown on the collapsed box
- Q: Where should the expanded homework list appear? → A: Full-width list below the top summary row (under all stats boxes)
- Q: After removing Progreso, what happens to attended-classes count and placement level summary? → A: Remove both with Progreso (no new home on this profile for now)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher expands Tareas to review all homeworks (Priority: P1)

Paula opens a student's profile in the admin panel. Before expanding, the Tareas summary box already shows the total homework count plus a pending / submitted / completed breakdown so she can see at a glance what needs attention. She clicks the Tareas summary box and a full-width list appears below the top summary row, showing every homework assigned to that student — each with its status (for example pending or submitted), score when available, and a way to open the student's responses — the same useful details she used to find in the separate Tareas list further down the page.

**Why this priority**: This is the core change — consolidating homework review into one expandable control so Paula can act without scrolling past other sections or hunting a duplicate list.

**Independent Test**: Open a student profile that has several homeworks in different statuses; click the Tareas box; confirm the expanded list shows status, score when graded, and working links/actions to open responses, matching the usefulness of the former lower Tareas section.

**Acceptance Scenarios**:

1. **Given** a student with assigned homeworks in mixed statuses, **When** the teacher views the profile without expanding Tareas, **Then** the collapsed Tareas box shows the total count and a pending / submitted / completed breakdown.
2. **Given** a student with assigned homeworks, **When** the teacher clicks the Tareas summary box, **Then** a full-width homework list appears below the top summary row (under all stats boxes) and lists all of that student's homeworks.
3. **Given** the Tareas box is expanded, **When** the teacher views a homework that is still pending or submitted (awaiting review), **Then** the list clearly shows that status.
4. **Given** a graded homework with a score, **When** the teacher views it in the expanded Tareas list, **Then** the score is visible.
5. **Given** a homework with a submission the teacher can open, **When** the teacher uses the open/view action in the expanded list, **Then** they can review the student's responses (same capability as the former lower Tareas section).
6. **Given** the Tareas box is expanded, **When** the teacher clicks it again (or uses the same control to collapse), **Then** the homework list collapses and the summary view returns (still showing count plus pending / submitted / completed breakdown).

---

### User Story 2 - Teacher no longer sees the incomplete Progreso section (Priority: P1)

Paula opens a student profile and no longer sees the Progreso section. Progress metrics that were not meaningfully defined are gone, so the profile focuses on concrete sections she actually uses (classes, expandable homeworks, presentations, etc.).

**Why this priority**: Removing misleading or half-defined progress UI is an explicit product decision and reduces noise on every student profile visit.

**Independent Test**: Open any student profile and confirm there is no Progreso section (no unit progress summary, no homework/activity breakdown cards that lived under Progreso).

**Acceptance Scenarios**:

1. **Given** any student profile, **When** the teacher views the page, **Then** the Progreso section is not shown.
2. **Given** a student who previously would have shown unit completion, homework breakdown, attended-classes count, or placement level under Progreso, **When** the teacher opens the profile, **Then** those Progreso-only summaries are absent (attended count and level are not relocated elsewhere in this overhaul) while other profile sections remain available.

---

### User Story 3 - Duplicate lower Tareas section is removed (Priority: P2)

After the expandable Tareas box carries the full homework list, Paula no longer sees a second, separate Tareas section lower on the student profile. Homework review happens in one place only.

**Why this priority**: Avoids duplicate UI and confusion once the expandable box is the single homework surface; depends on P1 expand behavior being in place.

**Independent Test**: Open a student profile with homeworks and confirm there is exactly one homework list surface (the expandable Tareas box), with no second Tareas section further down the page.

**Acceptance Scenarios**:

1. **Given** a student with one or more homeworks, **When** the teacher views the profile, **Then** there is no separate lower Tareas section listing the same homeworks again.
2. **Given** a student with no homeworks, **When** the teacher expands (or views) the Tareas box, **Then** they see a clear empty state and still do not see a duplicate lower Tareas section.

---

### Edge Cases

- A student with no assigned homeworks: the Tareas box still appears with a zero/empty indication; expanding it shows an empty state rather than an error.
- A homework that is pending (not submitted): status is shown; score is not shown; open-responses action is unavailable until there is something to open (same rules as today's lower list).
- A homework that is submitted and needs review: status and review/open actions remain available from the expanded list.
- A graded homework without a numeric score: status is shown; score is omitted without breaking the row.
- Collapsing and re-expanding the Tareas box preserves the same list content for the current profile load.
- Other profile sections (upcoming/past classes, presentations, interests, placement evaluation, etc.) remain unchanged unless they were part of Progreso or the duplicate lower Tareas section.
- Attended-classes count and placement-level chips from Progreso do not reappear elsewhere on the profile after Progreso is removed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The admin student profile MUST NOT display the Progreso section (including its unit progress, homework/activity breakdown summaries, attended-classes count, and placement-level cards that lived only in that section). Attended-classes count and placement level MUST NOT be relocated to another part of the profile in this overhaul.
- **FR-002**: The admin student profile MUST provide a Tareas summary control that the teacher can click to expand and collapse.
- **FR-002a**: While collapsed, the Tareas summary control MUST show the total homework count plus a pending / submitted / completed status breakdown (same three buckets previously used under Progreso for homework).
- **FR-003**: When expanded, the Tareas control MUST show a full-width list of all homeworks assigned to that student, rendered below the top summary row (under all stats boxes), pushing subsequent profile sections down.
- **FR-004**: Each homework in the expanded list MUST show its status (including pending and submitted states among others the product already surfaces).
- **FR-005**: Each graded homework in the expanded list MUST show its score when a score is available.
- **FR-006**: Where a submission can be opened for review or result viewing, the expanded list MUST offer an action/link to open the student's responses, equivalent in capability to the former lower Tareas section.
- **FR-007**: The admin student profile MUST NOT show a separate lower Tareas section that duplicates the expanded homework list.
- **FR-008**: When a student has no homeworks, the expanded Tareas view MUST show a clear empty state.
- **FR-009**: Sections unrelated to Progreso and the duplicate Tareas list (for example upcoming/past classes, presentations, interests, placement evaluation) MUST continue to appear as they do today.

### Key Entities

- **Student profile (admin view)**: Teacher-facing page summarizing one student; after this feature, homework is accessed via an expandable Tareas control and Progreso is absent.
- **Homework assignment (on profile)**: An assigned homework for the student, with title, status, optional submission date, optional score, and optional actions to open responses or review.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On every student profile, teachers see zero Progreso sections (100% removal of that section from the admin student view).
- **SC-002**: Teachers can reveal the full homework list for a student in one click on the Tareas control, without navigating away from the profile.
- **SC-002a**: Without expanding, teachers can read pending / submitted / completed homework counts from the collapsed Tareas box on every student profile that has homework data (or zeros when empty).
- **SC-003**: For students with homeworks, 100% of assigned homeworks appear in the expanded Tareas list with status visible, and scores/open actions visible when applicable.
- **SC-004**: Teachers encounter exactly one homework list surface on the profile (no duplicate lower Tareas section).
- **SC-005**: In a manual walkthrough, a teacher can open a submitted or graded homework's responses from the expanded Tareas list on the first attempt without using a second homework section.

## Assumptions

- "Tareas box" means the existing homework summary control/stat on the admin student profile (the place that already surfaces a homework count), not a new top-level navigation item.
- Collapsed Tareas shows count plus pending / submitted / completed breakdown (three buckets: pending, submitted awaiting review, completed/graded), so removing Progreso does not remove that at-a-glance homework signal.
- Expanded Tareas renders as a full-width list directly below the top summary/stats row, not cramped inside the Tareas card column and not replacing the stats row.
- Removing Progreso means removing that entire section from the UI for now; redefining progress metrics is out of scope and may return in a later feature when the product definition is ready.
- Attended-classes count and placement-level summary that lived only under Progreso are dropped with that section (not relocated). Attendance no-show marking on past classes remains available in the past-classes area. The separate placement evaluation area on the profile (if present) is unchanged and remains the place to see level results in detail.
- Presentation/activity breakdowns that lived only under Progreso are removed with Progreso; consolidating presentation activities into the Tareas expander is out of scope unless they already appeared in the lower Tareas homework list.
- Expand/collapse is a simple disclosure on the same page; no new filtering, search, or bulk actions are required for this overhaul.
- Empty, pending, submitted, graded, score, and open-response behaviors follow the same business rules as the current lower Tareas list; this feature relocates and consolidates that experience rather than changing grading rules.
