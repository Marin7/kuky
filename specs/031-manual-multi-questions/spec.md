# Feature Specification: Multi-Question Manual Homework

**Feature Branch**: `031-manual-multi-questions`

**Created**: 2026-08-10

**Status**: Draft

**Input**: User description: "Enhance manual homework types to allow multiple questions, each one having a separate free-text answer. I want for example in an audio homework to see the requirement, then the audio/video, then the questions with a small free-text allowing answers"

## Clarifications

### Session 2026-08-10

- Q: Should WRITE homework use the same multi compact free-text questions as AUDIO/READ? → A: WRITE keeps one large answer field; other MANUAL types (AUDIO, READ, …) use multiple compact free-text questions.
- Q: Are MANUAL presentation activities in scope? → A: Yes — include MANUAL presentation activities with the same multi free-text-question model as non-WRITE MANUAL homework.
- Q: Do compact per-question answer fields support rich-text formatting? → A: Plain text only on compact per-question fields; rich text remains for WRITE answers and teacher feedback.
- Q: What happens to answers when the teacher later removes a question? → A: Already-submitted answers keep a snapshot of removed questions for review; active/new work only shows the current question list.
- Q: May students submit with blank per-question answers? → A: No — every question must have non-empty text before submit.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher authors several free-text questions on a manual homework (Priority: P1)

Paula creates or edits a manual (non–auto-graded) homework that is not Writing — for example a listening homework with instructions and an audio source. Instead of a single catch-all answer box, she adds several short questions in order (e.g. “¿Qué palabra se repite?”, “¿Cuál es el tema principal?”). Each question is only a prompt for the student to answer in free text. She saves the homework and assigns it as usual. Writing (WRITE) homeworks keep a single large free-text answer and are not authored as a multi-question list.

**Why this priority**: Without teacher authoring of multiple questions, students cannot answer question-by-question. This is the foundation of the feature.

**Independent Test**: Create a MANUAL AUDIO homework with instructions, an audio source, and at least two free-text questions; reopen the editor and confirm the questions appear in the same order with their prompts intact.

**Acceptance Scenarios**:

1. **Given** the teacher is creating or editing a MANUAL homework, **When** they add two or more free-text questions with prompts and save, **Then** the homework stores those questions in the order defined and remains MANUAL (not auto-graded).
2. **Given** a MANUAL homework already has free-text questions, **When** the teacher reorders, edits, or removes a question and saves, **Then** the published student view reflects the updated question list.
3. **Given** the teacher is authoring a MANUAL AUDIO homework, **When** they set instructions, attach audio, and add questions, **Then** all three parts are retained together on that homework.

---

### User Story 2 - Student answers each question with a separate free-text reply (Priority: P1)

A student opens a manual homework (e.g. listening). They see the requirement/instructions first, then the audio (or other media when present), then the list of questions. Under each question is a compact free-text answer field. They fill in answers for the questions, submit once for the whole homework, and can revisit their answers afterward (read-only once reviewed, editable while still awaiting review — consistent with today’s manual submission rules).

**Why this priority**: This is the student-facing outcome the request describes; it delivers the pedagogical layout (requirement → media → questions).

**Independent Test**: Open a MANUAL AUDIO homework that has instructions, audio, and two questions; confirm page order; submit different text for each question; reopen and confirm each answer is shown under its own question.

**Acceptance Scenarios**:

1. **Given** a MANUAL AUDIO homework with instructions, audio, and multiple free-text questions, **When** the student opens it, **Then** they see instructions, then audio, then the questions each with its own answer field — not a single shared answer box for the whole homework.
2. **Given** a student is completing such a homework, **When** they enter different non-empty free-text answers for two questions and submit, **Then** each answer is stored against its question and the homework is marked submitted.
3. **Given** a student tries to submit with any question left blank, **When** they submit, **Then** the system blocks the submit and asks them to answer every question.
4. **Given** a MANUAL reading homework with multiple free-text questions, **When** the student opens it, **Then** they see instructions (and the reading passage) followed by the per-question compact answer fields.
5. **Given** a MANUAL Writing (WRITE) homework, **When** the student opens it, **Then** they see the existing single large free-text answer experience (not a multi-question compact list).
6. **Given** a submission has been reviewed by the teacher, **When** the student opens the homework again, **Then** all question answers (or the single Writing answer) are read-only, and any teacher feedback for that submission remains visible as today.

---

### User Story 3 - Teacher reviews per-question answers on a manual submission (Priority: P2)

When reviewing a submitted manual homework, the teacher sees each question with the student’s free-text answer underneath it (and instructions/media context as helpful). They leave feedback on the submission and mark it reviewed, as they do today for writing/manual work.

**Why this priority**: Review must remain usable once answers are split across questions; without this, multi-question submissions cannot be graded in practice. It builds on Stories 1–2.

**Independent Test**: Submit a multi-question MANUAL homework as a student; open it from the teacher review queue; confirm each question+answer pair is visible; save feedback and mark reviewed.

**Acceptance Scenarios**:

1. **Given** a student has submitted answers to multiple free-text questions on a MANUAL homework, **When** the teacher opens that submission for review, **Then** they see every question prompt paired with that student’s answer for that question.
2. **Given** the teacher is reviewing such a submission, **When** they save feedback and mark it reviewed, **Then** the submission becomes reviewed and the student can see the feedback with their per-question answers.

---

### Edge Cases

