# Feature Specification: Admin Student View — Summary Boxes for Every Section

**Feature Branch**: `053-admin-student-view-boxes`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "I want to make some changes to student view from admin mode: Change the way the information about tareas (pending, completed) is shown, I want the texts to be on separate lines. Make Presentaciones compartidas display using the box representing presentations (similar to tareas and errores). Same thing for clases. Same for Pruebas de evaluación (create a new box for it)."

## Context

The teacher's per-student profile page (admin mode) currently opens with a row of summary boxes: **Clases** (count only, not interactive), **Tareas** (count plus a pending / submitted / completed breakdown laid out side by side; clicking expands the homework list), **Presentaciones** (count only, not interactive) and **Errores frecuentes** (count; clicking expands the mistakes list). Below the boxes, the page always shows four long sections: upcoming classes, past classes, shared presentations and assessment quizzes ("Pruebas de evaluación").

This feature makes the whole page follow the Tareas / Errores pattern: every section is represented by a summary box, and its detailed list only appears when the teacher opens that box. It also makes the Tareas breakdown easier to read.

## Clarifications

### Session 2026-09-27

- Q: Should the new boxes show breakdown lines like Tareas? → A: Clases shows "próximas / pasadas" and Pruebas de evaluación shows "por corregir / corregidas", each on its own line like Tareas; Presentaciones shows the total only.
- Q: Should collapsed boxes show an unseen indicator? → A: Yes — a collapsed Tareas or Pruebas de evaluación box shows an unseen dot whenever its list contains an unseen item; the dot disappears once no item in that list is unseen.
- Q: In what order should the five boxes appear? → A: Clases · Tareas · Presentaciones · Errores frecuentes · Pruebas de evaluación (existing order kept, new box appended at the end).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Readable homework breakdown in the Tareas box (Priority: P1)

When the teacher looks at a student's profile, the Tareas box shows the pending, submitted and completed counts each on its own line (e.g. "2 pendientes" / "1 entregadas" / "5 completadas"), instead of three cramped columns side by side.

**Why this priority**: Smallest, self-contained change that directly fixes a readability complaint on the most-used box.

**Independent Test**: Open any student's profile and check that the Tareas box lists the three statuses stacked vertically, each line showing its number and label, with values matching the student's homework.

**Acceptance Scenarios**:

1. **Given** a student with 2 pending, 1 submitted and 5 completed homeworks, **When** the teacher opens the student's profile, **Then** the Tareas box shows three separate lines — one per status — each with its count and label, and the total of 8.
2. **Given** a student with no homework, **When** the teacher opens the profile, **Then** the Tareas box still shows the three lines, each with 0.
3. **Given** the Tareas box on a narrow (phone-width) screen, **When** it is displayed, **Then** each line remains legible without labels being truncated or overlapping.

---

### User Story 2 - Classes as an expandable box (Priority: P2)

The Clases box becomes interactive like Tareas and Errores. Collapsed, it shows the number of classes plus two breakdown lines — upcoming ("próximas") and past ("pasadas") — each on its own line, like the Tareas breakdown; clicking it expands the class details (upcoming classes and past classes, including the existing "mark / unmark no-show" control on past classes) below the boxes. Clicking again collapses it. The standalone "Próximas clases" and "Clases pasadas" sections below the boxes are removed.

**Why this priority**: Classes are the longest always-visible sections, so collapsing them gives the biggest reduction in page length.

**Independent Test**: Open a profile of a student with upcoming and past classes; confirm the class lists are hidden until the Clases box is clicked, that both lists appear when expanded, and that marking a no-show still works from there.

**Acceptance Scenarios**:

1. **Given** the profile page has just loaded, **When** the teacher looks at it, **Then** the Clases box is collapsed and no class lists are visible on the page.
2. **Given** a student with 2 upcoming and 7 past confirmed classes, **When** the teacher opens the profile, **Then** the Clases box shows a total of 9 with "2 próximas" and "7 pasadas" on separate lines.
3. **Given** the Clases box is collapsed, **When** the teacher clicks it, **Then** the upcoming and past class lists appear (each with its own heading and count), and the box shows it is expanded and can be collapsed.
4. **Given** the Clases box is expanded, **When** the teacher marks a past class as a no-show, **Then** the change is saved and reflected immediately, and the box stays expanded.
5. **Given** a student with no classes, **When** the teacher expands the Clases box, **Then** the existing empty-state messages for upcoming and past classes are shown.

