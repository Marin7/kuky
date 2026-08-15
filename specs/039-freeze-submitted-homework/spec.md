# Feature Specification: Freeze Submitted Homework

**Feature Branch**: `039-freeze-submitted-homework`

**Created**: 2026-08-15

**Status**: Draft

**Input**: User description: "When editing an already existing homework, I don't want to update anything to already submitted homeworks. They submissions should stay exactly as they are."

## Clarifications

### Session 2026-08-15

- Q: Should presentation activities freeze the same way as homework? → A: Homework only; activity edits can still change already-submitted activities
- Q: What if the teacher saves an edit while a student has the homework open but has not submitted? → A: Unsubmitted students always take the current homework; an open take must continue on the new version (in-progress answers are not frozen)
- Q: How does that student learn the homework changed while they were answering? → A: Show a message that the homework was updated and they must continue on the new version
- Q: When is that message shown? → A: On their next reload, return to the homework, or submit attempt
- Q: What happens to answers already typed if they had not submitted? → A: Discard in-progress answers; the student starts the current homework fresh

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher edits homework without touching existing submissions (Priority: P1)

The teacher opens an existing homework that some students have already submitted, changes questions, options, the answer key, instructions, media, or other homework content, and saves. Those students’ submissions are left exactly as they were at submit time: same answers, same scores, same questions they answered, same per-question right/wrong, same teacher review already given. Anyone who has not submitted yet takes the live edited homework, including a student who had it open when the teacher saved. Presentation activities are unchanged by this feature.

**Why this priority**: This is the whole request. Today an edit can rewrite the live questions that submitted work is displayed against, so a student or teacher reopening a past submission can see different prompts, different “correct” answers, or missing choices even when the overall percentage did not change. Freezing submitted work is the only way the teacher can safely correct a homework after students have turned it in.

**Independent Test**: Assign a homework to two students; one submits. Teacher then changes a prompt, swaps the correct option, and deletes another question. Reopen the submitted student’s work: it still shows the original questions, their original answers, and the original score and breakdown. The second student, who has not submitted, sees the edited homework.

**Acceptance Scenarios**:

1. **Given** at least one student has submitted a homework, **When** the teacher edits that homework’s questions, options, answer key, instructions, media, title, or skill/level and saves, **Then** every already-submitted submission keeps the same student answers, overall score, per-question scores, and per-item right/wrong that were recorded at submit (or at last teacher review, for manual percentages).
2. **Given** a student has submitted, **When** the teacher later deletes or reorders questions, or deletes options the student had selected, **Then** that submission still shows those questions, options, and selections exactly as at submit — nothing in the submitted work is removed, blanked, or relabelled by the edit.
3. **Given** a student has submitted and the teacher has already left review (percentages, annotations, or a short note), **When** the teacher later edits the homework, **Then** that review content is unchanged.
4. **Given** a homework has existing submissions, **When** the teacher saves an edit, **Then** the save succeeds without requiring those submissions to be discarded, re-graded, or re-taken.

---

### User Story 2 - Submitted student still sees the homework they actually took (Priority: P1)

A student who has already submitted opens that homework again (result or reviewed view). They see the homework as it was when they submitted — not the teacher’s later edits. Their answers and scores match what they were shown at submit time.

**Why this priority**: The freeze is only real if the student-facing result is frozen, not only a hidden stored percentage. This is independently testable from the student learning area.

**Independent Test**: Student submits an auto-graded homework, notes the prompts, chosen options, and score. Teacher then changes prompts and the answer key. Student reopens the homework: same prompts, same choices, same score and per-question feedback as immediately after submit.

**Acceptance Scenarios**:

1. **Given** a student has submitted (awaiting teacher or already graded/reviewed), **When** they open that homework, **Then** they see the questions, options, media, and instructions they submitted against — not the current live homework if it has been edited since.
2. **Given** an auto-graded or mixed homework was submitted and later the answer key was changed, **When** the student views results, **Then** overall percentage, fully-correct count, and each item’s right/wrong still match the original grading (they are not re-checked against the new key).
3. **Given** a numbered opción única (or any structured question) was submitted, **When** the teacher later changes markers, options, or accepted answers, **Then** the student’s result still shows the items they answered and the original breakdown for those items.

---

### User Story 3 - Teacher reviews a past submission against the original homework (Priority: P2)

The teacher opens a student’s already-submitted work to review or inspect results. That view shows the homework as that student submitted it, even if the live homework has been rewritten. The teacher can still grade remaining manual answers, edit allowed review fields, and leave feedback on that frozen copy.

