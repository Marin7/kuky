# Feature Specification: Unit Viewer Contents & Level Navigation

**Feature Branch**: `056-unit-viewer-navigation`

**Created**: 2026-10-10

**Status**: Draft

**Input**: User description: "Enhance the student view of units. I want to add a content view on the left side displaying each page's title (activities too). The student can navigate from that page to other units in the same category (A1, A2, etc)"

## Context

Today a student opens a unit from **Mi aprendizaje** and lands straight on the unit's PDF: every page is shown one under the other, each with its title above it, and each page that is an activity is followed by its answer block. Long units mean a lot of scrolling with no overview of what the unit contains, no way to jump to a specific page or activity, and no way to move to the next unit without going back to **Mi aprendizaje**. Units already belong to one CEFR level (A1, A2, B1, B2, C1, C2) and have a teacher-defined order within that level; that level is the "category" this feature refers to.

## Clarifications

### Session 2026-10-10

- Q: Which units appear in the level navigation? → A: Only the student's assigned units of that level (no locked/unassigned entries).
- Q: Where does the level navigation sit relative to the contents panel? → A: A single left panel: the level's units are listed, and the current unit is expanded in place into its pages and activities (table-of-contents style); other units appear collapsed as single entries.
- Q: How do activity pages appear in the contents? → A: As two entries — the page entry (page title), with the activity nested just beneath it (indented, activity marker, activity title and status).
- Q: Should the unit's end-of-unit quizzes appear in the contents? → A: Yes — listed in the teacher's order as the last entries of the current unit (quiz marker, status, unseen dot); selecting one opens it (see the follow-up below).
- Q (follow-up after implementation): Should a unit quiz open on its own page? → A: No — it opens inside the unit page, showing only the quiz (no slides), and the unit collapses in the left panel while it is open.
- Q (follow-up after implementation): Should the unit show all pages stacked? → A: No — one page at a time (its activity grouped right below it), with previous/next arrows; the shown page is kept in the page address. Selecting a page in the contents shows that page; selecting an activity shows its page and brings the activity into view.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See and jump through a unit's contents (Priority: P1)

A student opens a unit and sees, on the left side of the page, a contents panel listing every page of the unit by its title, in page order. Pages that are activities are listed too, so the student can tell at a glance where the exercises are. Selecting any entry brings that page (or that activity's answer block) into view without leaving the unit.

**Why this priority**: This is the core request. It turns a long, undifferentiated scroll into a navigable document and lets students go straight to the activity they need to finish.

**Independent Test**: Assign a unit with a multi-page PDF (some pages marked as activities, some pages renamed) to a student; open it as that student and confirm the panel lists every page title in order with activities identifiable, and that choosing an entry scrolls the viewer to the matching page or activity.

**Acceptance Scenarios**:

1. **Given** a unit whose PDF has 12 pages, three of them activities, **When** the student opens the unit, **Then** a contents panel on the left lists all 12 page titles in page order, and each of the three activities is listed as an indented entry, with an activity marker, directly beneath its page's entry (15 entries in total).
2. **Given** the contents panel is shown, **When** the student selects the entry for page 8, **Then** the viewer brings page 8 into view, with its title at the top of the visible area.
3. **Given** page 5 is an activity titled "Completa el diálogo", **When** the student selects that activity entry, **Then** the viewer brings that activity's answer block into view.
4. **Given** the teacher has renamed a page, **When** the student opens (or reloads) the unit, **Then** the contents panel shows the new title, matching the title shown above that page in the viewer.
5. **Given** a student has submitted one activity and not the other, **When** they look at the contents panel, **Then** each activity entry shows its current status (e.g., pending vs. submitted/graded) using the same status vocabulary as the activity block, without showing a score.
6. **Given** the unit has two quizzes linked to its end, one of them new to the student, **When** the student opens the unit, **Then** both quizzes are listed in the teacher's order after the last page entry, each with its status, the new one with an unseen dot; **When** the student selects a quiz, **Then** that quiz's page opens.

---

### User Story 2 - Move to another unit of the same level (Priority: P2)

From inside a unit, the student can see the other units of the same level (e.g., all their A2 units) in the teacher's order, see which one they are on, and open another one directly — without going back to **Mi aprendizaje**. The units are listed in the same left panel as the contents: the current unit is expanded into its pages and activities, and the other units of the level are collapsed single entries above and below it.

**Why this priority**: Requested explicitly, and it removes a round trip for the most common next step (continuing to the next unit), but the unit is still fully usable without it.

**Independent Test**: Assign a student three A2 units and one B1 unit; open one of the A2 units and confirm the level navigation lists exactly the three A2 units in teacher order with the current one marked, and that choosing another opens that unit's viewer.

**Acceptance Scenarios**:

1. **Given** a student is assigned A2 units "Unidad 1", "Unidad 2", "Unidad 3" and B1 unit "Unidad 1", **When** they open A2 "Unidad 2", **Then** the level navigation shows the A2 label and lists exactly the three A2 units in the teacher's order, with "Unidad 2" marked as current, and does not list the B1 unit.
2. **Given** the level navigation is shown, **When** the student selects "Unidad 3", **Then** the "Unidad 3" viewer opens with its own contents panel, starting at the top of its first page.
3. **Given** another unit in the same level has new content the student hasn't seen (an unseen dot on **Mi aprendizaje**), **When** the student views the level navigation, **Then** that unit shows the same unseen indicator, and it clears once the student opens that unit (consistent with today's behaviour).
4. **Given** a student is assigned only one unit of a level, **When** they open it, **Then** the level navigation shows just that unit (or is omitted) and offers no broken or empty links.

