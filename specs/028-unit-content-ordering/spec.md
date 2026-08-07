# Feature Specification: Unit Content Ordering

**Feature Branch**: `028-unit-content-ordering`

**Created**: 2026-08-07

**Status**: Draft

**Input**: User description: "Add the option to order and mix presentations and homeworks inside a unit"

## Clarifications

### Session 2026-08-07

- Q: How should the student unit view present mixed content? → A: Single interleaved list of accessible presentations and homeworks in the teacher’s order (not separate type sections).
- Q: How are existing unit contents seeded into the mixed sequence? → A: All presentations first (existing presentation order), then all homeworks (existing homework order).
- Q: When does the seeded order apply for students? → A: Seeded order is active for students immediately on launch.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher sets a mixed learning sequence inside a unit (Priority: P1)

The teacher opens a unit in the Units tab and sees its presentations and homeworks
as one ordered list — not two separate type-grouped lists. She can place items in
any order she wants (e.g. presentation → homework → presentation → homework) so
the unit reflects the real teaching sequence for that lesson package.

**Why this priority**: Mixing and ordering content is the core of this feature. Until
the teacher can define a single sequence across both content types, the unit cannot
represent a pedagogical path — only two disconnected piles of material.

**Independent Test**: Create or open a unit with at least two presentations and two
homeworks. Reorder them into an interleaved sequence, reload the unit, and confirm
the same mixed order is preserved. No student assignment is required.

**Acceptance Scenarios**:

1. **Given** a unit that contains presentations and homeworks, **When** the teacher
   opens the unit's contents, **Then** she sees a single ordered list that includes
   both content types together (not separate presentation-only and homework-only
   sections).
2. **Given** a unit with mixed contents, **When** the teacher moves an item up or
   down in the list, **Then** that item's position in the sequence changes and the
   new order is saved.
3. **Given** the teacher has reordered a unit's contents, **When** she leaves and
   returns to that unit (or reloads), **Then** the contents appear in the order she
   set.
4. **Given** a unit whose contents are interleaved (e.g. presentation, homework,
   presentation), **When** the teacher views the list, **Then** each item is clearly
   identifiable as a presentation or a homework while still appearing in the mixed
   sequence.

---

### User Story 2 - Students follow the teacher's unit sequence (Priority: P2)

A student who has access to material in a unit opens that unit and sees **one
interleaved list** of accessible presentations and homeworks — not separate
presentation and homework sections. Items appear in the teacher’s sequence so the
learning path matches what she planned.

**Why this priority**: Ordering only helps if students experience the intended
sequence. This builds on US1 and delivers the student-facing value of mixed unit
content without changing who can access what.

**Independent Test**: With a unit sequenced as presentation A → homework 1 →
presentation B → homework 2, assign the unit (and homework 1 only) to a student.
Confirm the student sees A, then homework 1, then B — in that order — and does not
see homework 2.

**Acceptance Scenarios**:

1. **Given** a unit with a mixed teacher-defined sequence and a student who has
   access to some of its items, **When** the student opens that unit in their
   learning area, **Then** they see a single interleaved list of those accessible
   items in the teacher's order (no separate presentation-only or homework-only
   sections within the unit).
2. **Given** a homework in the middle of a unit sequence that has not been assigned
   to the student, **When** the student views the unit, **Then** that homework does
   not appear, and the surrounding accessible items keep their relative order.
3. **Given** the teacher changes the order of items in an already-assigned unit,
   **When** the student next views the unit, **Then** they see the updated sequence
   for items they can access.

---

### User Story 3 - New and removed items keep a sensible sequence (Priority: P3)

When the teacher adds a presentation or homework to a unit, it joins the sequence
in a predictable place. When she removes an item, the remaining items keep their
relative order without gaps or confusion.

**Why this priority**: Everyday attach/detach flows must not undo careful ordering.
This is supporting behaviour for US1 rather than the primary value.

**Independent Test**: Add a new homework to a unit that already has an ordered mix;
confirm it appears at the end. Remove a middle item; confirm neighbours stay in the
same relative order.

**Acceptance Scenarios**:

1. **Given** a unit with an existing ordered sequence, **When** the teacher adds a
   new presentation or homework to the unit, **Then** the new item appears at the
   end of the sequence by default.
2. **Given** a unit with items in positions 1–4, **When** the teacher removes the
   item in position 2, **Then** the remaining three items keep their previous
   relative order.