**Why this priority**: Review is how the teacher uses submitted work. It must match what the student saw. Slightly below P1 because the freeze already delivers value if the student result is correct; review must follow so the teacher is not grading against different questions.

**Independent Test**: Student submits mixed or manual homework. Teacher edits the homework (changes a free-text prompt and an auto-graded option). Teacher opens that student’s submission: original prompts and answers are shown; teacher can still enter a percentage on the original free-text answer.

**Acceptance Scenarios**:

1. **Given** a submitted homework whose live homework has since been edited, **When** the teacher opens that student’s submission (exercise result or manual/mixed review), **Then** they see the original questions, student answers, and scores — not the live edited homework.
2. **Given** a submission still awaiting teacher percentages, **When** the live homework is edited, **Then** the teacher still grades the original free-text (or Writing) answers; they are not asked to grade new questions that did not exist at submit, and original questions are not missing from the review.
3. **Given** the teacher is allowed to revise review on an already-reviewed submission, **When** they do so after the homework was edited, **Then** they still annotate and score the frozen answers (homework edits do not replace the review screen with the new homework).

---

### User Story 4 - Students who have not submitted yet get the updated homework (Priority: P2)

The live homework remains editable. Anyone who has not submitted — including students assigned later and students who had the homework open but not turned in — takes the current version. Freeze applies only after submit.

**Why this priority**: The teacher’s reason for editing is to fix the homework for students who have not turned it in yet. Without this, a freeze would trap everyone on a broken homework.

**Independent Test**: Two assigned students; only one has submitted. Teacher fixes a wrong answer key. The unsubmitted student takes the homework and is graded with the new key. The submitted student still has the original score and original questions.

**Acceptance Scenarios**:

1. **Given** a student is assigned the homework and has not submitted, **When** the teacher saves an edit, **Then** that student sees and submits against the updated homework (current questions, media, instructions, and answer key).
2. **Given** a student is assigned after the homework was edited, **When** they open it for the first time, **Then** they see the current homework, not any earlier version.
3. **Given** a student has the homework open and has not submitted, **When** the teacher saves an edit, **Then** on that student’s next reload, return to the homework, or submit attempt they are told the homework was updated, any in-progress answers are discarded, and they must start the current homework fresh.
4. **Given** some classmates have submitted and some have not, **When** the teacher edits, **Then** only the not-yet-submitted students are affected by the new content.

---

### Edge Cases

