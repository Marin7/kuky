# Feature Specification: Multiple Correct Blank Answers

**Feature Branch**: `034-multi-correct-blanks`

**Created**: 2026-08-10

**Status**: Draft

**Input**: User description: "Some homeworks which require fill in the gaps or select from the bank can have multiple correct responses"

## Clarifications

### Session 2026-08-10

- Q: When a blank has multiple accepted answers, when should the full accepted set be shown in results/review? → A: Always after grading — for both correct and incorrect blanks
- Q: For word-bank questions, may the bank include pure distractors (items never correct for any blank)? → A: Yes — bank may include items that are not correct for any blank (true distractors)
- Q: What is the maximum number of accepted answers (typed) or correct bank items allowed per blank? → A: 10 per blank
- Q: With distractors and alternates allowed, what is the maximum word-bank size (total items in the bank for one question)? → A: 30 total
- Q: Should table-fill blanks get the same new rules (always-show-all feedback; max 10 accepted answers), or stay as today aside from no-regression? → A: No — table-fill unchanged except must not regress
- Q: May the same bank word be correct for more than one blank (e.g. “___ and ___” either order)? → A: Yes — the same bank item may appear in multiple blanks’ correct sets; grading remains any-of per blank (order-independent when both blanks share the same accepted set)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Author alternate answers for typed fill-in gaps (Priority: P1)

The teacher authors a fill-in-the-gaps question (single blank or multi-blank passage) where a blank may have more than one fully correct answer — for example synonyms, interchangeable articles, or acceptable spelling variants that should all score as correct. The student types one answer; matching any accepted response grades that blank as correct.

**Why this priority**: Typed gap-fill is the most common drill for Spanish homework; without alternate accepted answers, teachers either mark valid student answers wrong or avoid auto-grading. This is the baseline capability students and the teacher rely on daily.

**Independent Test**: Teacher creates a self-correcting homework with a multi-blank (or single-blank) gap question, enters two or more accepted answers for at least one blank, assigns it; a student submits one of the non-primary accepted answers and that blank is marked correct with full credit for the blank.

**Acceptance Scenarios**:

1. **Given** the teacher is authoring a typed fill-in-the-gaps question, **When** they edit a blank’s answer key, **Then** they can add, edit, and remove multiple accepted answers (up to 10), and at least one accepted answer is required per blank to save.
2. **Given** a blank has accepted answers `compré` and `he comprado`, **When** a student submits either value (under the existing matching rules: trim, case-insensitive, accent-exact), **Then** that blank is graded correct.
3. **Given** a blank has multiple accepted answers and the student submits a value that matches none of them, **When** grading completes, **Then** the blank is graded incorrect and feedback reveals the accepted answer(s) for that blank.
4. **Given** a blank has only one accepted answer, **When** the student submits that answer, **Then** grading behaves as today’s single-answer blanks (no change in outcome).

---

### User Story 2 - Author multiple correct placements for word-bank blanks (Priority: P1)

The teacher authors a select-from-the-bank (drag-and-drop) passage where a blank may accept more than one bank item as correct — for example two interchangeable words both present in the bank. The student places one bank item into the blank; placing any of that blank’s designated correct items grades the blank as correct.

**Why this priority**: Word-bank exercises today allow only one correct item per blank (positional 1:1). That blocks real drills where several bank words are valid in the same gap. Same priority band as typed alternates because both are the user’s stated need.

**Independent Test**: Teacher authors a word-bank question with at least one blank that accepts two different bank items, bank size at least as large as needed for alternatives; student places either accepted item into that blank, submits, and the blank is marked correct.

**Acceptance Scenarios**:

1. **Given** the teacher is authoring a word-bank question, **When** they set the answer key for a blank, **Then** they can designate one or more bank items as correct for that blank (at least one, at most 10).
2. **Given** a blank accepts bank items A and B, **When** the student places A or B into that blank and submits, **Then** that blank is graded correct.
3. **Given** a blank accepts A and B, **When** the student places a different bank item C into that blank and submits, **Then** that blank is graded incorrect and feedback reveals the accepted item(s) for that blank.
4. **Given** the teacher tries to save a word-bank question where a blank has no correct bank item, a designated correct item is not in the bank, or the bank has more than 30 items, **When** they save, **Then** the system prevents saving and explains what is missing.
5. **Given** an existing word-bank question authored under the previous “one bank item per blank in order” model, **When** a student takes it after this feature ships, **Then** grading still treats each blank’s original single correct item as the only accepted placement (no silent score changes).
6. **Given** the teacher adds a bank item that is not designated correct for any blank, **When** a student places that distractor into a blank and submits, **Then** that blank is graded incorrect.

