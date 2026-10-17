# Feature Specification: Sentence-Starter Answer (new question type)

**Feature Branch**: `057-sentence-starter-answer`

**Created**: 2026-10-11

**Status**: Draft

**Input**: User description: "Add another type of exercise (homework & activity) which consists of a free answer, usually short, like a sentence, but has to begin with something (a word, a group of words). Example: There's a text or a photo about something, the exercise is like: \"porque...\" \"para...\" \"por...\""

## Clarifications

### Session 2026-10-11

- Q: Should a homework sentence-starter question support attaching a photo? → A: No — text prompt only in homework for v1; photo-based exercises use unit page activities.
- Q: How is a question with several starters graded? → A: One teacher % for the whole question (all sentences judged together), exactly like FREE_TEXT; annotations/note point at individual sentences.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Student completes sentences from given starters (Priority: P1)

A student opens an assigned homework (or reaches a unit page activity) containing a "completa la frase" question. They read the stimulus — a short text, or the unit page with its text or photo — and then see one line per starter the teacher set, e.g. *porque…*, *para…*, *por…*. Each line shows the starter as fixed, non-editable text followed by an input where the student writes the rest of the sentence. They fill in every line and submit. Because the answers are free text, the submission waits for the teacher's correction; the student sees their sentences (each beginning with its starter) as submitted.

**Why this priority**: This is the core learning interaction; without it the question type delivers nothing.

**Independent Test**: Seed a homework with one sentence-starter question (prompt text + 3 starters), assign it to a student, fill all three continuations, submit, and verify the submission is stored with each sentence beginning with its starter and awaits correction.

**Acceptance Scenarios**:

1. **Given** an assigned, unsubmitted homework with a sentence-starter question, **When** the student opens it, **Then** they see the question's prompt and, for each starter in the teacher's order, the starter as fixed text immediately followed by an empty input for the continuation.
2. **Given** the student is answering, **When** they type in a line, **Then** only the continuation is editable — the starter cannot be removed or altered.
3. **Given** at least one line's continuation is empty (or only spaces), **When** the student submits, **Then** submission is refused with the existing "answer all questions" message and nothing is saved.
4. **Given** every line has a continuation, **When** the student submits, **Then** the submission is accepted and the stored answer for each line is the starter followed by the continuation, exactly as the student saw it.
5. **Given** the student left the homework before submitting, **When** they come back, **Then** the inputs are empty again, as for free-text questions today (no device draft).

---

### User Story 2 - Teacher authors a sentence-starter question (Priority: P1)

In the homework editor — and in the unit page activity editor — the teacher adds a question of the new type. They write the prompt (e.g. "Lee el texto y explica por qué Ana se va a Madrid") and add one or more starters (a word or a group of words such as *porque*, *para*, *por*, *Si yo fuera tú,*), reorderable and removable. They can preview the question as the student sees it before saving. It can sit alongside any other question type.

**Why this priority**: Students cannot do the exercise unless the teacher can create it; both stories form the MVP.

**Independent Test**: Create a homework with one sentence-starter question with 3 starters, save, reopen, and verify the prompt and starters (text and order) are preserved exactly; repeat for a unit page activity.

**Acceptance Scenarios**:

1. **Given** the teacher is editing a homework or a page activity, **When** they add a question, **Then** "completa la frase" is offered alongside the existing types.
2. **Given** a new sentence-starter question, **When** the teacher adds, edits, reorders or removes starters, **Then** the preview reflects the change immediately.
3. **Given** the question has no starter, or a starter is empty, **When** the teacher saves, **Then** saving is refused with a clear message.
4. **Given** a saved question, **When** the teacher reopens it, **Then** the prompt and the starters are shown exactly as saved, in the same order.
5. **Given** a student has already submitted, **When** the teacher edits the question's starters, **Then** that student's submission is still shown and corrected against the version they answered.

---

### User Story 3 - Teacher corrects and the student sees the result (Priority: P1)

The teacher opens the submission in the existing correction view. They see each starter with the student's continuation, can annotate and leave a note as for other free answers, and assign a 0–100% for the question. When the correction is finalised the student gets the usual unseen dot and sees their sentences, the teacher's % and feedback, and — if the teacher provided one — a model answer for each starter.

**Why this priority**: Without correction the exercise has no feedback loop; it reuses the existing manual-correction flow, so it is cheap and required for the MVP.

**Independent Test**: With a submitted sentence-starter answer, finalize a 75% correction with a note; verify the student's graded result shows the sentences, 75%, the note, and model answers where given.

**Acceptance Scenarios**:

1. **Given** a submitted sentence-starter question, **When** the teacher opens the submission, **Then** each starter is shown clearly distinguished from the student's continuation, in the teacher's starter order.
2. **Given** the correction view, **When** the teacher assigns a % to the question, **Then** it is treated exactly like a FREE_TEXT question in the homework's lifecycle and combined % (partial saves allowed, finalised only when every manual question has a %).
3. **Given** a finalised correction, **When** the student opens the graded result, **Then** they see each sentence, the question's %, any annotation/note, and, under each sentence, the model answer if one was set.
4. **Given** the question is a unit page activity, **When** the student submits it, **Then** it counts as submitted for unlocking the following pages and end-of-unit quizzes, and appears in the teacher's activity review queue like other manually graded activities.

---

### Edge Cases

