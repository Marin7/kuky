# Feature Specification: Multi-Item Opción Única

**Feature Branch**: `038-single-choice-multi`

**Created**: 2026-08-15

**Status**: Draft

**Input**: User description: "For homeworks of type opcion-unica, I want to be able to have more than one questions per entry. Something like \"Select A/B and C/D and E/F\""

## Clarifications

### Session 2026-08-15

- Q: How should a grouped opción única entry count in the homework score? → A: Each item counts as its own question in the homework percentage and fully-correct count (three items = three questions), even though they sit in one visual entry.
- Q: How many options may each pick-one item have? → A: Each item is a normal pick-one: at least two options, exactly one correct; pairs are allowed but not required.
- Q: How should multiple items appear to the student? → A: The question text is shown as authored (including `(1)`, `(2)`, …). For each numbered item the student only selects a radio option (stacked radio groups; no typing).
- Q: What prompt text is required on a multi-item opción única question? → A: The question text is required. Multiple items are defined by `(1)`, `(2)`, … markers in that text — not by a separate per-item label field.
- Q: How does the teacher create multiple questions inside one opción única entry? → A: If the question text contains a number in brackets such as `(1)`, the entry has multiple items. For each distinct number the teacher defines that item’s options and marks the correct option. If the text has no such marker, the entry stays a classic single pick-one (existing homeworks are left unchanged).
- Q: When does an opción única question switch from classic (one option list) to numbered items? → A: As soon as the text contains any one marker such as `(1)` (even a single number). No marker = classic one option list.
- Q: When the teacher adds the first marker or removes all markers, what happens to the option lists? → A: First marker: copy classic options onto `(1)`; further numbers start empty. All markers removed: classic list becomes `(1)`’s options.
- Q: Must the numbers in the text form a consecutive sequence? → A: Yes — markers must be `(1)` … `(N)` with no gaps and must start at `(1)`. Save is blocked otherwise.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher authors several pick-one items by numbering the text (Priority: P1)

Paula is authoring an **opción única** homework question. She writes the question text and, when she wants several pick-one questions in that same entry, she puts numbers in brackets in the text — `(1)`, `(2)`, `(3)`, and so on. As soon as those markers appear, the editor shows an options list for each number. For every number she adds the choices (for example *ser / estar* for `(1)`, *por / para* for `(2)`) and marks which option is correct.

If she does not put any `(1)`-style marker in the text, the question stays a normal opción única: one option list for the whole prompt, same as today.

**Why this priority**: The numbered markers in the text are how she declares “this entry has several questions.” Without that authoring path, students cannot take a multi-item drill.

**Independent Test**: Create an opción única question whose text contains `(1)`, `(2)`, and `(3)`; for each number set at least two options and one correct; save; reopen and confirm the text, the three numbered items, their options, and the correct answers are intact.

**Acceptance Scenarios**:

1. **Given** the teacher is editing an opción única question, **When** the question text contains `(1)` and `(2)` (and they set options plus one correct choice for each number) and they save, **Then** that single entry stores two pick-one items in number order under that text.
2. **Given** a multi-item opción única entry already has `(1)` and `(2)`, **When** the teacher adds `(3)` to the text, sets that item’s options and correct answer, and saves, **Then** the student-facing question includes the third item.
3. **Given** a multi-item entry has `(1)`, `(2)`, and `(3)`, **When** the teacher removes `(2)` from the text but leaves `(1)` and `(3)`, **Then** save is blocked until the markers are a consecutive `(1)`…`(N)` sequence (for example they change `(3)` to `(2)`).
4. **Given** a multi-item entry has `(1)`, `(2)`, and `(3)`, **When** the teacher removes `(3)` (the last number) and saves, **Then** students who have not yet submitted only see `(1)` and `(2)`.
5. **Given** the teacher puts only `(1)` in the text (no `(2)`), **When** they configure options for that number and save, **Then** the question is numbered-item mode with one item — not the classic single option list.
6. **Given** the teacher authors an opción única question whose text has **no** number-in-brackets marker, **When** they save with one option list and exactly one correct option, **Then** it behaves as today’s classic single pick-one.
7. **Given** the teacher is authoring a presentation activity that uses opción única, **When** they put `(1)` and `(2)` in the text and configure options per number, **Then** the same multi-item model applies as for homework.

