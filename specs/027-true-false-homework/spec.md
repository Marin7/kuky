# Feature Specification: True/False Homework Exercises

**Feature Branch**: `027-true-false-homework`

**Created**: 2026-08-06

**Status**: Draft

**Input**: User description: "Create a new type of homework -> true or false"

## Clarifications

### Session 2026-08-06

- Q: What formatting is allowed in a true/false statement? → A: Same rich-text formatting as other exercise question prompts
- Q: In what order are true/false options shown to the student? → A: Fixed order — always True, then False (never shuffled)
- Q: Is there teacher-authored explanation text after a wrong true/false answer? → A: No — only mark correct/incorrect and reveal the right true/false value when wrong

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Author and take a true/false exercise (Priority: P1)

The teacher authors a self-correcting homework with one or more true/false questions — each a statement the student must judge as true or false. The student selects an answer for each statement, submits once, and receives automatic feedback (correct/incorrect with the right answer revealed for wrong items) plus an overall score, without teacher review.

**Why this priority**: True/false is a foundational drill type for Spanish grammar and facts (e.g. agreement, tense usage, cultural statements). It is the entire feature value; without authoring and taking working end-to-end, nothing else matters.

**Independent Test**: Teacher creates a self-correcting homework with at least three true/false statements and a complete answer key, assigns it, student answers and submits, and sees per-question correctness and an overall score without waiting for the teacher.

**Acceptance Scenarios**:

1. **Given** the teacher is authoring a self-correcting exercise, **When** they add a true/false question, **Then** they can enter a statement using the same rich-text formatting as other exercise question prompts and mark the correct answer as either true or false.
2. **Given** a true/false question has an empty statement or no correct answer selected, **When** the teacher tries to save, **Then** the system prevents saving and explains what is missing.
3. **Given** a student opens an assigned true/false question, **When** they view it, **Then** they see the statement and exactly two choices in fixed order (true, then false), with no indication of which is correct before submit.
4. **Given** the student has selected true or false, **When** they submit, **Then** the answer is graded against the answer key, wrong answers reveal the correct choice, and the result contributes to the overall score.

---

### User Story 2 - Mix true/false with existing exercise kinds (Priority: P2)

The teacher can include true/false questions alongside existing auto-gradable kinds (single-correct choice, multiple-correct choice, fill-in-the-blank, multi-blank, drag-and-drop, table fill, matching) in the same exercise. Existing homeworks that do not use true/false continue to work unchanged.

**Why this priority**: Teachers routinely combine short true/false checks with other drills in one homework; mixing unlocks practical authoring without forcing a separate assignment. Regression safety for existing kinds is required for a safe rollout.

**Independent Test**: Author an exercise with one true/false question and one existing kind (e.g. single-correct choice), take it as a student, and confirm both grade correctly; also reopen an older exercise with no true/false questions and confirm behaviour is unchanged.

**Acceptance Scenarios**:

1. **Given** the teacher is authoring an exercise, **When** they add questions, **Then** they can mix true/false with any other supported auto-gradable kinds in one exercise.
2. **Given** an existing homework that uses only previously supported kinds, **When** a student opens and submits it after this feature ships, **Then** grading, feedback, and status behave as before.
3. **Given** a mixed exercise that includes true/false, **When** the student submits, **Then** each question — including true/false — is scored with the same overall percentage and fully-correct question count model used today.

---

### Edge Cases