---

### User Story 3 - Know where I am while scrolling (Priority: P3)

As the student scrolls through the unit, the contents panel highlights the page (or activity) currently in view, so they always know where they are in the unit.

**Why this priority**: Improves orientation in long units but is a refinement on top of Story 1.

**Independent Test**: Open a long unit, scroll manually through it, and confirm the highlighted contents entry follows the page in view.

**Acceptance Scenarios**:

1. **Given** the student scrolls from page 2 down to page 6, **When** page 6 is the page occupying the reading area, **Then** the page 6 entry is highlighted and no other page entry is.
2. **Given** the contents list is longer than the panel, **When** the highlighted entry moves out of the panel's visible area, **Then** the panel keeps the highlighted entry visible.

---

### User Story 4 - Use the contents on a phone (Priority: P2)

On a narrow screen, where a permanent left column would squeeze the PDF, the student can still open the contents (and the level navigation) on demand, pick an entry, and get back to reading.

**Why this priority**: Many students study on phones; the feature must not degrade the reading experience there.

**Independent Test**: Open a unit at phone width and confirm the PDF uses the full width, the contents can be opened from a clearly visible control, and selecting an entry closes the contents and scrolls to the target.

**Acceptance Scenarios**:

1. **Given** a phone-width screen, **When** the student opens a unit, **Then** the PDF takes the full width and a visible control opens the contents.
2. **Given** the contents are opened on a phone, **When** the student selects an entry, **Then** the contents close and the viewer brings the selected page or activity into view.
3. **Given** a desktop-width screen, **When** the student opens a unit, **Then** the contents panel is shown on the left alongside the PDF and stays reachable while scrolling through the unit.

---

### Edge Cases

- **Unit with no PDF yet**: the existing "unit empty" message is shown; the contents panel shows no page entries (no broken list), and level navigation still works so the student can move to another unit.
- **Pages still rendering**: entries are listed from the unit's page titles straight away; selecting an entry for a page that hasn't finished rendering still lands on that page once it appears, and does not jump to the wrong place.
- **Very long titles**: long page or activity titles wrap or are truncated in the panel without breaking the layout; the full title remains available (e.g., in the viewer itself).
- **Many pages (e.g., 60+)**: the panel scrolls on its own, independently of the PDF.
- **Teacher replaces the PDF or changes activities while the student is viewing**: the contents reflect the new state after the student reloads the unit; no live push is required.
- **Unit unassigned from the student**: it disappears from the level navigation on the next load; opening it directly keeps today's "not found" behaviour.
- **Unit quizzes**: quizzes linked to the end of the unit are listed as the last entries of the current unit in the contents (selecting one opens it inside the unit page in place of the PDF). A unit with quizzes but no PDF yet still lists its quizzes. A quiz the student has already started or submitted stays listed with its status.
- **"Otros" bucket** (material not attached to any unit): unchanged — it has no level and gets neither the contents panel nor level navigation.
- **Teacher (ADMIN) viewing the student unit page**: sees the same contents panel and level navigation, scoped to what the student view would show for their own account.

## Requirements *(mandatory)*

### Functional Requirements

**Contents panel**