---

### User Story 2 - Student answers by selecting a radio per numbered item (Priority: P1)

A student opens a homework (or presentation activity) whose opción única question text includes `(1)`, `(2)`, … They see that text as the teacher wrote it. For each number they only choose one radio option — they do not type answers. They must pick an option for every numbered item before they can submit.

On a classic opción única question with no `(1)` marker, they still pick a single radio for the whole question, as today.

**Why this priority**: This is the student-facing outcome; authoring markers alone do not complete the feature.

**Independent Test**: Open a homework whose opción única text contains `(1)`, `(2)`, and `(3)` with options per number; confirm the text is visible and each number has only a radio group; select one option per number; submit; reopen and confirm each selection is stored on its number.

**Acceptance Scenarios**:

1. **Given** a homework with an opción única question whose text includes `(1)`, `(2)`, and `(3)`, **When** the student opens it, **Then** they see that text and a radio group per number (not one shared option list, and not a text field to type into).
2. **Given** the student has selected one radio on every numbered item (and completed the rest of the homework), **When** they submit, **Then** each selection is stored against its number and the homework is submitted as today.
3. **Given** any numbered item has no radio selected, **When** the student tries to submit, **Then** the system blocks submit and asks them to choose an option for every number.
4. **Given** a classic opción única question with no `(1)`-style marker, **When** the student opens it, **Then** they pick exactly one radio for that question, as today.
5. **Given** the student has already submitted, **When** they reopen the homework, **Then** their per-item radio selections are read-only (same single-submission lock as other auto-graded questions).

---

### User Story 3 - Student and teacher see per-item auto-feedback (Priority: P1)

On submit, each numbered item is graded automatically. The student immediately sees which numbers they got right or wrong, and the correct option is revealed for each wrong item. The teacher reviewing the submission sees the same per-item breakdown. **Each numbered item counts as its own question** in the homework’s overall percentage and fully-correct count: a `(1)` `(2)` `(3)` entry is three questions (each 0% or 100%), even though they share one visual entry.

**Why this priority**: Grouped items are only useful if feedback and scoring are clear per number, without waiting for the teacher.

**Independent Test**: Submit a three-number opción única entry with two items correct and one wrong, on a homework that contains only that entry; confirm per-item right/wrong plus the correct option on the wrong item; confirm the overall percentage is 67% (2 of 3) and the fully-correct count is 2 of 3.

**Acceptance Scenarios**:

1. **Given** the student submitted a multi-item opción única question, **When** grading completes, **Then** each numbered item is marked correct only if the selected radio is that item’s unique correct option.
2. **Given** one or more items are wrong, **When** the student views results, **Then** each wrong item reveals its correct option (same reveal rule as today’s opción única) and correct items stay marked correct.
3. **Given** a three-item entry with two items correct and one incorrect, and no other questions on the homework, **When** the overall homework score is computed, **Then** the overall percentage is two-thirds (displayed as a whole percent) and the fully-correct count is 2 of 3.
4. **Given** every numbered item on a three-item entry is correct, and no other questions on the homework, **When** the overall score is computed, **Then** the overall is 100% and the fully-correct count is 3 of 3.
5. **Given** a homework has one three-item opción única entry plus one other question (for example a free-text question), **When** the overall score is computed, **Then** the assignment has four equal-weight questions (three pick-one items plus the other question).
6. **Given** the teacher opens a graded submission, **When** they inspect that entry, **Then** they see the question text, each numbered item’s options, the student’s radio choice, and whether it was correct — including the same model on presentation activities.

---

### Edge Cases