- **Not yet submitted**: Pending or in-progress work that has not been submitted is not frozen. Those students always take the current live homework, even if they had it open when the teacher saved. They are told the homework was updated (on their next reload, return to the homework, or submit attempt), in-progress answers are discarded, and they must start the current homework fresh. There is no retake for someone who already submitted.
- **Submit at the same time as an edit**: If the student’s submit finishes first, that work is frozen as submitted. If the teacher’s save finishes first, the student has not submitted — they get the update message and start the current homework fresh.
- **Awaiting teacher vs already graded**: Both count as submitted. Freeze applies as soon as the student has submitted, including work still waiting for teacher percentages.
- **Teacher review after freeze**: The teacher may still score, annotate, and comment on a frozen submission. That is review of the submission, not a homework edit. Homework edits must not overwrite that review.
- **Deleting questions or options**: Live homework loses them for students who have not submitted. Already-submitted work keeps the questions and options the student actually had, including selections tied to options the teacher later removed.
- **Changing question kind or replacing the whole exercise**: Already-submitted work stays on the original kind and content. Students who have not submitted yet get the new kind and content.
- **Writing (single long answer)**: Same freeze: submitted Writing keeps the instructions and the student’s answer as they were; later instruction edits apply only to students who have not submitted.
- **Title and due date**: Result and review of a submitted item show the content frozen at submit (including title and instructions as the student saw them). The teacher’s homework list and editor show the current live homework so they can find and keep editing it. Due date and who is assigned remain live operational settings and do not rewrite submitted answers or scores.
- **Deleting the homework entirely**: Out of scope — existing delete behaviour is unchanged. This feature covers **edits** to a homework that continues to exist.
- **Presentation activities**: Out of scope — editing an activity may still change how already-submitted activity work is shown; freeze applies only to homework.
- **Placement test**: Out of scope.
- **Submissions that already exist when this ships**: They must keep their stored answers and scores. Result/review must not start showing a later live edit as if it were what the student took. If an older original cannot be recovered, the freeze starts from that moment forward (no silent re-grade).
- **Editing with zero submissions**: Behaves as today; there is nothing to freeze.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The teacher MUST still be able to edit an existing homework after students have submitted, including questions, options, answer keys, instructions, media, title, type, and level.
- **FR-002**: Saving such an edit MUST NOT change any already-submitted submission’s student answers, overall score, per-question scores, per-item right/wrong, selected options, written answers, or existing teacher review.
- **FR-003**: Opening an already-submitted homework (student result or teacher result/review) MUST present the homework content as it was at that student’s submit time — questions, options, structure, media, and instructions included — not the current live homework when the two differ.
- **FR-004**: Students who have not submitted MUST see and be graded against the current live homework after an edit, including a student who had the homework open when the teacher saved.
- **FR-005**: Changing the answer key after submissions exist MUST NOT re-grade, re-average, or otherwise alter already-shown scores or item-level correctness on those submissions. The new key applies only to submissions made after the edit.
- **FR-006**: Removing or replacing questions or options on the live homework MUST NOT delete, blank, or unlink answers on already-submitted work.
- **FR-007**: Freeze MUST apply to every homework composition in use: all auto-graded, all manual, mixed, and Writing.
- **FR-008**: Freeze MUST apply at the moment of submit (including auto-graded-on-submit and submitted-awaiting-teacher). Later teacher review of that same submission MUST use the frozen content.
- **FR-009**: The teacher MUST still be able to complete or revise allowed review on a frozen homework submission (percentages, annotations, short note) without the live homework’s current questions replacing what is being reviewed.
- **FR-010**: Presentation activities MUST remain unchanged by this feature (no freeze vs live split). Editing an activity may still affect already-submitted activity work, as today.
- **FR-011**: The placement test MUST remain unchanged.
- **FR-012**: Assigning or unassigning students, changing due date, and publishing state MUST remain possible without rewriting submitted answers or scores.
- **FR-013**: A student MUST NOT be able to re-submit in order to pick up an edited homework; single-submission lock stays as today. Only students who have not yet submitted take the new version.
- **FR-014**: An unsubmitted take MUST NOT keep a frozen copy of an older homework after the teacher edits. In-progress answers MUST be discarded. The student MUST be told the homework was updated on their next reload, return to the homework, or submit attempt, and MUST start the current live homework fresh. The message is not required at the instant the teacher saves.

### Key Entities

- **Live homework**: The current homework the teacher is editing. This is what not-yet-submitted students take.
- **Submission**: One student’s submitted work on one homework (answers, status, scores, optional teacher review). Once submitted, it is frozen against a copy of the homework as it existed at submit time.
- **Frozen homework copy**: The questions, options, answer key, instructions, media, and title that belonged to that submission. Result and review for that submission use this copy, never a later live edit.
- **Teacher review**: Percentages, annotations, and notes attached to a homework submission. Independent of live homework edits; must survive them.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After the teacher edits a homework that already has submissions, 100% of those submissions — when reopened by the student or the teacher — still show the original questions, original student answers, original overall score, and original per-question / per-item right/wrong.
- **SC-002**: In a class where some students have submitted and some have not, 100% of not-yet-submitted students see the edited homework (including if they had it open — on next reload, return, or submit they are told it was updated, in-progress answers are discarded, and they start the current homework fresh), and 100% of submitted students still see their original work.
- **SC-003**: Deleting or changing a question or option after submit never causes an already-submitted answer to disappear or change, in 100% of such edits.
- **SC-004**: The teacher can save a typical correction (typo in a prompt or wrong correct-option) on a homework that already has submissions in under 2 minutes, without extra confirmation that discards or re-grades those submissions.
- **SC-005**: After this feature ships, editing a presentation activity behaves as it does today (no new freeze). 100% of homework freeze checks above apply only to homework.

## Assumptions

- Presentation activities are out of scope: freeze applies only to homework. Placement test is excluded.
- “Submitted” means the student has turned the work in (awaiting teacher or already graded/reviewed). Not submitted includes never opened and opened-but-not-submitted; both take the current live homework after an edit.
- The teacher’s authoring screen always shows the live homework (so they can keep editing). Freeze is on each homework submission’s result/review, not on the editor.
- Homework list labels can show the current live title so the teacher can find the homework; opening a specific student’s submitted work shows the frozen copy.
- Due date and assignee list stay live operational settings; they do not rewrite submitted content.
- Existing submissions from before this feature keep their stored answers and scores and are not re-graded. Result/review after ship must not apply later live edits to them.
- Students cannot retake after submit; this feature does not add a retake.
- Deleting a homework entirely remains existing behaviour and is out of scope.
- No new teacher-facing “version history” browser is required — only that each submission stays as it was.