- **No selection before submit**: A student may submit a true/false question without choosing true or false; that question scores 0 (same incomplete-answer behaviour as other choice kinds).
- **Exactly two options**: True/false questions always present precisely the two choices true and false in that fixed order (true first, false second); teachers cannot add, remove, reorder, or shuffle the options — they only select which of the two is correct.
- **Answer key hidden**: The correct true/false value is never shown to students before submit; after submit it is revealed for incorrect answers (and available in the graded result as today). There is no separate teacher-authored explanation or rationale field for true/false questions.
- **Post-submit lock**: Same as today — single submission, read-only afterward.
- **Teacher edits after submissions**: Existing submissions and shown scores are preserved; a changed answer key applies only to future submissions.
- **Manual free-text homework**: Unchanged and separate; true/false is only for self-correcting exercise homework.
- **Placement test**: Placement-test authoring and taking stay on their existing question kinds; true/false is homework-only for this feature.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Self-correcting **homework** exercises MUST support a new auto-gradable question kind **true/false** in addition to the existing kinds (single-correct choice, multiple-correct choice, single-blank fill-in-the-blank, multi-blank passage, drag-and-drop word bank, table fill, and matching). The placement test MUST NOT gain this kind in this feature.
- **FR-002**: The teacher MUST be able to add, edit, reorder, and remove true/false questions on the existing dedicated homework authoring page.
- **FR-003**: For each true/false question, the teacher MUST provide a non-empty statement (using the same rich-text formatting available for other exercise question prompts) and designate exactly one correct answer: true or false.
- **FR-004**: The system MUST prevent saving a true/false question that lacks a statement or a designated correct answer.
- **FR-005**: The answer key (which of true/false is correct) MUST never be exposed to students before they submit.
- **FR-006**: When a student opens a true/false question, the system MUST show the statement and exactly two selectable choices representing true and false in fixed order (true first, false second — never shuffled), with localized labels as appropriate for the product’s language support.
- **FR-007**: On submission, the system MUST auto-grade each true/false question without teacher involvement: correct if the student’s selection matches the answer key, otherwise incorrect; reveal the correct choice when wrong; score the question as fully correct (1) or incorrect (0); and contribute to the existing overall percentage score and fully-correct question count. True/false questions MUST NOT include a teacher-authored explanation or rationale shown after submit.
- **FR-008**: A student MUST complete true/false questions on the existing dedicated exercise page alongside other kinds.
- **FR-009**: Existing homework using only previously supported kinds MUST continue to behave as before (no forced migration, no data loss).
- **FR-010**: Teachers MUST be able to mix true/false questions with other supported auto-gradable kinds in a single exercise.

### Key Entities *(include if feature involves data)*

- **Exercise Question**: Existing entity; gains a true/false kind alongside the existing auto-gradable kinds.
- **True/False Item**: A rich-text statement (same formatting capabilities as other exercise prompts) plus a binary correct answer (true or false).
- **Student Answer (extended)**: Captures the student’s true or false selection (or no selection) for a true/false question.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can author an exercise with at least five true/false statements and a complete answer key in under 5 minutes without leaving the homework authoring page.
- **SC-002**: A student can answer and submit a typical true/false drill (≤ 10 statements) in under 3 minutes.
- **SC-003**: After submission, students see per-question true/false feedback and an overall score for 100% of completed true/false questions without waiting for the teacher.
- **SC-004**: At least 95% of testers correctly understand how to select true or false and submit on the first attempt without help.
- **SC-005**: Existing self-correcting homeworks that do not use true/false continue to open, submit, and score correctly with no teacher re-authoring.
- **SC-006**: Teachers can combine at least one true/false question with at least one other auto-gradable kind in a single exercise without workarounds.

## Assumptions

- True/false is a new auto-gradable question kind within the existing self-correcting exercise homework model (not a separate free-text / manual homework format).
- Each true/false question is one statement with exactly two possible answers (true / false); multi-statement “batches” as a single question are out of scope — teachers add multiple true/false questions instead. Statement authoring uses the same rich-text capabilities as other exercise question prompts (not a plain-text-only field).
- Option labels follow the product’s existing language support (e.g. Spanish/Romanian UI); the underlying meaning is always true vs false. Student-facing options always appear in fixed order (true, then false) and are never shuffled.
- Scoring uses the existing per-question 0–1 and overall percentage / fully-correct count model; a true/false question is a single scorable unit. Post-submit feedback is correctness plus revealing the correct true/false value when wrong — no optional explanation text.
- Teacher and student roles, assignment flow, single-submission lock, and post-submit answer reveal remain as in the current homework exercises feature.
- Mixing true/false with existing auto-gradable kinds in one exercise is allowed; combining auto-graded questions with manual free-text in one homework remains out of scope (unchanged).
- **Out of scope**: Placement-test questions do not gain true/false in this feature; only self-correcting homework exercises do.