- **Starter punctuation**: the teacher may write the starter with or without trailing ellipsis ("porque" / "porque..."). Any trailing "..." / "…" is shown only as a visual cue while answering and is not part of the stored sentence; the starter and continuation are joined with a single space unless the continuation begins with punctuation.
- **Student retypes the starter**: if the continuation begins with the starter again ("porque porque…"), it is stored as typed; the teacher sees and corrects it. The system does not try to de-duplicate.
- **Capitalisation/accents**: the starter is reproduced exactly as the teacher wrote it; no case or accent normalisation.
- **Long answers**: each continuation is limited (see FR-009) and the student is told when they reach the limit; usually a sentence, but more is allowed up to the limit.
- **Many starters**: the editor caps the number of starters per question (FR-003).
- **Phone width**: the starter and its input stay visually connected (the starter may sit on its own line above a full-width input) without horizontal scrolling.
- **Teacher edits after submissions**: starters added, removed or reworded after a student submitted do not change that student's stored sentences or their correction (FR-012).
- **Quiz**: the type is not offered in the quiz editor in v1.

## Requirements *(mandatory)*

### Functional Requirements

**Authoring (teacher)**

- **FR-001**: The teacher MUST be able to add a "completa la frase" (sentence-starter) question to a homework and to a unit page activity, mixed freely with all existing question types.
- **FR-002**: A sentence-starter question MUST consist of a prompt (the instruction and/or the stimulus text, like any other question's prompt) and an ordered list of starters; each starter is a word or group of words.
- **FR-003**: The question MUST have at least 1 and at most 10 starters; each starter MUST be non-empty and at most 80 characters. Violations are refused on save with a clear message.
- **FR-004**: For each starter the teacher MAY provide a model answer (a full example sentence), shown to the student only after the correction is finalised.
- **FR-005**: The teacher MUST be able to add, edit, reorder and remove starters, and preview the question as the student will see it before saving.
- **FR-006**: The stimulus the student answers about is the question's prompt text (homework) or the unit page shown above the activity (activities). No image can be attached to a homework question in v1; photo-based exercises are authored as unit page activities, where the page carries the photo.

**Answering (student)**

- **FR-007**: For each starter, the student MUST see the starter as fixed, non-editable text immediately followed by an input for the continuation, in the teacher's order.
- **FR-008**: Every continuation MUST be non-empty (ignoring whitespace) for the homework or activity to be submitted, consistent with the existing rule that every question must be answered.
- **FR-009**: Each continuation MUST be plain text limited to 500 characters, and the whole answer (all composed sentences) to 2,000 characters — the limit teacher annotation already enforces; over-limit submissions are refused.
- **FR-010**: The stored answer for each line MUST be the full sentence: the starter (without a trailing ellipsis) followed by the continuation, so the submitted sentence always begins with the starter.
- **FR-011**: Unsubmitted continuations are not kept as a draft, consistent with free-text questions today (only Writing homework has a device draft).

**Correction & results**

- **FR-012**: The question MUST behave as a manually graded question exactly like FREE_TEXT: it makes a homework MANUAL/MIXED in composition, keeps the submission SUBMITTED until the teacher assigns a 0–100% to it, participates in the combined % as one question, supports annotations and the ≤500-character note, and triggers the same unseen dots for the student when finalised. Submissions are always shown and corrected against the version of the question the student answered.
- **FR-013**: The teacher MUST give one % for the whole question (not one per starter); in the correction view each starter MUST be visually distinguished from the student's continuation.
- **FR-014**: After finalisation the student MUST see their sentences, the question's %, the teacher's feedback, and the model answer under each sentence where one was set. Model answers MUST NOT be visible before finalisation.
- **FR-015**: As a unit page activity, a submitted sentence-starter question MUST count as submitted for the existing page and end-of-unit quiz unlocking, and MUST appear in the teacher's activity review (Unidades dot) like other manually graded activities.

**Wider integration**

- **FR-016**: The question type is NOT offered in quizzes in v1 and does NOT appear in "Errores frecuentes" (which covers auto-graded questions only).
- **FR-017**: All new student-facing and teacher-facing labels MUST be provided in every site language (es/en/ro); validation messages are in Spanish like existing ones.

### Key Entities

- **Sentence-starter question**: a question within a homework or page activity. Attributes: prompt, ordered list of starters, each with an optional model answer.
- **Student answer**: one continuation per starter, stored as the full sentence (starter + continuation); corrected as a whole with one teacher % plus optional annotations and note.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The teacher can author a sentence-starter question with a short text and 3 starters in under 2 minutes.
- **SC-002**: 100% of submitted sentence-starter answers begin with their starter exactly as the teacher wrote it (without a trailing ellipsis).
- **SC-003**: A student can answer a 3-starter question on a phone-width screen without horizontal scrolling and without being able to alter a starter.
- **SC-004**: Correcting a sentence-starter question takes the teacher no more steps than correcting an existing free-text question.
- **SC-005**: Editing a question after students have submitted changes none of their stored sentences or corrections.

## Assumptions

- This is a new **question type** inside the existing homework/activity question model (like FREE_TEXT or fill-in-the-blanks), not a new kind of homework.
- Grading is manual: a free sentence cannot be judged automatically, so the existing FREE_TEXT correction flow (teacher %, annotations, note, finalise) is reused unchanged.
- The "begins with" requirement is guaranteed by the answering UI (the starter is fixed text the student cannot edit) rather than by checking what the student typed.
- One % per question; per-starter scoring is out of scope. A teacher who wants separate scores can create separate questions.
- Quizzes are out of scope for v1 (the request names homework and activities).
- Reuses the existing assignment, submission lifecycle, frozen snapshot at submission, labels, notifications/unseen dots and new-homework email behaviour.
