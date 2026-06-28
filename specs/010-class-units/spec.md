# Feature Specification: Class Units (Packages)

**Feature Branch**: `010-class-units`

**Created**: 2026-06-28

**Status**: Draft

**Input**: User description: "Group homeworks and presentations tabs from the teacher's Panel into one tab, called "Units" which will contain different class packages for students, like A1 - unit 1,2,3; A2-1,2,3,4,5, etc. (different subjects, like Family, Food, Holiday). Once the teacher assigns a package to a student, they will get access to all the "Presentations" inside that package, but NOT the homeworks. Those will have to be assigned by the teacher."

## Clarifications

### Session 2026-06-28

- Q: Can a presentation/homework belong to multiple units, or only one? → A: One unit only — each presentation/homework belongs to at most one unit.
- Q: Do units carry an explicit sequence number, or are they just level + subject? → A: Explicit ordering — each unit has a teacher-controlled position within its level (e.g. A1 unit 1, 2, 3); students see them in that sequence.
- Q: Must all content belong to a unit, or can items be standalone? → A: Unit-required — every newly created presentation/homework must belong to a unit; only pre-existing legacy content may remain unattached.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher organizes content into Units (Priority: P1)

The teacher opens the Panel and finds a single **Units** tab that replaces the
separate Homework and Presentations tabs. Inside it she creates class packages
("units"), each labelled by proficiency level (A1, A2, …, C2) and a subject/theme
(e.g. "Family", "Food", "Holiday"). Within each unit she adds the presentations
and homeworks that belong to that lesson package, authoring them in place or
attaching ones she already created.

**Why this priority**: This is the structural foundation of the feature. Nothing
else (assigning packages, granting presentation access) can happen until the
teacher can group her teaching material into units. It also delivers immediate
value on its own: a tidy, single place to manage all course content instead of
two disconnected tabs.

**Independent Test**: Can be fully tested by creating a unit, labelling it with a
level and subject, adding two presentations and one homework to it, and confirming
the unit and its contents persist and are listed in the Units tab. No student
assignment is required to demonstrate value.

**Acceptance Scenarios**:

1. **Given** the teacher is on the Panel, **When** she opens the tabs, **Then** she
   sees a single "Units" tab and no longer sees separate "Homework" and
   "Presentations" tabs.
2. **Given** the teacher is in the Units tab, **When** she creates a unit with a
   level (e.g. "A1") and a subject (e.g. "Family"), **Then** the unit appears in
   the list identified by both its level and subject.
3. **Given** an existing unit, **When** the teacher adds a presentation and a
   homework to it, **Then** both appear as contents of that unit.
4. **Given** a unit with contents, **When** the teacher removes a presentation
   from it, **Then** that presentation is no longer part of the unit (but the
   presentation itself still exists and can be added to another unit).
5. **Given** units of different levels exist, **When** the teacher views the list,
   **Then** units are grouped/sortable by level so that all A1 units, all A2 units,
   etc. can be reviewed together.

---

### User Story 2 - Teacher assigns a Unit to a student (Priority: P2)

The teacher selects a student and assigns one or more units to them. As soon as a
unit is assigned, the student gains access to **all presentations** contained in
that unit — and to any presentation later added to the same unit. Homeworks in the
unit are **not** granted by this action. The student sees the unit's presentations
in their learning area, organized by unit.

**Why this priority**: This is the primary outcome the teacher wants: a one-click
way to give a student a whole lesson package's worth of presentation material. It
depends on US1 (units must exist) but is the feature's main value driver.

**Independent Test**: Assign a unit containing two presentations to a student, log
in as that student, and confirm both presentations are visible and openable, while
the unit's homeworks remain absent. Then add a third presentation to the unit and
confirm it appears for the student without re-assigning.

**Acceptance Scenarios**:

1. **Given** a unit with presentations and homeworks, **When** the teacher assigns
   the unit to a student, **Then** the student can view every presentation in the
   unit.
2. **Given** a unit assigned to a student, **When** the teacher later adds a new
   presentation to that unit, **Then** the student automatically gains access to
   the new presentation without any further assignment action.
3. **Given** a unit assigned to a student, **When** the student opens their
   learning area, **Then** the unit's homeworks are NOT visible/accessible to them
   unless separately assigned.