---

### User Story 3 - Shared presentations as an expandable box (Priority: P2)

The Presentaciones box becomes interactive. Clicking it expands the list of presentations shared with the student (title and level, as today). The standalone "Presentaciones compartidas" section is removed.

**Why this priority**: Same value as classes, smaller content.

**Independent Test**: Open a profile of a student with shared presentations; confirm the list is only visible after clicking the Presentaciones box and matches what was previously shown.

**Acceptance Scenarios**:

1. **Given** the profile page has just loaded, **When** the teacher looks at it, **Then** the presentation list is not visible.
2. **Given** the Presentaciones box is collapsed, **When** the teacher clicks it, **Then** the shared presentations list (title and level) appears, and clicking again hides it.
3. **Given** a student with no shared presentations, **When** the box is expanded, **Then** the existing empty-state message is shown.

---

### User Story 4 - New "Pruebas de evaluación" box (Priority: P2)

A new summary box, **Pruebas de evaluación**, joins the others. Collapsed, it shows how many assessment quizzes the student has plus two breakdown lines — awaiting the teacher's correction ("por corregir") and already graded ("corregidas") — each on its own line, like the Tareas breakdown. Clicking it expands the existing quiz list (title, status, score, per-skill results and the "view / review" action that opens the quiz review dialog). The standalone quiz section below the boxes is removed.

**Why this priority**: Completes the consistent box layout; quizzes currently have no box at all.

**Independent Test**: Open a profile of a student with quiz attempts; confirm the new box shows the right count, expands to the full quiz list, and that reviewing a quiz from it still works.

**Acceptance Scenarios**:

1. **Given** a student with 3 quiz attempts (1 submitted and awaiting correction, 2 graded), **When** the teacher opens the profile, **Then** a "Pruebas de evaluación" box shows 3 with "1 por corregir" and "2 corregidas" on separate lines, and the quiz list is hidden.
2. **Given** the box is collapsed, **When** the teacher clicks it, **Then** the quiz list appears with the same information and actions as before.
3. **Given** the quiz list is expanded, **When** the teacher opens and saves a quiz review, **Then** the list refreshes with the updated status/score and stays expanded.
4. **Given** a student with a submitted quiz the teacher has not yet seen, **When** the teacher opens the profile, **Then** the collapsed Pruebas de evaluación box displays an unseen indicator so the new submission is not hidden.
5. **Given** a student with no quizzes, **When** the box is expanded, **Then** the existing "no quizzes" empty-state message is shown.

---

### Edge Cases

