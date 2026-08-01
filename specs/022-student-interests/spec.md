# Feature Specification: Student Interests on Profile

**Feature Branch**: `022-student-interests`

**Created**: 2026-08-01

**Status**: Draft

**Input**: User description: "Add the option for each student to select their interests from their profile page. The teacher can then see that info and verify it before each class in order to better prepare the next lecture."

## Clarifications

### Session 2026-08-01

- Q: Who maintains the interest catalogue? → A: Fixed catalogue defined by the product (seeded list; not editable in the admin UI)
- Q: Where should the teacher see interests when preparing for a class? → A: Only on the admin student profile
- Q: Beyond the fixed list, can students add a free-text note? → A: Catalogue multi-select plus an optional short free-text note
- Q: Who can edit interests on the account profile? → A: Only students (accounts with student access) see and edit the interests section
- Q: Maximum length for the optional free-text note? → A: 280 characters

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Student selects interests on their profile (Priority: P1)

A student opens their account profile and selects the topics and hobbies they enjoy talking about (for example travel, music, sports, food, cinema). They may also add an optional short free-text note for details the catalogue does not cover. They can change their selection and note later whenever their preferences evolve. Saving persists the choices so the teacher can rely on them when planning conversation-focused lessons.

**Why this priority**: Without students being able to declare interests, there is nothing for the teacher to consult — this is the core data-capture half of the feature.

**Independent Test**: Log in as a student, open the account profile, select several interests from the offered list, optionally write a short note, save, reload the page, and confirm the same interests and note remain.

**Acceptance Scenarios**:

1. **Given** a logged-in student with no interests saved yet, **When** they open their account profile, **Then** they see an interests section with a clear empty/unselected state, a list of selectable interest options, and an optional free-text note field.
2. **Given** a logged-in user without student access, **When** they open their account profile, **Then** they do not see the interests section.
3. **Given** a student viewing the interests section, **When** they select one or more interests and save, **Then** those interests are stored and a success confirmation is shown.
4. **Given** a student who previously saved interests, **When** they reopen their profile, **Then** their previously selected interests (and note, if any) are shown.
5. **Given** a student with saved interests, **When** they deselect some, select others, edit or clear the note, and save, **Then** only the new selection and note are stored.
6. **Given** a student viewing the interests section, **When** they leave catalogue selections empty but enter a short note and save, **Then** the note is stored and remains visible on reload.

---

### User Story 2 - Teacher reviews student interests before class (Priority: P1)

Before a class, Paula opens a student's profile in the admin panel and immediately sees which interests that student has declared, including any optional free-text note. She uses that information to choose conversation topics, examples, and vocabulary that will resonate with the student for the upcoming lesson.

**Why this priority**: This is the teacher-facing value of the feature — interests only help if Paula can find them quickly while preparing. Equal priority with student capture because both halves are required for the outcome.

**Independent Test**: As a student, save a known set of interests and an optional note; as the teacher, open that student's admin profile and confirm the same interests and note are listed in a dedicated, easy-to-scan section.

**Acceptance Scenarios**:

1. **Given** a student who has selected several interests, **When** the teacher opens that student's profile in the admin panel, **Then** the teacher sees those interests listed clearly (labels, not opaque codes).
2. **Given** a student who has entered an optional free-text note (with or without catalogue selections), **When** the teacher opens that student's profile, **Then** the note is shown with the interests section.
3. **Given** a student who has not selected any interests and has no note, **When** the teacher opens that student's profile, **Then** the teacher sees an explicit empty state (for example "No interests selected yet") rather than a missing or broken section.
4. **Given** a student updates their interests or note, **When** the teacher reloads the student's profile, **Then** the updated content is shown.

---

### User Story 3 - Student clears or leaves interests empty (Priority: P2)

A student may prefer not to share interests, or may want to clear a previous selection and note. The profile still works normally, and the teacher simply sees that nothing has been provided yet.

**Why this priority**: Optional data should not block profile editing or create confusing error states; secondary to the happy path of selecting and viewing interests.

**Independent Test**: Save interests and a note, then clear all selections and the note and save again; confirm the student profile shows none selected and the teacher profile shows the empty state.

**Acceptance Scenarios**:

1. **Given** a student with saved interests and/or a note, **When** they deselect all interests, clear the note, and save, **Then** the profile stores an empty selection and empty note and shows the empty/unselected state.
2. **Given** a student with an empty interest selection and no note, **When** the teacher views the student profile, **Then** the empty-state message is shown (not an error).

---

### Edge Cases