---

### User Story 3 - Student feedback shows all accepted alternatives (Priority: P2)

After submission, for every graded blank that has multiple accepted answers — whether the student was correct or incorrect — the student and teacher see every accepted response for that blank, so learners understand the full set of valid alternatives (including synonyms they did not use).

**Why this priority**: Multiple correct answers are only pedagogically useful if feedback teaches the full accepted set; slightly lower than authoring/grading because grading correctness alone already delivers the core value.

**Independent Test**: Submit both a correct and an incorrect answer on blanks that each have three accepted answers (typed or word-bank); open the result view and confirm all three accepted responses are shown for each blank.

**Acceptance Scenarios**:

1. **Given** a typed blank with multiple accepted answers was answered incorrectly, **When** the student views results, **Then** all accepted answers for that blank are shown.
2. **Given** a typed blank with multiple accepted answers was answered correctly (matching any one accepted answer), **When** the student views results, **Then** all accepted answers for that blank are still shown.
3. **Given** a word-bank blank with multiple correct bank items was answered (correctly or incorrectly), **When** the student views results, **Then** all accepted bank item labels for that blank are shown.
4. **Given** the teacher reviews a graded submission, **When** they inspect a blank with multiple accepted answers, **Then** they see the same full accepted set the student sees.

---

### Edge Cases

- **Empty alternate**: Blank accepted-answer fields that are empty after trim cannot be saved; removing the last accepted answer for a blank is blocked until another is added (or save is rejected). A blank cannot have more than 10 accepted answers (typed) or 10 correct bank items.
- **Duplicate accepted answers**: Duplicate typed strings (after the same normalize rules used for matching) or the same bank item listed twice for one blank are treated as a single accepted answer — they do not change scoring.
- **Same bank item on two blanks**: Allowed — e.g. both blanks in “___ and ___” may accept the same two bank words so either placement order scores fully correct. Each bank chip still occupies at most one blank while the student answers.
- **Bank larger than blanks**: When some blanks have alternate correct items, or the teacher adds pure distractors (bank items never correct for any blank), the bank MAY contain more items than blanks, up to 30 items total. Unused items remain in the bank and are never required. Each bank item still occupies at most one blank at a time while the student works.
- **Bank smaller than blanks**: Still invalid — there must be enough distinct correct designations to cover every blank (each blank ≥1 correct item; items not shared across blanks).
- **Partial credit**: Unchanged — each blank scores 0 or 1; the question score remains the average across blanks. Matching any one accepted answer yields full credit for that blank (not partial credit among alternatives).
- **Matching rules for typed answers**: Unchanged — trim surrounding whitespace, case-insensitive, accent-exact; teachers add further accepted answers for additional leniency.
- **Presentation activities**: Activities that reuse the same gap-fill and word-bank question model MUST follow the same multiple-accepted-answer rules and feedback behaviour.
- **Out of scope kinds**: Matching pairs, choice questions, true/false, and table-fill behaviour changes are out of scope (table-fill must not regress; it does not adopt this feature’s always-show-all feedback or max-10 cap).
- **Teacher edits after submissions**: Existing submissions and shown scores are preserved; the updated answer key applies only to future submissions.
- **Feedback with multiple accepted answers**: After grading, every fill-in-gaps or word-bank blank that has more than one accepted answer shows the full accepted set in results/review, including when the student answered correctly (table-fill excluded).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: For typed fill-in-the-gaps questions (single-blank and multi-blank passage), the teacher MUST be able to record one or more accepted answers per blank (maximum 10), with at least one required to save.
- **FR-002**: A typed blank MUST be graded correct when the student’s response matches any of that blank’s accepted answers under the existing matching rules (trim, case-insensitive, accent-exact).
- **FR-003**: For word-bank (select-from-the-bank) questions, the teacher MUST be able to designate one or more bank items as correct for each blank (maximum 10 correct items per blank), with at least one required per blank to save.
- **FR-004**: A word-bank blank MUST be graded correct when the student places any bank item that the teacher designated as correct for that blank.
- **FR-005**: The system MUST reject authoring configurations where a blank has no accepted answer / correct bank item, where a blank exceeds 10 accepted answers or 10 correct bank items, where the word bank exceeds 30 items, or where a designated correct bank item is missing from the bank. The same bank item MAY be marked correct for more than one blank.
- **FR-006**: The word bank MAY contain more items than blanks (maximum 30 bank items per question). Extra items MAY be (a) alternate correct items for a blank and/or (b) pure distractors that are not designated correct for any blank. Placement rules for the student remain: each bank item in at most one blank at a time; blanks may be left empty (empty scores incorrect).
- **FR-007**: After grading, feedback for every graded blank that has multiple accepted answers MUST show all accepted answers (typed strings or bank item labels) for that blank — whether the student’s response was correct or incorrect — to both the student and the teacher reviewing the submission. Blanks with a single accepted answer MAY continue to show that one answer as today.
- **FR-008**: Existing word-bank questions that previously used one correct item per blank in bank order MUST continue to grade equivalently (each blank accepts its original single correct item) without requiring the teacher to re-author them.
- **FR-009**: Presentation activities that use the same fill-in-gaps and word-bank question kinds MUST apply FR-001 through FR-007 the same way as homework exercises.
- **FR-010**: Question kinds outside fill-in-gaps and word-bank (choice, true/false, matching, table fill) MUST NOT regress. Table-fill blanks keep today’s behaviour for accepted answers and feedback; this feature does NOT require always-show-all feedback or a new max-10 cap on table-fill cells.