- **Several boxes expanded at once**: Each box toggles independently (as Tareas and Errores do today); when several are open, their detail panels appear below the box row in the same order as the boxes.
- **Unseen items hidden by collapsing**: Items that previously drew attention while always visible (unseen quiz submissions, unseen homework submissions) must not become invisible — the Tareas / Pruebas de evaluación box shows an unseen dot. After the teacher opens and reviews the last unseen item (the page reloads its data), the dot on the box disappears.
- **Five boxes on small screens**: The box row must wrap cleanly on phone and tablet widths without horizontal scrolling or uneven, clipped boxes.
- **Data still loading or failed**: A box whose data is still loading shows a placeholder count (as Errores does today); if the quiz list fails to load, the box shows 0 / the empty state rather than breaking the page (current behaviour).
- **Refresh after an action**: After a review, feedback save or no-show toggle, counts in every box update and the expanded/collapsed state of each box is preserved.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Tareas box MUST display the pending, submitted and completed homework counts on three separate lines, each line showing the count and its label.
- **FR-002**: The student profile MUST show five summary boxes in this order: Clases, Tareas, Presentaciones, Errores frecuentes, Pruebas de evaluación.
- **FR-003**: Every summary box MUST be clickable, toggling its detail panel open and closed, and MUST visibly indicate its expanded state and offer an expand/collapse hint, consistent with the existing Tareas and Errores boxes.
- **FR-004**: All boxes MUST start collapsed when the profile page loads.
- **FR-005**: The Clases box MUST show the number of confirmed classes and, on separate lines, the number of upcoming ("próximas") and past ("pasadas") confirmed classes; its expanded panel MUST contain the upcoming classes list and the past classes list (each with heading, count and empty state), with the past-class no-show mark/unmark action working as today.
- **FR-006**: The Presentaciones box MUST show only the number of shared presentations (no breakdown lines); its expanded panel MUST show the shared presentations list (title and level) with the existing empty state.
- **FR-007**: The new Pruebas de evaluación box MUST show the number of the student's quiz attempts and, on separate lines, the number awaiting the teacher's correction ("por corregir" — submitted, not yet graded) and the number already graded ("corregidas"); its expanded panel MUST show the quiz list with the same content and actions as the current quiz section (status, score, per-skill results, view/review).
- **FR-008**: The standalone sections currently rendered below the boxes (upcoming classes, past classes, shared presentations, assessment quizzes) MUST be removed; their content is reachable only through the corresponding box.
- **FR-009**: The Tareas and Pruebas de evaluación boxes MUST show an unseen indicator (the same dot used elsewhere in the panel) whenever at least one item in their list is unseen (unseen homework submissions for Tareas; unseen quiz submissions for Pruebas de evaluación), and MUST stop showing it once no item in that list is unseen. Clases, Presentaciones and Errores frecuentes boxes show no unseen indicator.
- **FR-010**: Boxes MUST toggle independently; when multiple are expanded, detail panels MUST appear in the same order as the boxes.
- **FR-011**: After any action on the page that reloads student data (review, feedback, quiz review, no-show toggle), box counts MUST update and each box's expanded/collapsed state MUST be preserved.
- **FR-012**: All new or changed labels MUST be available in every language the admin interface supports (Spanish, English, Romanian).
- **FR-013**: The box layout MUST remain usable on phone, tablet and desktop widths without horizontal scrolling.

### Key Entities

No new data. The boxes summarise data the profile page already has: the student's bookings (classes), homework assignments, shared presentations, quiz attempts and common mistakes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On initial load, the student profile page shows no long lists — only the header and the five summary boxes — for any student, regardless of how many classes, presentations or quizzes they have.
- **SC-002**: The teacher can reach any section's details (classes, homework, presentations, mistakes, quizzes) in exactly one click from the top of the profile.
- **SC-003**: Every piece of information and every action available on the profile before this change (no-show toggling, homework review, result viewing, quiz review) remains available after it — zero functionality lost.
- **SC-004**: The breakdowns in the Tareas, Clases and Pruebas de evaluación boxes can be read at a glance on a phone-width screen: every line is fully visible with no truncated or overlapping labels.
- **SC-005**: An unseen quiz or homework submission is noticeable on the profile without expanding anything.

## Assumptions

- "Separate lines" for Tareas means the three statuses are stacked vertically inside the box (one status per line), keeping the count visible next to each label; the same three statuses (pending, submitted, completed) are kept.
- The Clases box count stays as today (all confirmed classes, upcoming and past combined); upcoming and past remain two sub-lists inside a single Clases panel rather than two separate boxes.
- The Pruebas de evaluación count is the number of quiz attempts shown in the list today (one entry per attempt). Attempts the student has started but not yet submitted never appear in this list, so every attempt is either "por corregir" or "corregidas" and the two lines always add up to the total.
- The breakdown lines in Clases and Pruebas de evaluación use the same visual style as the stacked Tareas breakdown, so all three boxes read consistently.
- Only the admin student profile page changes; the compact homework breakdown shown elsewhere (e.g. the Students tab list) is out of scope unless it shares the same presentation and the change is harmless there.
- Expanded/collapsed state is not remembered between visits; each visit starts with all boxes collapsed.
- No back-end or data changes are needed; this is a presentation-only change using data the page already loads.