- What if the question text contains no `(1)`-style marker? The question MUST stay a classic single opción única (one option list, one correct option). Existing homeworks without that marker MUST NOT be migrated or rewritten; they keep today’s behaviour and scores.
- What if the teacher types the first `(1)` into a classic question that already has options? Those options MUST become item `(1)`’s list; additional numbers start with no options until the teacher adds them.
- What if the teacher deletes every `(1)`-style marker? The question MUST return to classic mode using item `(1)`’s options (or an empty list if `(1)` had none).
- What if the text contains only `(1)` and no `(2)`? That is still numbered-item mode: one item, with its own option list (not the classic single list).
- What if the text contains `(1)` but the teacher has not finished options for that number? The system MUST reject save until every numbered item has at least two options and exactly one correct option.
- What if an item has fewer than two options, or zero or more than one option marked correct? The system MUST reject save (same rule as today’s single opción única, applied per number).
- What if the teacher writes `(ser)` or other non-numeric brackets? That MUST NOT create an item — only a number between brackets (for example `(1)`, `(2)`, `(10)`) counts.
- What if the same number appears twice, e.g. `(1)` … `(1)`? Both refer to the same item (one option list). Distinct numbers must still form `(1)`…`(N)` with no gaps.
- What if the teacher writes `(1)` and `(3)` without `(2)`, or starts at `(2)` with no `(1)`? The system MUST block save until the markers are consecutive `(1)`…`(N)` starting at `(1)`.
- What if the teacher removes a number from the text after students have already submitted? Already-submitted answers MUST keep a snapshot of the items they answered. Students who have not yet submitted MUST only see the numbers that remain in the current text.
- What if the student leaves one numbered radio unselected? Submit is blocked until every numbered item has a choice.
- What about other question kinds (opción múltiple, verdadero/falso, gaps, matching, free text)? Unchanged — this feature applies only to opción única.
- What about the placement test? Out of scope — placement questions keep today’s single pick-one shape.
- What if a homework mixes a three-item opción única entry with other question kinds? Each numbered opción única item counts as its own question; other kinds keep their existing weight (a gap-fill or matching question still counts as one).
- What if a three-item entry is the only work on the homework? The assignment has three questions (not one).
- What if the teacher edits the text, numbers, or answer key after submissions exist? Existing submissions and already-shown scores MUST be preserved; the new key applies only to future submissions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: An opción única question whose text contains **any** number-in-brackets marker such as `(1)` — even a single marker — MUST be treated as a numbered-item entry: each distinct number is a pick-one item under that shared text. A question with no such marker MUST remain classic (one option list).
- **FR-002**: The teacher MUST define multiple items by writing those markers in the question text (not by a separate “add item” list that is independent of the text). Adding or removing items is done by changing the markers in the text; the editor MUST show an options editor for each number in the required `(1)`…`(N)` sequence.
- **FR-003**: Each numbered item MUST have its own option list (two or more options; not limited to binary pairs) and exactly one option marked correct. Different items MAY have different options and different option counts.
- **FR-004**: The question text MUST be required and non-blank. There is no separate per-item label field; the number in the text (`(1)`, `(2)`, …) identifies the item.
- **FR-005**: The system MUST prevent saving a numbered-item opción única question that has a numbered marker without a complete option list, an item with fewer than two options, an item that does not have exactly one correct option, or a marker set that is not consecutive `(1)`…`(N)` starting at `(1)`.
- **FR-006**: Students MUST see the question text as authored (including the numbers in brackets). For each numbered item they MUST only select one radio option — no typing. Answer keys MUST NOT be visible before submit.
- **FR-007**: Students MUST select exactly one radio per numbered item. The system MUST block homework/activity submit while any numbered item has no selection.
- **FR-008**: On submit, the system MUST auto-grade each numbered item (correct if and only if the selected radio is that item’s correct option) without teacher involvement, reveal the correct option for each wrong item, and keep the question read-only afterward.
- **FR-009**: Each numbered item MUST count as its own question in the assignment’s overall percentage and fully-correct count (equal weight with every other question on the assignment). Each item scores 100% if correct and 0% if incorrect. A `(1)` `(2)` `(3)` entry therefore contributes three questions. Other question kinds MUST keep their existing weight (for example a multi-blank passage remains one question regardless of how many blanks it has).
- **FR-010**: An opción única question whose text contains **no** number-in-brackets marker MUST keep today’s classic behaviour: one option list, one radio selection, score 0 or 100. Existing homeworks without that marker MUST be left as they are — no conversion, no re-authoring, no silent score change.
- **FR-011**: Presentation activities that use opción única questions MUST support the same marker-based authoring, radio-only taking, grading, and review behaviour as homework.
- **FR-012**: Other question kinds and the placement test MUST remain unchanged by this feature.
- **FR-013**: Teacher review and student results MUST show each numbered item paired with the student’s radio choice and correctness (plus the correct option when the item is wrong).
- **FR-014**: When the teacher changes markers, options, or the answer key after students have submitted, already-graded submissions MUST keep the snapshot and score they received; only later submissions use the updated question.
- **FR-015**: Only a number inside brackets counts as a marker (e.g. `(1)`, `(12)`). Parentheses around non-numbers MUST NOT create items.
- **FR-016**: When the question text gains its first number-in-brackets marker, the system MUST copy the current classic option list (if any) onto item `(1)` and MUST start any further numbered items with empty option lists. When the text no longer contains any such marker, the system MUST use item `(1)`’s options as the classic option list (empty if `(1)` had none).
- **FR-017**: In numbered-item mode the distinct markers MUST be exactly `(1)`, `(2)`, … `(N)` for some N ≥ 1 — consecutive, no gaps, starting at `(1)`. Repeating the same number still refers to the same item. Items are ordered by that number, not by where the marker sits in the text.

