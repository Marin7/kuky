# Feature Specification: New Exercise Types

**Feature Branch**: `024-new-exercise-types`

**Created**: 2026-08-03

**Status**: Draft

**Input**: User description: "Enhance types of exercises with these 4: (1) Given a list of 4-5 words, and same number of empty spaces in a text, the student must drag-and-drop each word/phrase in the given empty space; (2) A text with many empty spaces. The student must fill each space with the correct word (currently, you have to write one sentence at a time); (3) Exercise with a table in which the student must fill the correct word (useful for verb conjugations); (4) Given a list of words/phrases on the left and one on the right (not necessarily the same count), match them correctly"

## Clarifications

### Session 2026-08-03

- Q: Are the four new kinds homework-only, or also available in the placement test? → A: Homework exercises only (placement test unchanged)
- Q: How does the teacher mark blanks inside a passage? → A: Teacher types `___` tokens in the text; the system parses each `___` as a blank with its own answer key
- Q: Should word-bank items and matching lists be shuffled for the student? → A: Always shuffle word bank and matching lists when the student takes the exercise
- Q: Minimum blanks for multi-blank passage and drag-and-drop vs existing single-blank? → A: Multi-blank passage and drag-and-drop require ≥ 2 blanks; use existing single-blank kind for one blank
- Q: May the drag-and-drop word bank include unused distractor items? → A: No — bank size must equal blank count (no unused extra bank items)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Author and take a multi-blank passage (Priority: P1)

The teacher authors a short text with several blanks in one passage (instead of one blank per separate question). The student fills every blank by typing, submits once, and receives automatic feedback per blank plus an overall score.

**Why this priority**: This directly removes the main pain of today's fill-in-the-blank flow ("one sentence at a time") and is the highest-value enhancement for typical Spanish drills (articles, prepositions, verb forms in context).

**Independent Test**: Teacher creates a self-correcting homework with one multi-blank passage (e.g. 4 blanks), assigns it, student fills all blanks and submits, and sees per-blank correctness and an overall score without teacher review.

**Acceptance Scenarios**:

1. **Given** the teacher is authoring a self-correcting exercise, **When** they add a multi-blank passage question, **Then** they can enter a single text containing `___` tokens for blanks and an answer key for each blank (one or more accepted answers per blank), in blank order.
2. **Given** a multi-blank passage is incomplete (fewer than 2 `___` blanks, or a blank without an accepted answer), **When** the teacher tries to save, **Then** the system prevents saving and explains what is missing.
3. **Given** a student opens an assigned multi-blank passage, **When** they view it, **Then** they see the full text with one input per blank (not a separate prompt per sentence).
4. **Given** the student has filled the blanks, **When** they submit, **Then** each blank is graded against its answer key and the student sees per-blank feedback (including the correct answer for wrong blanks) plus an overall score.

---

### User Story 2 - Author and take a drag-and-drop word-bank exercise (Priority: P2)

The teacher provides a short text with blanks and a word bank of the same size (typically 4–5 words or phrases). The student places each bank item into exactly one blank by dragging (or an equivalent accessible action), then receives automatic feedback.

**Why this priority**: Strong pedagogical fit for vocabulary-in-context and recognition drills; distinct from typed multi-blank because the answer set is constrained to the bank. Slightly lower than P1 because typed multi-blank already covers "many blanks in one text."

**Independent Test**: Teacher authors a passage with 4 blanks and a matching 4-item word bank, student places each item into a blank and submits, and sees which placements were correct with an overall score.

**Acceptance Scenarios**:

