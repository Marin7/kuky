# Feature Specification: Homework Exercises & Self-Correction

**Feature Branch**: `007-homework-exercises`

**Created**: 2026-06-12

**Status**: Draft

**Input**: User description: "I want to enhance the Homeworks a bit. When creating a new homework, I want it to be a separate page entirely, because of the complexity. The options to set the type and all that look good, I want to keep them, but in a separate page. Then, I want some of the homeworks to be self-correcting, meaning that the student may have a multiple-choice type of exercise which the teacher can set the correct responses and the student will get automatic feedback. This obviously works only for this kind of exercises with multiple choice or 'use the correct form of the verb...'. Others, like write a text, will need manual verification from the teacher."

## Clarifications

### Session 2026-06-12

- Q: For fill-in-the-blank, should answer matching be accent-sensitive (Spanish)? → A: Accent-sensitive — matching is case-insensitive and whitespace-trimmed, but accents must match exactly (`compré` ≠ `compre`). The teacher adds extra accepted answers when leniency is wanted.
- Q: How is a multiple-choice question scored when answered partially right? → A: Partial credit per question — each correct option selected and each incorrect option correctly left unselected contributes; the question yields a fractional score between 0 and 1.
- Q: Can a student retake a self-correcting exercise after seeing the score? → A: No — single submission; the result is final and the exercise becomes read-only after submitting.
- Q: After submitting, does the student see the correct answers for questions they got wrong? → A: Yes — the feedback reveals the correct answer for each wrong question (safe because the exercise is locked after submission).
- Q: How is the overall score expressed? → A: Percentage as the primary figure plus a raw fully-correct count (e.g. "80% — 8 of 10 questions fully correct"); no pass/fail threshold.
- Q: Should choice questions distinguish single-answer from multiple-answer? → A: Yes — the teacher picks "single correct" (radio, scored 0/1) or "multiple correct" (checkboxes, partial credit) per question.
- Q: Where does the student answer a self-correcting exercise? → A: On a dedicated full page (the manual free-text homework keeps its existing dialog).
- Q: Can a fill-in-the-blank question contain multiple inline blanks? → A: No — one blank (one answer field) per question; the teacher adds several consecutive questions for a drill.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Author homework on a dedicated page (Priority: P1)

The teacher creates and edits homework on a full-screen, dedicated page instead of a cramped dialog. The page keeps all the existing options (title, instructions, skill type, level, due date, assignee selection) but gives them room to breathe and acts as the home for the new, more complex exercise-authoring controls.

**Why this priority**: The current dialog cannot accommodate the added complexity of authoring structured questions with answer keys. A roomy dedicated page is the foundation everything else in this feature is built on, and it delivers immediate value on its own (a clearer, less constrained authoring experience).

**Independent Test**: From the homework list in the teacher panel, click "New homework", confirm the browser navigates to a separate page showing all existing fields, fill them in, save, and confirm the new homework appears back in the list. Editing an existing homework opens the same page pre-filled.

**Acceptance Scenarios**:

1. **Given** the teacher is on the homework tab of the panel, **When** they choose to create a new homework, **Then** they are taken to a dedicated full page (not a dialog) containing title, instructions, type, level, due date, and assignee fields.
2. **Given** the teacher is editing an existing homework, **When** they open it, **Then** the dedicated page loads pre-filled with that homework's current values.
3. **Given** the teacher has filled in the required fields on the dedicated page, **When** they save, **Then** the homework is persisted and they are returned to the homework list with the new/updated item visible.
4. **Given** the teacher is partway through authoring, **When** they cancel or navigate back, **Then** no changes are saved and they return to the homework list.

---

### User Story 2 - Author a self-correcting exercise with an answer key (Priority: P2)

For suitable homework, the teacher builds one or more structured questions — multiple-choice (pick one or more correct options) or fill-in-the-blank (e.g. "use the correct form of the verb") — and records the correct answer(s) for each. This turns the homework into a self-correcting exercise that the system can grade automatically.