### Key Entities

- **Opción única question**: One homework or activity visual entry with question text. If that text contains number-in-brackets markers, it holds several pick-one items; otherwise it is a classic single pick-one.
- **Number-in-brackets marker**: A number in parentheses in the question text, such as `(1)`, that identifies a pick-one item.
- **Pick-one item**: A scorable **question** tied to one distinct marker; has its own options and exactly one correct option; counts once in the assignment overall percentage and fully-correct count.
- **Item option**: A selectable radio choice belonging to one numbered item.
- **Student item answer**: The radio option the student selected for one numbered item; graded 100% or 0% for that item.
- **Assignment score**: Equal average of every question on the assignment, where each numbered opción única item is one question.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can author one opción única entry by writing text with `(1)`, `(2)`, and `(3)` and setting options plus one correct answer per number, in under 3 minutes, without creating three separate homework questions.
- **SC-002**: In a first-use check, at least 90% of students who open a three-number opción única question see the text and three radio groups, understand they must pick one radio per number (not type, and not one radio for the whole entry), and complete all three selections without help.
- **SC-003**: After submit, 100% of graded multi-item opción única entries show per-item right/wrong feedback, reveal the correct option on every wrong item, and show an assignment percentage and fully-correct count that treat each numbered item as its own question (three numbers = three questions).
- **SC-004**: A student can complete a typical three-item opción única drill (radio selections only, no extra media) in under 1 minute once the page is open.
- **SC-005**: 100% of existing opción única questions that do not contain a number-in-brackets marker still open, accept a single radio selection, and score 0 or 100 with no teacher re-authoring.

## Assumptions

- “Opción única” means the existing **single-choice question kind** inside a homework or presentation activity, not a separate homework type.
- A marker is an opening parenthesis, a whole number, and a closing parenthesis, with no extra characters inside (examples: `(1)`, `(2)`, `(10)`). `(01)` is the same item as `(1)`.
- Distinct markers must be exactly `(1)`…`(N)` with no gaps. Repeating the same number refers to the same item. Items are ordered by number.
- Each item is a full pick-one (two or more options, exactly one correct), not limited to binary pairs.
- Items do not share one option bank; each number has its own options.
- Scoring: each numbered item counts as its own question in the overall average and fully-correct count. Other kinds keep today’s weight.
- Existing homeworks are detected as classic because their text has no `(1)`-style marker; the product does not rewrite or convert them.
- Student interaction on multi-item entries is radio-only (no typing into the question text).
- Submit rules for the rest of the assignment stay as they are; every numbered item on a multi-item opción única question must have a radio selected.
- Single-submission lock, pre-submit key hiding, and post-submit reveal of correct options stay as they are for auto-graded work.
- **Out of scope**: opción múltiple, verdadero/falso, and other kinds do not gain this marker syntax; the placement test is unchanged; this feature does not change how mixed/manual questions are teacher-graded.