1. **Given** the teacher adds a drag-and-drop word-bank question, **When** they define a passage with `___` blanks and the word/phrase bank, **Then** the bank must contain exactly one item per blank and each blank must have exactly one correct bank item.
2. **Given** the bank size does not match the blank count, or there are fewer than 2 blanks, **When** the teacher tries to save, **Then** the system prevents saving and explains the problem.
3. **Given** a student opens the exercise, **When** they interact with it, **Then** they can place each bank item into a blank (drag-and-drop on pointer devices; an equivalent keyboard-accessible action must also exist), each bank item can occupy at most one blank at a time, and the word bank is presented in a shuffled order (not the teacher's authored order).
4. **Given** the student has placed items and submits, **When** grading completes, **Then** each blank is marked correct only if it holds its designated bank item, wrong blanks reveal the correct item, and an overall score is shown.

---

### User Story 3 - Author and take a conjugation / fill-in table (Priority: P2)

The teacher authors a table (rows and columns with headers) where some cells are blanks the student must complete — classic verb conjugation grids and similar paradigms. The student fills the blank cells, submits, and gets automatic per-cell feedback.

**Why this priority**: High value for Spanish verb practice and structured paradigms; independent of passage blanks but equally auto-gradable. Same priority band as drag-and-drop; can ship after P1 without blocking it.

**Independent Test**: Teacher creates a 3×3 conjugation-style table with several blank cells and answer keys, student fills those cells and submits, and sees per-cell correctness and an overall score.

**Acceptance Scenarios**:

1. **Given** the teacher adds a table-fill question, **When** they define rows, columns, headers, and which cells are blanks vs fixed labels, **Then** they can set one or more accepted answers for every blank cell.
2. **Given** a table has no blank cells, or a blank cell has no accepted answer, **When** the teacher tries to save, **Then** the system prevents saving.
3. **Given** a student opens the table exercise, **When** they view it, **Then** fixed cells show as read-only labels and blank cells show as inputs in the same table layout.
4. **Given** the student submits, **When** grading completes, **Then** each blank cell is graded independently, wrong cells reveal the correct answer, and an overall score is shown.

---

### User Story 4 - Author and take a matching exercise (Priority: P3)

The teacher defines two lists (left and right) of words or phrases. The counts need not be equal — extras act as distractors. The student pairs items from left to right correctly, submits, and receives automatic feedback on each pairing.

**Why this priority**: Valuable for vocabulary and translation pairs, but pedagogically distinct and slightly less urgent than passage and table drills for daily homework.

**Independent Test**: Teacher creates a matching question with 5 left items and 7 right items (including distractors), marks the correct pairs, student matches them and submits, and sees which pairs were correct with an overall score.

**Acceptance Scenarios**:

1. **Given** the teacher adds a matching question, **When** they enter left items, right items, and the correct pairings, **Then** every left item that should be matched has exactly one correct right partner, and unpaired items on either side are treated as distractors.
2. **Given** no correct pairings are defined, **When** the teacher tries to save, **Then** the system prevents saving.
3. **Given** a student opens the matching exercise, **When** they interact with it, **Then** they can pair left items with right items (and change or clear a pairing before submit); a right item can be used in at most one pairing at a time; and both lists are presented in a shuffled order (not the teacher's authored order).
4. **Given** the student submits, **When** grading completes, **Then** each expected pair is scored as correct or incorrect, incorrect pairs reveal the correct partner, unmatched expected left items score as incorrect, and an overall score is shown.

---

### User Story 5 - Existing exercise kinds still work (Priority: P3)

Single-correct choice, multiple-correct choice, and single-blank fill-in-the-blank exercises continue to work as today for both authoring and students. New kinds appear alongside them; nothing forces migration of existing homework.

**Why this priority**: Explicit no-regression guarantee so the enhancement does not break current homework.

**Independent Test**: Open an existing self-correcting homework that uses only the original three kinds, take it as a student, and confirm behaviour and scores match pre-feature behaviour.

**Acceptance Scenarios**:

1. **Given** an existing homework with only the original question kinds, **When** a student opens and submits it after this feature ships, **Then** grading, feedback, and status behave as before.
2. **Given** the teacher authors a new exercise, **When** they add questions, **Then** they can mix original kinds and any of the four new kinds in one exercise.

---

### Edge Cases

- **Partial completion**: A student may submit with some blanks empty, some table cells empty, or some expected pairs unmatched; each incomplete item scores 0.
- **Answer matching for typed answers** (multi-blank and table cells): Same rules as today's fill-in-the-blank — trim surrounding whitespace, case-insensitive, accent-exact; teachers add alternate accepted answers for leniency.
- **Drag-and-drop reuse**: Moving a bank item from one blank to another frees the previous blank; the bank shows which items are still unused.
- **Matching distractors**: Extra left or right items that are not part of any correct pair never need to be used; using a distractor as a partner makes that pair incorrect.
- **Shuffle on take**: For drag-and-drop word banks and matching lists, the student always sees a shuffled presentation order; grading uses stable item identity, not display position. Teacher authoring and review views keep authored order.
- **Bank item identity**: Two bank items with identical text are still distinct placements; grading follows the teacher's designated correct item per blank, not free text matching.
- **Blank token parsing**: Each `___` in an authored passage is one blank; literal underscore characters that are not part of a blank token are out of scope for v1 (teachers should not need bare underscores in passage text). Multi-blank and drag-and-drop passages with fewer than 2 blanks cannot be saved.
- **Table size bounds**: Extremely large tables are discouraged in authoring guidance; the system rejects tables with zero rows/columns or zero blank cells.
- **Mixed exercise**: One homework may combine original and new auto-gradable kinds; free-text manual homework remains a separate designation (unchanged).
- **Post-submit lock**: Same as today — single submission, read-only afterward, correct answers revealed for wrong items.
- **Teacher edits after submissions**: Existing submissions and shown scores are preserved; the new answer key applies only to future submissions.
- **Accessibility**: Drag-and-drop and matching MUST remain completable without a pointing device (keyboard-equivalent pairing/placement).
- **Placement test**: Placement-test authoring and taking stay on their existing question kinds; these four kinds are homework-only.

## Requirements *(mandatory)*

### Functional Requirements

**Shared exercise framework**

- **FR-001**: Self-correcting **homework** exercises MUST support four new auto-gradable question kinds in addition to the existing single-correct choice, multiple-correct choice, and single-blank fill-in-the-blank: **drag-and-drop word bank**, **multi-blank passage**, **table fill**, and **matching**. The placement test MUST NOT gain these kinds in this feature.
- **FR-002**: The teacher MUST be able to add, edit, reorder, and remove questions of any supported kind on the existing dedicated homework authoring page.
- **FR-003**: For every new kind, the answer key MUST never be exposed to students before they submit.
- **FR-004**: On submission, the system MUST auto-grade each scorable unit (blank, cell, or expected pair) without teacher involvement, produce a per-question score between 0 and 1 as the average of its units, reveal the correct answer for each wrong unit, and contribute to the existing overall percentage score and fully-correct question count.
- **FR-005**: Existing homework using only the original three kinds MUST continue to behave as before (no forced migration, no data loss).
- **FR-006**: A student MUST complete all question kinds — including the four new ones — on the existing dedicated exercise page, with controls appropriate to each kind.

**Multi-blank passage (US1)**

- **FR-007**: The teacher MUST be able to author one passage by typing text that includes `___` tokens; each `___` becomes an ordered blank with one or more accepted answers. Adjacent underscores that form a single `___` token count as one blank; answer keys are aligned to blanks in left-to-right order of appearance. A multi-blank passage MUST contain at least 2 blanks (a single blank uses the existing fill-in-the-blank kind).
- **FR-008**: The student MUST see the full passage with an input control per blank (in place of each `___`) and fill them by typing.
- **FR-009**: Typed answers for multi-blank passages MUST use the same matching rules as existing fill-in-the-blank (trim, case-insensitive, accent-exact).

**Drag-and-drop word bank (US2)**

- **FR-010**: The teacher MUST author a passage with at least 2 `___` blanks and a word/phrase bank whose size equals the blank count, and MUST designate exactly which bank item belongs in each blank (aligned to blank order).
- **FR-011**: The student MUST place bank items into blanks such that each item occupies at most one blank; placement MUST work via drag-and-drop and via a keyboard-accessible alternative. The word bank MUST be shown to the student in shuffled order (stable for that attempt; not the teacher-authored order).
- **FR-012**: A blank scores correct only when it holds the teacher-designated bank item for that blank.

**Table fill (US3)**

- **FR-013**: The teacher MUST author a table with row/column headers (or labels), mark which cells are blanks vs fixed content, and provide accepted answers for every blank cell.
- **FR-014**: The student MUST fill blank cells by typing within the displayed table layout; fixed cells remain read-only.
- **FR-015**: Typed table-cell answers MUST use the same matching rules as existing fill-in-the-blank (trim, case-insensitive, accent-exact).

**Matching (US4)**

- **FR-016**: The teacher MUST define a left list, a right list, and one or more correct pairings; list lengths MAY differ (distractors allowed on either side).
- **FR-017**: The student MUST be able to create, change, and clear pairings before submit; each right item MAY be paired to at most one left item at a time (and vice versa). Left and right lists MUST be shown to the student in shuffled order (stable for that attempt; not the teacher-authored order).
- **FR-018**: Each expected pairing is a scorable unit; a pairing is correct only when the student connects the teacher-designated partners. Unmatched expected left items score 0.

**Authoring validation**

- **FR-019**: The system MUST prevent saving any new-kind question that lacks a complete answer key (e.g. fewer than 2 `___` blanks for multi-blank or drag-and-drop, a blank without accepted answers, bank/blank count mismatch, table with no blanks, matching with no pairings).

### Key Entities *(include if feature involves data)*

- **Exercise Question**: Existing entity; gains four new kinds alongside the original three.
- **Multi-Blank Passage**: A prompt text containing `___` blank tokens; each blank (in left-to-right order) has accepted answers.
- **Word-Bank Placement**: A passage with `___` blanks plus an ordered bank of words/phrases and a correct bank-item reference per blank.
- **Fill Table**: A grid with headers/labels, fixed cells, and blank cells each with accepted answers.
- **Matching Set**: Left items, right items, and the set of correct left↔right pairings (distractors are items with no pairing).
- **Student Answer (extended)**: Captures per-blank text, per-blank bank placements, per-cell text, or left↔right pairings as appropriate to the question kind.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can author a multi-blank passage with at least 4 blanks and a complete answer key in under 5 minutes without leaving the homework authoring page.
- **SC-002**: A student can complete and submit any one of the four new exercise kinds in under 3 minutes for a typical short drill (≤ 6 blanks/cells/pairs).
- **SC-003**: After submission, students see per-unit feedback (blank, cell, or pair) and an overall score for 100% of completed new-kind questions without waiting for the teacher.
- **SC-004**: At least 90% of testers successfully place all word-bank items into blanks on the first attempt using either drag-and-drop or the keyboard alternative.
- **SC-005**: Existing self-correcting homeworks that use only the original three question kinds continue to open, submit, and score correctly with no teacher re-authoring.
- **SC-006**: Teachers can create a conjugation-style table (at least 2 headers × 3 blank cells) and a matching set with unequal list lengths in a single exercise without workarounds (no splitting into many single-blank questions).

## Assumptions

- All four new kinds are auto-gradable self-correcting question types within the existing exercise homework model (not manual free-text homework).
- The existing single-blank fill-in-the-blank kind remains available; multi-blank passage is an additional kind that solves the "one sentence at a time" limitation rather than replacing single-blank. Multi-blank passage and drag-and-drop word-bank questions require at least 2 blanks.
- Typical drag-and-drop bank size is about 4–5 items; the product allows other equal blank/bank counts within a reasonable upper bound set at planning time. Extra unused bank distractors are out of scope (matching covers distractors).
- Passage blanks for multi-blank and drag-and-drop kinds are authored by typing the literal token `___` in the text; the system parses each token as one blank in left-to-right order.
- Word-bank items and matching left/right lists are always shuffled for the student when taking the exercise; authoring and teacher review keep the authored order.
- Matching allows unequal list lengths; extras are distractors and do not need to be paired.
- Scoring aggregates per-unit correctness into the existing per-question 0–1 score and overall percentage / fully-correct count model from the current exercises feature.
- Accent-sensitive Spanish matching rules from the existing fill-in-the-blank behaviour apply to all typed new kinds.
- Mobile and desktop are both in scope; pointer drag-and-drop is preferred where available, with a non-pointer alternative required for accessibility.
- Mixing new kinds with existing choice / single-blank kinds in one exercise is allowed; combining auto-graded questions with manual free-text in one homework remains out of scope (unchanged).
- Teacher and student roles, assignment flow, single-submission lock, and post-submit answer reveal remain as in the current homework exercises feature.
- **Out of scope**: Placement-test questions do not gain these four kinds in this feature; only self-correcting homework exercises do.