**Why this priority**: This is the core new capability. It enables automatic feedback (User Story 3) and saves the teacher review time, but depends on the dedicated authoring page from User Story 1.

**Independent Test**: On the dedicated authoring page, mark a homework as a self-correcting exercise, add a multiple-choice question with two options (one marked correct) and a fill-in-the-blank question with an accepted answer, save, reopen the homework, and confirm the questions and answer key persisted exactly.

**Acceptance Scenarios**:

1. **Given** the teacher is authoring a homework, **When** they designate it as a self-correcting exercise, **Then** they can add an ordered list of questions.
2. **Given** the teacher adds a multiple-choice question, **When** they enter the options, **Then** they can mark which option(s) are correct, and at least one correct option is required to save.
3. **Given** the teacher adds a fill-in-the-blank question, **When** they enter the prompt, **Then** they can record one or more accepted answers, and at least one is required to save.
4. **Given** a self-correcting exercise has no questions, **When** the teacher tries to save, **Then** the system prevents it and explains that an exercise needs at least one question.
5. **Given** the teacher reorders, edits, or deletes questions, **When** they save, **Then** the persisted exercise reflects those changes.

---

### User Story 3 - Student receives automatic feedback on a self-correcting exercise (Priority: P3)

When a student opens a self-correcting homework, they answer the structured questions and, on submission, immediately see which answers were correct, which were wrong, and an overall score — with no waiting for the teacher.

**Why this priority**: This is the student-facing payoff of the feature, but it cannot exist without authored exercises (User Story 2). It delivers the headline value: instant feedback.

**Independent Test**: As a student assigned a self-correcting exercise, answer the questions, submit, and confirm an immediate result showing per-question correctness and an overall score, without any teacher action.

**Acceptance Scenarios**:

1. **Given** a student is assigned a self-correcting exercise, **When** they open it, **Then** they are taken to a dedicated page showing the questions with appropriate answer controls (radios for single-correct, checkboxes for multiple-correct, text inputs for fill-in-the-blank).
2. **Given** the student has answered the questions, **When** they submit, **Then** the system immediately shows per-question feedback (including the correct answer for any wrong question) and an overall percentage score with a fully-correct count.
3. **Given** the student submitted a self-correcting exercise, **When** the submission completes, **Then** the homework's status reflects that it has been automatically graded — without requiring teacher review — and the exercise becomes read-only (no retake).
4. **Given** the teacher later views the homework, **When** they look at that student's entry, **Then** they can see the student's answers and the automatic score.

---

### User Story 4 - Open-ended homework still requires manual teacher review (Priority: P3)

Homework that asks for free-form work (e.g. "write a text about your holidays") continues to work exactly as today: the student writes a response, submits it, and the teacher reviews it manually. The new exercise machinery does not disrupt this path.

**Why this priority**: Preserves existing behaviour and makes explicit that self-correction is opt-in, not universal. Lower priority because it is mostly a no-regression guarantee rather than new capability.

**Independent Test**: Create a manual (non-exercise) homework, assign it to a student, have the student submit a free-text response, and confirm it appears for the teacher as awaiting manual review (no automatic score), exactly as before this feature.

**Acceptance Scenarios**:

1. **Given** a homework is not marked as a self-correcting exercise, **When** a student opens it, **Then** they see the existing free-text response experience.
2. **Given** a student submits a manual homework, **When** the submission completes, **Then** it is marked as submitted and awaits manual teacher review, with no automatic score.
3. **Given** existing homework created before this feature, **When** it is viewed after the change, **Then** it behaves as a manual homework with no loss of data.

---

### Edge Cases