- What if the teacher tries to save a non-WRITE MANUAL homework with no free-text questions? The system MUST require at least one free-text question. WRITE homeworks keep the single large-answer model and MUST NOT require a multi-question list.
- What if the student leaves one or more question answers empty on submit? The system MUST reject the submit and require non-empty text for every question on a multi-question MANUAL homework/activity. (WRITE keeps its existing single-answer rules.)
- What if a MANUAL homework that already existed with a single overall answer is opened after this change? Existing submissions and homeworks MUST remain usable: legacy single answers are shown as the answer to the homework’s (migrated) single free-text question, and new submissions use the per-question model.
- What if the teacher removes a question after students have already answered it? For submissions already submitted, the system MUST keep a snapshot so review still shows that question prompt with the student’s answer. Students who have not yet submitted (and new attempts) MUST only see the homework’s current question list.
- What about EXERCISE (auto-graded) homeworks? Unchanged — this feature applies only to MANUAL free-text questions, not to auto-graded question kinds.
- What about presentation activities that use MANUAL format? Confirmed in scope: MANUAL activities use the same multi free-text-question pattern as non-WRITE MANUAL homework.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow the teacher to define an ordered list of free-text questions on MANUAL homeworks that are not Writing (prompts only — no auto-graded options or answer keys). Writing (WRITE) homeworks MUST keep a single large free-text answer and MUST NOT use the multi-question compact list.
- **FR-002**: System MUST require at least one free-text question on a non-WRITE MANUAL homework before it can be saved.
- **FR-003**: System MUST present non-WRITE MANUAL student homework in this order when content exists: (1) title/requirement/instructions, (2) media such as audio when the homework includes it, (3) the free-text questions each with its own compact answer field. WRITE homeworks keep the existing single large-answer layout.
- **FR-004**: System MUST let the student enter a separate free-text answer for each question on a multi-question MANUAL homework/activity and submit all answers together as one submission.
- **FR-004a**: System MUST require non-empty text for every question before a multi-question MANUAL submission is accepted; empty answers MUST block submit with a clear validation message.
- **FR-005**: System MUST persist each answer against its question so answers remain correctly paired after reload, review, and later visits.
- **FR-005a**: System MUST retain a per-submission snapshot of question prompts (and their answers) for already-submitted work, so teacher/student review still shows removed questions; unfinished or new work MUST follow only the current question list.
- **FR-006**: System MUST show the teacher, during review of a multi-question MANUAL submission, each question paired with that student’s answer for that question (including snapshot prompts for questions later removed). WRITE reviews keep the existing single-answer + feedback view.
- **FR-007**: System MUST preserve the existing MANUAL lifecycle (pending → submitted → reviewed), including when the student may still edit vs when answers become read-only, and whole-submission teacher feedback.
- **FR-008**: System MUST keep per-question student answer fields compact (“small” plain-text areas suited to short replies) on non-WRITE MANUAL homeworks and MANUAL activities — no color/highlight/strikethrough formatting on those fields. WRITE continues to use the large rich-text writing editor; teacher feedback remains rich-text as today.
- **FR-009**: System MUST apply the same multi free-text-question model to MANUAL presentation activities (parity with non-WRITE MANUAL homework). Activity student layout follows the same order: instructions (PDF), then free-text questions with compact answer fields.
- **FR-010**: System MUST leave EXERCISE (auto-graded) authoring, taking, and grading behavior unchanged.
- **FR-011**: System MUST migrate existing non-WRITE MANUAL homeworks/activities that had a single overall answer into the multi-question model so historical submissions remain viewable and new work uses per-question answers. WRITE homeworks and their submissions MUST remain on the single large-answer model (no forced multi-question migration).

### Key Entities

- **Manual free-text question**: A teacher-authored prompt belonging to a MANUAL homework or MANUAL activity; ordered; has no correct answer key; expects a free-text student reply.
- **Per-question free-text answer**: The student’s free-text response to one manual question within a single submission of that homework/activity.
- **Manual submission**: The student’s overall attempt at a MANUAL homework/activity (status and teacher feedback remain at submission level; answers are nested per question).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can author a MANUAL listening homework with instructions, audio, and at least two free-text questions and confirm the saved structure in under 5 minutes without needing a workaround.
- **SC-002**: A student opening that homework always sees instructions, then audio, then questions — verified in a walkthrough with no misplaced single answer box above the media.
- **SC-003**: In 100% of test submissions with two or more questions, each stored answer is shown under the correct question for both student and teacher review views.
- **SC-004**: Teachers can complete review of a multi-question MANUAL submission (see all answers, leave feedback, mark reviewed) in one continuous flow without exporting or copying answers elsewhere.
- **SC-005**: Existing MANUAL homeworks with a prior single answer remain openable by student and teacher after the change, with the historical answer still visible in context of the migrated question list.

## Assumptions

- “Manual homework types” in the multi-question sense means non-WRITE MANUAL homeworks (AUDIO, READ, GRAMMAR, and any other non-Writing MANUAL type). WRITE stays a single large free-text answer.
- “Small free-text” means a compact per-question **plain-text** answer UI suited to short replies on multi-question MANUAL homeworks/activities (no rich-text toolbar on those fields). WRITE keeps the large rich-text editor; teacher feedback stays rich-text.
- Teacher feedback stays at the whole-submission level (one feedback block per submission), not separate feedback per question, matching today’s review model.
- Video as a new media type is out of scope; when the homework already has playable media (audio today), it appears between instructions and questions. Future video sources would follow the same layout slot.
- MANUAL presentation activities are explicitly in scope and follow the same multi-question free-text rules as non-WRITE MANUAL homework.
- Every question on a multi-question MANUAL homework/activity MUST be answered (non-empty) before submit — stricter than today’s optional single free-text response.
- Minimum of one free-text question is required for non-WRITE MANUAL homeworks and MANUAL activities going forward. WRITE is exempt.