3. **Given** the teacher moves an item from one unit to another, **When** it arrives
   in the destination unit, **Then** it is placed at the end of that unit's
   sequence (and removed from the source unit's sequence).

---

### Edge Cases

- **Unit with only one content type**: A unit that contains only presentations (or
  only homeworks) still shows a single ordered list; ordering within that one type
  remains available.
- **Empty unit**: An empty unit has no sequence to display; adding the first item
  starts the sequence at position one.
- **Student with no accessible items in a unit**: If a unit is assigned but the
  student has no presentations yet (empty unit) and no assigned homeworks, the unit
  shows no content items (existing empty-state behaviour).
- **Legacy / unattached content**: Presentations or homeworks not belonging to a
  unit are out of scope for mixed unit ordering; they continue to appear outside
  unit sequences as they do today.
- **Concurrent edit**: If two teachers (or two sessions) reorder the same unit, the
  last successful save wins; the next load shows the persisted order.
- **Existing units at launch**: After the feature ships, every unit already has a
  defined sequence seeded as presentations-then-homeworks (see FR-013), and that
  sequence is immediately visible to students as an interleaved list (FR-014); no
  unit is left without an order or waiting for a teacher confirm step.

## Requirements *(mandatory)*

### Functional Requirements

#### Teacher — mixed sequence in a unit

- **FR-001**: Within a unit, the teacher MUST be able to view presentations and
  homeworks as one combined ordered sequence (not as two type-segregated lists).
- **FR-002**: The teacher MUST be able to reorder any item in a unit's sequence
  relative to any other item in that unit, including placing a homework between
  presentations and a presentation between homeworks.
- **FR-003**: The system MUST persist the unit's content sequence and restore it
  whenever the teacher reopens the unit.
- **FR-004**: Each item in the sequence MUST remain visually distinguishable as a
  presentation or a homework while sharing the same ordered list.
- **FR-005**: Reordering content MUST NOT change which students can access an item;
  access rules for presentations (via unit assignment) and homeworks (via explicit
  assignment) remain unchanged.

#### Student — sequence visibility

- **FR-006**: On the student's unit view, accessible presentations and homeworks
  MUST appear as one interleaved list in the teacher-defined sequence (not as
  separate type-segregated sections within that unit).
- **FR-007**: Items the student cannot access (e.g. unassigned homeworks) MUST NOT
  appear in that interleaved list, while still preserving relative order among
  accessible items.
- **FR-008**: When the teacher updates a unit's content order, students MUST see the
  updated relative order for their accessible items on their next view of that unit.

#### Sequence maintenance

- **FR-009**: When a presentation or homework is newly added to a unit, it MUST be
  placed at the end of that unit's content sequence by default.
- **FR-010**: When an item is removed from a unit (or moved to another unit), the
  remaining items in the source unit MUST keep their relative order.
- **FR-011**: When an item is moved into another unit, it MUST be appended to the
  end of the destination unit's content sequence.
- **FR-012**: Existing unit-level ordering (position of units within a proficiency
  level) MUST remain unchanged by this feature.
- **FR-013**: When this feature is introduced, each existing unit's content sequence
  MUST be seeded as: all of that unit's presentations in their current order,
  followed by all of that unit's homeworks in their current order. Teachers can
  reorder afterward.
- **FR-014**: The seeded sequence MUST be active for students immediately on launch
  (no per-unit teacher confirmation required). Students see the interleaved unit
  list as soon as the feature is available.

### Key Entities *(include if feature involves data)*

- **Unit**: Existing class package (level, subject, position within level). Now also
  owns a single ordered sequence of its content items.
- **Unit content item**: A presentation or homework that belongs to a unit and
  occupies one position in that unit's mixed sequence. Type (presentation vs
  homework) and access rules are unchanged; only the shared position in the unit
  sequence is new.
- **Presentation / Homework**: Existing content types; membership in at most one
  unit and existing student-access rules are unchanged.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can rearrange a unit containing at least two presentations
  and two homeworks into any interleaved order and confirm the saved order after
  reload in under 2 minutes.
- **SC-002**: 100% of accessible items a student sees on a unit page appear in one
  interleaved list following the teacher's sequence (skipping only items the
  student cannot access).
- **SC-002a**: On the day the feature launches, students viewing an untouched unit
  see presentations then homeworks (the seed order) in one interleaved list, with
  no extra teacher action required.
- **SC-003**: After adding a new item to a unit, it appears at the end of the
  sequence without disturbing the relative order of existing items in 100% of cases.
- **SC-004**: Reordering unit contents never grants or revokes student access; access
  outcomes remain identical to before the reorder.
- **SC-005**: Teachers can complete the primary reorder task without using separate
  presentation-only and homework-only lists inside the unit (single-list workflow).

## Assumptions

- Units already group presentations and homeworks; this feature only adds a
  **shared, teacher-controlled order** across both types inside a unit.
- Presentation access via unit assignment and homework access via per-student
  assignment are unchanged; ordering is display/sequence only.
- Newly attached items default to the **end** of the unit sequence (teacher can
  reorder afterward).
- Existing units are migrated once to presentations-then-homeworks order; no
  blank/undefined sequence remains after launch. That seeded order is immediately
  active for students (interleaved unit list), without requiring the teacher to
  touch each unit first.
- Students see only items they can access, in one interleaved unit list following
  the teacher's order; locked placeholders for unassigned homeworks are out of
  scope for this version.
- Other learning-area surfaces outside the unit detail view (if any) are unchanged
  except as needed to open the unit’s interleaved list.
- Drag-and-drop vs up/down controls is an interaction detail left to planning; the
  requirement is that the teacher can change order reliably.
- Unit reordering within a level (existing behaviour) is out of scope.
- Authoring, formats, grading, and assignment flows for presentations and homeworks
  are out of scope except where needed to show or persist the mixed sequence.
- Only the teacher (admin) can edit unit content order; students are read-only
  consumers of the sequence.