- **Empty exercise**: A homework marked self-correcting but with zero questions cannot be saved.
- **Choice question with no correct option**: Cannot be saved until at least one correct option is marked.
- **Fill-in-the-blank with no accepted answer**: Cannot be saved until at least one accepted answer is provided.
- **Accent / capitalisation in fill-in-the-blank** (Spanish): Matching trims surrounding whitespace and is case-insensitive but accent-exact — "Fui" matches "fui", but "compre" does not match "compré" (FR-009).
- **Partial answers**: A student may submit with some questions unanswered; unanswered questions score 0.
- **Teacher edits an exercise after students submitted**: Existing submissions and their already-shown scores are preserved (not silently re-graded); the edited answer key applies only to future submissions.
- **Switching a homework between manual and self-correcting after submissions exist**: The teacher is warned, and existing submissions are preserved rather than discarded.
- **Resubmission**: A self-correcting exercise accepts a single submission and locks afterward; there is no retake (FR-020).
- **Mixed homework**: A single homework that combines auto-graded questions and free-text-needing-review is out of scope for this version (see Assumptions).

## Requirements *(mandatory)*

### Functional Requirements

**Dedicated authoring page (US1)**

- **FR-001**: The teacher MUST author and edit homework on a dedicated, full-page screen rather than a modal dialog.
- **FR-002**: The dedicated page MUST retain all existing homework fields: title, instructions, skill type, level, optional due date, and assignee selection.
- **FR-003**: Opening an existing homework for editing MUST pre-fill the dedicated page with its current values.
- **FR-004**: Saving MUST persist the homework and return the teacher to the homework list with the change reflected; cancelling MUST discard unsaved changes.

**Self-correcting exercise authoring (US2)**

- **FR-005**: The teacher MUST be able to designate a homework as a self-correcting exercise (versus a manual, free-text homework).
- **FR-006**: For a self-correcting exercise, the teacher MUST be able to add, edit, reorder, and remove an ordered list of one or more questions.
- **FR-007**: The system MUST support these auto-gradable question kinds: **single-correct choice** (the student picks exactly one option, shown as radio controls), **multiple-correct choice** (the student picks any number of options, shown as checkboxes), and **fill-in-the-blank** (free-text answer compared against an answer key). The teacher selects the kind per question.
- **FR-008**: For a choice question (single- or multiple-correct), the teacher MUST be able to define the selectable options and mark which option(s) are correct. A single-correct question requires exactly one correct option; a multiple-correct question requires at least one correct option.
- **FR-009**: For a fill-in-the-blank question, the teacher MUST be able to record one or more accepted answers for its single blank, with at least one required. A student response matches an accepted answer when, after trimming surrounding whitespace, it equals an accepted answer case-insensitively **but with exact accent matching** (e.g. `compré` does not match `compre`); the teacher covers acceptable variants by adding more accepted answers. A question contains exactly one blank — a verb drill is authored as several consecutive fill-in-the-blank questions.
- **FR-010**: The system MUST prevent saving a self-correcting exercise that has no questions, or any question lacking a valid answer key.
- **FR-011**: The answer key for a self-correcting exercise MUST never be exposed to students before they submit.

**Automatic feedback for students (US3)**

- **FR-012**: A student MUST take a self-correcting exercise on a dedicated full page (not the free-text dialog), presented with answer controls appropriate to each question kind: radio controls for single-correct choice, checkboxes for multiple-correct choice, and a text field for fill-in-the-blank.
- **FR-013**: On submission of a self-correcting exercise, the system MUST automatically grade each question against the answer key without teacher involvement, producing a per-question score between 0 and 1. Fill-in-the-blank and single-correct choice questions score 0 or 1. Multiple-correct choice questions are scored with **partial credit**: the fraction of options answered correctly (correct options selected plus incorrect options correctly left unselected) over the total number of options.
- **FR-014**: Immediately after submission, the student MUST see, for each question, whether their answer was correct (or its partial score) **and the correct answer when they got it wrong**, plus an overall score expressed as a **percentage** with a supporting raw count of fully-correct questions (e.g. "80% — 8 of 10 questions fully correct"). There is no pass/fail threshold.
- **FR-015**: A submitted self-correcting exercise MUST be recorded with a status indicating it was automatically graded, distinct from "awaiting manual review".
- **FR-016**: The teacher MUST be able to view each student's submitted answers and automatic score for a self-correcting exercise.
- **FR-020**: A self-correcting exercise MUST allow only a single submission per student; once submitted it becomes read-only (no retake or edit), and the stored result is final.