4. **Given** a unit assigned to a student, **When** the teacher unassigns the unit,
   **Then** the student loses access to the unit's presentations.
5. **Given** a student opens their learning area, **When** they have presentations
   from one or more assigned units, **Then** the presentations are grouped under
   their unit (level + subject) so the student understands which package they
   belong to.

---

### User Story 3 - Teacher assigns homeworks individually (Priority: P3)

Even after a unit is assigned to a student, the homeworks inside that unit stay
locked. The teacher decides when each student is ready and assigns specific
homeworks from the unit to that student, one (or several) at a time. Only then do
those homeworks appear in the student's learning area.

**Why this priority**: This enforces the deliberate, per-student pacing the teacher
wants for assessments. It is lower priority than US2 because homework assignment
already exists today (per-student targeting); this story re-frames it within the
unit context rather than introducing entirely new capability.

**Independent Test**: With a unit assigned to a student (presentations visible),
confirm no homeworks are visible. Then assign one homework from that unit to the
student and confirm exactly that homework becomes available, while the unit's other
homeworks remain locked.

**Acceptance Scenarios**:

1. **Given** a unit assigned to a student with no homeworks assigned, **When** the
   student views their learning area, **Then** no homeworks from that unit appear.
2. **Given** a unit assigned to a student, **When** the teacher assigns one homework
   from the unit to the student, **Then** that single homework appears for the
   student and the unit's other homeworks remain unavailable.
3. **Given** the teacher is viewing a unit's homeworks, **When** she manages
   assignments, **Then** she can see which students currently have each homework
   assigned.

---

### Edge Cases

- **Unassigning a unit while homework is in progress**: If a homework from the unit
  was separately assigned and the student has started or submitted it, unassigning
  the unit revokes only presentation access; the in-progress/submitted homework and
  its history are preserved.
- **Removing a presentation from a unit**: Students assigned to that unit
  immediately lose access to the removed presentation (unless it was also granted by
  a legacy direct share).
- **Empty unit**: A unit with no presentations can still be created and assigned;
  the student simply sees no presentations until the teacher adds some.
- **Legacy directly-shared presentations/homeworks**: Presentations and homeworks
  that were shared/assigned before units existed remain accessible to those students
  even if not yet placed in a unit.
- **Deleting a unit**: Deleting a unit removes its grouping and assignments; the
  underlying presentations and homeworks are not deleted unless the teacher deletes
  them explicitly. Students lose unit-derived presentation access.
- **Duplicate level+subject**: Two units may share the same level and subject label
  (e.g. two different "A1 — Family" packages) and are still distinguishable as
  separate units.

## Requirements *(mandatory)*

### Functional Requirements

#### Unit management (teacher)

- **FR-001**: The Panel MUST present a single "Units" tab that replaces the
  previously separate "Homework" and "Presentations" tabs for content management.
- **FR-002**: The teacher MUST be able to create a unit, specifying a proficiency
  level (one of A1, A2, B1, B2, C1, C2) and a subject/theme name (free text, e.g.
  "Family").
- **FR-002a**: Each unit MUST have an explicit position/sequence number within its
  level (e.g. A1 unit 1, 2, 3). The teacher MUST be able to set and reorder unit
  positions within a level.
- **FR-003**: The teacher MUST be able to edit a unit's level and subject and to
  delete a unit.
- **FR-004**: The teacher MUST be able to add presentations to and remove
  presentations from a unit.
- **FR-005**: The teacher MUST be able to add homeworks to and remove homeworks
  from a unit.
- **FR-005a**: A presentation or homework MUST belong to at most one unit at a
  time. Adding an item already in a unit to a different unit moves it (removes it
  from the previous unit).
- **FR-005b**: Every newly created presentation or homework MUST belong to a unit
  (no orphan content can be created). Pre-existing legacy content from before this
  feature is the only exception and may remain unattached (see FR-017).
- **FR-006**: The teacher MUST be able to author/edit presentations and homeworks
  within the Units tab (the authoring capabilities previously on the separate tabs
  remain available, now reached via units).
- **FR-007**: The Units tab MUST allow the teacher to view units grouped by level
  and ordered by their position within each level so packages can be reviewed in
  teaching sequence.