### Key Entities

- **Blank answer key**: Per blank, the set of responses that count as fully correct — either accepted typed strings or designated correct bank items.
- **Word-bank item**: A placeable word or phrase in the bank; identity is stable for grading and placement; label is what students see.
- **Word-bank question**: A passage with blanks plus a bank; answer key maps each blank to one or more bank items rather than a single positional pairing.
- **Graded blank result**: Per-blank correctness plus, for fill-in-gaps and word-bank blanks with multiple accepted answers, the full accepted set shown after grading (correct or incorrect).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a scripted authoring check, a teacher can add a second accepted answer to a typed gap blank and a second correct bank item to a word-bank blank in under 1 minute without leaving the question editor.
- **SC-002**: In verification trials, 100% of student submissions that use any non-primary accepted answer (typed or word-bank) for a blank are scored correct for that blank.
- **SC-003**: In verification trials, 100% of graded blanks that have multiple accepted answers (correct or incorrect) show every accepted answer in the result/review view.
- **SC-004**: 100% of previously authored single-correct word-bank questions continue to produce the same per-blank correct/incorrect outcomes for the same placements after the feature ships (compatibility check on a representative sample).
- **SC-005**: Teachers can complete a full author → assign → student take → review cycle for a homework that uses both multi-answer typed gaps and multi-correct word-bank blanks without needing a manual override to fix a “wrong but acceptable” answer.

## Assumptions

- “Fill in the gaps” means typed single-blank and multi-blank passage questions; “select from the bank” means drag-and-drop word-bank questions.
- “Multiple correct responses” means any-of matching: the student provides one response per blank; any accepted alternative yields full credit for that blank (not “select all correct” and not partial credit among alternatives).
- Typed fill-in-the-gaps already allow multiple accepted answers in product intent; this feature treats that as a required, first-class capability and extends the same pedagogical rule to word-bank blanks.
- Bank size may exceed blank count so alternatives and pure distractors can coexist; bank items remain single-use while answering.
- A bank item may be the correct answer for more than one blank in the same question (supports order-independent gaps such as “___ and ___”).
- Accent-exact, case-insensitive typed matching stays as today; teachers encode further leniency via additional accepted answers.
- A blank may have at most 10 accepted typed answers or 10 correct bank items.
- A word-bank question may have at most 30 bank items.
- Matching, choice, true/false, and table-fill behaviour changes are out of scope except for no-regression on table-fill (and the other kinds).
- Presentation activities that share the exercise question model inherit the same rules for fill-in-gaps and word-bank only.
- Existing submissions are not re-graded when the teacher later adds or removes accepted answers.