**Manual homework preservation (US4)**

- **FR-017**: Homework not designated as a self-correcting exercise MUST continue to use the existing free-text submission and manual review flow.
- **FR-018**: Homework created before this feature MUST continue to function as manual homework with no data loss.
- **FR-019**: The system MUST clearly distinguish, to both teacher and student, whether a homework is self-correcting or manual.

### Key Entities *(include if feature involves data)*

- **Homework Assignment**: The existing unit of homework (title, instructions, type, level, due date, assignees). Gains a designation of whether it is a self-correcting exercise or a manual homework.
- **Exercise Question**: Belongs to a homework; has an order position, a prompt, and a kind (single-correct choice, multiple-correct choice, or fill-in-the-blank). Manual homework has no questions.
- **Question Option**: For choice questions; a selectable label flagged as correct or not.
- **Accepted Answer**: For a fill-in-the-blank question's single blank; one of the answer-key strings a student response is matched against.
- **Student Submission**: The existing per-student homework state, extended to capture the student's per-question answers, the computed automatic score, and a status that distinguishes automatically-graded from awaiting-manual-review.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can create a complete self-correcting exercise with at least two questions and an answer key from start to save in under 5 minutes.
- **SC-002**: 100% of self-correcting exercise submissions return per-question feedback and an overall score to the student immediately, with zero teacher action required.
- **SC-003**: For self-correcting exercises, the teacher's manual review workload for those items drops to zero (all grading is automatic).
- **SC-004**: 100% of homework created before this feature continues to open and submit correctly as manual homework after the change (no regressions, no data loss).
- **SC-005**: Automatic grading agrees with the teacher's intended answer key in 100% of cases for the defined matching rules — a Spanish answer typed with the expected accents in any capitalisation is marked correct, and a partially-correct multiple-choice answer receives the expected fractional score.

## Assumptions

- **Binary homework format**: Each homework is either a self-correcting exercise (all questions auto-gradable) or a manual free-text homework. Mixing auto-graded questions and manual-review questions within one homework is out of scope for this version.
- **Question kinds in scope**: Single-correct choice (radio), multiple-correct choice (checkboxes, partial credit), and fill-in-the-blank (one blank, text match). Other kinds (matching, ordering, multi-blank prompts, audio response, etc.) are out of scope for this version.
- **Skill type vs. format are independent**: The existing skill-type tag (Escucha / Lectura / Escritura / Gramática) and level remain descriptive labels and are separate from whether a homework is self-correcting. The teacher chooses both.
- **Fill-in-the-blank matching rule**: A student answer matches an accepted answer when, after trimming surrounding whitespace, it equals an accepted answer case-insensitively and with exact accents (`compré` ≠ `compre`). The teacher supplies multiple accepted answers to cover acceptable variants. (Resolved in Clarifications.)
- **Resubmission rule**: A student submits a self-correcting exercise exactly once; it then locks read-only with the final score and feedback shown. (Resolved in Clarifications.)
- **Editing after submissions**: Editing an exercise's questions or answer key does not retroactively re-grade existing submissions; existing scores are preserved.
- **Reuses existing systems**: This feature extends the existing teacher panel homework tab, the per-student assignment/targeting model, the authenticated student learning view, and the existing homework submission lifecycle rather than introducing parallel systems.
- **Existing free-text path unchanged**: Manual homework keeps the current single free-text response and the existing PENDING → SUBMITTED → REVIEWED lifecycle.