#### Unit assignment & presentation access (teacher → student)

- **FR-008**: The teacher MUST be able to assign one or more units to a specific
  student and to unassign them.
- **FR-009**: When a unit is assigned to a student, the student MUST gain access to
  all presentations currently contained in that unit.
- **FR-010**: Presentation access derived from a unit MUST be dynamic: presentations
  added to a unit after assignment MUST automatically become accessible to all
  students that unit is assigned to, with no re-assignment.
- **FR-011**: When a unit is unassigned from a student (or a presentation is removed
  from the unit, or the unit is deleted), the student MUST lose access to the
  affected presentations — unless the same presentation is still granted by a legacy
  direct share.
- **FR-012**: Assigning a unit MUST NOT grant the student access to any homework in
  that unit.

#### Homework assignment (teacher → student)

- **FR-013**: Homeworks MUST remain unavailable to a student until the teacher
  explicitly assigns each homework to that student, independently of unit assignment.
- **FR-014**: The teacher MUST be able to assign and unassign individual homeworks
  to/from a student and MUST be able to see which students have a given homework
  assigned.

#### Student experience

- **FR-015**: A student MUST see, in their learning area, the presentations from all
  units assigned to them, organized/grouped by unit (identified by level, position
  number, and subject) and displayed in the unit's sequence within each level.
- **FR-016**: A student MUST only see homeworks that have been explicitly assigned to
  them, regardless of unit assignment.

#### Continuity / migration

- **FR-017**: Presentations and homeworks that were shared/assigned to students
  before units existed MUST continue to be accessible to those students.

### Key Entities *(include if feature involves data)*

- **Unit (Package)**: A named class package representing one lesson grouping.
  Attributes: proficiency level (A1–C2), position/sequence number within its level,
  subject/theme name. Contains a set of presentations and a set of homeworks. Can be
  assigned to many students.
- **Presentation**: Existing teaching slide-deck content. May belong to a unit and
  may be granted to students via unit assignment (and/or legacy direct share).
- **Homework**: Existing assignment/exercise content. May belong to a unit for
  organizational grouping but is only ever granted to students by explicit
  per-student assignment.
- **Unit Assignment**: The link between a unit and a student that grants access to
  the unit's presentations. Independent of homework assignment.
- **Homework Assignment**: The existing per-student link that grants access to an
  individual homework.
- **Student**: A user with the STUDENT role who receives unit and homework
  assignments.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From the Units tab, the teacher can create a labelled package and
  assign it to a student so the student gains all its presentations in under 2
  minutes, without leaving the tab.
- **SC-002**: After a teacher assigns a unit, the student sees 100% of that unit's
  current presentations and 0% of its homeworks in their learning area.
- **SC-003**: A presentation added to an already-assigned unit becomes visible to
  the assigned student on their next visit with no additional teacher action.
- **SC-004**: 100% of student-facing homeworks are ones the teacher explicitly
  assigned; no homework is ever exposed solely because its unit was assigned.
- **SC-005**: Content management for presentations and homeworks happens entirely
  within a single tab; the teacher no longer navigates two separate tabs to manage
  course content.
- **SC-006**: No student loses access to presentations or homeworks they could see
  before this feature shipped (zero regressions in existing access).

## Assumptions

- A presentation or homework belongs to **at most one** unit at a time (confirmed in
  Clarifications). Re-filing an item into another unit moves it.
- The proficiency levels reuse the existing A1–C2 scale already used for homeworks
  and presentations.
- "Subject" is free-text entered by the teacher (e.g. "Family", "Food", "Holiday");
  there is no fixed predefined subject list.
- Unit assignment and unassignment are performed per individual student (consistent
  with the existing per-student homework targeting model); bulk/class-wide assignment
  is out of scope for this version.
- The Units tab is teacher-only (ADMIN role); students never see the authoring view,
  only the resulting presentations and assigned homeworks in their learning area.
- Existing per-student homework assignment behaviour is reused unchanged; this
  feature only re-frames it within the unit context and does not alter homework
  formats, statuses, or grading.
- Legacy direct presentation shares and homework assignments remain valid; migrating
  existing content into units is optional and can be done gradually by the teacher.