- A registered account that is not a student (USER role only) does not see the interests editor on their profile; interests editing is gated to accounts with student access.
- Revoking a student's STUDENT role does not delete their saved interests or note; if they are promoted again, previous selections remain. The interests editor is hidden while they lack student access.
- Selecting the maximum allowed number of interests (if a soft cap applies) prevents adding more until one is deselected; the limit is communicated in the UI.
- A free-text note longer than 280 characters is rejected with a clear validation message; the previous saved note is not overwritten.
- Concurrent edits are last-write-wins: the most recent successful save replaces the previous selection and note.
- Interest options that are later removed from the catalogue no longer appear as selectable; previously stored values for removed options are not shown to the teacher as active interests. A saved free-text note is unaffected by catalogue changes.
- Interests and the optional note are private to the student and the teacher/admin — they are not shown on public pages (landing, testimonials, etc.).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Students MUST be able to view and update their interests from their account profile page.
- **FR-002**: Interests MUST be chosen from a fixed, product-defined catalogue of conversation-relevant topics (multi-select). The teacher MUST NOT be able to add, edit, or remove catalogue options from the admin panel in this feature.
- **FR-012**: The interest catalogue MUST be seeded by the product (stable identifiers + localized labels). Expanding or changing the catalogue is a product update, not a runtime admin action.
- **FR-013**: Students MUST be able to optionally enter a short free-text note alongside the catalogue selection to capture details the list does not cover. The note MAY be saved with or without catalogue selections, and MAY be cleared independently.
- **FR-014**: The free-text note MUST be at most 280 characters; over-length input MUST be rejected with a clear validation message.
- **FR-003**: Students MUST be able to select multiple interests, deselect any interest, and save an empty selection.
- **FR-004**: The system MUST persist a student's interest selection and optional note and restore them when they reopen their profile.
- **FR-005**: The teacher-facing student profile MUST display the student's current interests and optional note in a dedicated, scannable section suitable for pre-class preparation. Interests MUST NOT be required on booking or schedule views in this feature.
- **FR-006**: When a student has no catalogue interests selected and no note, the teacher profile MUST show an explicit empty state rather than omitting the section or showing an error.
- **FR-007**: Interest labels shown to students and to the teacher MUST be human-readable in the site's supported languages. The free-text note is shown as entered by the student (not translated).
- **FR-008**: Interests and the optional note MUST NOT appear on public, unauthenticated pages.
- **FR-009**: Only the student themselves may change their own interests and note via self-service profile; the teacher views them but does not edit them in this feature.
- **FR-010**: The interests editor on the account profile MUST be shown only to accounts with student access. Registered non-student accounts MUST NOT see the interests section.
- **FR-011**: Saving interests MUST NOT interfere with existing profile fields (name, username, avatar, timezone) — interests and note can be saved independently or together with other profile edits without losing unrelated data.

### Key Entities

- **Interest Option**: A product-seeded catalogue entry representing a conversation topic or hobby suitable for Spanish class preparation (stable identifier + display label). Shared across all students; not created or edited by students or the teacher.
- **Student Interest Selection**: The set of Interest Options a given student has chosen, plus an optional short free-text note. Owned by the student; readable by the teacher on that student's admin profile. Catalogue selection and/or note may each be empty.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A student can select interests (and optionally add a short note) and confirm they are saved in under 1 minute from opening their profile.
- **SC-002**: A teacher can open a student's profile and locate that student's interests and note within 10 seconds without navigating away from the profile page.
- **SC-003**: 100% of interest values and note text shown to the teacher match the student's last saved content (no stale or mismatched labels in normal use).
- **SC-004**: Students who leave interests and note empty can still use their profile and book classes without errors or blocking prompts.
- **SC-005**: Teachers preparing for a class can tell at a glance whether a student has shared interests or a note (selected list and/or note vs. explicit empty state).

## Assumptions

- Explicit out of scope for v1: teacher-managed catalogue CRUD; interests on booking/schedule views; approval/"verified" status workflow; treating free text as student-defined catalogue tags (the note is a single optional remark, not custom multi-select tags).
- "Verify before each class" means the teacher consults/reviews the student's interests on the existing admin student profile while preparing; there is no separate approval workflow, checklist tick, or "verified" status in this feature.
- Interests use a fixed, product-seeded multi-select catalogue of conversation topics (travel, music, sports, food, cinema, reading, technology, nature, art, work/career, family, culture/traditions, and similar), plus one optional short free-text note for details the catalogue does not cover.
- A soft upper bound of about 10 selected catalogue interests is enough; exact catalogue size can be refined during planning without changing the product intent. The optional free-text note is capped at 280 characters.
- Teacher visibility is only on the existing per-student profile in the admin panel. Surfacing interests on bookings or schedule views is explicitly out of scope for v1.
- Interests and the note are optional; students are never required to fill them before booking or accessing learning content.
- Interests and the note are edited only by students (student-access accounts); the teacher never edits them in this feature.
- Interests and note remain when student status is revoked, consistent with how other student history is preserved.