- **FR-001**: The student unit page MUST show a contents panel listing every page of the unit's PDF, in page order, each entry labelled with that page's current title.
- **FR-002**: Every activity MUST appear in the contents as its own entry nested directly beneath its page's entry (indented, with an activity marker, showing the activity title). The page entry itself stays listed with the page title, so an activity page contributes two entries: page, then its activity.
- **FR-003**: Each activity entry MUST show the activity's current status for the student (same status vocabulary as the activity block), and MUST NOT show a score.
- **FR-004**: Selecting a page entry MUST bring that page into view in the viewer; selecting an activity entry MUST bring that activity's answer block into view. Neither action may reload the page or lose in-progress answers.
- **FR-004a**: The quizzes linked to the end of the current unit MUST be listed, in the teacher's order, as the last entries of that unit's contents (after the last page), each with a quiz marker, its title, its current status for the student and the same unseen indicator shown on **Mi aprendizaje**. Selecting a quiz entry MUST show that quiz inside the unit page in place of the PDF (only the quiz — no slides), and MUST collapse the current unit in the panel to its quiz entries, with the open quiz highlighted; selecting the unit name (or "Volver a la unidad") returns to the PDF. The open quiz is part of the page address, so reload and browser back keep it. Opening it clears its unseen indicator exactly as today.
- **FR-005**: Titles shown in the contents MUST always match the titles shown above the pages in the viewer and the activity blocks.
- **FR-006**: The contents panel MUST highlight the entry for the page currently in view and keep that entry visible within the panel as the student scrolls.
- **FR-007**: On desktop widths, the contents panel MUST be on the left of the PDF and remain reachable while the student scrolls through the unit (it does not scroll away with the first pages).
- **FR-008**: On phone widths, the left panel (contents and level navigation together) MUST be hidden by default, openable from a visible control, and close automatically after an entry is selected; the PDF MUST use the full available width.

**Level navigation**

- **FR-009**: The left panel MUST show the unit's level (A1–C2) as its heading and list, in the teacher's unit order, every unit of that level the student has access to. The current unit MUST be marked and expanded in place into its contents (FR-001–FR-003); the other units MUST appear collapsed as single entries (their pages are not listed).
- **FR-010**: Selecting another unit in the level navigation MUST open that unit's viewer (with its own contents panel), starting at its first page.
- **FR-011**: The level navigation MUST list only units of the current unit's level that are assigned to the student; it MUST NOT list units of other levels, nor show unassigned units in any form (not even as locked entries).
- **FR-012**: Units in the level navigation MUST show the same unseen indicator the student sees for them on **Mi aprendizaje**; opening a unit through the level navigation MUST clear its indicator exactly as opening it from **Mi aprendizaje** does today.
- **FR-013**: The existing "back to Mi aprendizaje" link MUST remain available.

**Unchanged behaviour**

- **FR-014**: Activity answering, submission, status and result display within the viewer MUST behave exactly as today.
- **FR-015**: Unit PDFs MUST remain view-only (no file name, no download), and access rules MUST remain unchanged: a student only sees units assigned to them.

### Key Entities

- **Unit**: a teacher-authored lesson belonging to exactly one CEFR level (A1–C2), with a subject (its display name) and a position that orders it within its level; owns at most one PDF.
- **Unit page**: one page of the unit's PDF, identified by its page number, always carrying a title (defaults to "Página N", renamable by the teacher).
- **Page activity**: an exercise tied to exactly one page of the unit; has a title and a per-student status (pending, submitted, graded).
- **Unit quiz**: a quiz linked to the end of at most one unit, ordered within it by the teacher; has a title, a per-student status (available, in progress, submitted, graded) and an unseen indicator.
- **Level group**: the set of units of a given level that a particular student is assigned, in the teacher's order — the scope of the level navigation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From the moment a unit is open, a student can reach any page or activity of it in at most 2 interactions (1 on desktop, 2 on phone: open contents + select entry).
- **SC-002**: A student can move from one unit to another unit of the same level in at most 2 interactions, without passing through **Mi aprendizaje**.
- **SC-003**: For 100% of pages and activities, the title in the contents panel matches the title shown in the viewer.
- **SC-004**: The highlighted contents entry matches the page in view at least 95% of the time during ordinary scrolling (brief lag during fast scrolling is acceptable).
- **SC-005**: At phone width (375 px), the unit page shows no horizontal scrolling and the PDF keeps the full content width.
- **SC-006**: Opening a unit with the contents panel adds no noticeable delay to the time until the first page is readable compared with today.

## Assumptions

- "Category (A1, A2, etc.)" means the unit's existing CEFR level; no new grouping concept is introduced.
- Level navigation only lists units the student is currently assigned to — consistent with the existing rule that units are reachable only through assignment.
- Units in the level navigation are listed by subject under a single level heading (e.g., "Nivel A2") and ordered by the teacher's existing per-level unit order.
- The information needed (page titles, activities with status, the student's assigned units with level, order and unseen state) is already available to the student unit page; the feature is expected to be mostly a presentation change, with no new teacher-side authoring.
- Unit-end quizzes taken from the contents are shown inside the unit page; the standalone quiz page and the **Mi aprendizaje** / **Pruebas** entry points keep working as today.
- No change to the teacher's unit editor in the admin panel.
- Interface copy is in Spanish like the rest of the student area.
